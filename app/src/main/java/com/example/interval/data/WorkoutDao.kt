package com.example.interval.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<Workout>>

    @Insert
    suspend fun insert(workout: Workout)

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    @Transaction
    suspend fun updateOrder(workouts: List<Workout>) {
        workouts.forEachIndexed { index, workout ->
            update(workout.copy(sortOrder = index))
        }
    }

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun count(): Int

    @Query("""
        SELECT w.id, w.name, w.sortOrder, COUNT(e.id) as exerciseCount
        FROM workouts w
        LEFT JOIN exercises e ON w.id = e.workoutId
        GROUP BY w.id
        ORDER BY w.sortOrder ASC
    """)
    fun getAllWithCount(): Flow<List<WorkoutWithCount>>
}
