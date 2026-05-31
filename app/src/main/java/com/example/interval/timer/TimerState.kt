package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.FreeSetRecordInput

sealed class TimerPhase {
    object Idle : TimerPhase()
    data class ExercisePhase(
        val exerciseIndex: Int,
        val currentSet: Int,
        val currentRep: Int = 1,
        val remainingSeconds: Int
    ) : TimerPhase()
    data class RepRestPhase(
        val exerciseIndex: Int,
        val currentSet: Int,
        val completedReps: Int,
        val remainingSeconds: Int
    ) : TimerPhase()
    data class RestPhase(
        val exerciseIndex: Int,
        val completedSets: Int,
        val remainingSeconds: Int
    ) : TimerPhase()
    object Complete : TimerPhase()
}

data class TimerState(
    val exercises: List<Exercise> = emptyList(),
    val phase: TimerPhase = TimerPhase.Idle,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val freeSetRecords: List<FreeSetRecordInput> = emptyList()
) {
    val currentExercise: Exercise?
        get() = when (val p = phase) {
            is TimerPhase.ExercisePhase -> exercises.getOrNull(p.exerciseIndex)
            is TimerPhase.RepRestPhase -> exercises.getOrNull(p.exerciseIndex)
            is TimerPhase.RestPhase -> exercises.getOrNull(p.exerciseIndex)
            else -> null
        }

    val nextExercise: Exercise?
        get() {
            return when (val p = phase) {
                is TimerPhase.ExercisePhase -> {
                    val ex = exercises.getOrNull(p.exerciseIndex) ?: return null
                    val isLastRep = ex.mode != ExerciseMode.REPS || p.currentRep >= ex.repsPerSet
                    if (isLastRep && p.currentSet >= ex.sets) exercises.getOrNull(p.exerciseIndex + 1) else null
                }
                is TimerPhase.RepRestPhase -> null
                is TimerPhase.RestPhase -> {
                    val ex = exercises.getOrNull(p.exerciseIndex) ?: return null
                    if (p.completedSets >= ex.sets) exercises.getOrNull(p.exerciseIndex + 1) else null
                }
                else -> null
            }
        }

    val totalSeconds: Int
        get() = exercises.sumOf { ex ->
            val perSet = when (ex.mode) {
                ExerciseMode.TIMED -> ex.durationSeconds
                ExerciseMode.REPS ->
                    ex.durationSeconds * ex.repsPerSet + ex.repRestSeconds * (ex.repsPerSet - 1).coerceAtLeast(0)
                ExerciseMode.FREE -> 0
            }
            perSet * ex.sets + ex.restSeconds * ex.sets
        }
}
