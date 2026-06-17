package dev.marufeuille.intervo.companion.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.marufeuille.intervo.companion.CompanionApplication
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug ビルド専用: ADB からダミー履歴を投入し、PDS 直接同期まで走らせる。
 *
 * adb shell am broadcast \
 *   -n dev.marufeuille.intervo.debug/dev.marufeuille.intervo.companion.debug.DebugWorkoutHistoryReceiver \
 *   -a dev.marufeuille.intervo.DEBUG_SEED_WORKOUT_HISTORY \
 *   --es sourceRef emulator-test-001
 */
class DebugWorkoutHistoryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as CompanionApplication
        val sourceRef = intent.getStringExtra(EXTRA_SOURCE_REF)
            ?.takeIf { it.isNotBlank() }
            ?: "debug-${System.currentTimeMillis()}"
        val shouldSync = intent.getBooleanExtra(EXTRA_SYNC, true)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val repository = app.container.repository
                val pdsUrl = intent.getStringExtra(EXTRA_PDS_URL)
                val identifier = intent.getStringExtra(EXTRA_IDENTIFIER)
                val appPassword = intent.getStringExtra(EXTRA_APP_PASSWORD)
                if (!pdsUrl.isNullOrBlank() && !identifier.isNullOrBlank() && !appPassword.isNullOrBlank()) {
                    repository.savePdsSettings(
                        serviceUrl = pdsUrl,
                        identifier = identifier,
                        appPassword = appPassword,
                    )
                }
                repository.receive(sampleHistory(sourceRef))
                val synced = if (shouldSync) repository.writePendingPds() else 0
                Log.i(TAG, "Seeded sourceRef=$sourceRef pdsSynced=$synced sync=$shouldSync")
            }.onFailure { error ->
                Log.e(TAG, "Failed to seed debug workout history", error)
            }
            pendingResult.finish()
        }
    }

    private fun sampleHistory(sourceRef: String): CompanionWorkoutHistory {
        val completedAt = System.currentTimeMillis()
        val totalSeconds = 20 * 60 + 34
        return CompanionWorkoutHistory(
            id = sourceRef,
            workoutId = "debug-workout-upper",
            workoutName = "エミュレータ検証ワークアウト",
            completedAt = completedAt,
            totalSeconds = totalSeconds,
            exerciseCount = 2,
            workoutSnapshotJson = JSONObject()
                .put("workout_id", "debug-workout-upper")
                .put("workout_name", "エミュレータ検証ワークアウト")
                .put("sort_order", 0)
                .put("exercise_type", "STRENGTH_TRAINING")
                .toString(),
            exerciseSnapshotsJson = JSONArray()
                .put(
                    JSONObject()
                        .put("exercise_id", "debug-push-up")
                        .put("workout_id", "debug-workout-upper")
                        .put("exercise_name", "腕立て伏せ")
                        .put("mode", "REPS")
                        .put("duration_seconds", -1)
                        .put("sets", 3)
                        .put("rest_seconds", 60)
                        .put("reps_per_set", 12)
                        .put("rep_rest_seconds", 0)
                        .put("sort_order", 0)
                )
                .put(
                    JSONObject()
                        .put("exercise_id", "debug-plank")
                        .put("workout_id", "debug-workout-upper")
                        .put("exercise_name", "プランク")
                        .put("mode", "TIMED")
                        .put("duration_seconds", 45)
                        .put("sets", 3)
                        .put("rest_seconds", 30)
                        .put("reps_per_set", -1)
                        .put("rep_rest_seconds", 0)
                        .put("sort_order", 1)
                )
                .toString(),
            startHr = 82,
            avgHr = 128,
            maxHr = 166,
            exerciseHrJson = JSONArray()
                .put(
                    JSONObject()
                        .put("exercise_index", 0)
                        .put("exercise_name", "腕立て伏せ")
                        .put("start_hr", 108)
                        .put("end_hr", 154)
                        .put("sort_order", 0)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 1)
                        .put("exercise_name", "プランク")
                        .put("start_hr", 118)
                        .put("end_hr", 148)
                        .put("sort_order", 1)
                )
                .toString(),
            performedSetsJson = JSONArray()
                .put(
                    JSONObject()
                        .put("exercise_index", 0)
                        .put("exercise_name", "腕立て伏せ")
                        .put("set_index", 0)
                        .put("reps", 12)
                        .put("completed", true)
                        .put("sort_order", 0)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 0)
                        .put("exercise_name", "腕立て伏せ")
                        .put("set_index", 1)
                        .put("reps", 12)
                        .put("completed", true)
                        .put("sort_order", 1)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 0)
                        .put("exercise_name", "腕立て伏せ")
                        .put("set_index", 2)
                        .put("reps", 10)
                        .put("completed", false)
                        .put("sort_order", 2)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 1)
                        .put("exercise_name", "プランク")
                        .put("set_index", 0)
                        .put("duration_seconds", 45)
                        .put("completed", true)
                        .put("sort_order", 3)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 1)
                        .put("exercise_name", "プランク")
                        .put("set_index", 1)
                        .put("duration_seconds", 45)
                        .put("completed", true)
                        .put("sort_order", 4)
                )
                .put(
                    JSONObject()
                        .put("exercise_index", 1)
                        .put("exercise_name", "プランク")
                        .put("set_index", 2)
                        .put("duration_seconds", 22)
                        .put("completed", false)
                        .put("sort_order", 5)
                )
                .toString(),
        )
    }

    companion object {
        private const val TAG = "DebugWorkoutHistory"
        private const val EXTRA_SOURCE_REF = "sourceRef"
        private const val EXTRA_SYNC = "sync"
        private const val EXTRA_PDS_URL = "pdsUrl"
        private const val EXTRA_IDENTIFIER = "identifier"
        private const val EXTRA_APP_PASSWORD = "appPassword"
    }
}
