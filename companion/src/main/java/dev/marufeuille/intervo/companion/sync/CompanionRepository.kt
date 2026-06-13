package dev.marufeuille.intervo.companion.sync

import android.content.Context
import dev.marufeuille.intervo.companion.data.CompanionDatabase
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.health.HealthConnectWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class SyncResult(
    val uploaded: Int,
    val failed: Int,
    val skipped: Boolean = false,
    val message: String? = null,
)

class CompanionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = CompanionDatabase.getInstance(context)
    private val dao = db.workoutHistoryDao()
    private val settings = CompanionSettings(context)
    private val ingestClient = BigQueryIngestClient()
    private val requestAuthorizer by lazy { FirebaseRequestAuthorizer(appContext) }
    private val healthConnectWriter by lazy { HealthConnectWriter(appContext) }

    val histories: Flow<List<CompanionWorkoutHistory>> = dao.getAll()
    val pendingCount: Flow<Int> = dao.pendingCount(MAX_AUTO_SYNC_ATTEMPTS)

    var ingestEndpoint: String
        get() = settings.ingestEndpoint
        set(value) {
            settings.ingestEndpoint = value
        }

    val currentUid: String?
        get() = runCatching { requestAuthorizer.currentUid }.getOrNull()

    suspend fun authenticate(): AuthHeaders = withContext(Dispatchers.IO) {
        requestAuthorizer.getHeaders()
    }

    suspend fun receive(history: CompanionWorkoutHistory) {
        dao.insertIgnore(history)
    }

    val healthConnectAvailable: Boolean
        get() = healthConnectWriter.isAvailable

    suspend fun healthConnectPermitted(): Boolean = healthConnectWriter.hasPermissions()

    /** 未書き込みの履歴を Health Connect に書き出す。権限が無ければ何もしない。 */
    suspend fun writePendingHealthConnect(): Int = withContext(Dispatchers.IO) {
        if (!healthConnectWriter.hasPermissions()) return@withContext 0
        var written = 0
        dao.getPendingHealthConnect().forEach { history ->
            if (healthConnectWriter.write(history)) {
                dao.markHealthConnectWritten(history.id, System.currentTimeMillis())
                written += 1
            }
        }
        written
    }

    suspend fun syncPending(): SyncResult = withContext(Dispatchers.IO) {
        val endpoint = settings.ingestEndpoint
        if (endpoint.isBlank()) {
            return@withContext SyncResult(
                uploaded = 0,
                failed = 0,
                skipped = true,
                message = "送信先 URL が未設定です"
            )
        }

        var uploaded = 0
        var failed = 0
        var lastError: String? = null
        val authHeaders = requestAuthorizer.getHeaders()

        dao.getPending(MAX_AUTO_SYNC_ATTEMPTS).forEach { history ->
            runCatching {
                ingestClient.upload(endpoint, history, authHeaders)
            }.onSuccess {
                dao.markSynced(history.id, System.currentTimeMillis())
                uploaded += 1
            }.onFailure { error ->
                val message = error.message ?: error::class.java.simpleName
                dao.markSyncError(history.id, message, System.currentTimeMillis())
                failed += 1
                lastError = message
            }
        }

        SyncResult(uploaded = uploaded, failed = failed, message = lastError)
    }

    companion object {
        const val MAX_AUTO_SYNC_ATTEMPTS = 3
    }
}
