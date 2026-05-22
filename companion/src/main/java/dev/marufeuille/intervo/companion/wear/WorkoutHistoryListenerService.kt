package dev.marufeuille.intervo.companion.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WorkoutHistoryListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val histories = dataEvents
            .filter { event ->
                event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path?.startsWith(PATH_PREFIX) == true
            }
            .map { event ->
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                CompanionWorkoutHistory(
                    id = dataMap.getString(KEY_ID).orEmpty(),
                    workoutId = dataMap.getString(KEY_WORKOUT_ID).orEmpty(),
                    workoutName = dataMap.getString(KEY_WORKOUT_NAME).orEmpty(),
                    completedAt = dataMap.getLong(KEY_COMPLETED_AT),
                    totalSeconds = dataMap.getInt(KEY_TOTAL_SECONDS),
                    exerciseCount = dataMap.getInt(KEY_EXERCISE_COUNT),
                    workoutSnapshotJson = dataMap.getString(KEY_WORKOUT_SNAPSHOT_JSON).orEmpty(),
                    exerciseSnapshotsJson = dataMap.getString(KEY_EXERCISE_SNAPSHOTS_JSON).orEmpty()
                        .ifBlank { "[]" },
                )
            }
            .filter { it.id.isNotBlank() }

        if (histories.isEmpty()) return

        scope.launch {
            val repository = CompanionRepository(applicationContext)
            histories.forEach { repository.receive(it) }
            repository.syncPending()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PATH_PREFIX = "/workout_history"
        private const val KEY_ID = "id"
        private const val KEY_WORKOUT_ID = "workout_id"
        private const val KEY_WORKOUT_NAME = "workout_name"
        private const val KEY_COMPLETED_AT = "completed_at"
        private const val KEY_TOTAL_SECONDS = "total_seconds"
        private const val KEY_EXERCISE_COUNT = "exercise_count"
        private const val KEY_WORKOUT_SNAPSHOT_JSON = "workout_snapshot_json"
        private const val KEY_EXERCISE_SNAPSHOTS_JSON = "exercise_snapshots_json"
    }
}
