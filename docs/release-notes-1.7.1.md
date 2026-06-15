# Intervo 1.7.1 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
不具合修正とヘルスケア連携の改善を行いました。
・ワークアウト詳細画面でワークアウト名と種別が重なって見えなくなる表示崩れを修正
・Health Connect への記録方法を見直し、運動の実施時間が他のヘルスケアアプリ（カロミル等）で運動時間として認識されやすくなりました
```

---

## Play Console（ユーザー向け・詳細）

### 概要
表示崩れの修正と、Health Connect 連携の改善を行ったメンテナンスリリースです。

### 修正・改善
- **表示崩れの修正** — ワークアウトを選択した後の詳細画面で、ワークアウト名と種別が重なってワークアウト名が見えなくなることがある問題を修正しました。
- **ヘルスケア連携の改善** — Health Connect への運動記録を「アクティブに計測した記録」として書き込むよう変更しました。これにより、連携先のヘルスケアアプリ（カロミル等）で運動の実施時間が運動時間として認識されやすくなります。

---

## Internal Notes

### バージョン
- app: `versionCode` 21 / `versionName` 1.7.1
- companion: `versionCode` 22 / `versionName` 1.7.1
- ※ app と companion は同一 `applicationId`。Play 内で versionCode はグローバル一意のため、使用済みの 20（companion 1.7.0）を避け 21/22 を採番。

### 主な変更
- **詳細画面のレイアウト修正**: `WorkoutDetailScreen` 先頭の `item {}` 内に `Text`（名前）→`Spacer`→`Chip`（種別）を直接並べていたため、`ScalingLazyColumn` の item スロットが複数子要素を縦積みせず重ねて描画し、名前が Chip の背後に隠れていた。中身を `Column` でラップして縦並びを保証。
- **Health Connect 記録方法**: `HealthConnectWriter` の `ExerciseSessionRecord` / `HeartRateRecord` を `Metadata.manualEntry(...)` から `Metadata.activelyRecorded(device = Device(TYPE_WATCH), ...)` に変更。Wear で実測しているのに手動入力扱いだったため、手動エントリを運動時間集計から除外する連携アプリで時間が反映されなかった想定への対処。
- **Health Connect title→notes**: `ExerciseSessionRecord.title`（ワークアウト名）を `null` にし、名前は `notes` へ移動。Android Health の一覧で主見出しが名前に置き換わり継続時間が前面に出なくなる挙動を避ける。継続時間（`startTime`〜`endTime`）の書き込み自体は従来から不変。

### Play 提出時の注意
- 内部テストで Wear→Companion→Health Connect 経由の記録が、カロミル等で**運動時間として認識される**ことを実機確認してから製品版へ昇格すること（recordingMethod 変更は仮説ベースの対処のため）。
- 既存（旧バージョンで手動入力として書き込み済み）のレコードは遡って修正されない。本バージョン以降の新規記録から `activelyRecorded` になる。
