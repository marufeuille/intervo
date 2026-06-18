package dev.marufeuille.intervo.companion.pds

import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import java.time.Clock
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class PdsRecordRef(
    val uri: String,
    val cid: String,
)

/**
 * Companion の完了履歴から、PDS に置く共有向け record を作る。
 *
 * - WorkoutPlan: 再利用・公開されるメニュー定義。
 * - WorkoutCheckin: そのメニューを実行したという軽い記録。
 */
class WorkoutPdsRecordMapper(private val clock: Clock = Clock.systemUTC()) {

    fun planRkey(history: CompanionWorkoutHistory): String =
        history.workoutId.ifBlank { "workout-${history.id}" }

    fun checkinRkey(history: CompanionWorkoutHistory): String = history.id

    fun mapPlan(history: CompanionWorkoutHistory): JsonObject {
        val workoutSnapshot = history.workoutSnapshotJson.parseObjectOrNull()
        val exercises = parsePlanExercises(history.exerciseSnapshotsJson)
        val title = history.workoutName
            .ifBlank { workoutSnapshot?.string("workout_name").orEmpty() }
            .takeIf { it.isNotBlank() }

        return buildJsonObject {
            put("\$type", PLAN_COLLECTION)
            put("source", SOURCE)
            put("sourceRef", history.workoutId)
            title?.let { put("title", it) }
            put("exerciseType", workoutSnapshot?.string("exercise_type").toExerciseType())
            if (exercises.isNotEmpty()) put("exercises", JsonArray(exercises))
            estimatedDurationSeconds(exercises)?.let { put("estimatedDurationSeconds", it) }
            put("createdAt", Instant.now(clock).toString())
        }
    }

    fun mapCheckin(history: CompanionWorkoutHistory, planRef: PdsRecordRef): JsonObject {
        val completedAt = Instant.ofEpochMilli(history.completedAt)
        val startedAt = completedAt.minusSeconds(history.totalSeconds.toLong().coerceAtLeast(0L))
        val workoutSnapshot = history.workoutSnapshotJson.parseObjectOrNull()
        val title = history.workoutName
            .ifBlank { workoutSnapshot?.string("workout_name").orEmpty() }
            .takeIf { it.isNotBlank() }
        val summary = performedSummary(history.performedSetsJson)

        return buildJsonObject {
            put("\$type", CHECKIN_COLLECTION)
            put("source", SOURCE)
            put("sourceRef", history.id)
            put("plan", planRef.toJson())
            put("planSourceRef", history.workoutId)
            title?.let { put("title", it) }
            put("status", "completed")
            put("startedAt", startedAt.toString())
            put("completedAt", completedAt.toString())
            put("durationSeconds", history.totalSeconds.coerceAtLeast(0))
            put("exerciseCount", history.exerciseCount.coerceAtLeast(0))
            summary?.let { put("performed", it) }
            put("createdAt", Instant.now(clock).toString())
        }
    }

    private fun parsePlanExercises(snapshotsJson: String): List<JsonObject> {
        val array = snapshotsJson.parseArrayOrNull() ?: return emptyList()
        return array.mapIndexedNotNull { index, element ->
            val snapshot = element as? JsonObject ?: return@mapIndexedNotNull null
            val mode = snapshot.string("mode").toMode()
            val planned = snapshot.toPlanned(mode)
            val name = snapshot.string("exercise_name")
                ?.takeIf { it.isNotBlank() }
                ?: "種目${index + 1}"

            buildJsonObject {
                put("name", name)
                snapshot.string("exercise_type")?.toExerciseType()?.let { put("exerciseType", it) }
                mode?.let { put("mode", it) }
                snapshot.int("sort_order")?.takeIf { it >= 0 }?.let { put("order", it) }
                if (planned.isNotEmpty()) put("planned", planned)
            }
        }
    }

    private fun JsonObject.toPlanned(mode: String?): JsonObject = buildJsonObject {
        int("sets")?.takeIf { it >= 1 }?.let { put("sets", it) }
        when (mode) {
            "reps" -> int("reps_per_set")?.dropSentinel()?.takeIf { it >= 1 }?.let { put("reps", it) }
            "time" -> int("duration_seconds")?.dropSentinel()?.takeIf { it >= 1 }?.let {
                put("durationSeconds", it)
            }
        }
        int("rest_seconds")?.takeIf { it >= 0 }?.let { put("restSeconds", it) }
        int("rep_rest_seconds")?.takeIf { it > 0 }?.let { put("repRestSeconds", it) }
    }

    private fun estimatedDurationSeconds(exercises: List<JsonObject>): Int? {
        var total = 0
        exercises.forEach { exercise ->
            val planned = exercise["planned"] as? JsonObject ?: return null
            val sets = planned.int("sets") ?: return null
            val duration = planned.int("durationSeconds") ?: return null
            val rest = planned.int("restSeconds") ?: 0
            total += (duration * sets) + (rest * (sets - 1).coerceAtLeast(0))
        }
        return total.takeIf { it > 0 }
    }

    private fun performedSummary(performedSetsJson: String): JsonObject? {
        val array = performedSetsJson.parseArrayOrNull() ?: return null
        if (array.isEmpty()) return null
        var completed = 0
        var total = 0
        array.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            total += 1
            if (obj.boolean("completed") != false) completed += 1
        }
        return buildJsonObject {
            put("setCount", total)
            put("completedSetCount", completed)
        }
    }

    private fun PdsRecordRef.toJson(): JsonObject = buildJsonObject {
        put("uri", uri)
        put("cid", cid)
    }

    private fun String?.toMode(): String? = when (this) {
        "TIMED" -> "time"
        "REPS" -> "reps"
        "time", "reps" -> this
        else -> null
    }

    private fun String?.toExerciseType(): String =
        when (this) {
            "STRENGTH_TRAINING", "strength_training" -> "strength_training"
            "HIGH_INTENSITY_INTERVAL_TRAINING", "high_intensity_interval_training" ->
                "high_intensity_interval_training"
            "STRETCHING", "stretching" -> "stretching"
            "CALISTHENICS", "calisthenics" -> "calisthenics"
            "YOGA", "yoga" -> "yoga"
            "RUNNING", "running" -> "running"
            "WALKING", "walking" -> "walking"
            "OTHER_WORKOUT", "other_workout" -> "other_workout"
            else -> "other_workout"
        }

    private fun String.parseObjectOrNull(): JsonObject? =
        runCatching { Json.parseToJsonElement(this).jsonObject }.getOrNull()

    private fun String.parseArrayOrNull(): JsonArray? =
        runCatching { Json.parseToJsonElement(this).jsonArray }.getOrNull()

    private fun JsonObject.string(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

    private fun Int.dropSentinel(): Int? = takeIf { it >= 0 }

    companion object {
        const val PLAN_COLLECTION = "dev.marufeuille.workout.plan"
        const val CHECKIN_COLLECTION = "dev.marufeuille.workout.checkin"
        private const val SOURCE = "intervo"
    }
}
