package dev.marufeuille.intervo.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(AppDatabase.getInstance(application))

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _isAmbient = MutableStateFlow(false)
    val isAmbient: StateFlow<Boolean> = _isAmbient.asStateFlow()

    private var timerService: TimerService? = null
    private var pendingExercises: List<Exercise>? = null
    private var pendingWorkoutName: String = ""

    private var currentWorkoutId = ""
    private var currentWorkoutName = ""
    private var historySaved = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.LocalBinder).getService()
            viewModelScope.launch {
                var prevPhase: TimerPhase = TimerPhase.Idle
                timerService?.state?.collect { newState ->
                    if (prevPhase !is TimerPhase.Complete && newState.phase is TimerPhase.Complete && !historySaved) {
                        historySaved = true
                        saveHistory(newState)
                    }
                    prevPhase = newState.phase
                    _state.value = newState
                }
            }
            pendingExercises?.let {
                timerService?.start(it)
                pendingExercises = null
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    fun bindService() {
        val intent = Intent(getApplication(), TimerService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: IllegalArgumentException) {}
    }

    fun start(workoutId: String) {
        currentWorkoutId = workoutId
        historySaved = false
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            currentWorkoutName = workout?.name ?: ""
            val exercises = repository.getExercisesOnce(workoutId)
            if (exercises.isEmpty()) return@launch
            val svc = timerService
            if (svc != null) {
                svc.start(exercises)
            } else {
                pendingExercises = exercises
                pendingWorkoutName = currentWorkoutName
            }
        }
    }

    fun pause() { timerService?.pause() }
    fun resume() { timerService?.resume() }
    fun skipRest() { timerService?.skipRest() }
    fun skipRep() { timerService?.skipRep() }
    fun stop() { timerService?.stop() }

    fun setAmbient(ambient: Boolean) { _isAmbient.value = ambient }

    private fun saveHistory(state: TimerState) {
        viewModelScope.launch {
            repository.addHistory(
                workoutId = currentWorkoutId,
                workoutName = currentWorkoutName,
                totalSeconds = state.elapsedSeconds,
                exerciseCount = state.exercises.size
            )
        }
    }
}
