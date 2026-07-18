package dev.marufeuille.intervo.companion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.marufeuille.intervo.companion.data.CompanionDatabase
import dev.marufeuille.intervo.companion.data.CompanionWorkoutHistory
import dev.marufeuille.intervo.companion.sync.CompanionRepository
import dev.marufeuille.intervo.companion.ui.CompanionTheme
import dev.marufeuille.intervo.companion.ui.navigation.CompanionNavHost
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * companion（スマホアプリ）のシナリオベース E2E（instrumented）。
 *
 * app モジュールの [WorkoutCreationE2ETest] と同じ方針: MainActivity ではなく空の
 * [ComponentActivity] に [CompanionNavHost] を直接載せ、起動時の権限ダイアログを挟まずに
 * UI 遷移を検証する。`companionViewModel` が `LocalContext` 経由で `CompanionApplication`
 * を取得するため、ComponentActivity ホストでも実 Repository（実 DB）がそのまま繋がる。
 *
 * 各 @Test の前に Room を `clearAllTables()` して決定的にする。Health Connect の実際の通信を
 * 伴うジャーニーは本スコープ外（CompanionRepository が外部依存を by lazy で直 new しており
 * 差し替え不可）。別 PR で扱う。
 *
 * ナビゲーションはボトム NavBar のタブ（履歴/プラン/設定）。各タブ label は画面タイトルと
 * 同名で `onNodeWithText` が複数ヒットするため、`hasClickAction()` で NavBar 側を絞り込む。
 */
@RunWith(AndroidJUnit4::class)
class CompanionE2ETest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val db get() = CompanionDatabase.getInstance(targetContext)

    @Before
    fun resetState() {
        db.clearAllTables()
    }

    private fun launchApp() {
        compose.setContent { CompanionTheme { CompanionNavHost() } }
    }

    /** テキストが表示されるまで待つ（DB→Flow 反映や ViewModel の stateIn は非同期なため）。 */
    private fun awaitText(text: String, substring: Boolean = false, timeoutMillis: Long = 10_000) {
        compose.waitUntil(timeoutMillis = timeoutMillis) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** NavBar のタブを開く。タブ label は画面タイトルと同名なので hasClickAction で絞り込む。 */
    private fun openTab(label: String) {
        compose.onAllNodes(hasText(label) and hasClickAction())[0].performClick()
    }

    @Test
    fun 履歴がないときは空状態が表示される() {
        launchApp()
        awaitText("まだ履歴がありません")
        compose.onNodeWithText("まだ履歴がありません").assertIsDisplayed()
    }

    @Test
    fun 履歴をシードすると一覧に出て詳細で確認できる() {
        runBlocking {
            CompanionRepository(targetContext).receive(
                CompanionWorkoutHistory(
                    id = "e2e-001",
                    workoutId = "w-001",
                    workoutName = "テストワークアウト",
                    completedAt = COMPLETED_AT,
                    totalSeconds = 1800,
                    exerciseCount = 3,
                )
            )
        }
        launchApp()

        awaitText("テストワークアウト")
        compose.onNodeWithText("テストワークアウト").assertIsDisplayed()
        compose.onNodeWithText("Health Connect 未連携").assertIsDisplayed()

        // カードをタップ → 詳細画面へ
        compose.onNodeWithText("テストワークアウト").performClick()

        // SummaryCard の StatItem ラベル（Loaded になるまで待つ）
        awaitText("合計時間")
        compose.onNodeWithText("合計時間").assertIsDisplayed()
        compose.onNodeWithText("セット").assertIsDisplayed()

        // 戻る IconButton → 一覧へ（履歴があるので「最近のワークアウト」見出しが出る）
        compose.onNodeWithContentDescription("戻る").performClick()
        awaitText("最近のワークアウト")
    }

    @Test
    fun 設定タブでHealthConnectカードが表示される() {
        launchApp()
        openTab("設定")

        awaitText("Health Connect")
        compose.onNodeWithText("Health Connect").assertIsDisplayed()
    }

    private companion object {
        // 決定的な日時。表示フォーマットの検証ではなく存在/遷移の検証が目的なので固定値。
        private const val COMPLETED_AT = 1_700_000_000_000L
    }
}
