package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.effectiveRepsPerSet
import dev.marufeuille.intervo.data.isDurationUnlimited
import dev.marufeuille.intervo.data.isOpenEndedReps
import dev.marufeuille.intervo.timer.TimerPhase
import dev.marufeuille.intervo.timer.TimerState
import dev.marufeuille.intervo.ui.theme.*

/** 休憩/レップのスキップボタン（アイコンのみでテキストが無いため E2E 用に testTag を付与） */
internal const val SKIP_BUTTON_TAG = "timerSkipButton"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ActiveTimerContent(
    state: TimerState,
    onTap: () -> Unit,
    onSkipRest: () -> Unit,
    onSkipRep: () -> Unit,
    onFinishOpenEndedRepSet: () -> Unit,
    onFinishCurrentSet: () -> Unit,
    onLongPress: () -> Unit
) {
    val phase = state.phase
    val isRestLike = phase is TimerPhase.RestPhase || phase is TimerPhase.RepRestPhase
    val currentExercise = when (phase) {
        is TimerPhase.ExercisePhase -> state.exercises.getOrNull(phase.exerciseIndex)
        is TimerPhase.RepRestPhase -> state.exercises.getOrNull(phase.exerciseIndex)
        is TimerPhase.RestPhase -> state.exercises.getOrNull(phase.exerciseIndex)
        else -> null
    }
    val isOpenEndedRepSet = when (phase) {
        is TimerPhase.ExercisePhase -> currentExercise?.isOpenEndedReps() == true
        is TimerPhase.RepRestPhase -> currentExercise?.isOpenEndedReps() == true
        else -> false
    }
    val canSkipRep = phase is TimerPhase.ExercisePhase &&
        currentExercise?.mode == ExerciseMode.REPS &&
        !isOpenEndedRepSet
    val canFinishFixedRepSet = (phase is TimerPhase.ExercisePhase || phase is TimerPhase.RepRestPhase) &&
        currentExercise?.mode == ExerciseMode.REPS &&
        !isOpenEndedRepSet
    val canFinishTimedSet = phase is TimerPhase.ExercisePhase &&
        currentExercise?.mode == ExerciseMode.TIMED &&
        currentExercise?.isDurationUnlimited() == false
    val info = timerDisplayInfo(state)
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
            state.currentHeartRate?.let { hr ->
                Text(
                    text = "♥ $hr",
                    fontSize = 13.sp,
                    color = ExerciseOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isOpenEndedRepSet) {
                FinishSetButton(onClick = onFinishOpenEndedRepSet)
            } else if (canSkipRep && canFinishFixedRepSet) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkipButton(onClick = onSkipRep)
                    FinishSetButton(onClick = onFinishCurrentSet)
                }
            } else if (phase is TimerPhase.RepRestPhase && canFinishFixedRepSet) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkipButton(onClick = onSkipRest)
                    FinishSetButton(onClick = onFinishCurrentSet)
                }
            } else if (isRestLike) {
                SkipButton(onClick = onSkipRest)
            } else if (canFinishTimedSet) {
                FinishSetButton(onClick = onFinishCurrentSet)
            } else if (nextExercise != null) {
                Text("次: ${nextExercise.name}", fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

/** 現在のフェーズから画面表示用の情報（残り秒・ラベル・色・進捗分母）を組み立てる。 */
private fun timerDisplayInfo(state: TimerState): TimerDisplayInfo = when (val phase = state.phase) {
    is TimerPhase.ExercisePhase -> {
        val ex = state.exercises.getOrNull(phase.exerciseIndex)
        val isReps = ex?.mode == ExerciseMode.REPS
        val isFree = ex?.isDurationUnlimited() == true
        val isOpenEndedReps = ex?.isOpenEndedReps() == true
        val targetReps = ex?.effectiveRepsPerSet() ?: 0
        TimerDisplayInfo(
            remaining = phase.remainingSeconds,
            exerciseName = ex?.name ?: "",
            phaseLabel = if (state.isPaused) "一時停止" else if (isFree) "フリー" else if (isOpenEndedReps) "限界まで" else "運動中",
            phaseColor = ExerciseOrange,
            setInfo = "${phase.currentSet} / ${ex?.sets ?: 0} セット",
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
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .testTag(SKIP_BUTTON_TAG),
        colors = ButtonDefaults.buttonColors(backgroundColor = ButtonDark)
    ) {
        FastForwardIcon(
            modifier = Modifier.size(width = 20.dp, height = 14.dp),
            color = TextPrimary
        )
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
internal fun AmbientTimerContent(state: TimerState) {
    val (remaining, phaseLabel) = when (val phase = state.phase) {
        is TimerPhase.ExercisePhase -> {
            val ex = state.exercises.getOrNull(phase.exerciseIndex)
            phase.remainingSeconds to when {
                ex?.isDurationUnlimited() == true -> "フリー"
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

internal data class TimerDisplayInfo(
    val remaining: Int,
    val exerciseName: String,
    val phaseLabel: String,
    val phaseColor: Color,
    val setInfo: String,
    val repInfo: String?,
    val totalSecs: Int
)
