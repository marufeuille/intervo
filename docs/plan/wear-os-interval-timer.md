# Wear OS 筋トレインターバルタイマー アプリ 実装計画

## 概要

Pixel Watch 4 向けの筋トレインターバルタイマーアプリ。
**手元でワークアウト（複数）とエクササイズを編集・追加できること**が最重要機能。
設定した内容で運動・休憩のカウントダウンをバイブレーションで通知する。

## 確定した仕様

| 項目 | 決定 |
|------|------|
| ワークアウト構成 | **複数対応** — 「胸トレ」「脚トレ」など名前付きワークアウトを複数管理 |
| エクササイズ編集 | **必須** — 各ワークアウト内で追加・編集・削除・並び替えすべて手元で行える |
| Ambient Mode | 対応する（タイマー中スリープしても残り時間を常時表示） |
| フィードバック | バイブレーションのみ（音なし） |

---

## 環境調査結果

| 項目 | 状況 |
|------|------|
| Android Studio | インストール済み (`/Applications/Android Studio.app`) |
| Android SDK | `~/Library/Android/sdk/` に配置済み |
| Platforms | android-32, android-33, android-34, **android-36（追加済み）** |
| System Images | android-36 / android-wear-signed / arm64-v8a（Wear OS エミュレータ用） |
| Build Tools | 30.0.3, 33.0.0, 34.0.0 |
| JDK | OpenJDK 11 (Android Studio バンドル版) |
| adb | PATH 設定済み |

### 残りのセットアップ

- AVD Manager で Wear OS エミュレータ作成（android-36/android-wear-signed/arm64-v8a）

---

## ターゲット仕様

| 項目 | 値 |
|------|-----|
| デバイス | Pixel Watch 4 |
| compileSdk / targetSdk | 36 |
| minSdk | 30 |
| 画面形状 | ラウンドディスプレイ |
| アーキテクチャ | arm64 |

---

## 技術選定

| 領域 | 選択 | 理由 |
|------|------|------|
| 言語 | Kotlin | Wear OS 公式推奨 |
| UI | Jetpack Compose for Wear OS | 新規アプリの標準、ラウンドUI対応が容易 |
| アーキテクチャ | MVVM | ViewModel でタイマー状態・編集状態を保持 |
| 状態管理 | StateFlow + Coroutines | タイマーのカウントダウンループと相性良好 |
| 永続化 | Room (SQLite) | ワークアウト・エクササイズ設定をデバイスに保存 |
| ビルド | Gradle Kotlin DSL | タイプセーフ |
| ナビゲーション | Wear Navigation Compose | Wear OS 向けの公式ナビゲーションライブラリ |
| 数値入力 UI | Wear OS Stepper (+/-ボタン) | クラウン回転・タッチ両対応 |

---

## データモデル（API）

```kotlin
// ワークアウト（エクササイズのグループ）
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,       // 表示名（例: "胸トレ"）
    val sortOrder: Int      // 表示順（0始まり）
)

// エクササイズ1種目
@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE   // ワークアウト削除時に紐づくエクササイズも削除
    )]
)
data class Exercise(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,          // 所属するワークアウトのID
    val name: String,               // 表示名（例: "腕立て伏せ"）
    val durationSeconds: Int,       // 運動時間（秒、5〜300）
    val sets: Int,                  // セット数（1〜20）
    val restSeconds: Int,           // 休憩時間（秒、0〜120）
    val sortOrder: Int              // ワークアウト内での表示順（0始まり）
)
```

Room DAO インターフェース:

```kotlin
interface WorkoutDao {
    fun getAll(): Flow<List<Workout>>                    // sortOrder順
    suspend fun insert(workout: Workout)
    suspend fun update(workout: Workout)
    suspend fun delete(workout: Workout)                 // CASCADE で Exercise も削除
    suspend fun updateOrder(workouts: List<Workout>)
}

interface ExerciseDao {
    fun getByWorkout(workoutId: String): Flow<List<Exercise>>  // sortOrder順
    suspend fun insert(exercise: Exercise)
    suspend fun update(exercise: Exercise)
    suspend fun delete(exercise: Exercise)
    suspend fun updateOrder(exercises: List<Exercise>)
}
```

### プリセットデータ（初回起動時に挿入）

```
ワークアウト「上半身」
  ├ 腕立て伏せ: 30秒 × 3 / 休憩10秒
  └ プランク:   30秒 × 3 / 休憩10秒

ワークアウト「下半身」
  ├ スクワット: 40秒 × 3 / 休憩20秒
  └ ランジ:     30秒 × 3 / 休憩15秒
```

---

## 画面構成と遷移

