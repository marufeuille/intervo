package dev.marufeuille.intervo.companion.di

import android.app.Application
import dev.marufeuille.intervo.companion.sync.CompanionRepository

/**
 * アプリ全体の依存を保持する手動 DI コンテナ。
 * [CompanionRepository] を 1 インスタンスだけ生成し、画面/サービス間で共有する。
 * 将来 PDS 連携の依存もここへ集約する。
 */
class AppContainer(application: Application) {
    val repository: CompanionRepository by lazy { CompanionRepository(application) }
}
