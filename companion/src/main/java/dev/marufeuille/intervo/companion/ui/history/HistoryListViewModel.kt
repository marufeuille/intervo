package dev.marufeuille.intervo.companion.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryListViewModel(private val repository: CompanionRepository) : ViewModel() {

    data class UiState(
        val histories: List<CompanionWorkoutHistory> = emptyList(),
        val pendingHealthConnect: Int = 0,
        val pendingPds: Int = 0,
    )

    val uiState: StateFlow<UiState> =
        combine(
            repository.histories,
            repository.pendingHealthConnectCount,
            repository.pendingPdsCount,
        ) { histories, pendingHealthConnect, pendingPds ->
            UiState(
                histories = histories,
                pendingHealthConnect = pendingHealthConnect,
                pendingPds = pendingPds,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 未同期ぶんの再送を試みる（権限/設定が無ければ何もしない）。 */
    fun retrySync() {
        viewModelScope.launch {
            runCatching { repository.writePendingHealthConnect() }
            runCatching { repository.writePendingPds() }
        }
    }
}
