package com.example.interval.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.interval.data.AppDatabase
import com.example.interval.data.Exercise
import com.example.interval.data.WorkoutRepository
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.LocalBinder).getService()
            viewModelScope.launch {
                timerService?.state?.collect { _state.value = it }
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
        viewModelScope.launch {
            val exercises = repository.getExercisesOnce(workoutId)
            if (exercises.isEmpty()) return@launch
            val svc = timerService
            if (svc != null) {
                svc.start(exercises)
            } else {
                pendingExercises = exercises
            }
        }
    }

    fun pause() { timerService?.pause() }
    fun resume() { timerService?.resume() }
    fun stop() { timerService?.stop() }

    fun setAmbient(ambient: Boolean) { _isAmbient.value = ambient }
}
