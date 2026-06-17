package dev.marufeuille.intervo.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseHrInput
import dev.marufeuille.intervo.data.PerformedSetRecordInput
import dev.marufeuille.intervo.data.WorkoutHistory
import dev.marufeuille.intervo.data.effectiveRepsPerSet
import dev.marufeuille.intervo.timer.HrSample
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

class WorkoutHistorySyncClient(context: Context) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    suspend fun send(
        history: WorkoutHistory,
        workoutSortOrder: Int?,
        workoutExerciseType: String,
        exercises: List<Exercise>,
        exerciseHrRecords: List<ExerciseHrInput> = emptyList(),
        hrSamples: List<HrSample> = emptyList(),
        performedSetRecords: List<PerformedSetRecordInput> = emptyList(),
    ) {
        val request = PutDataMapRequest.create("$PATH_PREFIX/${history.id}").apply {
            dataMap.putString(KEY_ID, history.id)
            dataMap.putString(KEY_WORKOUT_ID, history.workoutId)
            dataMap.putString(KEY_WORKOUT_NAME, history.workoutName)
            dataMap.putLong(KEY_COMPLETED_AT, history.completedAt)
            dataMap.putInt(KEY_TOTAL_SECONDS, history.totalSeconds)
            dataMap.putInt(KEY_EXERCISE_COUNT, history.exerciseCount)
            history.startHr?.let { dataMap.putInt(KEY_START_HR, it) }
            history.avgHr?.let { dataMap.putInt(KEY_AVG_HR, it) }
            history.maxHr?.let { dataMap.putInt(KEY_MAX_HR, it) }
            dataMap.putString(
                KEY_WORKOUT_SNAPSHOT_JSON,
                JSONObject()
                    .put("workout_id", history.workoutId)
                    .put("workout_name", history.workoutName)
                    .put("sort_order", workoutSortOrder)
                    .put("exercise_type", workoutExerciseType)
                    .toString()
            )
            dataMap.putString(KEY_EXERCISE_SNAPSHOTS_JSON, exercises.toSnapshotJson())
            dataMap.putString(KEY_EXERCISE_HR_JSON, exerciseHrRecords.toExerciseHrJson())
            dataMap.putString(KEY_HR_SAMPLES_JSON, hrSamples.toSamplesJson())
            dataMap.putString(KEY_PERFORMED_SETS_JSON, performedSetRecords.toPerformedSetsJson())
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).awaitTask()
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }

    companion object {
        const val PATH_PREFIX = "/workout_history"
        const val KEY_ID = "id"
        const val KEY_WORKOUT_ID = "workout_id"
        const val KEY_WORKOUT_NAME = "workout_name"
        const val KEY_COMPLETED_AT = "completed_at"
        const val KEY_TOTAL_SECONDS = "total_seconds"
        const val KEY_EXERCISE_COUNT = "exercise_count"
        const val KEY_WORKOUT_SNAPSHOT_JSON = "workout_snapshot_json"
        const val KEY_EXERCISE_SNAPSHOTS_JSON = "exercise_snapshots_json"
        const val KEY_START_HR = "start_hr"
        const val KEY_AVG_HR = "avg_hr"
        const val KEY_MAX_HR = "max_hr"
        const val KEY_EXERCISE_HR_JSON = "exercise_hr_json"
        const val KEY_HR_SAMPLES_JSON = "hr_samples_json"
        const val KEY_PERFORMED_SETS_JSON = "performed_sets_json"
    }
}

private fun List<ExerciseHrInput>.toExerciseHrJson(): String {
    val array = JSONArray()
    forEach { record ->
        array.put(
            JSONObject()
                .put("exercise_index", record.exerciseIndex)
                .put("exercise_name", record.exerciseName)
                .put("start_hr", record.startHr)
                .put("end_hr", record.endHr)
                .put("sort_order", record.sortOrder)
        )
    }
    return array.toString()
}

private fun List<HrSample>.toSamplesJson(): String {
    val array = JSONArray()
    forEach { sample ->
        array.put(
            JSONObject()
                .put("t", sample.timeMillis)
                .put("bpm", sample.bpm)
        )
    }
    return array.toString()
}

private fun List<PerformedSetRecordInput>.toPerformedSetsJson(): String {
    val array = JSONArray()
    forEach { record ->
        array.put(
            JSONObject()
                .put("exercise_index", record.exerciseIndex)
                .put("exercise_name", record.exerciseName)
                .put("set_index", record.setIndex)
                .put("completed", record.completed)
                .put("sort_order", record.sortOrder)
                .apply {
                    record.durationSeconds?.let { put("duration_seconds", it) }
                    record.reps?.let { put("reps", it) }
                }
        )
    }
    return array.toString()
}

private fun List<Exercise>.toSnapshotJson(): String {
    val array = JSONArray()
    forEach { exercise ->
        array.put(
            JSONObject()
                .put("exercise_id", exercise.id)
                .put("workout_id", exercise.workoutId)
                .put("exercise_name", exercise.name)
                .put("mode", exercise.mode.name)
                .put("duration_seconds", exercise.durationSeconds)
                .put("sets", exercise.sets)
                .put("rest_seconds", exercise.restSeconds)
                .put("reps_per_set", exercise.effectiveRepsPerSet())
                .put("rep_rest_seconds", exercise.repRestSeconds)
                .put("sort_order", exercise.sortOrder)
        )
    }
    return array.toString()
}
