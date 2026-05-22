# Companion app と BigQuery 同期

Intervo Companion は Wear OS アプリから完了済みワークアウト履歴を Wear OS Data Layer 経由で受け取り、スマホ内に保存してから、未送信レコードを設定済みの HTTPS 取り込み API に送信します。

スマホアプリから BigQuery へ直接書き込む構成にはしていません。BigQuery のサービスアカウント認証情報は Cloud Run、Cloud Functions、自前 API などのサーバー側に置いてください。

## Data flow

1. ウォッチアプリがワークアウト完了時に `WorkoutHistory` を保存します。
2. ウォッチアプリが同じ履歴を `/workout_history/{event_id}` の Data Layer アイテムとして送信します。
3. Companion app が Data Layer アイテムを受信し、スマホ側 Room DB に保存します。
4. 取り込み URL が設定済みなら、Companion app が未送信レコードをその URL に `POST` します。
5. 取り込み API がリクエストを検証し、BigQuery に insert します。

## Ingest request

Companion app は `Content-Type: application/json` で `POST` します。

```json
{
  "event_id": "uuid",
  "source": "intervo_wear",
  "app_build_type": "debug",
  "app_application_id": "dev.marufeuille.intervo.debug",
  "workout_id": "uuid",
  "workout_name": "上半身",
  "completed_at_millis": 1763712000000,
  "total_seconds": 1200,
  "exercise_count": 5,
  "workout_snapshot": {
    "workout_id": "uuid",
    "workout_name": "上半身",
    "sort_order": 0
  },
  "exercise_snapshots": [
    {
      "exercise_id": "uuid",
      "workout_id": "uuid",
      "exercise_name": "腕立て伏せ",
      "mode": "REPS",
      "duration_seconds": 30,
      "sets": 3,
      "rest_seconds": 30,
      "reps_per_set": 10,
      "rep_rest_seconds": 0,
      "sort_order": 0
    }
  ]
}
```

`2xx` レスポンスを成功として扱います。`2xx` 以外は未送信のまま残り、Companion app から再送できます。

## BigQuery table

推奨テーブル定義:

```sql
CREATE TABLE `PROJECT.DATASET.workout_history` (
  event_id STRING NOT NULL,
  firebase_uid STRING NOT NULL,
  source STRING NOT NULL,
  app_build_type STRING NOT NULL,
  app_application_id STRING NOT NULL,
  workout_id STRING NOT NULL,
  workout_name STRING NOT NULL,
  completed_at TIMESTAMP NOT NULL,
  completed_at_millis INT64 NOT NULL,
  total_seconds INT64 NOT NULL,
  exercise_count INT64 NOT NULL,
  inserted_at TIMESTAMP NOT NULL
);
```

取り込み API 側では `event_id` を冪等キーとして扱い、スマホから同じイベントが再送されても重複行を作らないようにしてください。

このリポジトリの Functions 実装では、debug build は `intervo_dev`、release build は `intervo_prod` に書き込みます。テーブルは以下です。

- `workout_history`: 完了履歴
- `workout_snapshot`: 完了時点のワークアウト定義
- `exercise_snapshot`: 完了時点の種目定義

`workout_snapshot` と `exercise_snapshot` は `event_id` を持つため、同じ `workout_id` / `exercise_id` が後から編集されても、過去の実施内容を再現できます。

## 現在のアプリ挙動

- Companion app の debug package はウォッチ側と同じ `dev.marufeuille.intervo.debug` です。
- 取り込み URL は Companion app 内で設定します。debug build の初期値は `https://asia-northeast1-intervo-app.cloudfunctions.net/ingestWorkoutHistory` です。
- URL 設定済みの場合、新規受信した履歴は自動同期します。
- Companion app から手動再送できます。
- 既存のウォッチ履歴は自動バックフィルしません。このバージョン以降に完了した履歴が送信対象です。
