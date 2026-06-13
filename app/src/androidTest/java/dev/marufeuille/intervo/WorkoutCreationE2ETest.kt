package dev.marufeuille.intervo

import android.content.Intent
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.DURATION_UNLIMITED
import dev.marufeuille.intervo.data.ExerciseMode
import dev.marufeuille.intervo.data.WorkoutRepository
import dev.marufeuille.intervo.timer.TimerPhase
import dev.marufeuille.intervo.timer.TimerService
import dev.marufeuille.intervo.timer.TimerSnapshot
import dev.marufeuille.intervo.timer.TimerSnapshotStore
import dev.marufeuille.intervo.timer.TimerState
import dev.marufeuille.intervo.ui.navigation.AppNavigation
import dev.marufeuille.intervo.ui.screens.SKIP_BUTTON_TAG
import dev.marufeuille.intervo.ui.theme.IntervalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
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

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** TimerService を停止し、runningWorkoutId が null になるまで待つ（リーク連鎖の防止） */
    private fun stopTimerAndWait() {
        val ctx = targetContext
        ctx.startService(Intent(ctx, TimerService::class.java).setAction(TimerService.ACTION_STOP))
        val deadline = System.currentTimeMillis() + 3_000
        while (TimerService.runningWorkoutId.value != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
    }

    @Before
    fun resetState() {
        // 直前のテスト/前回実行が残したタイマー・スナップショットを確実に消して決定的にする
        stopTimerAndWait()
        TimerSnapshotStore(targetContext).clear()
        db.clearAllTables()
    }

    @After
    fun tearDown() {
        // テストが中断前に失敗してもサービスを残さない（次テストの自動再開を防ぐ）
        stopTimerAndWait()
    }

    private fun launchApp() {
        compose.setContent { IntervalTheme { AppNavigation() } }
    }

    /** テキストが表示されるまで待つヘルパー（DB→Flow 反映やタイマー進行は非同期なため）。
     *  スイート負荷時の StateFlow 初回 emit に余裕を持たせるため既定 10 秒。 */
    private fun awaitText(text: String, substring: Boolean = false, timeoutMillis: Long = 10_000) {
        compose.waitUntil(timeoutMillis = timeoutMillis) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** タイマー画面の中央をタップ（onTap = 一時停止/再開のトグル） */
    private fun tapTimer() {
        compose.onRoot().performTouchInput { click() }
    }

    /** 長押し → 「中断」でタイマーを止めて一覧へ戻る */
    private fun abortTimer() {
        compose.onRoot().performTouchInput { longClick() }
        awaitText("中断しますか？")
        compose.onNodeWithText("中断").performClick()
        awaitText("ワークアウト")
    }

    /** 桁のみ（残り秒）の Text を表すマッチャ。セット表記等は "/" を含むため除外される */
    private val digitsOnlyText = SemanticsMatcher("text is digits only") { node ->
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
        text != null && text.matches(Regex("\\d+"))
    }

    /** 画面上に出ている残り秒を読む（複数桁ノードがあれば最大値＝残り秒） */
    private fun readRemaining(): Int? =
        compose.onAllNodes(digitsOnlyText, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.toIntOrNull()
            }
            .maxOrNull()

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

    @Test
    fun ワークアウトを開始して中断すると一覧に戻る() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val workout = repo.addWorkout("脚トレ")
            repo.addExercise(
                workoutId = workout.id,
                name = "スクワット",
                mode = ExerciseMode.TIMED,
                durationSeconds = 60,
                sets = 2,
                restSeconds = 10,
                repsPerSet = 1,
                repRestSeconds = 0,
            )
        }

        launchApp()

        // 一覧 → 詳細 → スタート
        awaitText("脚トレ")
        compose.onNodeWithText("脚トレ").performClick()
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        // タイマー画面で種目名が出る＝TimerService が起動してエクササイズ中（権限が無くても
        // promoteToForeground は runCatching で握られ、カウントダウンは継続する）
        awaitText("スクワット")

        // 長押し → 中断ダイアログ → 「中断」
        compose.onRoot().performTouchInput { longClick() }
        awaitText("中断しますか？")
        compose.onNodeWithText("中断").performClick()

        // 一覧に戻る（stop() が同期で runningWorkoutId を null にするので自動再開しない）
        awaitText("ワークアウト")
        compose.onNodeWithText("ワークアウト").assertIsDisplayed()
    }

    @Test
    fun 短いワークアウトを完走すると完了画面が出て履歴に残る() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val workout = repo.addWorkout("朝ルーティン")
            // すぐ終わるよう 1 秒・1 セットの種目にする
            repo.addExercise(
                workoutId = workout.id,
                name = "深呼吸",
                mode = ExerciseMode.TIMED,
                durationSeconds = 1,
                sets = 1,
                restSeconds = 0,
                repsPerSet = 1,
                repRestSeconds = 0,
            )
        }

        launchApp()

        awaitText("朝ルーティン")
        compose.onNodeWithText("朝ルーティン").performClick()
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        // 数秒で完走 → 完了画面
        awaitText("完了", substring = true, timeoutMillis = 15_000)
        compose.onNodeWithText("閉じる").performClick()

        // 一覧に戻る → 履歴を開くと完了した記録が残っている
        awaitText("ワークアウト")
        compose.onRoot().performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("履歴").performClick()
        awaitText("朝ルーティン")
        compose.onNodeWithText("朝ルーティン").assertIsDisplayed()
    }

    @Test
    fun タイマーをタップで一時停止と再開ができる() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val w = repo.addWorkout("体幹")
            repo.addExercise(w.id, "プランク", ExerciseMode.TIMED, 60, 1, 0, 1, 0)
        }

        launchApp()
        awaitText("体幹")
        compose.onNodeWithText("体幹").performClick()
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        awaitText("運動中")
        tapTimer()
        awaitText("一時停止")
        tapTimer()
        awaitText("運動中")

        abortTimer()
    }

    @Test
    fun 運動中は残り秒がカウントダウンする() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val w = repo.addWorkout("ストレッチ")
            repo.addExercise(w.id, "前屈", ExerciseMode.TIMED, 60, 1, 0, 1, 0)
        }

        launchApp()
        awaitText("ストレッチ")
        compose.onNodeWithText("ストレッチ").performClick()
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        awaitText("運動中")
        val before = readRemaining() ?: error("残り秒が読み取れない")
        // 残り秒が減るまで待つ（減らなければ waitUntil がタイムアウトして失敗）
        compose.waitUntil(timeoutMillis = 5_000) { (readRemaining() ?: before) < before }
        val after = readRemaining() ?: before
        assertTrue("残り秒が減るはず: $before -> $after", after < before)

        abortTimer()
    }

    @Test
    fun 休憩をスキップすると次の種目へ進む() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val w = repo.addWorkout("サーキット")
            // 1 種目目はすぐ終わる（→休憩へ）、2 種目目は長く滞在させる
            repo.addExercise(w.id, "ジャンプ", ExerciseMode.TIMED, 1, 1, 20, 1, 0)
            repo.addExercise(w.id, "ランジ", ExerciseMode.TIMED, 60, 1, 0, 1, 0)
        }

        launchApp()
        awaitText("サーキット")
        compose.onNodeWithText("サーキット").performClick()
        // 2 種目あると「スタート」は一覧の下方にあり ScalingLazyColumn が未合成のことがある。
        // 種目が見えたらスクロールして「スタート」を出してからタップする。
        awaitText("ランジ")
        compose.onRoot().performTouchInput { swipeUp() }
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        // 1 種目目が完了 → 休憩中
        awaitText("休憩中", timeoutMillis = 15_000)
        // スキップボタン（アイコンのみ＝testTag）で休憩を飛ばす
        compose.onNodeWithTag(SKIP_BUTTON_TAG).performClick()
        awaitText("運動中")
        compose.onNodeWithText("ランジ").assertIsDisplayed()

        abortTimer()
    }

    @Test
    fun フリーセットを記録すると完了画面に進む() {
        runBlocking {
            val repo = WorkoutRepository(db)
            val w = repo.addWorkout("自重トレ")
            // durationSeconds = -1（DURATION_UNLIMITED）でフリーセット種目
            repo.addExercise(w.id, "懸垂", ExerciseMode.TIMED, DURATION_UNLIMITED, 1, 0, 1, 0)
        }

        launchApp()
        awaitText("自重トレ")
        compose.onNodeWithText("自重トレ").performClick()
        awaitText("スタート", substring = true)
        compose.onNodeWithText("スタート", substring = true).performClick()

        // フリーセットは「フリー」表示。タップで記録ダイアログが出る
        awaitText("フリー")
        tapTimer()
        awaitText("フリーセット")
        // 回数を 1 にして保存
        compose.onNodeWithText("＋").performClick()
        compose.onNodeWithText("保存").performClick()

        // 最後の 1 セット・1 種目なので完了画面へ
        awaitText("完了", substring = true, timeoutMillis = 10_000)
        compose.onNodeWithText("閉じる").performClick()
        awaitText("ワークアウト")
    }

    @Test
    fun 残ったスナップショットから再開ダイアログでタイマーへ戻れる() {
        val workoutId = runBlocking {
            val repo = WorkoutRepository(db)
            val w = repo.addWorkout("再開テスト")
            val ex = repo.addExercise(w.id, "スクワット", ExerciseMode.TIMED, 60, 2, 10, 1, 0)
            // 「種目1のセット1を実行中（残り20秒）」のスナップショットを直接保存
            TimerSnapshotStore(targetContext).save(
                TimerSnapshot(
                    workoutId = w.id,
                    workoutName = w.name,
                    workoutSortOrder = w.sortOrder,
                    state = TimerState(
                        exercises = listOf(ex),
                        phase = TimerPhase.ExercisePhase(
                            exerciseIndex = 0,
                            currentSet = 1,
                            currentRep = 1,
                            remainingSeconds = 20,
                        ),
                        elapsedSeconds = 5,
                    ),
                    savedAtEpochMillis = System.currentTimeMillis(),
                )
            )
            w.id
        }
        requireNotNull(workoutId)

        launchApp()

        // 起動時に再開ダイアログが出る（"再開しますか" はダイアログ固有）
        awaitText("再開しますか", substring = true)
        // 「再開」でタイマーへ復帰
        compose.onNodeWithText("再開").performClick()
        awaitText("運動中")
        compose.onNodeWithText("スクワット").assertIsDisplayed()

        abortTimer()
    }
}
