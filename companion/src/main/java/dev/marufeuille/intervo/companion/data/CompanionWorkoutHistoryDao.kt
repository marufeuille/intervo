package dev.marufeuille.intervo.companion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionWorkoutHistoryDao {
    @Query("SELECT * FROM companion_workout_history ORDER BY completedAt DESC")
    fun getAll(): Flow<List<CompanionWorkoutHistory>>

    @Query("SELECT COUNT(*) FROM companion_workout_history WHERE syncedAt IS NULL AND syncAttempts < :maxAttempts")
    fun pendingCount(maxAttempts: Int): Flow<Int>

    @Query("SELECT * FROM companion_workout_history WHERE syncedAt IS NULL AND syncAttempts < :maxAttempts ORDER BY completedAt ASC")
    suspend fun getPending(maxAttempts: Int): List<CompanionWorkoutHistory>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(history: CompanionWorkoutHistory): Long

    @Query("UPDATE companion_workout_history SET syncedAt = :syncedAt, syncError = NULL WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("UPDATE companion_workout_history SET syncError = :message, syncAttempts = syncAttempts + 1, lastSyncAttemptAt = :attemptedAt WHERE id = :id")
    suspend fun markSyncError(id: String, message: String, attemptedAt: Long)
}
