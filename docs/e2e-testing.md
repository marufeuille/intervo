# E2E テスト（シナリオベース）

Wear アプリ（`:app`）の UI 遷移を、実機/エミュレータ上で end-to-end に検証する instrumented テスト。
実 ViewModel・Room（`interval.db`）をそのまま動かし、ユーザー操作に沿ったシナリオを通す。

テストコード: `app/src/androidTest/java/dev/marufeuille/intervo/WorkoutCreationE2ETest.kt`

## 実行方法

Wear OS エミュレータ（または実機）を起動した状態で:

```bash
./gradlew :app:connectedDebugAndroidTest
```

レポート: `app/build/reports/androidTests/connected/debug/index.html`

CI（GitHub Actions）では `android-emulator-runner` 上で同じタスクを実行し、HTML レポートを
アーティファクトとして保存、ジョブサマリに PASS/FAIL 表を出力する（`.github/workflows/ci.yml` の `instrumented-test` ジョブ）。

## シナリオ一覧

| シナリオ | 操作 | 検証内容 |
| --- | --- | --- |
| シードしたワークアウトが一覧に出て詳細でスタートできる | DB にワークアウト＋種目をシード → 一覧でタップ → 詳細へ | 一覧にワークアウトが表示され、詳細で「▶ スタート」が出る |
| 履歴が空のときは履歴なしと表示される | 一覧 → スワイプ → 「履歴」 | 履歴画面に「履歴なし」が出る |
| UIだけでワークアウトを作成すると詳細画面に名前が反映される | 「追加して始めよう」→ 名前入力 → 保存 | 入力した名前が詳細画面に表示される |
| ワークアウトを長押しで削除すると一覧から消える | 行を長押し → 確認ダイアログ → 「削除」 | 対象が一覧から消え、選択画面に留まる |
| ワークアウトを開始して中断すると一覧に戻る | 詳細 → スタート → 長押し → 「中断」 | タイマー（`TimerService` バインド）が起動し、中断で一覧へ戻る |
| 短いワークアウトを完走すると完了画面が出て履歴に残る | 1秒×1セットの種目 → スタート → 完走 | 完了画面が出て、履歴に完了記録（ワークアウト名）が残る |
| タイマーをタップで一時停止と再開ができる | スタート → タップ → 再タップ | 「一時停止」⇄「運動中」が切り替わる |
| 運動中は残り秒がカウントダウンする | スタート → 残り秒を 2 回読む | 残り秒が時間経過で減少する（動的値アサーション） |
| 休憩をスキップすると次の種目へ進む | 1秒種目→休憩 → スキップボタン | 休憩を飛ばして次の種目（運動中）へ進む |
| フリーセットを記録すると完了画面に進む | フリー種目 → タップ → 回数入力 → 保存 | 記録ダイアログで保存し、完了画面へ進む |
| 残ったスナップショットから再開ダイアログでタイマーへ戻れる | スナップショットを保存して起動 → 「再開」 | 起動時に再開ダイアログが出て、タイマーへ復帰する |

> シナリオを追加・変更したら、この表も更新すること。

## 設計メモ / ハマりどころ

- **権限ダイアログの回避**: `MainActivity` は起動時に心拍系の実行時権限を要求してダイアログが UI を塞ぐ。
  そのため `createAndroidComposeRule<ComponentActivity>()` に `IntervalTheme { AppNavigation() }` を直接載せ、
  `MainActivity` を経由しない構成にしている。
- **決定性**: 各 `@Test` の冒頭（`@Before`）で `AppDatabase.getInstance(ctx).clearAllTables()` を呼び、
  必要なら `runBlocking { WorkoutRepository(db).addWorkout(...) }` でシードする。
- **保存後の navigate はメインスレッドで**: 「保存（DB＝別ディスパッチャ）→ `navigate`」を
  コルーチンから直接行うと、Compose テストのフレーム遅延インターセプタ経由で継続が非メインスレッドで
  再開され、`NavController` が `setCurrentState must be called on the main thread` で落ちる（実機では再現しない）。
  `WorkoutEditScreen` / `ExerciseEditScreen` では保存後の navigate を `withContext(Dispatchers.Main)` で
  メインに乗せて回避している。
- **Wear の `ScalingLazyColumn` は `performScrollTo()` 非対応**: 画面外の要素は
  `onRoot().performTouchInput { swipeUp() }` でスクロールしてからタップする。
- **遅延合成**: ScalingLazyColumn は可視範囲外の項目を合成しないため、画面外の要素は
  `assertExists()` でも見つからないことがある。状態変化の検証は `waitUntil { onAllNodesWithText(...) }` で待つ。

### タイマー系シナリオの注意点

- `TimerService` は health タイプの Foreground Service だが、`promoteToForeground()` が `runCatching` で
  包まれているため、**実行時権限が無くても SecurityException を握りつぶしてカウントダウンは継続する**。
  そのため E2E では権限付与なしでタイマーを進められる（FGS への昇格と心拍取得はされないだけ）。
- **サービスの後片付け（重要）**: タイマー系テストは静的な `TimerService.runningWorkoutId` とスナップショット
  ファイルを共有するため、テスト間で汚染しやすい。`@Before` と **`@After`** の両方で `stopTimerAndWait()`
  （`ACTION_STOP` を投げ `runningWorkoutId` が null になるまでポーリング）を呼び、`@Before` では
  `TimerSnapshotStore(ctx).clear()` でスナップショットも消す。`@After` があるので、テストが中断前に
  失敗してもサービスを残さず、次テスト（や次回実行）の自動再開連鎖を防げる。
- 中断（`stop()`）は同期的に `runningWorkoutId` を null にするので、サービス接続後（種目名が出た後）に
  中断すれば自動再開とは競合しない。
- 残り秒は「桁のみの Text」をセマンティクスから読む（`SemanticsMatcher` で `\d+` にマッチ。
  セット表記は `/` を含むので除外される）。
- **スキップ**: スキップボタンはアイコンのみなので `TimerContent` に `testTag(SKIP_BUTTON_TAG)` を付与。
  休憩へ到達させるには 1 種目目を短く（1 秒）、滞在させる次種目を長く（60 秒）する。
- **詳細画面の「スタート」が未合成になる罠**: 種目が複数あると「▶ スタート」が下方に押し出され
  ScalingLazyColumn が未合成 → `awaitText` が見つけられない。種目名を待ってから
  `onRoot().performTouchInput { swipeUp() }` でスクロールして出す。
- **再開ダイアログ**: 実際の中断では `stop()` がスナップショットを消すので、テストでは
  `TimerSnapshotStore(ctx).save(TimerSnapshot(... ExercisePhase ...))` で直接スナップショットを書いてから
  起動し、`runningWorkoutId == null`（@Before で保証）かつ有効なスナップショットがある状態を作る。

## 今後広げられるシナリオ

- 心拍 `♥` のアサーション（エミュレータの Health Services 次第で値が出ないことあり）
- レップ式種目のスキップ、フリーセット記録の履歴反映の検証