```
[WorkoutSelectScreen]（ワークアウト選択）
  ├─ タップ(ワークアウト行)  ──→ [WorkoutDetailScreen]（エクササイズ一覧）
  │                                  ├─ タップ(エクササイズ行) ──→ [ExerciseEditScreen]（編集）
  │                                  ├─ 「＋追加」ボタン       ──→ [ExerciseEditScreen]（新規）
  │                                  └─ 「▶ スタート」ボタン   ──→ [TimerScreen]
  │                                                                   └─ 全完了 ──→ [CompletionScreen]
  │                                                                                  └─ 確認 ──→ [WorkoutSelectScreen]
  └─ 「＋ワークアウト追加」  ──→ [WorkoutEditScreen]（ワークアウト名を入力）
```

---

## 各画面の仕様

### 1. WorkoutSelectScreen（ワークアウト選択・メイン画面）

- **表示内容**:
  - ワークアウトリスト（ScalingLazyColumn）: ワークアウト名 / エクササイズ数
  - 行タップ → `WorkoutDetailScreen` へ
  - 行長押し → 削除 / 名前編集メニュー
  - 末尾に「＋ ワークアウトを追加」行
- **空リスト時**: 「＋ 追加して始めよう」プレースホルダー表示

### 2. WorkoutEditScreen（ワークアウト名編集・追加）

- **入力項目**: ワークアウト名（Wear OS キーボード or プリセット名リストから選択）
- **プリセット名**: 上半身、下半身、全身、胸トレ、背中、腕、肩、脚、体幹、有酸素、その他
- **操作**:
  - 「保存」 → DBに保存、`WorkoutDetailScreen` へ遷移（新規の場合は空のエクササイズリスト）
  - バックジェスチャー → 破棄して戻る

### 3. WorkoutDetailScreen（エクササイズ一覧）

- **表示内容**:
  - ヘッダー: ワークアウト名（小さく）
  - エクササイズリスト（ScalingLazyColumn）: 名前 / 時間 / セット数 / 休憩時間
  - 行タップ → `ExerciseEditScreen` へ
  - 行長押し → 削除 / 並び替えメニュー
  - 末尾に「＋ エクササイズを追加」行
  - スタートボタン（エクササイズが1件以上の場合に表示）
- **空リスト時**: 「＋ エクササイズを追加」のみ表示

### 4. ExerciseEditScreen（エクササイズ編集・追加）

- **入力項目**:

  | 項目 | UI部品 | 範囲 |
  |------|--------|------|
  | 名前 | プリセット名リストから選択 + カスタム入力 | — |
  | 運動時間 | Stepper（+5秒 / -5秒、クラウン回転対応） | 5〜300秒 |
  | セット数 | Stepper（+1 / -1） | 1〜20 |
  | 休憩時間 | Stepper（+5秒 / -5秒、0秒も可） | 0〜120秒 |

- **プリセット名リスト**: 腕立て伏せ、スクワット、腹筋、プランク、ランジ、バーピー、マウンテンクライマー、ジャンピングジャック、その他（カスタム）

- **操作**:
  - 「保存」 → バリデーション後DBへ保存、`WorkoutDetailScreen` へ戻る
  - バックジェスチャー → 変更破棄して戻る

### 5. TimerScreen（タイマー画面）

- **表示内容**（アクティブ時）:
  - エクササイズ名
  - フェーズラベル（「運動中」/ 「休憩中」）
  - 残り時間（大きなテキスト、秒表示）
  - セット進捗（例: `2 / 3 セット`）
  - 円形プログレスインジケーター（画面外周）
- **操作**:
  - タップ → 一時停止 / 再開トグル
  - 長押し → 中断確認ダイアログ → `WorkoutSelectScreen` へ戻る

- **Ambient Mode（常時表示）時**:
  - バッテリー節約のため簡素化: 残り時間とフェーズのみ表示
  - 色を薄くしフレームレートを低下させる（Wear OS Ambient 推奨仕様）
  - `AmbientLifecycleObserver` で active ↔ ambient を切り替え
  - **タイマーはバックグラウンドで継続**（ForegroundService を使用）

### 6. CompletionScreen（完了画面）

- **表示内容**: 完了メッセージ、消費時間（合計）
- **操作**: 「完了」ボタン → `WorkoutSelectScreen` へ戻る

---

## タイマーステートマシン

```
[IDLE]
  │ start(workoutId)
  ▼
[EXERCISE(exerciseIndex, currentSet, remaining)]  ←──────────────────┐
  │ remaining == 0                                                    │
  │ → vibrate(PATTERN_1)                                             │
  ▼                                                                  │
[REST(exerciseIndex, completedSets, remaining)]                      │
  │ remaining == 0                                                   │
  │ → vibrate(PATTERN_2)                                             │
  │                                                                  │
  ├── completedSets < sets ──────────────────────────────────────────┘
  │
  ├── completedSets == sets かつ 次のExerciseあり
  │    → EXERCISE(exerciseIndex+1, currentSet=1, ...)
  │
  └── completedSets == sets かつ 全Exercise完了
       → [COMPLETE]
```

