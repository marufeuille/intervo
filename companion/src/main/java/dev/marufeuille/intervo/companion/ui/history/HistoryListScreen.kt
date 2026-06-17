package dev.marufeuille.intervo.companion.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.di.companionViewModel
import dev.marufeuille.intervo.companion.ui.components.ChipKind
import dev.marufeuille.intervo.companion.ui.components.CompanionCard
import dev.marufeuille.intervo.companion.ui.components.SectionHeader
import dev.marufeuille.intervo.companion.ui.components.StatusChip
import dev.marufeuille.intervo.companion.ui.components.formatDuration
import dev.marufeuille.intervo.companion.ui.components.formatHistoryDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListScreen(
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = companionViewModel { HistoryListViewModel(it.repository) }
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("履歴") },
                actions = {
                    IconButton(onClick = vm::retrySync) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "連携を再同期")
                    }
                },
            )
        },
    ) { padding ->
        if (state.histories.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SectionHeader("最近のワークアウト") }
                items(state.histories, key = { it.id }) { history ->
                    HistoryCard(history = history, onClick = { onOpenDetail(history.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(history: CompanionWorkoutHistory, onClick: () -> Unit) {
    CompanionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalGap = 8,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = history.workoutName.ifBlank { "ワークアウト" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatHistoryDate(history.completedAt)} ・ ${formatDuration(history.totalSeconds)} ・ ${history.exerciseCount}種目",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val hcWritten = history.healthConnectWrittenAt != null
        StatusChip(
            text = if (hcWritten) "Health Connect 連携済み" else "Health Connect 未連携",
            kind = if (hcWritten) ChipKind.Done else ChipKind.Pending,
        )
        val pdsSynced = history.pdsSyncedAt != null
        StatusChip(
            text = if (pdsSynced) "PDS 同期済み" else "PDS 未同期",
            kind = if (pdsSynced) ChipKind.Pds else ChipKind.Pending,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "まだ履歴がありません",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "ウォッチでワークアウトを完了すると、ここに表示されます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
