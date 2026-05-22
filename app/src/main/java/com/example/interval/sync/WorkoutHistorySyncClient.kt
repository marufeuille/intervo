package dev.marufeuille.intervo.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.WorkoutHistory
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
        exercises: List<Exercise>,
    ) {
        val request = PutDataMapRequest.create("$PATH_PREFIX/${history.id}").apply {
            dataMap.putString(KEY_ID, history.id)
            dataMap.putString(KEY_WORKOUT_ID, history.workoutId)
            dataMap.putString(KEY_WORKOUT_NAME, history.workoutName)
            dataMap.putLong(KEY_COMPLETED_AT, history.completedAt)
            dataMap.putInt(KEY_TOTAL_SECONDS, history.totalSeconds)
            dataMap.putInt(KEY_EXERCISE_COUNT, history.exerciseCount)
            dataMap.putString(
                KEY_WORKOUT_SNAPSHOT_JSON,
                JSONObject()
                    .put("workout_id", history.workoutId)
                    .put("workout_name", history.workoutName)
                    .put("sort_order", workoutSortOrder)
                    .toString()
            )
            dataMap.putString(KEY_EXERCISE_SNAPSHOTS_JSON, exercises.toSnapshotJson())
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
    }
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
                .put("reps_per_set", exercise.repsPerSet)
                .put("rep_rest_seconds", exercise.repRestSeconds)
                .put("sort_order", exercise.sortOrder)
        )
    }
    return array.toString()
}
