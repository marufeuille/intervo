package dev.marufeuille.intervo.companion.sync

import android.content.Context
import dev.marufeuille.intervo.companion.data.CompanionDatabase
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.health.HealthConnectWriter
import dev.marufeuille.intervo.companion.pds.PdsAccountSettings
import dev.marufeuille.intervo.companion.pds.PdsCredentialsStore
import dev.marufeuille.intervo.companion.pds.PdsDirectClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CompanionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = CompanionDatabase.getInstance(context)
    private val dao = db.workoutHistoryDao()
    private val healthConnectWriter by lazy { HealthConnectWriter(appContext) }
    private val pdsCredentialsStore by lazy { PdsCredentialsStore(appContext) }
    private val pdsClient by lazy { PdsDirectClient() }

    val histories: Flow<List<CompanionWorkoutHistory>> = dao.getAll()

    /** 1 件の履歴を購読する（履歴詳細画面用）。存在しなければ null を流す。 */
    fun history(id: String): Flow<CompanionWorkoutHistory?> = dao.getById(id)

    /** Health Connect へ未書き込みの履歴件数。 */
    val pendingHealthConnectCount: Flow<Int> = dao.pendingHealthConnectCount()

    /** PDS へ未同期の履歴件数。 */
    val pendingPdsCount: Flow<Int> = dao.pendingPdsCount()

    suspend fun receive(history: CompanionWorkoutHistory) {
        dao.insertIgnore(history)
    }

    /**
     * 未同期ぶんを Health Connect / PDS へ流す [SyncWorker] を予約する。
     * ウォッチからの受信時・PDS 設定保存時に呼ぶことで、手動の再同期ボタンに頼らず自動で同期させる。
     */
    fun scheduleSync() {
        SyncWorker.enqueue(appContext)
    }

    /**
     * [SyncWorker] から呼ぶ同期本体。Health Connect と PDS の未処理ぶんを流す。
     * PDS が設定済みなのに送り切れず残ったぶんがあれば true（= Worker に再試行させたい）を返す。
     * Health Connect の権限未許可で残るぶんは再試行対象にしない（無限リトライ防止）。
     */
    suspend fun syncPending(): Boolean = withContext(Dispatchers.IO) {
        writePendingHealthConnect()
        writePendingPds()
        pdsConfigured && dao.getPendingPds().isNotEmpty()
    }

    val healthConnectAvailable: Boolean
        get() = healthConnectWriter.isAvailable

    suspend fun healthConnectPermitted(): Boolean = healthConnectWriter.hasPermissions()

    val pdsConfigured: Boolean
        get() = pdsCredentialsStore.loadSettings().isConfigured

    fun loadPdsSettings(): PdsAccountSettings = pdsCredentialsStore.loadSettings()

    fun savePdsSettings(serviceUrl: String, identifier: String, appPassword: String?) {
        pdsCredentialsStore.save(serviceUrl = serviceUrl, identifier = identifier, appPassword = appPassword)
    }

    fun clearPdsSettings() {
        pdsCredentialsStore.clear()
    }

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

    /** 未同期の履歴を PDS に直接送る。認証情報未設定・失敗時は mark せず次回へ残す。 */
    suspend fun writePendingPds(): Int = withContext(Dispatchers.IO) {
        val credentials = pdsCredentialsStore.loadCredentials() ?: return@withContext 0
        var written = 0
        dao.getPendingPds().forEach { history ->
            if (pdsClient.write(history, credentials)) {
                dao.markPdsSynced(history.id, System.currentTimeMillis())
                written += 1
            }
        }
        written
    }
}
