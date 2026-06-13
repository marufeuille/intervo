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

## 今後広げられるシナリオ

- 種目追加 → タイマー開始 → 一時停止/完了 → 履歴反映（タイマーは `TimerService`＝health の Foreground
  Service と心拍権限が絡むため、権限付与とサービス起動の扱いを別途用意する必要がある）
- 動的値（残り秒・心拍 `♥`）のアサーション
- ワークアウトの再開ダイアログ（スナップショット復帰）
