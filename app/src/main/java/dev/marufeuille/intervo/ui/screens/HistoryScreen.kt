package dev.marufeuille.intervo.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.wear.compose.material.*
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.FreeSetRecord
import dev.marufeuille.intervo.data.WorkoutHistoryWithFreeSetRecords
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.ui.theme.CompletionGreen
import dev.marufeuille.intervo.ui.theme.TextPrimary
import dev.marufeuille.intervo.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.getInstance(app))
    val history: StateFlow<List<WorkoutHistoryWithFreeSetRecords>> = repo.recentHistoryWithFreeSetRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel()) {
    val history by vm.history.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)

    Scaffold(timeText = { TimeText() }) {
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("履歴なし", fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("ワークアウトを完了すると", fontSize = 11.sp, color = TextSecondary)
                    Text("ここに記録されます", fontSize = 11.sp, color = TextSecondary)
                }
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp)
            ) {
                item {
                    Text(
                        "履歴",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(history.size) { i ->
                    val item = history[i]
                    val h = item.history
                    val mins = h.totalSeconds / 60
                    val secs = h.totalSeconds % 60
                    val timeLabel = if (mins > 0) "${mins}分 ${secs}秒" else "${secs}秒"
                    val dateLabel = dateFormat.format(Date(h.completedAt))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(h.workoutName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(dateLabel, fontSize = 11.sp, color = TextSecondary)
                            Text("·", fontSize = 11.sp, color = TextSecondary)
                            Text(timeLabel, fontSize = 11.sp, color = CompletionGreen.copy(alpha = 0.8f))
                        }
                        if (item.freeSetRecords.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            item.freeSetRecords.sortedBy { it.sortOrder }.take(2).forEach { record ->
                                Text(
                                    freeSetRecordLabel(record),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            if (item.freeSetRecords.size > 2) {
                                Text(
                                    "他 ${item.freeSetRecords.size - 2}件",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun freeSetRecordLabel(record: FreeSetRecord): String {
    val minutes = record.durationSeconds / 60
    val seconds = record.durationSeconds % 60
    val duration = if (minutes > 0) "${minutes}分${seconds}秒" else "${seconds}秒"
    val reps = record.reps?.let { " / ${it}回" } ?: ""
    return "${record.exerciseName} ${record.setNumber}set $duration$reps"
}
