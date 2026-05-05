package com.example.interval.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import com.example.interval.timer.TimerPhase
import com.example.interval.timer.TimerState
import com.example.interval.timer.TimerViewModel
import com.example.interval.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    workoutId: String,
    onComplete: () -> Unit,
    onStop: () -> Unit,
    vm: TimerViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isAmbient by vm.isAmbient.collectAsStateWithLifecycle()
    var showStopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        vm.bindService()
        vm.start(workoutId)
    }

    LaunchedEffect(state.phase) {
        if (state.phase is TimerPhase.Complete) onComplete()
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
            onTap = { if (state.isPaused) vm.resume() else vm.pause() },
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveTimerContent(
    state: TimerState,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val phase = state.phase
    val (remaining, exerciseName, phaseLabel, phaseColor, setInfo, totalSecs) = when (phase) {
        is TimerPhase.ExercisePhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            TimerDisplayInfo(
                remaining = phase.remainingSeconds,
                exerciseName = ex?.name ?: "",
                phaseLabel = if (state.isPaused) "一時停止" else "運動中",
                phaseColor = ExerciseOrange,
                setInfo = "${phase.currentSet} / ${ex?.sets ?: 0} セット",
                totalSecs = ex?.durationSeconds ?: 1
            )
        }
        is TimerPhase.RestPhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            TimerDisplayInfo(
                remaining = phase.remainingSeconds,
                exerciseName = ex?.name ?: "",
                phaseLabel = "休憩中",
                phaseColor = RestBlue,
                setInfo = "${phase.completedSets} / ${ex?.sets ?: 0} セット",
                totalSecs = ex?.restSeconds ?: 1
            )
        }
        else -> TimerDisplayInfo(0, "", "", Color.Gray, "", 1)
    }

    val progress = if (totalSecs > 0) remaining.toFloat() / totalSecs else 0f

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
            Spacer(Modifier.height(4.dp))
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
        }
    }
}

@Composable
private fun AmbientTimerContent(state: TimerState) {
    val (remaining, phaseLabel) = when (val phase = state.phase) {
        is TimerPhase.ExercisePhase -> phase.remainingSeconds to "運動中"
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
    val totalSecs: Int
)
