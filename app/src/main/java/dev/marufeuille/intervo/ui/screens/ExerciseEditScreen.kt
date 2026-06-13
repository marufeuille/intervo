package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.*
import dev.marufeuille.intervo.timer.VibratePattern
import dev.marufeuille.intervo.timer.VibrationManager
import dev.marufeuille.intervo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                                // 保存後の navigate は必ずメインスレッドで行う
                                withContext(Dispatchers.Main) { onSaved() }
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
