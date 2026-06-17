package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.isDurationUnlimited
import dev.marufeuille.intervo.timer.TimerPhase
import dev.marufeuille.intervo.timer.TimerViewModel
import dev.marufeuille.intervo.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    workoutId: String,
    onComplete: (totalSeconds: Int) -> Unit,
    onStop: () -> Unit,
    isAmbient: Boolean = false,
    resume: Boolean = false,
    vm: TimerViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showStopDialog by remember { mutableStateOf(false) }
    var freeSetReview by remember { mutableStateOf<FreeSetReview?>(null) }

    LaunchedEffect(workoutId) {
        vm.bindService()
        vm.start(workoutId, resume)
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
                val exercisePhase = phase as? TimerPhase.ExercisePhase
                if (
                    exercisePhase != null &&
                    state.exercises.getOrNull(exercisePhase.exerciseIndex)?.isDurationUnlimited() == true
                ) {
                    vm.pause()
                    freeSetReview = FreeSetReview(durationSeconds = exercisePhase.remainingSeconds)
                } else if (state.isPaused) {
                    vm.resume()
                } else {
                    vm.pause()
                }
            },
            onSkipRest = { vm.skipRest() },
            onSkipRep = { vm.skipRep() },
            onFinishOpenEndedRepSet = { vm.finishOpenEndedRepSet() },
            onFinishCurrentSet = { vm.finishCurrentSet() },
            onLongPress = { showStopDialog = true }
        )
    }

    if (showStopDialog) {
        StopConfirmDialog(
            onDismiss = { showStopDialog = false },
            onConfirm = { vm.stop(); showStopDialog = false; onStop() }
        )
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

@Composable
private fun StopConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
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
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) { Text("続ける", fontSize = 11.sp, color = TextPrimary) }
                CompactButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) { Text("中断", fontSize = 11.sp, color = Color.White) }
            }
        }
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
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
                ) {
                    Text("再開", fontSize = 11.sp, color = TextPrimary)
                }
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

private data class FreeSetReview(
    val durationSeconds: Int
)

private fun formatRecordDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}分 ${seconds}秒" else "${seconds}秒"
}
