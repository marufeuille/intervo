package dev.marufeuille.intervo.companion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompanionViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = CompanionRepository(app)

    val histories: StateFlow<List<CompanionWorkoutHistory>> = repository.histories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _endpoint = MutableStateFlow(repository.ingestEndpoint)
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow("ウォッチの完了履歴を待機中")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _authUid = MutableStateFlow(repository.currentUid.orEmpty())
    val authUid: StateFlow<String> = _authUid.asStateFlow()

    fun onEndpointChange(value: String) {
        _endpoint.value = value
    }

    fun saveEndpoint() {
        repository.ingestEndpoint = _endpoint.value
        _statusMessage.value = "送信先 URL を保存しました"
    }

    fun prepareAuth() {
        viewModelScope.launch {
            _statusMessage.value = "Firebase 認証中..."
            runCatching {
                repository.authenticate()
            }.onSuccess { headers ->
                _authUid.value = headers.uid
                _statusMessage.value = "Firebase UID を取得しました"
            }.onFailure { error ->
                _statusMessage.value = error.message ?: error::class.java.simpleName
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "同期中..."
            val result = repository.syncPending()
            _authUid.value = repository.currentUid.orEmpty()
            _statusMessage.value = when {
                result.skipped -> result.message ?: "同期をスキップしました"
                result.failed > 0 -> "${result.uploaded}件送信、${result.failed}件失敗: ${result.message.orEmpty()}"
                result.uploaded > 0 -> "${result.uploaded}件を送信しました"
                else -> "未送信データはありません"
            }
            _isSyncing.value = false
        }
    }
}
