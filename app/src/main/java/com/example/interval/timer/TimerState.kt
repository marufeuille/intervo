package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.Exercise

sealed class TimerPhase {
    object Idle : TimerPhase()
    data class ExercisePhase(
        val exerciseIndex: Int,
        val currentSet: Int,
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
    val isPaused: Boolean = false
) {
    val currentExercise: Exercise?
        get() = when (val p = phase) {
            is TimerPhase.ExercisePhase -> exercises.getOrNull(p.exerciseIndex)
            is TimerPhase.RestPhase -> exercises.getOrNull(p.exerciseIndex)
            else -> null
        }

    val totalSeconds: Int
        get() = exercises.sumOf { it.durationSeconds * it.sets + it.restSeconds * it.sets }
}
