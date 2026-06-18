# Intervo 1.9.3 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
・プライバシー改善: Bluesky/PDS 連携をご利用の場合、PDS へ送る運動記録から心拍数を含めないようにしました。運動の時間や種目などの記録はこれまで通り保存されます。
・設定画面の「PDS に再同期」を実行すると、PDS 上の過去の記録もすべて、心拍数を含まない最新の形式に上書きされます。連携をご利用中の方は一度の再同期をおすすめします。
・心拍数は引き続きウォッチでの計測や Health Connect への保存に利用できます。今回の変更は PDS への送信内容のみが対象です。
・ウォッチアプリの機能に変更はありません。
```

---

## Play Console（ユーザー向け・詳細）

### 概要
Bluesky/PDS 連携で利用者指定の PDS へ送信する運動記録から、心拍数データを除外しました。プライバシーに配慮し、心拍数は端末内および Health Connect での利用に限定されます。

### 新機能 / 修正・改善
- **PDS 記録から心拍数を除外** — PDS に保存する運動記録（`dev.marufeuille.workout.session`）から、セッション全体および種目ごとの心拍数を含めないようにしました。運動の時間・種目情報・予定セット・実施セットはこれまで通り記録されます。
- **過去記録の一括上書き** — 設定画面の手動「PDS に再同期」で、PDS 上の既存記録もすべて最新の形式（心拍数なし）で上書きされます。過去に送信済みの心拍数入りレコードを置き換えられます。
- **プライバシーポリシーの更新** — PDS へは「心拍数データを除く運動記録」を送信する旨に記載を更新しました。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 109030 / `versionName` 1.9.3 → `wear:internal` トラック
- companion（phone）: `versionCode` 109031 / `versionName` 1.9.3 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。1.9.2（Wear 109020 / phone 109021）より単調増加。

### 主な変更
- **#54 Remove heart rate from PDS records** — `WorkoutSessionRecordMapper` から心拍フィールドを除去し、PDS ペイロードを心拍なしに変更。`CompanionRepository` に全履歴再送の `rewriteAllPds()` を追加（rkey=履歴 ID 固定で同一 record への冪等 upsert）。手動再同期 UI を `writePendingPds()` → `rewriteAllPds()` に切り替え、ステータス文言を「再同期」に更新。DAO に `getAllForPdsRewrite()` を追加。マッパーのユニットテストを心拍非依存に更新。privacy-policy.html / build.md を更新。

### Play 提出時の注意
- companion のみの変更。Wear app（`wear:internal`）に機能差分はない。
- データセーフティ申告: PDS（外部）への送信データから心拍数を除外したため、PDS 経由の「健康とフィットネス（心拍数）」の外部共有がなくなる。Play Console のデータセーフティで PDS 送信に関する心拍数の申告を見直すこと（心拍数は端末内保存と Health Connect 書込のみに限定）。
- DB スキーマの破壊的変更はなし（DAO クエリ追加のみ）。
