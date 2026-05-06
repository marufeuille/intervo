package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.*
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.launch

class ExerciseEditViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))
    val workoutId: String = saved["workoutId"]!!
    val exerciseId: String? = saved["exerciseId"]

    var name by mutableStateOf("")
    var durationSeconds by mutableStateOf(30)
    var sets by mutableStateOf(3)
    var restSeconds by mutableStateOf(10)
    var error by mutableStateOf(false)

    private var existing: Exercise? = null

    init {
        exerciseId?.let { id ->
            viewModelScope.launch {
                repo.exercises(workoutId).collect { list ->
                    list.find { it.id == id }?.let {
                        existing = it
                        name = it.name
                        durationSeconds = it.durationSeconds
                        sets = it.sets
                        restSeconds = it.restSeconds
                    }
                }
            }
        }
    }

    fun adjustDuration(delta: Int) {
        durationSeconds = (durationSeconds + delta).coerceIn(DURATION_MIN, DURATION_MAX)
    }
    fun adjustSets(delta: Int) {
        sets = (sets + delta).coerceIn(SETS_MIN, SETS_MAX)
    }
    fun adjustRest(delta: Int) {
        restSeconds = (restSeconds + delta).coerceIn(REST_MIN, REST_MAX)
    }

    suspend fun save(): Boolean {
        if (name.isBlank()) { error = true; return false }
        val ex = existing
        if (ex != null) {
            repo.updateExercise(ex.copy(name = name, durationSeconds = durationSeconds, sets = sets, restSeconds = restSeconds))
        } else {
            repo.addExercise(workoutId, name, durationSeconds, sets, restSeconds)
        }
        return true
    }
}

@Composable
fun ExerciseEditScreen(
    workoutId: String,
    exerciseId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    vm: ExerciseEditViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var showInput by remember { mutableStateOf(false) }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp)
        ) {
            item {
                Text(
                    text = if (exerciseId == null) "種目を追加" else "種目を編集",
                    fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Chip(
                    label = {
                        Text(
                            text = vm.name.ifBlank { "種目名を入力..." },
                            color = if (vm.name.isBlank()) TextSecondary else TextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    onClick = { showInput = true },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (vm.name.isBlank()) SurfaceDark else ExerciseOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(DividerColor))
            }
            item {
                Spacer(Modifier.height(4.dp))
                StepperRow(
                    label = "運動",
                    value = "${vm.durationSeconds}秒",
                    onMinus = { vm.adjustDuration(-DURATION_STEP) },
                    onPlus = { vm.adjustDuration(DURATION_STEP) },
                    accentColor = ExerciseOrange
                )
            }
            item {
                StepperRow(
                    label = "セット",
                    value = "${vm.sets}回",
                    onMinus = { vm.adjustSets(-1) },
                    onPlus = { vm.adjustSets(1) },
                    accentColor = ExerciseOrange
                )
            }
            item {
                StepperRow(
                    label = "休憩",
                    value = "${vm.restSeconds}秒",
                    onMinus = { vm.adjustRest(-REST_STEP) },
                    onPlus = { vm.adjustRest(REST_STEP) },
                    accentColor = RestBlue
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                if (vm.error) {
                    Text("種目名を入力してください", color = Color.Red, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = { scope.launch { if (vm.save()) onSaved() } },
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
            hint = "種目名",
            onConfirm = { vm.name = it; vm.error = false; showInput = false },
            onDismiss = { showInput = false }
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(
                onClick = onMinus,
                colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
            ) {
                Text("−", color = accentColor, fontSize = 16.sp)
            }
            Text(
                text = value,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center
            )
            CompactButton(
                onClick = onPlus,
                colors = ButtonDefaults.buttonColors(backgroundColor = accentColor)
            ) {
                Text("＋", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
