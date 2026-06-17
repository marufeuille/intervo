package dev.marufeuille.intervo.companion

import android.app.Application
import dev.marufeuille.intervo.companion.di.AppContainer

/**
 * アプリ全体で共有する依存（Repository など）を [AppContainer] にまとめて保持する。
 * Hilt は導入せず、手動 DI（ServiceLocator）で singleton を統一する。
 */
class CompanionApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
