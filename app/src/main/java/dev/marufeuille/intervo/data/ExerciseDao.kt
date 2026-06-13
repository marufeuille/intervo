package dev.marufeuille.intervo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY sortOrder ASC")
    fun getByWorkout(workoutId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY sortOrder ASC")
    suspend fun getByWorkoutOnce(workoutId: String): List<Exercise>

    @Insert
    suspend fun insert(exercise: Exercise)

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Transaction
    suspend fun updateOrder(exercises: List<Exercise>) {
        exercises.forEachIndexed { index, exercise ->
            update(exercise.copy(sortOrder = index))
        }
    }

    @Query("SELECT COUNT(*) FROM exercises WHERE workoutId = :workoutId")
    fun countByWorkout(workoutId: String): Flow<Int>

    @Query("SELECT workoutId, COUNT(*) as count FROM exercises GROUP BY workoutId")
    fun getAllCounts(): Flow<List<WorkoutExerciseCount>>
}
