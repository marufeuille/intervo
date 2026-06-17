package dev.marufeuille.intervo.companion.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryDetailViewModel(
    repository: CompanionRepository,
    historyId: String,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Missing : UiState
        data class Loaded(val detail: WorkoutDetailUiModel) : UiState
    }

    val uiState: StateFlow<UiState> =
        repository.history(historyId)
            .map { history ->
                if (history == null) UiState.Missing
                else UiState.Loaded(WorkoutDetailMapper.map(history))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
