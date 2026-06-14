# Intervo 1.7.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
ワークアウトに「種別」を設定できるようになりました。
・筋力トレーニング/HIIT/ストレッチ/ヨガ/ランニングなどから選択
・Health Connect 連携時、選んだ種別が他のヘルスケアアプリに正しいカテゴリとして記録されます（例：フォームローラー＝ストレッチ）
・ワークアウト詳細画面に編集ボタンを追加し、名前と種別を後から変更できます
```

---

## Play Console（ユーザー向け・詳細）

### 概要
ワークアウトごとに「種別」を設定できるようになりました。これまで Health Connect 連携では、すべてのワークアウトが汎用の「ワークアウト」として記録されていましたが、本バージョンからは選んだ種別（ストレッチ・筋力トレーニングなど）が、連携先のヘルスケアアプリに適切なカテゴリとして記録されます。ワークアウト名はそのまま残るので、たとえば「フォームローラー（種別＝ストレッチ）」のように管理できます。

### 新機能
- **ワークアウトの種別** — 筋力トレーニング／HIIT／ストレッチ／自重トレーニング／ヨガ／ランニング／ウォーキング／その他から選べます。
- **Health Connect への種別反映** — Companion アプリで連携すると、選んだ種別が連携先アプリに正しいカテゴリとして記録されます。
- **編集導線** — ワークアウト詳細画面に編集ボタンを追加。既存ワークアウトの名前と種別を後から変更できます。

---

## Internal Notes

### バージョン
- app: `versionCode` 19 / `versionName` 1.7.0
- companion: `versionCode` 20 / `versionName` 1.7.0

### 主な変更
- **ワークアウト種別**: `ExerciseCategory` enum（文字列キー＋日本語ラベル）を新設。`Workout` に `exerciseType` カラムを追加（app DB v8、`MIGRATION_7_8` で `DEFAULT 'OTHER_WORKOUT'`）。
- **転送**: 種別を `workout_snapshot_json` に相乗りさせて Wear→Companion へ送信（DataMap 個別キー／Companion DB スキーマ変更は不要）。`TimerService` / `TimerSnapshot` では `workoutSortOrder` と同じ経路で運搬。
- **Health Connect**: `HealthConnectWriter` が `exercise_type` を `ExerciseSessionRecord.EXERCISE_TYPE_*`（STRETCHING / STRENGTH_TRAINING / HIGH_INTENSITY_INTERVAL_TRAINING ほか）へ変換。`EXERCISE_TYPE_OTHER_WORKOUT` 固定を置換。未知／欠落キーは OTHER_WORKOUT にフォールバック（後方互換）。
- **編集 UI**: ワークアウト詳細画面に編集導線を追加。既存の `WorkoutEditScreen`（workoutId 付き）が名前・種別を両方ロード／更新。種別選択画面の背景を不透明化。

### Play 提出時の注意
- 内部テスト／クローズドテストトラックで Wear→Companion→Health Connect の種別反映を確認してから製品版へ昇格すること。
- 既存ワークアウトはマイグレーションで `OTHER_WORKOUT`（従来挙動）になる。詳細画面の編集から種別を付け替え可能。
