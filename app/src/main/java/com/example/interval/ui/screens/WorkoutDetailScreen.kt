package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.Exercise
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.Workout
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))
    val workoutId: String = saved["workoutId"]!!

    val exercises: StateFlow<List<Exercise>> = repo.exercises(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workout: StateFlow<Workout?> = repo.workouts
        .map { list -> list.find { it.id == workoutId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(exercise: Exercise) = viewModelScope.launch { repo.deleteExercise(exercise) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onExerciseClick: (String) -> Unit,
    onAddExercise: () -> Unit,
    onStart: () -> Unit,
    vm: WorkoutDetailViewModel = viewModel()
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val workout by vm.workout.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 28.dp)
        ) {
            item {
                Text(
                    text = workout?.name ?: "",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = TextSecondary, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            if (exercises.isEmpty()) {
                item {
                    Text("エクササイズを追加してください", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                items(exercises.size) { i ->
                    val ex = exercises[i]
                    ExerciseRow(
                        exercise = ex,
                        onClick = { onExerciseClick(ex.id) },
                        onLongClick = { deleteTarget = ex }
                    )
                    if (i < exercises.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(DividerColor))
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Chip(
                    label = {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("＋  追加", color = ExerciseOrange, fontSize = 13.sp)
                        }
                    },
                    onClick = onAddExercise,
                    colors = ChipDefaults.chipColors(backgroundColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (exercises.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange),
                        modifier = Modifier.width(160.dp)
                    ) {
                        Text("▶  スタート", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            name = target.name,
            onConfirm = { vm.delete(target); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            val summary = when (exercise.mode) {
                ExerciseMode.TIMED ->
                    "${exercise.durationSeconds}秒×${exercise.sets} / 休${exercise.restSeconds}秒"
                ExerciseMode.REPS ->
                    "${exercise.durationSeconds}秒×${exercise.repsPerSet}回×${exercise.sets}set / 休${exercise.restSeconds}秒"
            }
            Text(summary, fontSize = 11.sp, color = TextSecondary)
        }
        Text("›", fontSize = 18.sp, color = TextSecondary)
    }
}
