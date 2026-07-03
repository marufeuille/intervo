package dev.marufeuille.intervo.companion.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marufeuille.intervo.companion.di.companionViewModel
import dev.marufeuille.intervo.companion.social.SessionPostDraft
import dev.marufeuille.intervo.companion.ui.components.ChipKind
import dev.marufeuille.intervo.companion.ui.components.CompanionCard
import dev.marufeuille.intervo.companion.ui.components.SectionHeader
import dev.marufeuille.intervo.companion.ui.components.StatItem
import dev.marufeuille.intervo.companion.ui.components.StatusChip
import dev.marufeuille.intervo.companion.ui.components.formatDetailDateRange
import dev.marufeuille.intervo.companion.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    historyId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = companionViewModel { HistoryDetailViewModel(it.repository, historyId) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val title = (state as? HistoryDetailViewModel.UiState.Loaded)?.detail?.title ?: "詳細"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            HistoryDetailViewModel.UiState.Loading -> CenteredBox(Modifier.padding(padding)) {
                CircularProgressIndicator()
            }

            HistoryDetailViewModel.UiState.Missing -> CenteredBox(Modifier.padding(padding)) {
                Text("履歴が見つかりません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            is HistoryDetailViewModel.UiState.Loaded -> DetailContent(
                detail = s.detail,
                postDraft = s.postDraft,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun DetailContent(
    detail: WorkoutDetailUiModel,
    postDraft: SessionPostDraft,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(detail)
        PostDraftCard(postDraft)
        SyncCard(detail)
        SectionHeader("種目")
        detail.exercises.forEach { ExerciseCard(it) }
    }
}

@Composable
private fun SummaryCard(detail: WorkoutDetailUiModel) {
    CompanionCard(modifier = Modifier.fillMaxWidth(), verticalGap = 14) {
        Text(
            text = formatDetailDateRange(detail.completedAt, detail.totalSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem(formatDuration(detail.totalSeconds), "合計時間", Modifier.weight(1f))
            StatItem("${detail.exerciseCount}", "種目", Modifier.weight(1f))
            StatItem("${detail.totalPlannedSets}", "セット", Modifier.weight(1f))
        }
        if (detail.hasHeartRate) {
            HorizontalDivider()
            SectionHeader("心拍 (bpm)")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatItem(detail.startHr.bpmOrDash(), "開始", Modifier.weight(1f))
                StatItem(detail.avgHr.bpmOrDash(), "平均", Modifier.weight(1f))
                StatItem(detail.maxHr.bpmOrDash(), "最大", Modifier.weight(1f), accent = true)
            }
        }
    }
}

@Composable
private fun SyncCard(detail: WorkoutDetailUiModel) {
    CompanionCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "連携状況",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        StatusChip(
            text = if (detail.healthConnectWritten) "Health Connect 連携済み" else "Health Connect 未連携",
            kind = if (detail.healthConnectWritten) ChipKind.Done else ChipKind.Pending,
        )
        StatusChip(
            text = if (detail.pdsSynced) "PDS 同期済み" else "PDS 未同期",
            kind = if (detail.pdsSynced) ChipKind.Pds else ChipKind.Pending,
        )
    }
}

@Composable
private fun PostDraftCard(postDraft: SessionPostDraft) {
    val context = LocalContext.current
    val selectedPostIndex = remember(postDraft.sourceRef, postDraft.posts) { mutableIntStateOf(0) }
    val postCount = postDraft.posts.size
    val currentPost = postDraft.posts.getOrElse(selectedPostIndex.intValue) { "" }

    CompanionCard(modifier = Modifier.fillMaxWidth(), verticalGap = 12) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Bluesky 投稿",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (postCount > 1) {
                Text(
                    text = "${selectedPostIndex.intValue + 1}/$postCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                text = currentPost,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (postCount > 1) {
                TextButton(
                    onClick = { selectedPostIndex.intValue -= 1 },
                    enabled = selectedPostIndex.intValue > 0,
                ) {
                    Text("前へ")
                }
                TextButton(
                    onClick = { selectedPostIndex.intValue += 1 },
                    enabled = selectedPostIndex.intValue < postCount - 1,
                ) {
                    Text("次へ")
                }
            }
            Button(
                onClick = { context.sharePostText(currentPost) },
                enabled = currentPost.isNotBlank(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("共有")
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseDetail) {
    CompanionCard(modifier = Modifier.fillMaxWidth(), verticalGap = 8) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = exercise.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            exercise.mode?.let { mode ->
                Spacer(Modifier.width(8.dp))
                ModeBadge(mode)
            }
        }
        Text(
            text = plannedLabel(exercise),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = heartRateLabel(exercise),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeBadge(mode: ExerciseModeUi) {
    val (label, color) = when (mode) {
        ExerciseModeUi.TIMED -> "時間" to MaterialTheme.colorScheme.tertiary
        ExerciseModeUi.REPS -> "回数" to MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

private fun Int?.bpmOrDash(): String = this?.toString() ?: "—"

/** app 側 `REST_UNLIMITED = -1` と同値。companion は app 定数を参照できないためローカル定義。 */
private const val REST_UNLIMITED = -1

private fun plannedLabel(e: ExerciseDetail): String {
    val per = when {
        e.mode == ExerciseModeUi.REPS -> e.reps?.let { "${it}回" } ?: "上限なし"
        e.mode == ExerciseModeUi.TIMED -> e.durationSeconds?.let { "${it}秒" } ?: "時間無制限"
        else -> null
    }
    val head = listOfNotNull(e.sets?.let { "${it}セット" }, per).joinToString(" × ")
    val parts = buildList {
        if (head.isNotBlank()) add(head)
        when (e.restSeconds) {
            REST_UNLIMITED -> add("休憩 無制限")
            null -> {}
            else -> if (e.restSeconds > 0) add("休憩 ${e.restSeconds}秒")
        }
        when (e.repRestSeconds) {
            REST_UNLIMITED -> add("レップ間 無制限")
            null -> {}
            else -> if (e.repRestSeconds > 0) add("レップ間 ${e.repRestSeconds}秒")
        }
    }
    return if (parts.isEmpty()) "計画情報なし" else "計画  " + parts.joinToString("  ・  ")
}

private fun heartRateLabel(e: ExerciseDetail): String {
    if (e.startHr == null && e.endHr == null) return "心拍  —"
    return "心拍  ${e.startHr.bpmOrDash()} → ${e.endHr.bpmOrDash()} bpm"
}

private fun android.content.Context.sharePostText(text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(intent, "共有"))
}
