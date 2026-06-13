package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.FreeSetRecordInput
import dev.marufeuille.intervo.data.effectiveRepsPerSet
import dev.marufeuille.intervo.data.isDurationUnlimited
import dev.marufeuille.intervo.data.isOpenEndedReps

sealed interface TimerEffect {
    data class Vibrate(val pattern: VibratePattern) : TimerEffect
    data class Speak(val text: String) : TimerEffect
    object WorkoutFinished : TimerEffect
}

data class TimerTransition(
    val state: TimerState,
    val effects: List<TimerEffect> = emptyList()
)

/**
 * タイマーの状態遷移ロジック。Android 依存を持たない純粋関数の集まりで、
 * 副作用（バイブ・読み上げ・サービス停止）は TimerEffect として返し、呼び出し側が実行する。
 */
object TimerEngine {

    fun start(exercises: List<Exercise>): TimerTransition? {
        if (exercises.isEmpty()) return null
        val first = exercises[0]
        return TimerTransition(
            state = TimerState(
                exercises = exercises,
                phase = TimerPhase.ExercisePhase(
                    exerciseIndex = 0,
                    currentSet = 1,
                    currentRep = 1,
                    remainingSeconds = first.initialExerciseSeconds()
                )
            ),
            effects = listOf(
                TimerEffect.Vibrate(VibratePattern.WORKOUT_START),
                TimerEffect.Speak(first.name)
            )
        )
    }

