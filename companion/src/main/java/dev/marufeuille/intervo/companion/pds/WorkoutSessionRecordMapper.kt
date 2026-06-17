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

/**
 * `my-pds/lexicons/dev.marufeuille.workout.session.json` に合わせて
 * Companion の履歴を ATProto record JSON へ変換する。
 */
class WorkoutSessionRecordMapper(private val clock: Clock = Clock.systemUTC()) {

    fun map(history: CompanionWorkoutHistory): JsonObject {
        val workoutSnapshot = history.workoutSnapshotJson.parseObjectOrNull()
        val exercises = parseExercises(
            snapshotsJson = history.exerciseSnapshotsJson,
            hrJson = history.exerciseHrJson,
            performedSetsJson = history.performedSetsJson,
        )
        val completedAt = Instant.ofEpochMilli(history.completedAt)
        val startedAt = completedAt.minusSeconds(history.totalSeconds.toLong().coerceAtLeast(0L))
        val title = history.workoutName
            .ifBlank { workoutSnapshot?.string("workout_name").orEmpty() }
            .takeIf { it.isNotBlank() }

        return buildJsonObject {
            put("\$type", COLLECTION)
            put("source", SOURCE)
            put("sourceRef", history.id)
            put("exerciseType", workoutSnapshot?.string("exercise_type").toExerciseType())
            title?.let { put("title", it) }
            put("startedAt", startedAt.toString())
            put("completedAt", completedAt.toString())
            put("durationSeconds", history.totalSeconds.coerceAtLeast(0))
            history.heartRateSummary()?.let { put("heartRate", it) }
            if (exercises.isNotEmpty()) put("exercises", JsonArray(exercises))
            put("createdAt", Instant.now(clock).toString())
        }
    }

    private fun parseExercises(
        snapshotsJson: String,
        hrJson: String,
        performedSetsJson: String,
    ): List<JsonObject> {
        val hrByIndex = parseExerciseHr(hrJson)
        val performedSetsByIndex = parsePerformedSets(performedSetsJson)
        val array = snapshotsJson.parseArrayOrNull() ?: return emptyList()
        return array.mapIndexedNotNull { index, element ->
            val snapshot = element as? JsonObject ?: return@mapIndexedNotNull null
            val mode = snapshot.string("mode").toMode()
            val planned = snapshot.toPlanned(mode)
            val performed = performedFor(
                sets = performedSetsByIndex[index].orEmpty(),
                heartRate = hrByIndex[index],
            )
            val name = snapshot.string("exercise_name")
                ?.takeIf { it.isNotBlank() }
                ?: "種目${index + 1}"

            buildJsonObject {
                put("name", name)
                snapshot.string("exercise_type")?.toExerciseType()?.let { put("exerciseType", it) }
                mode?.let { put("mode", it) }
                snapshot.int("sort_order")?.takeIf { it >= 0 }?.let { put("order", it) }
                if (planned.isNotEmpty()) put("planned", planned)
                performed?.let { put("performed", it) }
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

    private fun CompanionWorkoutHistory.heartRateSummary(): JsonObject? {
        val heartRate = buildJsonObject {
            startHr?.takeIf { it >= 0 }?.let { put("start", it) }
            avgHr?.takeIf { it >= 0 }?.let { put("avg", it) }
            maxHr?.takeIf { it >= 0 }?.let { put("max", it) }
        }
        return heartRate.takeIf { it.isNotEmpty() }
    }

    private fun performedFor(
        sets: List<JsonObject>,
        heartRate: ExerciseHeartRate?,
    ): JsonObject? {
        val heartRateJson = heartRate?.toJson()
        if (sets.isEmpty() && heartRateJson == null) return null
        return buildJsonObject {
            if (sets.isNotEmpty()) put("sets", JsonArray(sets))
            heartRateJson?.let { put("heartRate", it) }
        }
    }

    private fun ExerciseHeartRate.toJson(): JsonObject? {
        val heartRate = buildJsonObject {
            start?.takeIf { it >= 0 }?.let { put("start", it) }
            end?.takeIf { it >= 0 }?.let { put("end", it) }
        }
        return heartRate.takeIf { it.isNotEmpty() }
    }

    private fun parseExerciseHr(hrJson: String): Map<Int, ExerciseHeartRate> {
        val array = hrJson.parseArrayOrNull() ?: return emptyMap()
        return array.mapIndexedNotNull { fallbackIndex, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val index = obj.int("exercise_index") ?: fallbackIndex
            index to ExerciseHeartRate(start = obj.int("start_hr"), end = obj.int("end_hr"))
        }.toMap()
    }

    private fun parsePerformedSets(performedSetsJson: String): Map<Int, List<JsonObject>> {
        val array = performedSetsJson.parseArrayOrNull() ?: return emptyMap()
        return array.mapIndexedNotNull { fallbackIndex, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val exerciseIndex = obj.int("exercise_index") ?: return@mapIndexedNotNull null
            exerciseIndex to buildJsonObject {
                put("index", obj.int("set_index") ?: fallbackIndex)
                obj.int("reps")?.takeIf { it >= 0 }?.let { put("reps", it) }
                obj.int("duration_seconds")?.takeIf { it >= 0 }?.let { put("durationSeconds", it) }
                put("completed", obj.boolean("completed") ?: true)
            }
        }.groupBy({ it.first }, { it.second })
    }

    private data class ExerciseHeartRate(val start: Int?, val end: Int?)

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
        const val COLLECTION = "dev.marufeuille.workout.session"
        private const val SOURCE = "intervo"
    }
}
