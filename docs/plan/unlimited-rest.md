# 設計: 休憩を「無制限」に設定可能にする

> 本ドキュメントは実装前に設計を残すためのもの。実装は別セッションで main からクリーンに作り直す。

## 背景・目的

限界まで追い込むメニュー（例: 腕立て伏せを限界まで ×3セット）の場合、休憩は何秒という固定値ではなく、呼吸が整い、いけると判断したタイミングで再開したい。そのため休憩時間を「無制限（ユーザーがタップするまで待機）」として設定できるようにする。

## 対象範囲

- **セット間休憩**（`restSeconds`）
- **レップ間休憩**（`repRestSeconds`）

両方を無制限化する。レップ間休憩は「指定レップ数モード」で使われ、「腕立て伏せを限界まで ×3セット」のようにセット構成によってはレップ間も無制限にしたいケースがあるため。

## 設計方針

既存の確立パターンに準用する:

| 既存 sentinel | 定数 | 用途 |
|---|---|---|
| `DURATION_UNLIMITED = -1` | 「自由」duration | ExercisePhase でカウントアップ、ユーザーが finishFreeSet で終了 |
| `REPS_OPEN_ENDED = -1` | 「限界」reps | finishOpenEndedRepSet で終了 |
| **`REST_UNLIMITED = -1`（新規）** | **「自由」rest** | **RestPhase/RepRestPhase でカウントアップ、ユーザーが skipRest で終了** |

- DBスキーマ変更・マイグレーション不要（`restSeconds`/`repRestSeconds` は `INTEGER NOT NULL` 列で `-1` を保持可能）
- 無制限休憩中は経過時間を 0 からカウントアップ（自由 duration と同じ方式）
- 自動進行せず、ユーザーが既存の「スキップ」操作（`skipRest`）で次へ進む

## 実装ステップ

### 1. 定義層 — `app/.../data/Exercise.kt`

- `const val REST_UNLIMITED = -1` を追加（`REST_STEP` の直後）
- 拡張関数を追加:
  ```kotlin
  fun Exercise.isRestUnlimited(): Boolean = restSeconds == REST_UNLIMITED
  fun Exercise.isRepRestUnlimited(): Boolean = repRestSeconds == REST_UNLIMITED
  ```

### 2. タイマーエンジン — `app/.../timer/TimerEngine.kt`（コア）

3箇所を修正:

- **`tick` の RepRestPhase/RestPhase 分岐**: 無制限判定のときカウントダウンせず `remainingSeconds + 1` でカウントアップ（ExercisePhase の `isDurationUnlimited` 分岐と同じ）。終了判定なし。
- **`finishExerciseSet`（L247付近）**: ゲート `if (exercise.restSeconds > 0)` を `if (exercise.isRestUnlimited() || exercise.restSeconds > 0)` に変更。無制限時は `remainingSeconds = 0`（カウントアップ開始値）で RestPhase に入る。
- **`finishExerciseInterval`（L217付近）**: 同様に `repRestSeconds > 0` ゲートを拡張。無制限時は `remainingSeconds = 0` で RepRestPhase へ。

### 3. タイマー状態 — `app/.../timer/TimerState.kt`

- **`totalSeconds`（L79付近）**: `ex.restSeconds * ex.sets` で無制限 `-1` が混入すると合計が壊れる。無制限の場合は `0` として算入（`isDurationUnlimited` ガードと同様）。repRest も同様にガード。

### 4. 再生中 UI — `app/.../ui/screens/TimerContent.kt`

- RestPhase/RepRestPhase の `timerDisplayInfo`: 無制限時は `totalSecs = 0`（リング無効化、自由 duration と同じ）。
- フェーズラベルは無制限時「休憩（自由）」。

### 5. 編集 VM — `app/.../ui/screens/ExerciseEditViewModel.kt`

`setDurationUnlimited`/`adjustDuration` のパターンをクローン:
- `setRestUnlimited(b)` / `isRestUnlimited()`
- `setRepRestUnlimited(b)` / `isRepRestUnlimited()`
- `adjustRest` / `adjustRepRest` を sentinel 耐性に修正（`-1` から復帰）

### 6. 編集 UI トグル — `app/.../ui/screens/ExerciseEditControls.kt`

- `RestTargetToggle` を追加（`DurationTargetToggle` のクローン）。pill ラベル「指定/自由」、色は `RestBlue`。
- `ModePill` に `accentColor` パラメータを追加（デフォルト `ExerciseOrange`、rest は `RestBlue` を渡す）。

### 7. 編集画面 — `app/.../ui/screens/ExerciseEditScreen.kt`

- 「休憩」StepperRow の前に `RestTargetToggle` を配置し、無制限時は StepperRow を隠す（duration と同じ `if (!unlimited) { item { StepperRow(...) } }` 構造）。
- 「間休憩」StepperRow も同様にトグル化。

### 8. ワークアウト詳細表示（app）— `app/.../ui/screens/WorkoutDetailScreen.kt`

- `休${exercise.restSeconds}秒` の4箇所を `restLabel(exercise)` に置換。
- `restLabel` ヘルパーを追加: 無制限→「休憩 自由」、0秒→「休憩なし」、それ以外→「休N秒」。

### 9. Companion 表示・エクスポート

- **`companion/.../ui/detail/WorkoutDetailMapper.kt`**: rest の `-1` は UI 側で「無制限」と表示するため保持（duration/reps と違い dropSentinel しない）。doc コメントを更新。
- **`companion/.../ui/detail/HistoryDetailScreen.kt`**: `plannedLabel` で無制限（`REST_SENTINEL_UNLIMITED = -1`）時に「休憩 無制限」/「レップ間 無制限」ラベルを表示。
- **`companion/.../pds/WorkoutPdsRecordMapper.kt`**: 既存の `takeIf { it >= 0 }` で `-1` を除外（推定時間に算入しない）。コメントで意図を明記。

### 10. Sync（変更不要）

- `WorkoutPlanSyncClient.kt`、`WorkoutHistorySyncClient.kt`、`WorkoutHistoryListenerService.kt` は raw `-1` をそのまま送受信。変更不要。

### 11. テスト

- **`app/src/test/.../timer/TimerEngineTest.kt`**: 無制限 rest の新規ケース:
  - セット終了で無制限 RestPhase に進む（remainingSeconds=0）
  - カウントアップ動作・自動進行しない
  - skipRest で次セットへ
  - totalSeconds への影響（無制限は0算入）
  - 無制限 rep rest のカウントアップ・スキップ
- **`companion/src/test/.../pds/WorkoutPdsRecordMapperTest.kt`**: 無制限 rest フィクスチャ（planned から省略されることを検証）

## 注意事項

- **混在変更への依存**: 本機能のテスト用コード（ExerciseEditViewModel の planSyncClient 等）は、未コミットの plan sync 関連変更に依存する箇所がある。実装時は plan sync 関連が先に main に入っている前提で進めるか、依存を避ける設計にする。
- sentinel 定数は `REST_UNLIMITED` 1つ（restSeconds/repRestSeconds 共用）。duration と reps が別定数だが同値 `-1` なので、rest も同様。
