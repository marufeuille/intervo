package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import dev.marufeuille.intervo.timer.VibratePattern
import dev.marufeuille.intervo.timer.VibrationManager
import dev.marufeuille.intervo.ui.theme.*
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
    fun adjustRest(delta: Int) {
        restSeconds = (restSeconds + delta).coerceIn(REST_MIN, REST_MAX)
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
    fun adjustRepRest(delta: Int) {
        repRestSeconds = (repRestSeconds + delta).coerceIn(REP_REST_MIN, REP_REST_MAX)
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
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val vibrator = remember { VibrationManager(context) }

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
            state = listState
        ) {
            item {
                Text(
                    text = if (exerciseId == null) "種目を追加" else "種目を編集",
                    fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                val showError = vm.error && vm.name.isBlank()
                val nameChipBg = when {
                    showError -> Color.Red
                    vm.name.isBlank() -> SurfaceDark
                    else -> ExerciseOrange
                }
                val nameChipFg = when {
                    showError -> Color.White
                    vm.name.isBlank() -> TextSecondary
                    else -> TextPrimary
                }
                val placeholder = if (showError) "！種目名を入力してください" else "種目名を入力..."
                Chip(
                    label = {
                        Text(
                            text = vm.name.ifBlank { placeholder },
                            color = nameChipFg,
                            fontSize = 13.sp,
                            fontWeight = if (showError) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { showInput = true },
                    colors = ChipDefaults.chipColors(backgroundColor = nameChipBg),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(DividerColor))
            }
            item {
                Spacer(Modifier.height(4.dp))
                ModeToggle(mode = vm.mode, onChange = vm::updateMode)
                Spacer(Modifier.height(4.dp))
            }
            if (vm.mode == ExerciseMode.TIMED) {
                item {
                    DurationTargetToggle(
                        unlimited = vm.isDurationUnlimited(),
                        onChange = vm::setDurationUnlimited
                    )
                }
            }
            if (vm.mode == ExerciseMode.REPS || !vm.isDurationUnlimited()) {
                item {
                    StepperRow(
                        label = if (vm.mode == ExerciseMode.REPS) "ホールド" else "運動",
                        value = "${vm.durationSeconds}秒",
                        onMinus = { vm.adjustDuration(-DURATION_STEP) },
                        onPlus = { vm.adjustDuration(DURATION_STEP) },
                        accentColor = ExerciseOrange
                    )
                }
            }
            if (vm.mode == ExerciseMode.REPS) {
                item {
                    RepsTargetToggle(
                        openEnded = vm.repsPerSet == REPS_OPEN_ENDED,
                        onChange = vm::setOpenEndedReps
                    )
                }
                if (vm.repsPerSet != REPS_OPEN_ENDED) {
                    item {
                        StepperRow(
                            label = "回数",
                            value = "${vm.repsPerSet}回",
                            onMinus = { vm.adjustRepsPerSet(-1) },
                            onPlus = { vm.adjustRepsPerSet(1) },
                            accentColor = ExerciseOrange
                        )
                    }
                }
                item {
                    StepperRow(
                        label = "間休憩",
                        value = "${vm.repRestSeconds}秒",
                        onMinus = { vm.adjustRepRest(-REP_REST_STEP) },
                        onPlus = { vm.adjustRepRest(REP_REST_STEP) },
                        accentColor = RestBlue
                    )
                }
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
                Button(
                    onClick = {
                        scope.launch {
                            if (vm.save()) {
                                onSaved()
                            } else {
                                vibrator.vibrate(VibratePattern.ERROR)
                                listState.animateScrollToItem(0)
                            }
                        }
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
            hint = "種目名",
            onConfirm = { vm.name = it; vm.error = false; showInput = false },
            onDismiss = { showInput = false }
        )
    }
}

@Composable
private fun ModeToggle(
    mode: ExerciseMode,
    onChange: (ExerciseMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("方式", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModePill(label = "時間", selected = mode == ExerciseMode.TIMED) { onChange(ExerciseMode.TIMED) }
            ModePill(label = "回数", selected = mode == ExerciseMode.REPS) { onChange(ExerciseMode.REPS) }
        }
    }
}

@Composable
private fun DurationTargetToggle(
    unlimited: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("時間", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModePill(label = "指定", selected = !unlimited) { onChange(false) }
            ModePill(label = "自由", selected = unlimited) { onChange(true) }
        }
    }
}

@Composable
private fun RepsTargetToggle(
    openEnded: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("目標", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModePill(label = "指定", selected = !openEnded) { onChange(false) }
            ModePill(label = "限界", selected = openEnded) { onChange(true) }
        }
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) ExerciseOrange else ButtonDark
    val fg = if (selected) Color.White else TextSecondary
    CompactButton(
        onClick = onClick,
        modifier = Modifier.size(width = 44.dp, height = 32.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = bg)
    ) {
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
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
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(48.dp))
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
