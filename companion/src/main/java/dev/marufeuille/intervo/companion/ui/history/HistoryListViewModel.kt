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
    )

    val uiState: StateFlow<UiState> =
        combine(
            repository.histories,
            repository.pendingHealthConnectCount,
        ) { histories, pendingHealthConnect ->
            UiState(
                histories = histories,
                pendingHealthConnect = pendingHealthConnect,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** Health Connect へ未同期ぶんを再同期する。 */
    fun retrySync() {
        viewModelScope.launch {
            runCatching { repository.writePendingHealthConnect() }
        }
    }
}
