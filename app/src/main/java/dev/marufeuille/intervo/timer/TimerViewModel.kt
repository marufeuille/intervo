package dev.marufeuille.intervo.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var timerService: TimerService? = null
    private var pendingStart: Pair<String, Boolean>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.LocalBinder).getService()
            viewModelScope.launch {
                timerService?.state?.collect { _state.value = it }
            }
            pendingStart?.let { (workoutId, resume) ->
                timerService?.start(workoutId, resume)
                pendingStart = null
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

    fun start(workoutId: String, resume: Boolean = false) {
        val svc = timerService
        if (svc != null) {
            svc.start(workoutId, resume)
        } else {
            pendingStart = workoutId to resume
        }
    }

    fun pause() { timerService?.pause() }
    fun resume() { timerService?.resume() }
    fun skipRest() { timerService?.skipRest() }
    fun adjustRest(deltaSeconds: Int) { timerService?.adjustRest(deltaSeconds) }
    fun skipRep() { timerService?.skipRep() }
    fun finishFreeSet(reps: Int? = null) { timerService?.finishFreeSet(reps) }
    fun finishOpenEndedRepSet() { timerService?.finishOpenEndedRepSet() }
    fun finishCurrentSet() { timerService?.finishCurrentSet() }
    fun stop() { timerService?.stop() }
}