### TimerState / TimerPhase

```kotlin
sealed class TimerPhase {
    object Idle : TimerPhase()
    data class Exercise(
        val exerciseIndex: Int,
        val currentSet: Int,
        val remainingSeconds: Int
    ) : TimerPhase()
    data class Rest(
        val exerciseIndex: Int,
        val completedSets: Int,
        val remainingSeconds: Int
    ) : TimerPhase()
    object Complete : TimerPhase()
}

data class TimerState(
    val exercises: List<Exercise>,
    val phase: TimerPhase,
    val isPaused: Boolean
)
```

---

## バイブレーションパターン

| パターン | 用途 | 仕様 |
|---------|------|------|
| PATTERN_1 | 運動時間終了 | 短い振動 ×2（200ms ON → 100ms OFF → 200ms ON） |
| PATTERN_2 | 休憩時間終了 | 長い振動 ×1（600ms ON） |

`VibrationEffect.createWaveform` を使用（API 26+）。
Vibrator サービスが利用不可の場合は何もしない。

---

## Ambient Mode 実装方針

- `Activity` に `AmbientLifecycleObserver` を登録
- `AmbientState` を ViewModel に伝達し、`TimerScreen` が ambient フラグに応じて UI を切り替え
- タイマー継続のため `ForegroundService` + `WakeLock` を使用
  - サービスが残り時間を管理し、Activity/ViewModel はバインドして状態を購読
  - サービスは Notification を発行（Wear OS の ongoing notification として表示）

```kotlin
if (isAmbient) {
    AmbientTimerContent(remaining, phase)  // 最小限の情報のみ
} else {
    ActiveTimerContent(state)              // フル表示
}
```

---

