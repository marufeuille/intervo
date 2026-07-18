package dev.marufeuille.intervo.companion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** ステータスチップの種別。色はテーマ＋固定アンバーから導出（ダークでも破綻しないよう薄い tint）。 */
enum class ChipKind { Done, Pending }

@Composable
fun StatusChip(text: String, kind: ChipKind) {
    val color = when (kind) {
        ChipKind.Done -> MaterialTheme.colorScheme.secondary
        ChipKind.Pending -> Color(0xFFB45309)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** モック準拠の白カード（角丸16＋淡い影）。中身は呼び出し側が縦並びで積む。 */
@Composable
fun CompanionCard(
    modifier: Modifier = Modifier,
    verticalGap: Int = 10,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(verticalGap.dp),
            content = content,
        )
    }
}

/** 「値＋ラベル」の統計セル（合計時間・種目数・心拍など）。 */
@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** セクション見出し（例: 連携 / 種目）。 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