    fun tick(current: TimerState): TimerTransition = when (val phase = current.phase) {
        is TimerPhase.ExercisePhase -> {
            val exercise = current.exercises.getOrNull(phase.exerciseIndex)
            when {
                exercise?.isDurationUnlimited() == true -> TimerTransition(
                    current.copy(phase = phase.copy(remainingSeconds = phase.remainingSeconds + 1))
                )
                phase.remainingSeconds > 1 -> countdownStep(
                    current,
                    phase.remainingSeconds - 1
                ) { phase.copy(remainingSeconds = it) }
                else -> finishExerciseInterval(
                    current, phase, listOf(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE))
                )
            }
        }
        is TimerPhase.RepRestPhase ->
            if (phase.remainingSeconds > 1) {
                countdownStep(current, phase.remainingSeconds - 1) { phase.copy(remainingSeconds = it) }
            } else {
                advanceAfterRepRest(
                    current, phase.exerciseIndex, phase.currentSet, phase.completedReps,
                    listOf(TimerEffect.Vibrate(VibratePattern.REST_DONE))
                )
            }
        is TimerPhase.RestPhase ->
            if (phase.remainingSeconds > 1) {
                countdownStep(current, phase.remainingSeconds - 1) { phase.copy(remainingSeconds = it) }
            } else {
                advanceAfterRest(
                    current, phase.exerciseIndex, phase.completedSets,
                    listOf(TimerEffect.Vibrate(VibratePattern.REST_DONE))
                )
            }
        else -> TimerTransition(current)
    }

    fun skipRest(current: TimerState): TimerTransition? = when (val phase = current.phase) {
        is TimerPhase.RestPhase ->
            advanceAfterRest(current, phase.exerciseIndex, phase.completedSets, emptyList())
        is TimerPhase.RepRestPhase ->
            advanceAfterRepRest(current, phase.exerciseIndex, phase.currentSet, phase.completedReps, emptyList())
        else -> null
    }

    fun skipRep(current: TimerState): TimerTransition? {
        val phase = current.phase as? TimerPhase.ExercisePhase ?: return null
        val exercise = current.exercises.getOrNull(phase.exerciseIndex) ?: return null
        if (exercise.mode != ExerciseMode.REPS) return null
        return finishExerciseInterval(
            current, phase, listOf(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE))
        )
    }

    fun finishFreeSet(current: TimerState, reps: Int?): TimerTransition? {
        val phase = current.phase as? TimerPhase.ExercisePhase ?: return null
        val exercise = current.exercises.getOrNull(phase.exerciseIndex) ?: return null
        if (!exercise.isDurationUnlimited()) return null
        val updated = current.copy(
            freeSetRecords = current.freeSetRecords + FreeSetRecordInput(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setNumber = phase.currentSet,
                durationSeconds = phase.remainingSeconds.coerceAtLeast(0),
                reps = reps?.takeIf { it > 0 },
                sortOrder = current.freeSetRecords.size
            )
        )
        return finishExerciseInterval(
            updated, phase, listOf(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE))
        )
    }

    fun finishOpenEndedRepSet(current: TimerState): TimerTransition? {
        val exercisePhase = current.phase as? TimerPhase.ExercisePhase
        val repRestPhase = current.phase as? TimerPhase.RepRestPhase
        val exerciseIndex = exercisePhase?.exerciseIndex ?: repRestPhase?.exerciseIndex ?: return null
        val exercise = current.exercises.getOrNull(exerciseIndex) ?: return null
        if (!exercise.isOpenEndedReps()) return null
        val currentSet = exercisePhase?.currentSet ?: repRestPhase?.currentSet ?: return null
        val completedReps = repRestPhase?.completedReps
            ?: ((exercisePhase?.currentRep ?: 1) - 1).coerceAtLeast(0)
        val updated = current.copy(
            freeSetRecords = if (completedReps > 0) {
                current.freeSetRecords + FreeSetRecordInput(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    setNumber = currentSet,
                    durationSeconds = exercise.durationSeconds * completedReps,
                    reps = completedReps,
                    sortOrder = current.freeSetRecords.size
                )
            } else {
                current.freeSetRecords
            }
        )
        return finishExerciseSet(
            updated, exerciseIndex, currentSet,
            listOf(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE))
        )
    }

    private fun countdownStep(
        current: TimerState,
        next: Int,
        newPhase: (Int) -> TimerPhase
    ): TimerTransition {
        val effects = if (next in 1..3) {
            listOf(
                TimerEffect.Vibrate(VibratePattern.COUNTDOWN_TICK),
                TimerEffect.Speak(next.toString())
            )
        } else {
            emptyList()
        }
        return TimerTransition(current.copy(phase = newPhase(next)), effects)
    }

    private fun finishExerciseInterval(
        current: TimerState,
        phase: TimerPhase.ExercisePhase,
        leadEffects: List<TimerEffect>
    ): TimerTransition {
        val exercise = current.exercises[phase.exerciseIndex]
        val hasMoreReps = exercise.mode == ExerciseMode.REPS &&
            (exercise.isOpenEndedReps() || phase.currentRep < exercise.effectiveRepsPerSet())
        return if (hasMoreReps) {
            if (exercise.repRestSeconds > 0) {
                TimerTransition(
                    current.copy(
                        phase = TimerPhase.RepRestPhase(
                            exerciseIndex = phase.exerciseIndex,
                            currentSet = phase.currentSet,
                            completedReps = phase.currentRep,
                            remainingSeconds = exercise.repRestSeconds
                        )
                    ),
                    leadEffects
                )
            } else {
                advanceAfterRepRest(current, phase.exerciseIndex, phase.currentSet, phase.currentRep, leadEffects)
            }
        } else {
            finishExerciseSet(current, phase.exerciseIndex, phase.currentSet, leadEffects)
        }
    }

    private fun finishExerciseSet(
        current: TimerState,
        exerciseIndex: Int,
        currentSet: Int,
        leadEffects: List<TimerEffect>
    ): TimerTransition {
        val exercise = current.exercises[exerciseIndex]
        return if (exercise.restSeconds > 0) {
            TimerTransition(
                current.copy(
                    phase = TimerPhase.RestPhase(
                        exerciseIndex = exerciseIndex,
                        completedSets = currentSet,
                        remainingSeconds = exercise.restSeconds
                    )
                ),
                leadEffects + TimerEffect.Speak("休憩")
            )
        } else {
            advanceAfterRest(current, exerciseIndex, currentSet, leadEffects)
        }
    }

    private fun advanceAfterRepRest(
        current: TimerState,
        exerciseIndex: Int,
        currentSet: Int,
        completedReps: Int,
        leadEffects: List<TimerEffect>
    ): TimerTransition {
        val exercise = current.exercises[exerciseIndex]
        return TimerTransition(
            current.copy(
                phase = TimerPhase.ExercisePhase(
                    exerciseIndex = exerciseIndex,
                    currentSet = currentSet,
                    currentRep = completedReps + 1,
                    remainingSeconds = exercise.initialExerciseSeconds()
                )
            ),
            leadEffects + TimerEffect.Speak("次")
        )
    }

    private fun advanceAfterRest(
        current: TimerState,
        exerciseIndex: Int,
        completedSets: Int,
        leadEffects: List<TimerEffect>
    ): TimerTransition {
        val exercise = current.exercises[exerciseIndex]
        return when {
            completedSets < exercise.sets -> TimerTransition(
                current.copy(
                    phase = TimerPhase.ExercisePhase(
                        exerciseIndex = exerciseIndex,
                        currentSet = completedSets + 1,
                        currentRep = 1,
                        remainingSeconds = exercise.initialExerciseSeconds()
                    )
                ),
                leadEffects + TimerEffect.Speak("始め")
            )
            exerciseIndex + 1 < current.exercises.size -> {
                val next = current.exercises[exerciseIndex + 1]
                TimerTransition(
                    current.copy(
                        phase = TimerPhase.ExercisePhase(
                            exerciseIndex = exerciseIndex + 1,
                            currentSet = 1,
                            currentRep = 1,
                            remainingSeconds = next.initialExerciseSeconds()
                        )
                    ),
                    leadEffects + TimerEffect.Speak(next.name)
                )
            }
            else -> TimerTransition(
                current.copy(phase = TimerPhase.Complete),
                leadEffects + listOf(
                    TimerEffect.Vibrate(VibratePattern.WORKOUT_COMPLETE),
                    TimerEffect.Speak("完了"),
                    TimerEffect.WorkoutFinished
                )
            )
        }
    }

    private fun Exercise.initialExerciseSeconds(): Int =
        if (isDurationUnlimited()) 0 else durationSeconds
}
