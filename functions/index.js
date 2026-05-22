const {BigQuery} = require("@google-cloud/bigquery");
const {initializeApp} = require("firebase-admin/app");
const {getAppCheck} = require("firebase-admin/app-check");
const {getAuth} = require("firebase-admin/auth");
const {onRequest} = require("firebase-functions/v2/https");
const {setGlobalOptions} = require("firebase-functions/v2");

initializeApp();

setGlobalOptions({
  region: process.env.FUNCTION_REGION || "asia-northeast1",
  maxInstances: 3,
});

const bigquery = new BigQuery();
const PROJECT_ID = process.env.GCLOUD_PROJECT ||
  process.env.GCP_PROJECT ||
  process.env.GOOGLE_CLOUD_PROJECT ||
  "intervo-app";

const DATASET_DEV = process.env.BIGQUERY_DATASET_DEV || "intervo_dev";
const DATASET_PROD = process.env.BIGQUERY_DATASET_PROD || "intervo_prod";
const BIGQUERY_LOCATION = process.env.BIGQUERY_LOCATION || "asia-northeast1";
const TABLE_WORKOUT_HISTORY = "workout_history";
const TABLE_WORKOUT_SNAPSHOT = "workout_snapshot";
const TABLE_EXERCISE_SNAPSHOT = "exercise_snapshot";

exports.ingestWorkoutHistory = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  let auth;
  try {
    auth = await verifyRequest(req);
  } catch (error) {
    console.warn("auth rejected", error.message);
    res.status(401).send("Unauthorized");
    return;
  }

  if (!isAllowedUid(auth.uid)) {
    console.warn("uid rejected", auth.uid);
    res.status(403).send("Forbidden");
    return;
  }

  const payload = parsePayload(req.body);
  if (!payload.ok) {
    res.status(400).send(payload.message);
    return;
  }

  const datasetId = isDebugRecord(payload.history) ? DATASET_DEV : DATASET_PROD;
  const insertedAt = new Date().toISOString();

  try {
    await ensureTable(datasetId, TABLE_WORKOUT_HISTORY, historySchema());
    await ensureTable(datasetId, TABLE_WORKOUT_SNAPSHOT, workoutSnapshotSchema());
    await ensureTable(datasetId, TABLE_EXERCISE_SNAPSHOT, exerciseSnapshotSchema());

    await upsertHistory(datasetId, {
      ...payload.history,
      firebase_uid: auth.uid,
      inserted_at: insertedAt,
    });

    await upsertWorkoutSnapshot(datasetId, {
      ...payload.workoutSnapshot,
      event_id: payload.history.event_id,
      firebase_uid: auth.uid,
      source: payload.history.source,
      app_build_type: payload.history.app_build_type,
      app_application_id: payload.history.app_application_id,
      completed_at: payload.history.completed_at,
      inserted_at: insertedAt,
    });

    for (const exerciseSnapshot of payload.exerciseSnapshots) {
      await upsertExerciseSnapshot(datasetId, {
        ...exerciseSnapshot,
        event_id: payload.history.event_id,
        firebase_uid: auth.uid,
        source: payload.history.source,
        app_build_type: payload.history.app_build_type,
        app_application_id: payload.history.app_application_id,
        completed_at: payload.history.completed_at,
        inserted_at: insertedAt,
      });
    }
    res.status(204).send();
  } catch (error) {
    console.error("bigquery write failed", error);
    res.status(500).send("BigQuery write failed");
  }
});

