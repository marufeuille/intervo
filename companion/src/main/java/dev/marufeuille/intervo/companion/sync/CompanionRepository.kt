package dev.marufeuille.intervo.companion.sync

import android.content.Context
import dev.marufeuille.intervo.companion.data.CompanionDatabase
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.health.HealthConnectWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CompanionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = CompanionDatabase.getInstance(context)
    private val dao = db.workoutHistoryDao()
    private val healthConnectWriter by lazy { HealthConnectWriter(appContext) }

    val histories: Flow<List<CompanionWorkoutHistory>> = dao.getAll()

    /** 1 件の履歴を購読する（履歴詳細画面用）。存在しなければ null を流す。 */
    fun history(id: String): Flow<CompanionWorkoutHistory?> = dao.getById(id)

    /** Health Connect へ未書き込みの履歴件数。 */
    val pendingHealthConnectCount: Flow<Int> = dao.pendingHealthConnectCount()

    suspend fun receive(history: CompanionWorkoutHistory) {
        dao.insertIgnore(history)
    }

    /**
     * 未同期ぶんを Health Connect へ流す [SyncWorker] を予約する。
     * ウォッチからの受信時に呼ぶことで、手動の再同期ボタンに頼らず自動で同期させる。
     */
    fun scheduleSync() {
        SyncWorker.enqueue(appContext)
    }

    /**
     * [SyncWorker] から呼ぶ同期本体。Health Connect の未処理ぶんを流す。
     * Health Connect の権限未許可で残るぶんは再試行対象にしない（無限リトライ防止）。
     */
    suspend fun syncPending() = withContext(Dispatchers.IO) {
        writePendingHealthConnect()
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
}
