package dev.marufeuille.intervo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.ui.theme.CompletionGreen
import dev.marufeuille.intervo.ui.theme.TextSecondary

@Composable
fun CompletionScreen(totalSeconds: Int, onDone: () -> Unit) {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeLabel = if (minutes > 0) "${minutes}分 ${seconds}秒" else "${seconds}秒"

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("✓", fontSize = 48.sp, color = CompletionGreen)
            Spacer(Modifier.height(4.dp))
            Text("完了！", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text("お疲れ様でした", fontSize = 13.sp, color = TextSecondary)
            if (totalSeconds > 0) {
                Spacer(Modifier.height(4.dp))
                Text(timeLabel, fontSize = 13.sp, color = CompletionGreen.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A3A1A)),
                modifier = Modifier.width(140.dp)
            ) {
                Text("閉じる", color = CompletionGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