## プロジェクト構成

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/example/interval/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── Workout.kt                  # Entity
│       │   ├── Exercise.kt                 # Entity（workoutId FK含む）
│       │   ├── WorkoutDao.kt
│       │   ├── ExerciseDao.kt
│       │   ├── AppDatabase.kt
│       │   ├── WorkoutRepository.kt
│       │   └── DefaultWorkouts.kt          # プリセットデータ
│       ├── timer/
│       │   ├── TimerState.kt
│       │   ├── TimerViewModel.kt
│       │   ├── TimerService.kt             # ForegroundService（Ambient対応）
│       │   └── VibrationManager.kt
│       └── ui/
│           ├── navigation/
│           │   └── AppNavigation.kt
│           ├── screens/
│           │   ├── WorkoutSelectScreen.kt  # ★ メイン画面（ワークアウト一覧）
│           │   ├── WorkoutEditScreen.kt    # ワークアウト名編集
│           │   ├── WorkoutDetailScreen.kt  # エクササイズ一覧
│           │   ├── ExerciseEditScreen.kt   # ★ エクササイズ編集（重要）
│           │   ├── TimerScreen.kt
│           │   └── CompletionScreen.kt
│           └── theme/
│               ├── Theme.kt
│               └── Color.kt
```

---

## 主要ライブラリ

```kotlin
dependencies {
    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material:1.x.x")
    implementation("androidx.wear.compose:compose-foundation:1.x.x")
    implementation("androidx.wear.compose:compose-navigation:1.x.x")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.x.x")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.x.x")
    implementation("androidx.lifecycle:lifecycle-service:2.x.x")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x.x")

    // Room (永続化)
    implementation("androidx.room:room-runtime:2.x.x")
    implementation("androidx.room:room-ktx:2.x.x")
    kapt("androidx.room:room-compiler:2.x.x")

    // Ambient Mode
    implementation("androidx.wear:wear:1.x.x")
}
```

---

## ふるまい仕様（テスト観点）

### WorkoutSelectScreen のふるまい

| 操作 | 期待される結果 |
|------|---------------|
| ワークアウト行タップ | `WorkoutDetailScreen(workoutId)` へ遷移 |
| 「＋ ワークアウトを追加」タップ | `WorkoutEditScreen`（新規）へ遷移 |
| 行長押し → 削除 | DB から削除（紐づく Exercise も CASCADE 削除） |
| 空リスト状態 | プレースホルダー + 追加ボタンのみ表示 |

### WorkoutDetailScreen のふるまい

| 操作 | 期待される結果 |
|------|---------------|
| エクササイズ行タップ | `ExerciseEditScreen(exerciseId)` へ遷移 |
| 「＋ 追加」タップ | `ExerciseEditScreen`（新規、workoutId付き）へ遷移 |
| 「▶ スタート」タップ | `TimerScreen(workoutId)` へ遷移 |
| エクササイズ 0 件時 | スタートボタン非表示 |
| 行長押し → 削除 | DB から削除 |

### ExerciseEditScreen のふるまい

| 操作 | 期待される結果 |
|------|---------------|
| 運動時間 +5 ボタン | durationSeconds が 5 増える（上限300秒でストップ） |
| 運動時間 -5 ボタン | durationSeconds が 5 減る（下限5秒でストップ） |
| セット数 +1 ボタン | sets が 1 増える（上限20でストップ） |
| 休憩時間を 0 に設定 | バリデーション通過（0秒休憩は許容） |
| 名前未選択で保存 | エラー表示、保存されない |
| 「保存」タップ | DB に保存、`WorkoutDetailScreen` へ戻る |

### TimerViewModel のふるまい

| 操作 | 期待される状態変化 |
|------|-------------------|
| `start(workoutId)` | DB から exercises 取得 → Phase → `Exercise(index=0, set=1, remaining=durationSeconds)` |
| 1秒経過 | `remaining` が 1 減る |
| `remaining == 0`（Exercise中） | PATTERN_1 → Phase → `Rest(...)` |
| `remaining == 0`（Rest中）かつ `completedSets < sets` | PATTERN_2 → Phase → `Exercise(同index, set+1)` |
| `remaining == 0`（Rest中）かつ `completedSets == sets` かつ 次あり | PATTERN_2 → Phase → `Exercise(index+1, set=1)` |
| `remaining == 0`（Rest中）かつ 全完了 | PATTERN_2 → Phase → `Complete` |
| `pause()` | カウントダウン停止、状態保持 |
| `resume()` | カウントダウン再開 |
| `stop()` | Phase → `Idle` |

### VibrationManager のふるまい

| 呼び出し | 期待される振る舞い |
|---------|-------------------|
| `vibrate(PATTERN_1)` | 200ms-100ms-200ms のパターンで振動 |
| `vibrate(PATTERN_2)` | 600ms の単発振動 |
| Vibrator 利用不可 | 何もしない（クラッシュしない） |

### TimerService のふるまい

| 状況 | 期待される振る舞い |
|------|-------------------|
| Activity が Ambient Mode へ移行 | Service はタイマー継続、残り時間を更新し続ける |
| Activity が Active Mode へ復帰 | Service の現在状態を受け取り UI を同期表示 |
| バインドされていない間にタイマー完了 | 完了バイブレーションは実行される |

---

## 実装ステップ

### Step 1: プロジェクト作成
1. Android Studio → New Project → **Wear OS** テンプレート選択
2. パッケージ名: `com.example.interval`
3. Kotlin + Compose for Wear OS、compileSdk/targetSdk 36

### Step 2: データ層実装
- `Workout.kt`, `Exercise.kt` Entity 定義（ForeignKey 含む）
- `WorkoutDao.kt`, `ExerciseDao.kt` 定義
- `AppDatabase.kt` セットアップ
- `WorkoutRepository.kt` 実装
- `DefaultWorkouts.kt` プリセットデータ（初回起動時に挿入）

### Step 3: タイマーロジック実装
- `TimerState.kt` の sealed class 定義
- `VibrationManager.kt` のバイブレーション実装
- `TimerViewModel.kt` の StateFlow + Coroutines によるカウントダウン
- `TimerService.kt` の ForegroundService 実装（Ambient Mode対応）

### Step 4: UI 実装（重要度順）
1. `WorkoutSelectScreen.kt` — メイン画面
2. `WorkoutEditScreen.kt` — ワークアウト名入力
3. `WorkoutDetailScreen.kt` — エクササイズ一覧
4. `ExerciseEditScreen.kt` — Stepper UI（★ 最重要）
5. `TimerScreen.kt` — 円形プログレス + Ambient Mode
6. `CompletionScreen.kt`
7. `AppNavigation.kt` — ナビゲーショングラフ接続
8. `Theme.kt` / `Color.kt` — Wear OS テーマ

### Step 5: Ambient Mode 統合
- `MainActivity` に `AmbientLifecycleObserver` 登録
- TimerScreen の ambient/active 表示切り替え実装
- Service バインドの確認

### Step 6: 動作確認
1. エミュレータ（AVD Manager で android-36 wear-signed arm64 作成）で全操作フロー確認
2. 実機（Pixel Watch 4）で Wi-Fi ADB 接続
3. 実機でタイマー動作・バイブレーション・Ambient Mode 確認

---

## 実機接続手順

```bash
# Pixel Watch 4 側: 設定 → システム → 開発者向けオプション → ADBデバッグ を ON

adb devices  # デバイスが表示されれば接続成功

# Wi-Fi ADB
adb tcpip 5555
adb connect <WatchのIPアドレス>:5555
```
