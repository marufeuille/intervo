package dev.marufeuille.intervo.companion.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.marufeuille.intervo.companion.ui.detail.HistoryDetailScreen
import dev.marufeuille.intervo.companion.ui.history.HistoryListScreen
import dev.marufeuille.intervo.companion.ui.settings.SettingsScreen

/** ボトムナビのトップレベル目的地。ここに 4 つ目（ワークアウト）を足せば拡張できる。 */
enum class CompanionTab(val label: String, val icon: ImageVector) {
    History("履歴", Icons.Rounded.Home),
    Settings("設定", Icons.Rounded.Settings),
}

/**
 * 自前の軽量ナビゲーション。2 タブ＋詳細 push のみなので navigation-compose は使わず、
 * タブ状態と詳細 ID の保持＋BackHandler で構成する。詳細表示中はボトムナビを隠す。
 */
@Composable
fun CompanionNavHost() {
    var currentTab by rememberSaveable { mutableStateOf(CompanionTab.History) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = detailId != null) { detailId = null }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val openDetailId = detailId
            if (openDetailId != null) {
                HistoryDetailScreen(
                    historyId = openDetailId,
                    onBack = { detailId = null },
                )
            } else {
                when (currentTab) {
                    CompanionTab.History -> HistoryListScreen(onOpenDetail = { detailId = it })
                    CompanionTab.Settings -> SettingsScreen()
                }
            }
        }
        if (detailId == null) {
            NavigationBar {
                CompanionTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    }
}