async function verifyRequest(req) {
  const authHeader = req.header("Authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ?
    authHeader.slice("Bearer ".length) :
    "";
  const appCheckToken = req.header("X-Firebase-AppCheck") || "";

  if (!idToken || !appCheckToken) {
    throw new Error("missing auth headers");
  }

  const [auth] = await Promise.all([
    getAuth().verifyIdToken(idToken),
    getAppCheck().verifyToken(appCheckToken),
  ]);
  return auth;
}

function isAllowedUid(uid) {
  return allowedUids().has(uid);
}

function allowedUids() {
  return new Set(
    (process.env.ALLOWED_FIREBASE_UIDS || "")
      .split(",")
      .map((uid) => uid.trim())
      .filter(Boolean),
  );
}

function parsePayload(body) {
  const required = {
    event_id: "string",
    source: "string",
    app_build_type: "string",
    app_application_id: "string",
    workout_id: "string",
    workout_name: "string",
    completed_at_millis: "number",
    total_seconds: "number",
    exercise_count: "number",
  };

  for (const [key, type] of Object.entries(required)) {
    if (typeof body?.[key] !== type) {
      return {ok: false, message: `Invalid field: ${key}`};
    }
  }

  if (!Number.isFinite(body.completed_at_millis) || body.completed_at_millis <= 0) {
    return {ok: false, message: "Invalid completed_at_millis"};
  }
  if (!Number.isInteger(body.total_seconds) || body.total_seconds < 0) {
    return {ok: false, message: "Invalid total_seconds"};
  }
  if (!Number.isInteger(body.exercise_count) || body.exercise_count < 0) {
    return {ok: false, message: "Invalid exercise_count"};
  }

  const history = {
    event_id: body.event_id,
    source: body.source,
    app_build_type: body.app_build_type,
    app_application_id: body.app_application_id,
    workout_id: body.workout_id,
    workout_name: body.workout_name,
    completed_at: new Date(body.completed_at_millis).toISOString(),
    completed_at_millis: body.completed_at_millis,
    total_seconds: body.total_seconds,
    exercise_count: body.exercise_count,
  };

  const workoutSnapshot = parseWorkoutSnapshot(body.workout_snapshot, history);
  if (!workoutSnapshot.ok) {
    return workoutSnapshot;
  }

  const exerciseSnapshots = parseExerciseSnapshots(body.exercise_snapshots, history);
  if (!exerciseSnapshots.ok) {
    return exerciseSnapshots;
  }

  return {
    ok: true,
    history,
    workoutSnapshot: workoutSnapshot.value,
    exerciseSnapshots: exerciseSnapshots.value,
  };
}

function parseWorkoutSnapshot(rawSnapshot, history) {
  const snapshot = rawSnapshot || {};
  if (typeof snapshot !== "object" || Array.isArray(snapshot)) {
    return {ok: false, message: "Invalid workout_snapshot"};
  }

  const workoutId = snapshot.workout_id || history.workout_id;
  const workoutName = snapshot.workout_name || history.workout_name;
  const sortOrder = snapshot.sort_order;

  if (typeof workoutId !== "string" || typeof workoutName !== "string") {
    return {ok: false, message: "Invalid workout_snapshot fields"};
  }
  if (sortOrder !== undefined && sortOrder !== null && !Number.isInteger(sortOrder)) {
    return {ok: false, message: "Invalid workout_snapshot.sort_order"};
  }

  return {
    ok: true,
    value: {
      workout_id: workoutId,
      workout_name: workoutName,
      workout_sort_order: sortOrder ?? null,
    },
  };
}

function parseExerciseSnapshots(rawSnapshots, history) {
  if (rawSnapshots === undefined || rawSnapshots === null) {
    return {ok: true, value: []};
  }
  if (!Array.isArray(rawSnapshots)) {
    return {ok: false, message: "Invalid exercise_snapshots"};
  }

  const snapshots = [];
  for (const [index, snapshot] of rawSnapshots.entries()) {
    if (typeof snapshot !== "object" || Array.isArray(snapshot)) {
      return {ok: false, message: `Invalid exercise_snapshots[${index}]`};
    }
    const parsed = parseExerciseSnapshot(snapshot, history, index);
    if (!parsed.ok) return parsed;
    snapshots.push(parsed.value);
  }
  return {ok: true, value: snapshots};
}

function parseExerciseSnapshot(snapshot, history, index) {
  const required = {
    exercise_id: "string",
    workout_id: "string",
    exercise_name: "string",
    mode: "string",
    duration_seconds: "number",
    sets: "number",
    rest_seconds: "number",
    reps_per_set: "number",
    rep_rest_seconds: "number",
    sort_order: "number",
  };

  for (const [key, type] of Object.entries(required)) {
    if (typeof snapshot[key] !== type) {
      return {ok: false, message: `Invalid exercise_snapshots[${index}].${key}`};
    }
  }

  const integerFields = [
    "duration_seconds",
    "sets",
    "rest_seconds",
    "reps_per_set",
    "rep_rest_seconds",
    "sort_order",
  ];
  for (const field of integerFields) {
    if (!Number.isInteger(snapshot[field]) || snapshot[field] < 0) {
      return {ok: false, message: `Invalid exercise_snapshots[${index}].${field}`};
    }
  }

  return {
    ok: true,
    value: {
      exercise_id: snapshot.exercise_id,
      workout_id: snapshot.workout_id || history.workout_id,
      exercise_name: snapshot.exercise_name,
      mode: snapshot.mode,
      duration_seconds: snapshot.duration_seconds,
      sets: snapshot.sets,
      rest_seconds: snapshot.rest_seconds,
      reps_per_set: snapshot.reps_per_set,
      rep_rest_seconds: snapshot.rep_rest_seconds,
      sort_order: snapshot.sort_order,
    },
  };
}

function isDebugRecord(record) {
  return record.app_build_type === "debug" ||
    record.app_application_id.endsWith(".debug");
}

async function ensureTable(datasetId, tableId, schema) {
  const dataset = bigquery.dataset(datasetId);
  const [datasetExists] = await dataset.exists();
  if (!datasetExists) {
    await bigquery.createDataset(datasetId, {
      location: BIGQUERY_LOCATION,
    });
  }

  const table = dataset.table(tableId);
  const [tableExists] = await table.exists();
  if (tableExists) return;

  await dataset.createTable(tableId, {
    schema,
  });
}

async function upsertHistory(datasetId, record) {
  const tableRef = tableRefFor(datasetId, TABLE_WORKOUT_HISTORY);
  const query = `
    MERGE \`${tableRef}\` T
    USING (
      SELECT
        @event_id AS event_id,
        @firebase_uid AS firebase_uid,
        @source AS source,
        @app_build_type AS app_build_type,
        @app_application_id AS app_application_id,
        @workout_id AS workout_id,
        @workout_name AS workout_name,
        TIMESTAMP(@completed_at) AS completed_at,
        @completed_at_millis AS completed_at_millis,
        @total_seconds AS total_seconds,
        @exercise_count AS exercise_count,
        TIMESTAMP(@inserted_at) AS inserted_at
    ) S
    ON T.event_id = S.event_id
    WHEN NOT MATCHED THEN INSERT (
      event_id,
      firebase_uid,
      source,
      app_build_type,
      app_application_id,
      workout_id,
      workout_name,
      completed_at,
      completed_at_millis,
      total_seconds,
      exercise_count,
      inserted_at
    ) VALUES (
      S.event_id,
      S.firebase_uid,
      S.source,
      S.app_build_type,
      S.app_application_id,
      S.workout_id,
      S.workout_name,
      S.completed_at,
      S.completed_at_millis,
      S.total_seconds,
      S.exercise_count,
      S.inserted_at
    )
  `;

  await runQuery(query, record);
}

async function upsertWorkoutSnapshot(datasetId, record) {
  const tableRef = tableRefFor(datasetId, TABLE_WORKOUT_SNAPSHOT);
  const query = `
    MERGE \`${tableRef}\` T
    USING (
      SELECT
        @event_id AS event_id,
        @firebase_uid AS firebase_uid,
        @source AS source,
        @app_build_type AS app_build_type,
        @app_application_id AS app_application_id,
        @workout_id AS workout_id,
        @workout_name AS workout_name,
        @workout_sort_order AS workout_sort_order,
        TIMESTAMP(@completed_at) AS completed_at,
        TIMESTAMP(@inserted_at) AS inserted_at
    ) S
    ON T.event_id = S.event_id
    WHEN MATCHED THEN UPDATE SET
      firebase_uid = S.firebase_uid,
      source = S.source,
      app_build_type = S.app_build_type,
      app_application_id = S.app_application_id,
      workout_id = S.workout_id,
      workout_name = S.workout_name,
      workout_sort_order = S.workout_sort_order,
      completed_at = S.completed_at,
      inserted_at = S.inserted_at
    WHEN NOT MATCHED THEN INSERT (
      event_id,
      firebase_uid,
      source,
      app_build_type,
      app_application_id,
      workout_id,
      workout_name,
      workout_sort_order,
      completed_at,
      inserted_at
    ) VALUES (
      S.event_id,
      S.firebase_uid,
      S.source,
      S.app_build_type,
      S.app_application_id,
      S.workout_id,
      S.workout_name,
      S.workout_sort_order,
      S.completed_at,
      S.inserted_at
    )
  `;

  await runQuery(query, record);
}

async function upsertExerciseSnapshot(datasetId, record) {
  const tableRef = tableRefFor(datasetId, TABLE_EXERCISE_SNAPSHOT);
  const query = `
    MERGE \`${tableRef}\` T
    USING (
      SELECT
        @event_id AS event_id,
        @exercise_id AS exercise_id,
        @firebase_uid AS firebase_uid,
        @source AS source,
        @app_build_type AS app_build_type,
        @app_application_id AS app_application_id,
        @workout_id AS workout_id,
        @exercise_name AS exercise_name,
        @mode AS mode,
        @duration_seconds AS duration_seconds,
        @sets AS sets,
        @rest_seconds AS rest_seconds,
        @reps_per_set AS reps_per_set,
        @rep_rest_seconds AS rep_rest_seconds,
        @sort_order AS sort_order,
        TIMESTAMP(@completed_at) AS completed_at,
        TIMESTAMP(@inserted_at) AS inserted_at
    ) S
    ON T.event_id = S.event_id AND T.exercise_id = S.exercise_id
    WHEN MATCHED THEN UPDATE SET
      firebase_uid = S.firebase_uid,
      source = S.source,
      app_build_type = S.app_build_type,
      app_application_id = S.app_application_id,
      workout_id = S.workout_id,
      exercise_name = S.exercise_name,
      mode = S.mode,
      duration_seconds = S.duration_seconds,
      sets = S.sets,
      rest_seconds = S.rest_seconds,
      reps_per_set = S.reps_per_set,
      rep_rest_seconds = S.rep_rest_seconds,
      sort_order = S.sort_order,
      completed_at = S.completed_at,
      inserted_at = S.inserted_at
    WHEN NOT MATCHED THEN INSERT (
      event_id,
      exercise_id,
      firebase_uid,
      source,
      app_build_type,
      app_application_id,
      workout_id,
      exercise_name,
      mode,
      duration_seconds,
      sets,
      rest_seconds,
      reps_per_set,
      rep_rest_seconds,
      sort_order,
      completed_at,
      inserted_at
    ) VALUES (
      S.event_id,
      S.exercise_id,
      S.firebase_uid,
      S.source,
      S.app_build_type,
      S.app_application_id,
      S.workout_id,
      S.exercise_name,
      S.mode,
      S.duration_seconds,
      S.sets,
      S.rest_seconds,
      S.reps_per_set,
      S.rep_rest_seconds,
      S.sort_order,
      S.completed_at,
      S.inserted_at
    )
  `;

  await runQuery(query, record);
}

function tableRefFor(datasetId, tableId) {
  const tableRef = [
    PROJECT_ID,
    datasetId,
    tableId,
  ].map(sanitizeIdentifier).join(".");
  return tableRef;
}

async function runQuery(query, record) {
  await bigquery.query({
    query,
    params: record,
    location: BIGQUERY_LOCATION,
  });
}

function historySchema() {
  return [
    {name: "event_id", type: "STRING", mode: "REQUIRED"},
    {name: "firebase_uid", type: "STRING", mode: "REQUIRED"},
    {name: "source", type: "STRING", mode: "REQUIRED"},
    {name: "app_build_type", type: "STRING", mode: "REQUIRED"},
    {name: "app_application_id", type: "STRING", mode: "REQUIRED"},
    {name: "workout_id", type: "STRING", mode: "REQUIRED"},
    {name: "workout_name", type: "STRING", mode: "REQUIRED"},
    {name: "completed_at", type: "TIMESTAMP", mode: "REQUIRED"},
    {name: "completed_at_millis", type: "INT64", mode: "REQUIRED"},
    {name: "total_seconds", type: "INT64", mode: "REQUIRED"},
    {name: "exercise_count", type: "INT64", mode: "REQUIRED"},
    {name: "inserted_at", type: "TIMESTAMP", mode: "REQUIRED"},
  ];
}

function workoutSnapshotSchema() {
  return [
    {name: "event_id", type: "STRING", mode: "REQUIRED"},
    {name: "firebase_uid", type: "STRING", mode: "REQUIRED"},
    {name: "source", type: "STRING", mode: "REQUIRED"},
    {name: "app_build_type", type: "STRING", mode: "REQUIRED"},
    {name: "app_application_id", type: "STRING", mode: "REQUIRED"},
    {name: "workout_id", type: "STRING", mode: "REQUIRED"},
    {name: "workout_name", type: "STRING", mode: "REQUIRED"},
    {name: "workout_sort_order", type: "INT64", mode: "NULLABLE"},
    {name: "completed_at", type: "TIMESTAMP", mode: "REQUIRED"},
    {name: "inserted_at", type: "TIMESTAMP", mode: "REQUIRED"},
  ];
}

function exerciseSnapshotSchema() {
  return [
    {name: "event_id", type: "STRING", mode: "REQUIRED"},
    {name: "exercise_id", type: "STRING", mode: "REQUIRED"},
    {name: "firebase_uid", type: "STRING", mode: "REQUIRED"},
    {name: "source", type: "STRING", mode: "REQUIRED"},
    {name: "app_build_type", type: "STRING", mode: "REQUIRED"},
    {name: "app_application_id", type: "STRING", mode: "REQUIRED"},
    {name: "workout_id", type: "STRING", mode: "REQUIRED"},
    {name: "exercise_name", type: "STRING", mode: "REQUIRED"},
    {name: "mode", type: "STRING", mode: "REQUIRED"},
    {name: "duration_seconds", type: "INT64", mode: "REQUIRED"},
    {name: "sets", type: "INT64", mode: "REQUIRED"},
    {name: "rest_seconds", type: "INT64", mode: "REQUIRED"},
    {name: "reps_per_set", type: "INT64", mode: "REQUIRED"},
    {name: "rep_rest_seconds", type: "INT64", mode: "REQUIRED"},
    {name: "sort_order", type: "INT64", mode: "REQUIRED"},
    {name: "completed_at", type: "TIMESTAMP", mode: "REQUIRED"},
    {name: "inserted_at", type: "TIMESTAMP", mode: "REQUIRED"},
  ];
}

function sanitizeIdentifier(value) {
  if (!/^[A-Za-z0-9_-]+$/.test(value)) {
    throw new Error(`Invalid BigQuery identifier: ${value}`);
  }
  return value;
}
