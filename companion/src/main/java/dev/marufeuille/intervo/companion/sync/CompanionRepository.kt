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

    /** Health Connect へ未書き込みの履歴件数。 */
    val pendingHealthConnectCount: Flow<Int> = dao.pendingHealthConnectCount()

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
}
