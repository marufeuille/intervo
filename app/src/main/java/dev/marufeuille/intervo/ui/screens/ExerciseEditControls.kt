package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.ui.theme.*

@Composable
internal fun ModeToggle(
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
internal fun DurationTargetToggle(
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
internal fun RepsTargetToggle(
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
internal fun RestTargetToggle(
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
        Text("休憩", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModePill(label = "指定", selected = !unlimited, accentColor = RestBlue) { onChange(false) }
            ModePill(label = "自由", selected = unlimited, accentColor = RestBlue) { onChange(true) }
        }
    }
}

@Composable
internal fun RepRestTargetToggle(
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
        Text("間休憩", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModePill(label = "指定", selected = !unlimited, accentColor = RestBlue) { onChange(false) }
            ModePill(label = "自由", selected = unlimited, accentColor = RestBlue) { onChange(true) }
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    accentColor: Color = ExerciseOrange,
    onClick: () -> Unit
) {
    val bg = if (selected) accentColor else ButtonDark
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
internal fun StepperRow(
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
