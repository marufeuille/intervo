package dev.marufeuille.intervo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.ui.navigation.AppNavigation
import dev.marufeuille.intervo.ui.theme.IntervalTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * シナリオベース E2E（instrumented）。
 *
 * MainActivity ではなく空の ComponentActivity に AppNavigation を載せることで、
 * 起動時の権限ダイアログを挟まずに UI 遷移そのものを検証する。
 * 実 ViewModel・Room（interval.db）はそのまま動くので、データ層まで通したエンドツーエンド。
 *
 * 各 @Test の前に Room を clearAllTables() で初期化し、テストを決定的にしている。
 * Wear の ScalingLazyColumn は performScrollTo() 非対応なので、下方の要素は
 * onRoot().performTouchInput { swipeUp() } でスクロールしてからタップする。
 */
@RunWith(AndroidJUnit4::class)
class WorkoutCreationE2ETest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val db get() = AppDatabase.getInstance(
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    @Before
    fun resetDatabase() {
        // 各テストを決定的にするため、毎回 Room を空にする（シングルトン跨ぎでも確実）
        db.clearAllTables()
    }

    private fun launchApp() {
        compose.setContent { IntervalTheme { AppNavigation() } }
    }

    /** テキストが表示されるまで待つヘルパー（DB→Flow 反映は非同期なため） */
    private fun awaitText(text: String, substring: Boolean = false) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun シードしたワークアウトが一覧に出て詳細でスタートできる() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val workout = repo.addWorkout("胸トレ")
            repo.addExercise(
                workoutId = workout.id,
                name = "腕立て",
                mode = ExerciseMode.TIMED,
                durationSeconds = 30,
                sets = 3,
                restSeconds = 15,
                repsPerSet = 1,
                repRestSeconds = 0,
            )
        }

        launchApp()

        awaitText("胸トレ")
        compose.onNodeWithText("胸トレ").assertIsDisplayed()

        // ワークアウトをタップ → 詳細画面へ
        compose.onNodeWithText("胸トレ").performClick()

        // 種目が読み込まれると「▶ スタート」が出る
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).assertIsDisplayed()
    }

    @Test
    fun 履歴が空のときは履歴なしと表示される() {
        launchApp()

        compose.onNodeWithText("ワークアウト").assertIsDisplayed()

        // 「履歴」チップは画面下にあるので、スワイプして表示させてからタップ
        compose.onRoot().performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("履歴").performClick()

        awaitText("履歴なし")
        compose.onNodeWithText("履歴なし").assertIsDisplayed()
    }

    @Test
    fun UIだけでワークアウトを作成すると詳細画面に名前が反映される() {
        launchApp()

        compose.onNodeWithText("ワークアウト").assertIsDisplayed()

        // 「+ 追加して始めよう」→ 追加画面
        compose.onNodeWithText("追加して始めよう", substring = true).performClick()
        compose.onNodeWithText("ワークアウトを追加").assertIsDisplayed()

        // 名前入力ダイアログを開いて入力 → 確定
        compose.onNodeWithText("名前を入力...").performClick()
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("背中トレ")
        compose.onNodeWithText("✓").performClick()

        // 保存 → 詳細画面へ（navigate は Main 上で行われるので落ちない）
        compose.onNodeWithText("保存").performClick()

        awaitText("背中トレ")
        compose.onNodeWithText("背中トレ").assertIsDisplayed()
    }

    @Test
    fun ワークアウトを長押しで削除すると一覧から消える() {
        runBlocking { WorkoutRepository(db).addWorkout("削除対象") }

        launchApp()

        awaitText("削除対象")

        // 行を長押し → 削除確認ダイアログ → 「削除」
        compose.onNodeWithText("削除対象").performTouchInput { longClick() }
        awaitText("削除しますか？")
        compose.onNodeWithText("削除").performClick()

        // 一覧から消え、選択画面に留まっている
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("削除対象").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("ワークアウト").assertIsDisplayed()
    }
}
