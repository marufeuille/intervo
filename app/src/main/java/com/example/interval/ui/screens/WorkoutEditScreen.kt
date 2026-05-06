package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.Workout
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.launch

class WorkoutEditViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))
    val workoutId: String? = saved["workoutId"]
    var name by mutableStateOf("")
    private var existingWorkout: Workout? = null

    init {
        workoutId?.let { id ->
            viewModelScope.launch {
                repo.workouts.collect { list ->
                    list.find { it.id == id }?.let {
                        existingWorkout = it
                        name = it.name
                    }
                }
            }
        }
    }

    suspend fun save(): String {
        val existing = existingWorkout
        return if (existing != null) {
            repo.updateWorkout(existing.copy(name = name))
            existing.id
        } else {
            repo.addWorkout(name).id
        }
    }
}

@Composable
fun WorkoutEditScreen(
    workoutId: String?,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    vm: WorkoutEditViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp)
        ) {
            item {
                Text(
                    text = if (workoutId == null) "ワークアウトを追加" else "名前を変更",
                    fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                Chip(
                    label = {
                        Text(
                            text = vm.name.ifBlank { "名前を入力..." },
                            color = if (vm.name.isBlank()) TextSecondary else TextPrimary,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showInput = true },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (vm.name.isBlank()) SurfaceDark else ExerciseOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                if (error) {
                    Text("名前を入力してください", color = Color.Red, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = {
                        if (vm.name.isBlank()) { error = true; return@Button }
                        scope.launch { onSaved(vm.save()) }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text("保存", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showInput) {
        NameInputDialog(
            current = vm.name,
            hint = "ワークアウト名",
            onConfirm = { vm.name = it; error = false; showInput = false },
            onDismiss = { showInput = false }
        )
    }
}
