# Intervo 1.7.2 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
不具合修正とヘルスケア連携の改善を行いました。
・ワークアウト詳細画面で、ワークアウト名と種別が重なって見えなくなる表示崩れを修正
・Health Connect への運動記録を「実際に計測した記録」として書き込むよう改善し、運動の実施時間が他のヘルスケアアプリ（カロミル等）で運動時間として認識されやすくなりました
```

---

## Play Console（ユーザー向け・詳細）

### 概要
表示崩れの修正と Health Connect 連携の改善を含むメンテナンスリリースです。ウォッチ（Intervo）とスマートフォン（コンパニオン）の両方を 1.7.2 に揃えました。特に Health Connect 記録の改善は、スマートフォン側アプリへ今回のバージョンで初めて反映されます。

### 修正・改善
- **表示崩れの修正** — ワークアウトを選択した後の詳細画面で、ワークアウト名と種別が重なってワークアウト名が見えなくなることがある問題を修正しました。
- **ヘルスケア連携の改善** — Health Connect への運動記録を「アクティブに計測した記録」として書き込むよう変更しました。これにより、連携先のヘルスケアアプリ（カロミル等）で運動の実施時間が運動時間として認識されやすくなります。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 107020 / `versionName` 1.7.2 → `wear:internal` トラック
- companion（phone）: `versionCode` 107021 / `versionName` 1.7.2 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。現行（Wear 21 / phone 20）より単調増加。

### このリリースの位置づけ
- **タグ駆動 CI/CD による初の自動リリース**。`git tag v1.7.2 && git push` で app/companion を署名ビルドし、Play 内部テストへフォームファクタ別に自動配信した（詳細は [release-ci.md](release-ci.md)）。
- **ユーザー向けコードの実体は 1.7.1 の修正と同一**（詳細画面のレイアウト修正 + Health Connect の `activelyRecorded` 化）。1.7.2 では追加のコード変更はなく、CI/CD・ドキュメント整備が中心。
- 1.7.1 のコード修正は phone(companion) には未反映（phone は 1.7.0 のまま）だったため、本 1.7.2 で**両フォームファクタを同一バージョンに統一**し、Health Connect 記録の改善を phone にも反映する。

### 主な変更（1.7.1 から引き継いだユーザー向け修正の詳細）
- **詳細画面のレイアウト修正**: `WorkoutDetailScreen` 先頭の `item {}` 内に名前→`Spacer`→種別 Chip を直接並べていたため `ScalingLazyColumn` が重ねて描画していた。`Column` でラップして縦並びを保証。
- **Health Connect 記録方法**: `HealthConnectWriter` の `ExerciseSessionRecord` / `HeartRateRecord` を `Metadata.manualEntry(...)` から `Metadata.activelyRecorded(device = Device(TYPE_WATCH), ...)` に変更。`title`（ワークアウト名）は `notes` へ移動。

### Play 提出時の注意
- 内部テストで Wear→Companion→Health Connect 経由の記録が、カロミル等で**運動時間として認識される**ことを実機確認してから製品版へ昇格すること（recordingMethod 変更は仮説ベースの対処のため）。
- 既存（旧バージョンで手動入力として書き込み済み）のレコードは遡って修正されない。本バージョン以降の新規記録から `activelyRecorded` になる。
- 初回 API 自動配信にあたり、Play Console 側でプライバシーポリシー・データセーフティ・フォアグラウンドサービス権限（FOREGROUND_SERVICE_HEALTH、デモ動画リンク添付）・健康アプリの各申告を実施済み。
