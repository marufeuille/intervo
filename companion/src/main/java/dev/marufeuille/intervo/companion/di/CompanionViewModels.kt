package dev.marufeuille.intervo.companion.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.marufeuille.intervo.companion.CompanionApplication

/**
 * [AppContainer] から依存を受け取って ViewModel を生成する Compose ヘルパ。
 * 各画面は `val vm = companionViewModel { HistoryListViewModel(it.repository) }` のように使う。
 */
@Composable
inline fun <reified VM : ViewModel> companionViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as CompanionApplication).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
