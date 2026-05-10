package dev.marufeuille.intervo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class WorkoutRepository(private val db: AppDatabase) {
    val workouts: Flow<List<Workout>> = db.workoutDao().getAll()

    val workoutsWithCount: Flow<List<WorkoutWithCount>> = db.workoutDao().getAllWithCount()

    val allExerciseCounts: Flow<Map<String, Int>> = db.exerciseDao().getAllCounts()
        .map { list -> list.associate { it.workoutId to it.count } }

    fun exercises(workoutId: String): Flow<List<Exercise>> =
        db.exerciseDao().getByWorkout(workoutId)

    fun exerciseCount(workoutId: String): Flow<Int> =
        db.exerciseDao().countByWorkout(workoutId)

    suspend fun addWorkout(name: String): Workout {
        val count = db.workoutDao().count()
        val workout = Workout(name = name, sortOrder = count)
        db.workoutDao().insert(workout)
        return workout
    }

    suspend fun updateWorkout(workout: Workout) = db.workoutDao().update(workout)

    suspend fun deleteWorkout(workout: Workout) = db.workoutDao().delete(workout)

    suspend fun addExercise(
        workoutId: String,
        name: String,
        mode: ExerciseMode,
        durationSeconds: Int,
        sets: Int,
        restSeconds: Int,
        repsPerSet: Int,
        repRestSeconds: Int,
    ): Exercise {
        val existing = db.exerciseDao().getByWorkoutOnce(workoutId)
        val exercise = Exercise(
            workoutId = workoutId,
            name = name,
            mode = mode,
            durationSeconds = durationSeconds,
            sets = sets,
            restSeconds = restSeconds,
            repsPerSet = repsPerSet,
            repRestSeconds = repRestSeconds,
            sortOrder = existing.size
        )
        db.exerciseDao().insert(exercise)
        return exercise
    }

    suspend fun updateExercise(exercise: Exercise) = db.exerciseDao().update(exercise)

    suspend fun deleteExercise(exercise: Exercise) = db.exerciseDao().delete(exercise)

    suspend fun getExercisesOnce(workoutId: String): List<Exercise> =
        db.exerciseDao().getByWorkoutOnce(workoutId)

    suspend fun getWorkoutById(id: String): Workout? = db.workoutDao().getById(id)

    val recentHistory: Flow<List<WorkoutHistory>> = db.workoutHistoryDao().getRecent()

    suspend fun addHistory(
        workoutId: String,
        workoutName: String,
        totalSeconds: Int,
        exerciseCount: Int
    ) {
        db.workoutHistoryDao().insert(
            WorkoutHistory(
                workoutId = workoutId,
                workoutName = workoutName,
                totalSeconds = totalSeconds,
                exerciseCount = exerciseCount
            )
        )
    }
}
