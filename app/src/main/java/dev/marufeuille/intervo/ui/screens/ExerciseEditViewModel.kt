package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.data.*
import kotlinx.coroutines.launch

class ExerciseEditViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))
    val workoutId: String = saved["workoutId"]!!
    val exerciseId: String? = saved["exerciseId"]

    var name by mutableStateOf("")
    var mode by mutableStateOf(ExerciseMode.TIMED)
    var durationSeconds by mutableStateOf(30)
    var sets by mutableStateOf(3)
    var restSeconds by mutableStateOf(10)
    var repsPerSet by mutableStateOf(7)
    var repRestSeconds by mutableStateOf(3)
    var error by mutableStateOf(false)

    private var existing: Exercise? = null

    init {
        exerciseId?.let { id ->
            viewModelScope.launch {
                repo.exercises(workoutId).collect { list ->
                    list.find { it.id == id }?.let {
                        existing = it
                        name = it.name
                        mode = it.mode
                        durationSeconds = it.durationSeconds
                        sets = it.sets
                        restSeconds = it.restSeconds
                        repsPerSet = it.effectiveRepsPerSet()
                        repRestSeconds = it.repRestSeconds
                    }
                }
            }
        }
    }

    fun updateMode(newMode: ExerciseMode) {
        mode = newMode
        if (newMode == ExerciseMode.REPS && durationSeconds == DURATION_UNLIMITED) {
            durationSeconds = 30
        }
    }
    fun setDurationUnlimited(unlimited: Boolean) {
        if (mode != ExerciseMode.TIMED) return
        durationSeconds = if (unlimited) {
            DURATION_UNLIMITED
        } else {
            durationSeconds.takeIf { it != DURATION_UNLIMITED } ?: 30
        }
    }
    fun isDurationUnlimited(): Boolean =
        mode == ExerciseMode.TIMED && durationSeconds == DURATION_UNLIMITED
    fun adjustDuration(delta: Int) {
        val current = if (durationSeconds == DURATION_UNLIMITED) DURATION_MIN else durationSeconds
        durationSeconds = (current + delta).coerceIn(DURATION_MIN, DURATION_MAX)
    }
    fun adjustSets(delta: Int) {
        sets = (sets + delta).coerceIn(SETS_MIN, SETS_MAX)
    }
    fun setRestUnlimited(unlimited: Boolean) {
        restSeconds = if (unlimited) {
            REST_UNLIMITED
        } else {
            restSeconds.takeIf { it != REST_UNLIMITED } ?: REST_MIN
        }
    }
    fun isRestUnlimited(): Boolean = restSeconds == REST_UNLIMITED
    fun adjustRest(delta: Int) {
        val current = if (restSeconds == REST_UNLIMITED) REST_MIN else restSeconds
        restSeconds = (current + delta).coerceIn(REST_MIN, REST_MAX)
    }
    fun adjustRepsPerSet(delta: Int) {
        repsPerSet = (repsPerSet.takeIf { it >= REPS_PER_SET_MIN } ?: REPS_PER_SET_MIN)
            .plus(delta)
            .coerceIn(REPS_PER_SET_MIN, REPS_PER_SET_MAX)
    }
    fun setOpenEndedReps(openEnded: Boolean) {
        repsPerSet = if (openEnded) {
            REPS_OPEN_ENDED
        } else {
            repsPerSet.takeIf { it >= REPS_PER_SET_MIN } ?: REPS_PER_SET_MIN
        }
    }
    fun setRepRestUnlimited(unlimited: Boolean) {
        repRestSeconds = if (unlimited) {
            REST_UNLIMITED
        } else {
            repRestSeconds.takeIf { it != REST_UNLIMITED } ?: REP_REST_MIN
        }
    }
    fun isRepRestUnlimited(): Boolean = repRestSeconds == REST_UNLIMITED
    fun adjustRepRest(delta: Int) {
        val current = if (repRestSeconds == REST_UNLIMITED) REP_REST_MIN else repRestSeconds
        repRestSeconds = (current + delta).coerceIn(REP_REST_MIN, REP_REST_MAX)
    }

    suspend fun save(): Boolean {
        if (name.isBlank()) { error = true; return false }
        val ex = existing
        val savedDurationSeconds = if (mode == ExerciseMode.REPS && durationSeconds == DURATION_UNLIMITED) {
            30
        } else {
            durationSeconds
        }
        if (ex != null) {
            repo.updateExercise(ex.copy(
                name = name,
                mode = mode,
                durationSeconds = savedDurationSeconds,
                sets = sets,
                restSeconds = restSeconds,
                repsPerSet = repsPerSet,
                repRestSeconds = repRestSeconds
            ))
        } else {
            repo.addExercise(
                workoutId = workoutId,
                name = name,
                mode = mode,
                durationSeconds = savedDurationSeconds,
                sets = sets,
                restSeconds = restSeconds,
                repsPerSet = repsPerSet,
                repRestSeconds = repRestSeconds
            )
        }
        return true
    }
}
