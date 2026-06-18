package dev.marufeuille.intervo.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.companion.pds.PdsAccountSettings
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: CompanionRepository) : ViewModel() {

    val healthConnectAvailable: Boolean = repository.healthConnectAvailable

    val pendingPdsCount: StateFlow<Int> = repository.pendingPdsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _healthConnectPermitted = MutableStateFlow(false)
    val healthConnectPermitted: StateFlow<Boolean> = _healthConnectPermitted.asStateFlow()

    private val _healthConnectStatus = MutableStateFlow(
        if (repository.healthConnectAvailable) "Health Connect 連携可能" else "Health Connect が利用できません",
    )
    val healthConnectStatus: StateFlow<String> = _healthConnectStatus.asStateFlow()

    private val _pdsSettings = MutableStateFlow(repository.loadPdsSettings())
    val pdsSettings: StateFlow<PdsAccountSettings> = _pdsSettings.asStateFlow()

    private val _pdsStatus = MutableStateFlow(pdsStatusFor(_pdsSettings.value))
    val pdsStatus: StateFlow<String> = _pdsStatus.asStateFlow()

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

    fun savePdsSettings(serviceUrl: String, identifier: String, appPassword: String) {
        repository.savePdsSettings(
            serviceUrl = serviceUrl,
            identifier = identifier,
            appPassword = appPassword.takeIf { it.isNotBlank() },
        )
        refreshPdsSettings("PDS 設定を保存しました")
        // 設定済みになったら、過去に溜まった未同期ぶんを自動で送る（手動の再同期ボタンに頼らない）。
        if (repository.pdsConfigured) repository.scheduleSync()
    }

    fun clearPdsSettings() {
        repository.clearPdsSettings()
        refreshPdsSettings("PDS 設定を削除しました")
    }

    fun retryPds() {
        viewModelScope.launch {
            if (!repository.pdsConfigured) {
                refreshPdsSettings("PDS 設定が未完了です")
                return@launch
            }
            _pdsStatus.value = "PDS に再同期中..."
            val written = runCatching { repository.rewriteAllPds() }.getOrDefault(0)
            _pdsStatus.value = if (written > 0) {
                "${written}件を PDS に再同期しました"
            } else {
                "PDS 同期済み（新規なし）"
            }
        }
    }

    private fun refreshPdsSettings(status: String? = null) {
        val settings = repository.loadPdsSettings()
        _pdsSettings.value = settings
        _pdsStatus.value = status ?: pdsStatusFor(settings)
    }

    private fun pdsStatusFor(settings: PdsAccountSettings): String =
        if (settings.isConfigured) {
            "PDS 直接同期が利用できます"
        } else {
            "PDS URL、ハンドル、App Password を設定してください"
        }
}
