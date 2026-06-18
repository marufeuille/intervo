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

    @Query("SELECT * FROM companion_workout_history WHERE id = :id")
    fun getById(id: String): Flow<CompanionWorkoutHistory?>

    @Query("SELECT COUNT(*) FROM companion_workout_history WHERE healthConnectWrittenAt IS NULL")
    fun pendingHealthConnectCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM companion_workout_history WHERE pdsSyncedAt IS NULL")
    fun pendingPdsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(history: CompanionWorkoutHistory): Long

    @Query("SELECT * FROM companion_workout_history WHERE healthConnectWrittenAt IS NULL ORDER BY completedAt ASC")
    suspend fun getPendingHealthConnect(): List<CompanionWorkoutHistory>

    @Query("SELECT * FROM companion_workout_history WHERE pdsSyncedAt IS NULL ORDER BY completedAt ASC")
    suspend fun getPendingPds(): List<CompanionWorkoutHistory>

    @Query("SELECT * FROM companion_workout_history ORDER BY completedAt ASC")
    suspend fun getAllForPdsRewrite(): List<CompanionWorkoutHistory>

    @Query("UPDATE companion_workout_history SET healthConnectWrittenAt = :writtenAt WHERE id = :id")
    suspend fun markHealthConnectWritten(id: String, writtenAt: Long)

    @Query("UPDATE companion_workout_history SET pdsSyncedAt = :syncedAt WHERE id = :id")
    suspend fun markPdsSynced(id: String, syncedAt: Long)
}
