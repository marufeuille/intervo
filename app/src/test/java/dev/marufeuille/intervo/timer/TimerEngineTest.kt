package dev.marufeuille.intervo.timer

import dev.marufeuille.intervo.data.DURATION_UNLIMITED
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.REPS_OPEN_ENDED
import dev.marufeuille.intervo.data.REST_UNLIMITED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private fun exercise(
        name: String = "腕立て伏せ",
        mode: ExerciseMode = ExerciseMode.TIMED,
        durationSeconds: Int = 30,
        sets: Int = 2,
        restSeconds: Int = 10,
        repsPerSet: Int = 1,
        repRestSeconds: Int = 0
    ) = Exercise(
        id = "id-$name",
        workoutId = "workout",
        name = name,
        mode = mode,
        durationSeconds = durationSeconds,
        sets = sets,
        restSeconds = restSeconds,
        repsPerSet = repsPerSet,
        repRestSeconds = repRestSeconds,
        sortOrder = 0
    )

    private fun startState(vararg exercises: Exercise): TimerState =
        TimerEngine.start(exercises.toList())!!.state

    // ---- start ----

    @Test
    fun `start with empty list returns null`() {
        assertNull(TimerEngine.start(emptyList()))
    }

    @Test
    fun `start enters first exercise with vibration and name speech`() {
        val transition = TimerEngine.start(listOf(exercise(name = "スクワット", durationSeconds = 30)))!!
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(0, phase.exerciseIndex)
        assertEquals(1, phase.currentSet)
        assertEquals(1, phase.currentRep)
        assertEquals(30, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.WORKOUT_START)))
        assertTrue(transition.effects.contains(TimerEffect.Speak("スクワット")))
    }

    // ---- tick: countdown ----

    @Test
    fun `tick counts down without effects above three seconds`() {
        val state = startState(exercise(durationSeconds = 10))
        val transition = TimerEngine.tick(state)
        assertEquals(9, (transition.state.phase as TimerPhase.ExercisePhase).remainingSeconds)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `tick signals last three seconds with vibration and speech`() {
        var state = startState(exercise(durationSeconds = 5))
        state = TimerEngine.tick(state).state // 4
        val transition = TimerEngine.tick(state) // 3
        assertEquals(3, (transition.state.phase as TimerPhase.ExercisePhase).remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.COUNTDOWN_TICK)))
        assertTrue(transition.effects.contains(TimerEffect.Speak("3")))
    }

    // ---- tick: set / rest transitions ----

    @Test
    fun `set finish enters rest phase with rest speech`() {
        var state = startState(exercise(durationSeconds = 2, sets = 2, restSeconds = 10))
        state = TimerEngine.tick(state).state // 1
        val transition = TimerEngine.tick(state) // set finish
        val phase = transition.state.phase as TimerPhase.RestPhase
        assertEquals(1, phase.completedSets)
        assertEquals(10, phase.remainingSeconds)
        val performed = transition.state.performedSetRecords.single()
        assertEquals(0, performed.exerciseIndex)
        assertEquals(0, performed.setIndex)
        assertEquals(2, performed.durationSeconds)
        assertEquals(true, performed.completed)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE)))
        assertTrue(transition.effects.contains(TimerEffect.Speak("休憩")))
    }

    @Test
    fun `rest end starts next set`() {
        val base = startState(exercise(durationSeconds = 5, sets = 2, restSeconds = 1))
        val resting = base.copy(
            phase = TimerPhase.RestPhase(exerciseIndex = 0, completedSets = 1, remainingSeconds = 1)
        )
        val transition = TimerEngine.tick(resting)
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentSet)
        assertEquals(5, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.REST_DONE)))
        assertTrue(transition.effects.contains(TimerEffect.Speak("始め")))
    }

    @Test
    fun `zero rest skips rest phase entirely`() {
        var state = startState(exercise(durationSeconds = 1, sets = 2, restSeconds = 0))
        val transition = TimerEngine.tick(state)
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentSet)
        assertTrue(transition.effects.contains(TimerEffect.Speak("始め")))
    }

    @Test
    fun `rest after final set advances to next exercise with name speech`() {
        val first = exercise(name = "腕立て伏せ", durationSeconds = 5, sets = 1, restSeconds = 1)
        val second = exercise(name = "スクワット", durationSeconds = 20, sets = 1, restSeconds = 1)
        val base = startState(first, second)
        val resting = base.copy(
            phase = TimerPhase.RestPhase(exerciseIndex = 0, completedSets = 1, remainingSeconds = 1)
        )
        val transition = TimerEngine.tick(resting)
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(1, phase.exerciseIndex)
        assertEquals(20, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Speak("スクワット")))
    }

    @Test
    fun `last rest of last exercise completes workout`() {
        val base = startState(exercise(sets = 1, restSeconds = 1))
        val resting = base.copy(
            phase = TimerPhase.RestPhase(exerciseIndex = 0, completedSets = 1, remainingSeconds = 1)
        )
        val transition = TimerEngine.tick(resting)
        assertEquals(TimerPhase.Complete, transition.state.phase)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.WORKOUT_COMPLETE)))
        assertTrue(transition.effects.contains(TimerEffect.Speak("完了")))
        assertTrue(transition.effects.contains(TimerEffect.WorkoutFinished))
    }

    // ---- reps mode ----

    @Test
    fun `reps mode inserts rep rest between reps`() {
        val ex = exercise(mode = ExerciseMode.REPS, durationSeconds = 1, sets = 1, repsPerSet = 3, repRestSeconds = 5)
        val state = startState(ex)
        val transition = TimerEngine.tick(state) // rep 1 finish
        val phase = transition.state.phase as TimerPhase.RepRestPhase
        assertEquals(1, phase.completedReps)
        assertEquals(5, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Vibrate(VibratePattern.EXERCISE_DONE)))
    }

    @Test
    fun `rep rest end starts next rep`() {
        val ex = exercise(mode = ExerciseMode.REPS, durationSeconds = 3, sets = 1, repsPerSet = 3, repRestSeconds = 1)
        val base = startState(ex)
        val resting = base.copy(
            phase = TimerPhase.RepRestPhase(exerciseIndex = 0, currentSet = 1, completedReps = 1, remainingSeconds = 1)
        )
        val transition = TimerEngine.tick(resting)
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentRep)
        assertEquals(3, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Speak("次")))
    }

    @Test
    fun `reps mode without rep rest advances directly to next rep`() {
        val ex = exercise(mode = ExerciseMode.REPS, durationSeconds = 1, sets = 1, repsPerSet = 2, repRestSeconds = 0)
        val transition = TimerEngine.tick(startState(ex))
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentRep)
        assertTrue(transition.effects.contains(TimerEffect.Speak("次")))
    }

    @Test
    fun `skipRep finishes current rep in reps mode`() {
        val ex = exercise(mode = ExerciseMode.REPS, durationSeconds = 10, sets = 1, repsPerSet = 2, repRestSeconds = 0)
        val transition = TimerEngine.skipRep(startState(ex))!!
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentRep)
    }

    @Test
    fun `skipRep returns null for timed mode`() {
        assertNull(TimerEngine.skipRep(startState(exercise(mode = ExerciseMode.TIMED))))
    }

    // ---- finishCurrentSet ----

    @Test
    fun `finishCurrentSet records early timed set as incomplete`() {
        val ex = exercise(mode = ExerciseMode.TIMED, durationSeconds = 30, sets = 1, restSeconds = 0)
        val state = startState(ex).copy(
            phase = TimerPhase.ExercisePhase(exerciseIndex = 0, currentSet = 1, remainingSeconds = 12)
        )
        val transition = TimerEngine.finishCurrentSet(state)!!
        val performed = transition.state.performedSetRecords.single()
        assertEquals(18, performed.durationSeconds)
        assertNull(performed.reps)
        assertEquals(false, performed.completed)
        assertEquals(TimerPhase.Complete, transition.state.phase)
    }

    @Test
    fun `finishCurrentSet records early fixed reps as incomplete`() {
        val ex = exercise(
            mode = ExerciseMode.REPS,
            durationSeconds = 4,
            sets = 2,
            restSeconds = 10,
            repsPerSet = 5,
            repRestSeconds = 0
        )
        val state = startState(ex).copy(
            phase = TimerPhase.ExercisePhase(exerciseIndex = 0, currentSet = 1, currentRep = 4, remainingSeconds = 4)
        )
        val transition = TimerEngine.finishCurrentSet(state)!!
        val performed = transition.state.performedSetRecords.single()
        assertEquals(3, performed.reps)
        assertEquals(12, performed.durationSeconds)
        assertEquals(false, performed.completed)
        assertTrue(transition.state.phase is TimerPhase.RestPhase)
    }

    @Test
    fun `finishCurrentSet records fixed reps from rep rest`() {
        val ex = exercise(
            mode = ExerciseMode.REPS,
            durationSeconds = 4,
            sets = 1,
            restSeconds = 0,
            repsPerSet = 5,
            repRestSeconds = 5
        )
        val state = startState(ex).copy(
            phase = TimerPhase.RepRestPhase(exerciseIndex = 0, currentSet = 1, completedReps = 2, remainingSeconds = 3)
        )
        val transition = TimerEngine.finishCurrentSet(state)!!
        val performed = transition.state.performedSetRecords.single()
        assertEquals(2, performed.reps)
        assertEquals(false, performed.completed)
        assertEquals(TimerPhase.Complete, transition.state.phase)
    }

    @Test
    fun `finishCurrentSet returns null for unlimited timed exercise`() {
        assertNull(TimerEngine.finishCurrentSet(startState(exercise(durationSeconds = DURATION_UNLIMITED))))
    }

    // ---- skipRest ----

    @Test
    fun `skipRest advances from rest phase`() {
        val base = startState(exercise(durationSeconds = 5, sets = 2, restSeconds = 30))
        val resting = base.copy(
            phase = TimerPhase.RestPhase(exerciseIndex = 0, completedSets = 1, remainingSeconds = 25)
        )
        val transition = TimerEngine.skipRest(resting)!!
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentSet)
    }

    @Test
    fun `skipRest returns null during exercise phase`() {
        assertNull(TimerEngine.skipRest(startState(exercise())))
    }

    // ---- free set (duration unlimited) ----

    @Test
    fun `unlimited duration counts up`() {
        val ex = exercise(durationSeconds = DURATION_UNLIMITED, sets = 1)
        var state = startState(ex)
        assertEquals(0, (state.phase as TimerPhase.ExercisePhase).remainingSeconds)
        state = TimerEngine.tick(state).state
        state = TimerEngine.tick(state).state
        assertEquals(2, (state.phase as TimerPhase.ExercisePhase).remainingSeconds)
    }

    @Test
    fun `finishFreeSet records duration and reps`() {
        val ex = exercise(durationSeconds = DURATION_UNLIMITED, sets = 2, restSeconds = 10)
        var state = startState(ex)
        repeat(42) { state = TimerEngine.tick(state).state }
        val transition = TimerEngine.finishFreeSet(state, reps = 15)!!
        val record = transition.state.freeSetRecords.single()
        assertEquals(42, record.durationSeconds)
        assertEquals(15, record.reps)
        assertEquals(1, record.setNumber)
        val performed = transition.state.performedSetRecords.single()
        assertEquals(42, performed.durationSeconds)
        assertEquals(15, performed.reps)
        assertEquals(0, performed.setIndex)
        assertTrue(transition.state.phase is TimerPhase.RestPhase)
    }

    @Test
    fun `finishFreeSet treats non positive reps as null`() {
        val ex = exercise(durationSeconds = DURATION_UNLIMITED, sets = 1, restSeconds = 0)
        val transition = TimerEngine.finishFreeSet(startState(ex), reps = 0)!!
        assertNull(transition.state.freeSetRecords.single().reps)
    }

    @Test
    fun `finishFreeSet returns null for limited duration exercise`() {
        assertNull(TimerEngine.finishFreeSet(startState(exercise(durationSeconds = 30)), reps = 10))
    }

    // ---- open-ended reps ----

    @Test
    fun `finishOpenEndedRepSet records completed reps from exercise phase`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 4, sets = 2,
            restSeconds = 10, repsPerSet = REPS_OPEN_ENDED, repRestSeconds = 0
        )
        val base = startState(ex)
        val inThirdRep = base.copy(
            phase = TimerPhase.ExercisePhase(exerciseIndex = 0, currentSet = 1, currentRep = 3, remainingSeconds = 4)
        )
        val transition = TimerEngine.finishOpenEndedRepSet(inThirdRep)!!
        val record = transition.state.freeSetRecords.single()
        assertEquals(2, record.reps)
        assertEquals(8, record.durationSeconds)
        val performed = transition.state.performedSetRecords.single()
        assertEquals(2, performed.reps)
        assertEquals(8, performed.durationSeconds)
        assertEquals(0, performed.setIndex)
        assertTrue(transition.state.phase is TimerPhase.RestPhase)
    }

    @Test
    fun `finishOpenEndedRepSet records reps from rep rest phase`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 4, sets = 1,
            restSeconds = 0, repsPerSet = REPS_OPEN_ENDED, repRestSeconds = 5
        )
        val base = startState(ex)
        val resting = base.copy(
            phase = TimerPhase.RepRestPhase(exerciseIndex = 0, currentSet = 1, completedReps = 5, remainingSeconds = 3)
        )
        val transition = TimerEngine.finishOpenEndedRepSet(resting)!!
        assertEquals(5, transition.state.freeSetRecords.single().reps)
        assertEquals(TimerPhase.Complete, transition.state.phase)
    }

    @Test
    fun `finishOpenEndedRepSet with zero completed reps records nothing`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 4, sets = 1,
            restSeconds = 0, repsPerSet = REPS_OPEN_ENDED
        )
        val transition = TimerEngine.finishOpenEndedRepSet(startState(ex))!!
        assertTrue(transition.state.freeSetRecords.isEmpty())
    }

    @Test
    fun `finishOpenEndedRepSet returns null for fixed reps`() {
        val ex = exercise(mode = ExerciseMode.REPS, repsPerSet = 5)
        assertNull(TimerEngine.finishOpenEndedRepSet(startState(ex)))
    }

    // ---- full runs ----

    @Test
    fun `timed workout completes in exactly totalSeconds ticks`() {
        val ex = exercise(durationSeconds = 5, sets = 2, restSeconds = 3)
        var state = startState(ex)
        var ticks = 0
        while (state.phase !is TimerPhase.Complete) {
            state = TimerEngine.tick(state).state
            ticks++
            assertTrue("ワークアウトが完了しない", ticks < 1000)
        }
        assertEquals(state.totalSeconds, ticks)
        assertEquals(2, state.performedSetRecords.size)
        assertEquals(listOf(0, 1), state.performedSetRecords.map { it.setIndex })
    }

    @Test
    fun `reps workout completes in exactly totalSeconds ticks`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 3, sets = 2,
            restSeconds = 4, repsPerSet = 2, repRestSeconds = 2
        )
        var state = startState(ex)
        var ticks = 0
        while (state.phase !is TimerPhase.Complete) {
            state = TimerEngine.tick(state).state
            ticks++
            assertTrue("ワークアウトが完了しない", ticks < 1000)
        }
        assertEquals(state.totalSeconds, ticks)
        assertEquals(2, state.performedSetRecords.size)
        assertEquals(listOf(2, 2), state.performedSetRecords.map { it.reps })
    }

    // ---- unlimited rest ----

    @Test
    fun `set finish enters unlimited rest phase with remainingSeconds zero`() {
        var state = startState(exercise(durationSeconds = 1, sets = 2, restSeconds = REST_UNLIMITED))
        val transition = TimerEngine.tick(state)
        val phase = transition.state.phase as TimerPhase.RestPhase
        assertEquals(1, phase.completedSets)
        assertEquals(0, phase.remainingSeconds)
        assertTrue(transition.effects.contains(TimerEffect.Speak("休憩")))
    }

    @Test
    fun `unlimited rest counts up without auto advancing`() {
        val base = startState(exercise(durationSeconds = 1, sets = 2, restSeconds = REST_UNLIMITED))
        var state = TimerEngine.tick(base).state // -> RestPhase, remaining 0
        assertTrue(state.phase is TimerPhase.RestPhase)
        repeat(10) { state = TimerEngine.tick(state).state }
        val phase = state.phase as TimerPhase.RestPhase
        assertEquals(10, phase.remainingSeconds)
    }

    @Test
    fun `skipRest advances from unlimited rest phase`() {
        val base = startState(exercise(durationSeconds = 5, sets = 2, restSeconds = REST_UNLIMITED))
        val resting = base.copy(
            phase = TimerPhase.RestPhase(exerciseIndex = 0, completedSets = 1, remainingSeconds = 42)
        )
        val transition = TimerEngine.skipRest(resting)!!
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentSet)
    }

    @Test
    fun `totalSeconds excludes unlimited rest`() {
        val ex = exercise(durationSeconds = 10, sets = 3, restSeconds = REST_UNLIMITED)
        val state = startState(ex)
        assertEquals(30, state.totalSeconds)
    }

    @Test
    fun `rep finish enters unlimited rep rest phase with remainingSeconds zero`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 1, sets = 1,
            repsPerSet = 2, repRestSeconds = REST_UNLIMITED
        )
        val transition = TimerEngine.tick(startState(ex))
        val phase = transition.state.phase as TimerPhase.RepRestPhase
        assertEquals(0, phase.remainingSeconds)
    }

    @Test
    fun `unlimited rep rest counts up without auto advancing`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 1, sets = 1,
            repsPerSet = 2, repRestSeconds = REST_UNLIMITED
        )
        var state = TimerEngine.tick(startState(ex)).state // -> RepRestPhase, remaining 0
        repeat(7) { state = TimerEngine.tick(state).state }
        val phase = state.phase as TimerPhase.RepRestPhase
        assertEquals(7, phase.remainingSeconds)
    }

    @Test
    fun `skipRest advances from unlimited rep rest phase`() {
        val ex = exercise(
            mode = ExerciseMode.REPS, durationSeconds = 5, sets = 1,
            repsPerSet = 2, repRestSeconds = REST_UNLIMITED
        )
        val base = startState(ex)
        val resting = base.copy(
            phase = TimerPhase.RepRestPhase(exerciseIndex = 0, currentSet = 1, completedReps = 1, remainingSeconds = 20)
        )
        val transition = TimerEngine.skipRest(resting)!!
        val phase = transition.state.phase as TimerPhase.ExercisePhase
        assertEquals(2, phase.currentRep)
    }
}
