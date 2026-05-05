package com.example.interval.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.example.interval.data.Exercise
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
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        vibrationManager = VibrationManager(this)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    fun start(exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock?.release()
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "interval:timer").also {
            it.acquire(2 * 60 * 60 * 1000L) // 最大2時間
        }
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
                    _state.value = current.copy(
                        phase = phase.copy(remainingSeconds = phase.remainingSeconds - 1)
                    )
                } else {
                    vibrationManager.vibrate(VibratePattern.EXERCISE_DONE)
                    val exercise = current.exercises[phase.exerciseIndex]
                    if (exercise.restSeconds > 0) {
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
                    _state.value = current.copy(
                        phase = phase.copy(remainingSeconds = phase.remainingSeconds - 1)
                    )
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
                _state.value = current.copy(
                    phase = TimerPhase.ExercisePhase(
                        exerciseIndex = exerciseIndex + 1,
                        currentSet = 1,
                        remainingSeconds = next.durationSeconds
                    )
                )
            }
            else -> {
                _state.value = current.copy(phase = TimerPhase.Complete)
                releaseWakeLock()
                stopSelf()
            }
        }
    }
}
