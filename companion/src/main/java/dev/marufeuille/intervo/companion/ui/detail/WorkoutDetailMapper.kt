package dev.marufeuille.intervo.companion.ui.detail

import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wear から受け取ったスナップショット JSON（snake_case）を履歴詳細の表示モデルへ変換する。
 * キー名の真実は app 側 `sync/WorkoutHistorySyncClient.kt`。
 * duration/reps の `-1` は無制限/AMRAP のセンチネルなので null（＝制限なし表記）に落とす。
 * rest/repRest の `-1` は無制限休憩のセンチネル。UI で「無制限」と表示するためそのまま保持する。
 */
object WorkoutDetailMapper {

    fun map(history: CompanionWorkoutHistory): WorkoutDetailUiModel {
        val exercises = parseExercises(history.exerciseSnapshotsJson, history.exerciseHrJson)
        return WorkoutDetailUiModel(
            title = history.workoutName.ifBlank { "ワークアウト" },
            completedAt = history.completedAt,
            totalSeconds = history.totalSeconds,
            exerciseCount = history.exerciseCount,
            totalPlannedSets = exercises.sumOf { it.sets ?: 0 },
            startHr = history.startHr,
            avgHr = history.avgHr,
            maxHr = history.maxHr,
            healthConnectWritten = history.healthConnectWrittenAt != null,
            exercises = exercises,
        )
    }

    private fun parseExercises(snapshotsJson: String, hrJson: String): List<ExerciseDetail> = runCatching {
        val hrByIndex = parseExerciseHr(hrJson)
        val array = JSONArray(snapshotsJson)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val hr = hrByIndex[i]
            ExerciseDetail(
                name = obj.optString("exercise_name").ifBlank { "種目${i + 1}" },
                mode = when (obj.optString("mode")) {
                    "TIMED" -> ExerciseModeUi.TIMED
                    "REPS" -> ExerciseModeUi.REPS
                    else -> null
                },
                sets = obj.intOrNull("sets"),
                reps = obj.intOrNull("reps_per_set").dropSentinel(),
                durationSeconds = obj.intOrNull("duration_seconds").dropSentinel(),
                restSeconds = obj.intOrNull("rest_seconds"),
                repRestSeconds = obj.intOrNull("rep_rest_seconds"),
                startHr = hr?.first,
                endHr = hr?.second,
            )
        }
    }.getOrDefault(emptyList())

    /** exercise_index → (start_hr, end_hr)。 */
    private fun parseExerciseHr(hrJson: String): Map<Int, Pair<Int?, Int?>> = runCatching {
        val array = JSONArray(hrJson)
        (0 until array.length()).associate { i ->
            val obj = array.getJSONObject(i)
            obj.optInt("exercise_index", i) to (obj.intOrNull("start_hr") to obj.intOrNull("end_hr"))
        }
    }.getOrDefault(emptyMap())

    private fun JSONObject.intOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    /** -1（無制限/AMRAP）は「指定なし」として null に落とす。 */
    private fun Int?.dropSentinel(): Int? = this?.takeIf { it >= 0 }
}
