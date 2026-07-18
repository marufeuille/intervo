package dev.marufeuille.intervo.companion.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dev.marufeuille.intervo.companion.CompanionApplication
import java.util.concurrent.TimeUnit

/**
 * 未同期の履歴を Health Connect へ流す WorkManager ジョブ。
 *
 * ウォッチからの受信は [dev.marufeuille.intervo.companion.wear.WorkoutHistoryListenerService] の
 * 短命なサービス内で起きるため、その場でネットワーク同期まで走らせると完走前にサービスが破棄され
 * 取りこぼす。そこで同期は本 Worker に委譲し、ネットワーク接続を制約に、失敗時はバックオフ再試行させる。
 */
class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CompanionApplication).container.repository
        runCatching { repository.syncPending() }.getOrElse {
            // 予期せぬ例外は一時的失敗とみなして再試行に回す。
            return Result.retry()
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "intervo_companion_sync"

        /**
         * 同期ジョブを予約する。連続受信時は最新の 1 本だけ走れば十分なので
         * [ExistingWorkPolicy.REPLACE] で重複を畳む（同期処理は冪等）。
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
