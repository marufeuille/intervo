package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.effectiveRepsPerSet
import dev.marufeuille.intervo.data.isOpenEndedReps
import dev.marufeuille.intervo.timer.TimerPhase
import dev.marufeuille.intervo.timer.TimerState
import dev.marufeuille.intervo.timer.TimerViewModel
import dev.marufeuille.intervo.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    workoutId: String,
    onComplete: (totalSeconds: Int) -> Unit,
    onStop: () -> Unit,
    vm: TimerViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isAmbient by vm.isAmbient.collectAsStateWithLifecycle()
    var showStopDialog by remember { mutableStateOf(false) }
    var freeSetReview by remember { mutableStateOf<FreeSetReview?>(null) }

    LaunchedEffect(workoutId) {
        vm.bindService()
        vm.start(workoutId)
    }

    LaunchedEffect(state.phase) {
        if (state.phase is TimerPhase.Complete) onComplete(state.elapsedSeconds)
    }

    DisposableEffect(Unit) {
        onDispose { vm.unbindService() }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    if (isAmbient) {
        AmbientTimerContent(state)
    } else {
        ActiveTimerContent(
            state = state,
            onTap = {
                val phase = state.phase
                val isFreeSet = phase is TimerPhase.ExercisePhase &&
                    state.exercises.getOrNull(phase.exerciseIndex)?.mode == ExerciseMode.FREE
                if (isFreeSet && phase is TimerPhase.ExercisePhase) {
                    vm.pause()
                    freeSetReview = FreeSetReview(durationSeconds = phase.remainingSeconds)
                } else if (state.isPaused) {
                    vm.resume()
                } else {
                    vm.pause()
                }
            },
            onSkipRest = { vm.skipRest() },
            onSkipRep = { vm.skipRep() },
            onFinishOpenEndedRepSet = { vm.finishOpenEndedRepSet() },
            onLongPress = { showStopDialog = true }
        )
    }

    if (showStopDialog) {
        Dialog(onDismissRequest = { showStopDialog = false }) {
            Column(
                modifier = Modifier
                    .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("中断しますか？", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactButton(
                        onClick = { showStopDialog = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                    ) { Text("続ける", fontSize = 11.sp, color = TextPrimary) }
                    CompactButton(
                        onClick = { vm.stop(); showStopDialog = false; onStop() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) { Text("中断", fontSize = 11.sp, color = Color.White) }
                }
            }
        }
    }

    freeSetReview?.let { review ->
        FreeSetRecordDialog(
            durationSeconds = review.durationSeconds,
            onSave = { reps ->
                vm.finishFreeSet(reps)
                freeSetReview = null
            },
            onDismiss = {
                freeSetReview = null
                vm.resume()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveTimerContent(
    state: TimerState,
    onTap: () -> Unit,
    onSkipRest: () -> Unit,
    onSkipRep: () -> Unit,
    onFinishOpenEndedRepSet: () -> Unit,
    onLongPress: () -> Unit
) {
    val phase = state.phase
    val isRestLike = phase is TimerPhase.RestPhase || phase is TimerPhase.RepRestPhase
    val isOpenEndedRepSet = when (phase) {
        is TimerPhase.ExercisePhase -> state.exercises.getOrNull(phase.exerciseIndex)?.isOpenEndedReps() == true
        is TimerPhase.RepRestPhase -> state.exercises.getOrNull(phase.exerciseIndex)?.isOpenEndedReps() == true
        else -> false
    }
    val canSkipRep = phase is TimerPhase.ExercisePhase &&
        state.exercises.getOrNull(phase.exerciseIndex)?.mode == ExerciseMode.REPS &&
        !isOpenEndedRepSet
    val info = when (phase) {
        is TimerPhase.ExercisePhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            val isReps = ex?.mode == ExerciseMode.REPS
            val isFree = ex?.mode == ExerciseMode.FREE
            val isOpenEndedReps = ex?.isOpenEndedReps() == true
            val targetReps = ex?.effectiveRepsPerSet() ?: 0
            val setInfo = "${phase.currentSet} / ${ex?.sets ?: 0} セット"
            TimerDisplayInfo(
                remaining = phase.remainingSeconds,
                exerciseName = ex?.name ?: "",
                phaseLabel = if (state.isPaused) "一時停止" else if (isFree) "フリー" else if (isOpenEndedReps) "限界まで" else "運動中",
                phaseColor = ExerciseOrange,
                setInfo = setInfo,
                repInfo = if (isReps) {
                    if (isOpenEndedReps) "${phase.currentRep}回目 / 限界" else "${phase.currentRep} / $targetReps レップ"
                } else {
                    null
                },
                totalSecs = if (isFree) 0 else ex?.durationSeconds ?: 1
            )
        }
        is TimerPhase.RepRestPhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            val isOpenEndedReps = ex?.isOpenEndedReps() == true
            val targetReps = ex?.effectiveRepsPerSet() ?: 0
            TimerDisplayInfo(
                remaining = phase.remainingSeconds,
                exerciseName = ex?.name ?: "",
                phaseLabel = "レップ間",
                phaseColor = RestBlue,
                setInfo = "${phase.currentSet} / ${ex?.sets ?: 0} セット",
                repInfo = if (isOpenEndedReps) "${phase.completedReps}回完了 / 限界" else "${phase.completedReps} / $targetReps レップ",
                totalSecs = ex?.repRestSeconds?.takeIf { it > 0 } ?: 1
            )
        }
        is TimerPhase.RestPhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            val isTransitionToNext = ex != null && phase.completedSets >= ex.sets
            val upcoming = if (isTransitionToNext) {
                state.exercises.getOrNull(phase.exerciseIndex + 1) ?: ex
            } else {
                ex
            }
            val upcomingSetNum = if (isTransitionToNext) 1 else phase.completedSets + 1
            TimerDisplayInfo(
                remaining = phase.remainingSeconds,
                exerciseName = upcoming?.name ?: "",
                phaseLabel = "休憩中",
                phaseColor = RestBlue,
                setInfo = "$upcomingSetNum / ${upcoming?.sets ?: 0} セット",
                repInfo = null,
                totalSecs = ex?.restSeconds ?: 1
            )
        }
        else -> TimerDisplayInfo(0, "", "", Color.Gray, "", null, 1)
    }
    val remaining = info.remaining
    val exerciseName = info.exerciseName
    val phaseLabel = info.phaseLabel
    val phaseColor = info.phaseColor
    val setInfo = info.setInfo
    val repInfo = info.repInfo
    val totalSecs = info.totalSecs

    val progress = if (totalSecs > 0) remaining.toFloat() / totalSecs else 1f
    val nextExercise = state.nextExercise

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            startAngle = 270f,
            indicatorColor = phaseColor,
            trackColor = SurfaceDark,
            strokeWidth = 8.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(setInfo, fontSize = 12.sp, color = TextSecondary)
            if (repInfo != null) {
                Text(repInfo, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(2.dp))
            Text(exerciseName, fontSize = 13.sp, color = TextSecondary)
            Text(phaseLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = phaseColor, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$remaining",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 56.sp
                )
                Text(" 秒", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (isOpenEndedRepSet) {
                FinishSetButton(onClick = onFinishOpenEndedRepSet)
            } else if (isRestLike) {
                SkipButton(onClick = onSkipRest)
            } else if (canSkipRep) {
                SkipButton(onClick = onSkipRep)
            } else if (nextExercise != null) {
                Text("次: ${nextExercise.name}", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun FinishSetButton(onClick: () -> Unit) {
    CompactButton(
        onClick = onClick,
        modifier = Modifier.size(width = 54.dp, height = 28.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange)
    ) {
        Text("終了", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    CompactButton(
        onClick = onClick,
        modifier = Modifier.size(width = 48.dp, height = 28.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
    ) {
        FastForwardIcon(
            modifier = Modifier.size(width = 20.dp, height = 14.dp),
            color = TextPrimary
        )
    }
}

@Composable
private fun FreeSetRecordDialog(
    durationSeconds: Int,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember(durationSeconds) { mutableStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("フリーセット", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(formatRecordDuration(durationSeconds), fontSize = 18.sp, color = ExerciseOrange, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(
                    onClick = { reps = (reps - 1).coerceAtLeast(0) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) {
                    Text("−", color = ExerciseOrange, fontSize = 16.sp)
                }
                Text(
                    text = if (reps > 0) "${reps}回" else "回数なし",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(56.dp)
                )
                CompactButton(
                    onClick = { reps += 1 },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange)
                ) {
                    Text("＋", color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactButton(
                    onClick = { onSave(null) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) {
                    Text("なし", fontSize = 11.sp, color = TextSecondary)
                }
                CompactButton(
                    onClick = { onSave(reps.takeIf { it > 0 }) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = ExerciseOrange)
                ) {
                    Text("保存", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FastForwardIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val gap = size.width * 0.08f
        val triangleWidth = (size.width - gap) / 2f

        fun triangle(startX: Float): Path = Path().apply {
            moveTo(startX, 0f)
            lineTo(startX + triangleWidth, size.height / 2f)
            lineTo(startX, size.height)
            close()
        }

        drawPath(triangle(0f), color)
        drawPath(triangle(triangleWidth + gap), color)
    }
}

@Composable
private fun AmbientTimerContent(state: TimerState) {
    val (remaining, phaseLabel) = when (val phase = state.phase) {
        is TimerPhase.ExercisePhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            phase.remainingSeconds to when {
                ex?.mode == ExerciseMode.FREE -> "フリー"
                ex?.isOpenEndedReps() == true -> "限界まで"
                else -> "運動中"
            }
        }
        is TimerPhase.RepRestPhase -> phase.remainingSeconds to "レップ間"
        is TimerPhase.RestPhase -> phase.remainingSeconds to "休憩中"
        else -> 0 to ""
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(phaseLabel, fontSize = 11.sp, color = Color(0xFF666666), letterSpacing = 2.sp)
            Text("$remaining", fontSize = 68.sp, fontWeight = FontWeight.Light, color = Color(0xFFDDDDDD), lineHeight = 64.sp)
            Text("秒", fontSize = 13.sp, color = Color(0xFF666666))
        }
    }
}

private data class TimerDisplayInfo(
    val remaining: Int,
    val exerciseName: String,
    val phaseLabel: String,
    val phaseColor: Color,
    val setInfo: String,
    val repInfo: String?,
    val totalSecs: Int
)

private data class FreeSetReview(
    val durationSeconds: Int
)

private fun formatRecordDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}分 ${seconds}秒" else "${seconds}秒"
}
