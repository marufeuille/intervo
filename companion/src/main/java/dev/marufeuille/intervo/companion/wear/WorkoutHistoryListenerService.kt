package dev.marufeuille.intervo.companion.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
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
        val received = dataEvents
            .filter { event ->
                event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path?.startsWith(PATH_PREFIX) == true
            }
            .map { event ->
                val uri = event.dataItem.uri
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                uri to CompanionWorkoutHistory(
                    id = dataMap.getString(KEY_ID).orEmpty(),
                    workoutId = dataMap.getString(KEY_WORKOUT_ID).orEmpty(),
                    workoutName = dataMap.getString(KEY_WORKOUT_NAME).orEmpty(),
                    completedAt = dataMap.getLong(KEY_COMPLETED_AT),
                    totalSeconds = dataMap.getInt(KEY_TOTAL_SECONDS),
                    exerciseCount = dataMap.getInt(KEY_EXERCISE_COUNT),
                    workoutSnapshotJson = dataMap.getString(KEY_WORKOUT_SNAPSHOT_JSON).orEmpty(),
                    exerciseSnapshotsJson = dataMap.getString(KEY_EXERCISE_SNAPSHOTS_JSON).orEmpty()
                        .ifBlank { "[]" },
                    startHr = dataMap.getInt(KEY_START_HR, 0).takeIf { it > 0 },
                    avgHr = dataMap.getInt(KEY_AVG_HR, 0).takeIf { it > 0 },
                    maxHr = dataMap.getInt(KEY_MAX_HR, 0).takeIf { it > 0 },
                    exerciseHrJson = dataMap.getString(KEY_EXERCISE_HR_JSON).orEmpty().ifBlank { "[]" },
                    hrSamplesJson = dataMap.getString(KEY_HR_SAMPLES_JSON).orEmpty().ifBlank { "[]" },
                )
            }
            .filter { it.second.id.isNotBlank() }

        if (received.isEmpty()) return

        scope.launch {
            val repository = CompanionRepository(applicationContext)
            received.forEach { (uri, history) ->
                repository.receive(history)
                // Room に取り込めたら Data Layer 側のアイテムは消す（バッファ肥大化防止）。ベストエフォート。
                runCatching { Wearable.getDataClient(applicationContext).deleteDataItems(uri) }
            }
            repository.writePendingHealthConnect()
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
        private const val KEY_START_HR = "start_hr"
        private const val KEY_AVG_HR = "avg_hr"
        private const val KEY_MAX_HR = "max_hr"
        private const val KEY_EXERCISE_HR_JSON = "exercise_hr_json"
        private const val KEY_HR_SAMPLES_JSON = "hr_samples_json"
    }
}
