package dev.marufeuille.intervo.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import dev.marufeuille.intervo.data.Exercise
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private lateinit var vibrationManager: VibrationManager
    private lateinit var speechManager: SpeechManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        vibrationManager = VibrationManager(this)
        speechManager = SpeechManager(this)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        speechManager.shutdown()
        serviceScope.cancel()
    }

    fun start(exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock?.release()
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "interval:timer").also {
            it.acquire(2 * 60 * 60 * 1000L)
        }
        vibrationManager.vibrate(VibratePattern.WORKOUT_START)
        speechManager.speak(exercises[0].name)
        _state.value = TimerState(
            exercises = exercises,
            phase = TimerPhase.ExercisePhase(
                exerciseIndex = 0,
                currentSet = 1,
                remainingSeconds = exercises[0].durationSeconds
            )
        )
        startCountdown()
    }

    fun pause() {
        countdownJob?.cancel()
        _state.value = _state.value.copy(isPaused = true)
    }

    fun resume() {
        if (!_state.value.isPaused) return
        _state.value = _state.value.copy(isPaused = false)
        startCountdown()
    }

    fun skipRest() {
        val current = _state.value
        val phase = current.phase as? TimerPhase.RestPhase ?: return
        countdownJob?.cancel()
        val unpaused = current.copy(isPaused = false)
        advanceAfterRest(unpaused, phase.exerciseIndex, phase.completedSets)
        startCountdown()
    }

    fun stop() {
        countdownJob?.cancel()
        releaseWakeLock()
        _state.value = TimerState()
        stopSelf()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val current = _state.value
                if (current.isPaused) break
                tick(current)
            }
        }
    }

    private fun tick(current: TimerState) {
        when (val phase = current.phase) {
            is TimerPhase.ExercisePhase -> {
                if (phase.remainingSeconds > 1) {
                    val next = phase.remainingSeconds - 1
                    _state.value = current.copy(phase = phase.copy(remainingSeconds = next))
                    if (next in 1..3) {
                        vibrationManager.vibrate(VibratePattern.COUNTDOWN_TICK)
                        speechManager.speak(next.toString())
                    }
                } else {
                    vibrationManager.vibrate(VibratePattern.EXERCISE_DONE)
                    val exercise = current.exercises[phase.exerciseIndex]
                    if (exercise.restSeconds > 0) {
                        speechManager.speak("休憩")
                        _state.value = current.copy(
                            phase = TimerPhase.RestPhase(
                                exerciseIndex = phase.exerciseIndex,
                                completedSets = phase.currentSet,
                                remainingSeconds = exercise.restSeconds
                            )
                        )
                    } else {
                        advanceAfterRest(current, phase.exerciseIndex, phase.currentSet)
                    }
                }
            }
            is TimerPhase.RestPhase -> {
                if (phase.remainingSeconds > 1) {
                    val next = phase.remainingSeconds - 1
                    _state.value = current.copy(phase = phase.copy(remainingSeconds = next))
                    if (next in 1..3) {
                        vibrationManager.vibrate(VibratePattern.COUNTDOWN_TICK)
                        speechManager.speak(next.toString())
                    }
                } else {
                    vibrationManager.vibrate(VibratePattern.REST_DONE)
                    advanceAfterRest(current, phase.exerciseIndex, phase.completedSets)
                }
            }
            else -> {}
        }
    }

    private fun advanceAfterRest(current: TimerState, exerciseIndex: Int, completedSets: Int) {
        val exercise = current.exercises[exerciseIndex]
        when {
            completedSets < exercise.sets -> {
                speechManager.speak("始め")
                _state.value = current.copy(
                    phase = TimerPhase.ExercisePhase(
                        exerciseIndex = exerciseIndex,
                        currentSet = completedSets + 1,
                        remainingSeconds = exercise.durationSeconds
                    )
                )
            }
            exerciseIndex + 1 < current.exercises.size -> {
                val next = current.exercises[exerciseIndex + 1]
                speechManager.speak(next.name)
                _state.value = current.copy(
                    phase = TimerPhase.ExercisePhase(
                        exerciseIndex = exerciseIndex + 1,
                        currentSet = 1,
                        remainingSeconds = next.durationSeconds
                    )
                )
            }
            else -> {
                vibrationManager.vibrate(VibratePattern.WORKOUT_COMPLETE)
                speechManager.speak("完了")
                _state.value = current.copy(phase = TimerPhase.Complete)
                releaseWakeLock()
                stopSelf()
            }
        }
    }
}
