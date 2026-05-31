package dev.marufeuille.intervo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutHistoryDao {
    @Query("SELECT * FROM workout_history ORDER BY completedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<WorkoutHistory>>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY completedAt DESC LIMIT 50")
    fun getRecentWithFreeSetRecords(): Flow<List<WorkoutHistoryWithFreeSetRecords>>

    @Insert
    suspend fun insert(history: WorkoutHistory)

    @Insert
    suspend fun insertFreeSetRecords(records: List<FreeSetRecord>)

    @Query("SELECT * FROM free_set_records WHERE historyId = :historyId ORDER BY sortOrder")
    fun getFreeSetRecords(historyId: String): Flow<List<FreeSetRecord>>
}
