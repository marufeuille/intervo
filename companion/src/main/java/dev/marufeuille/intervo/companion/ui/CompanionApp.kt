package dev.marufeuille.intervo.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CompanionApp(vm: CompanionViewModel = viewModel()) {
    val histories by vm.histories.collectAsStateWithLifecycle()
    val pendingCount by vm.pendingCount.collectAsStateWithLifecycle()
    val endpoint by vm.endpoint.collectAsStateWithLifecycle()
    val isSyncing by vm.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val authUid by vm.authUid.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Header(pendingCount = pendingCount)
            }
            item {
                SyncPanel(
                    endpoint = endpoint,
                    pendingCount = pendingCount,
                    isSyncing = isSyncing,
                    statusMessage = statusMessage,
                    authUid = authUid,
                    onEndpointChange = vm::onEndpointChange,
                    onSaveEndpoint = vm::saveEndpoint,
                    onPrepareAuth = vm::prepareAuth,
                    onSync = vm::syncNow,
                )
            }
            item {
                Text(
                    text = "履歴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (histories.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(histories, key = { it.id }) { history ->
                    HistoryRow(history = history)
                }
            }
        }
    }
}

@Composable
private fun Header(pendingCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Intervo Companion",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (pendingCount > 0) "未送信 $pendingCount 件" else "BigQuery 同期済み",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SyncPanel(
    endpoint: String,
    pendingCount: Int,
    isSyncing: Boolean,
    statusMessage: String,
    authUid: String,
    onEndpointChange: (String) -> Unit,
    onSaveEndpoint: () -> Unit,
    onPrepareAuth: () -> Unit,
    onSync: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "BigQuery 送信",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("取り込み API URL") },
                placeholder = { Text("https://.../workout-history") }
            )
            Text(
                text = if (authUid.isBlank()) "Firebase UID 未取得" else "Firebase UID: $authUid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onSaveEndpoint) {
                    Text("保存")
                }
                TextButton(onClick = onPrepareAuth) {
                    Text("認証")
                }
                Button(
                    onClick = onSync,
                    enabled = !isSyncing && pendingCount > 0
                ) {
                    Text(if (isSyncing) "送信中" else "送信")
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "まだ履歴がありません",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ウォッチでワークアウトを完了すると、ここに表示されます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(history: CompanionWorkoutHistory) {
    val dateLabel = DateFormatter.format(Instant.ofEpochMilli(history.completedAt))
    val duration = formatDuration(history.totalSeconds)
    val isStopped = history.syncedAt == null &&
        history.syncAttempts >= CompanionRepository.MAX_AUTO_SYNC_ATTEMPTS
    val syncLabel = when {
        history.syncedAt != null -> "送信済み"
        isStopped -> "停止"
        history.syncAttempts > 0 -> "再試行待ち"
        else -> "未送信"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateLabel / $duration / ${history.exerciseCount}種目",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = syncLabel,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    history.syncedAt != null -> MaterialTheme.colorScheme.secondary
                    isStopped -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        if (history.syncError != null) {
            val errorPrefix = if (isStopped) {
                "${history.syncAttempts}回失敗。自動再送を停止: "
            } else {
                "${history.syncAttempts}回失敗: "
            }
            Text(
                text = errorPrefix + history.syncError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider()
    }
}

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.JAPAN)
        .withZone(ZoneId.systemDefault())

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}分 ${seconds}秒" else "${seconds}秒"
}
