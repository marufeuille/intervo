package dev.marufeuille.intervo.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: CompanionRepository) : ViewModel() {

    val healthConnectAvailable: Boolean = repository.healthConnectAvailable

    private val _healthConnectPermitted = MutableStateFlow(false)
    val healthConnectPermitted: StateFlow<Boolean> = _healthConnectPermitted.asStateFlow()

    private val _healthConnectStatus = MutableStateFlow(
        if (repository.healthConnectAvailable) "Health Connect 連携可能" else "Health Connect が利用できません",
    )
    val healthConnectStatus: StateFlow<String> = _healthConnectStatus.asStateFlow()

    init {
        refreshHealthConnect()
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            _healthConnectPermitted.value =
                runCatching { repository.healthConnectPermitted() }.getOrDefault(false)
        }
    }

    /** 権限要求ダイアログの結果を受けて、許可されていれば未書き込み履歴を Health Connect に流す。 */
    fun onHealthConnectPermissionResult() {
        viewModelScope.launch {
            val permitted = runCatching { repository.healthConnectPermitted() }.getOrDefault(false)
            _healthConnectPermitted.value = permitted
            if (!permitted) {
                _healthConnectStatus.value = "Health Connect の権限が許可されませんでした"
                return@launch
            }
            _healthConnectStatus.value = "Health Connect に書き込み中..."
            val written = runCatching { repository.writePendingHealthConnect() }.getOrDefault(0)
            _healthConnectStatus.value = if (written > 0) {
                "${written}件を Health Connect に書き込みました"
            } else {
                "Health Connect 連携済み（新規なし）"
            }
        }
    }
}
