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
        combine(repository.histories, repository.pendingHealthConnectCount) { histories, pending ->
            UiState(histories = histories, pendingHealthConnect = pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** Health Connect 未書き込みぶんの再送を試みる（権限が無ければ何もしない）。 */
    fun retryHealthConnect() {
        viewModelScope.launch { runCatching { repository.writePendingHealthConnect() } }
    }
}
