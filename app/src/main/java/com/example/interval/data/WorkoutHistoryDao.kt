package dev.marufeuille.intervo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutHistoryDao {
    @Query("SELECT * FROM workout_history ORDER BY completedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<WorkoutHistory>>

    @Insert
    suspend fun insert(history: WorkoutHistory)
}
