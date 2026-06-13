package dev.marufeuille.intervo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.marufeuille.intervo.sync.WorkoutHistorySyncClient
import dev.marufeuille.intervo.timer.HrSample


class WorkoutRepository(
    private val db: AppDatabase,
    private val historySyncClient: WorkoutHistorySyncClient? = null,
) {
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

    suspend fun reorderExercises(exercises: List<Exercise>) =
        db.exerciseDao().updateOrder(exercises)

    suspend fun deleteExercise(exercise: Exercise) = db.exerciseDao().delete(exercise)

    suspend fun getExercisesOnce(workoutId: String): List<Exercise> =
        db.exerciseDao().getByWorkoutOnce(workoutId)

    suspend fun getWorkoutById(id: String): Workout? = db.workoutDao().getById(id)

    val recentHistory: Flow<List<WorkoutHistory>> = db.workoutHistoryDao().getRecent()

    val recentHistoryWithFreeSetRecords: Flow<List<WorkoutHistoryWithFreeSetRecords>> =
        db.workoutHistoryDao().getRecentWithFreeSetRecords()

    suspend fun addHistory(
        workoutId: String,
        workoutName: String,
        totalSeconds: Int,
        exerciseCount: Int,
        workoutSortOrder: Int? = null,
        exercises: List<Exercise> = emptyList(),
        freeSetRecords: List<FreeSetRecordInput> = emptyList(),
        startHr: Int? = null,
        avgHr: Int? = null,
        maxHr: Int? = null,
        exerciseHrRecords: List<ExerciseHrInput> = emptyList(),
        hrSamples: List<HrSample> = emptyList(),
    ): WorkoutHistory {
        val history = WorkoutHistory(
            workoutId = workoutId,
            workoutName = workoutName,
            totalSeconds = totalSeconds,
            exerciseCount = exerciseCount,
            startHr = startHr,
            avgHr = avgHr,
            maxHr = maxHr
        )
        db.workoutHistoryDao().insert(history)
        if (freeSetRecords.isNotEmpty()) {
            db.workoutHistoryDao().insertFreeSetRecords(
                freeSetRecords.map { record ->
                    FreeSetRecord(
                        historyId = history.id,
                        exerciseId = record.exerciseId,
                        exerciseName = record.exerciseName,
                        setNumber = record.setNumber,
                        durationSeconds = record.durationSeconds,
                        reps = record.reps,
                        sortOrder = record.sortOrder
                    )
                }
            )
        }
        if (exerciseHrRecords.isNotEmpty()) {
            db.workoutHistoryDao().insertExerciseHrRecords(
                exerciseHrRecords.map { record ->
                    ExerciseHrRecord(
                        historyId = history.id,
                        exerciseIndex = record.exerciseIndex,
                        exerciseName = record.exerciseName,
                        startHr = record.startHr,
                        endHr = record.endHr,
                        sortOrder = record.sortOrder
                    )
                }
            )
        }
        historySyncClient?.send(history, workoutSortOrder, exercises, exerciseHrRecords, hrSamples)
        return history
    }
}
