# Intervo 1.9.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
スマートフォン用アプリを大きく更新しました。
・履歴一覧/詳細/設定をタブで整理し、完了したワークアウトの内容を見返しやすくしました
・任意の Bluesky/PDS 設定から、履歴を PDS に直接同期できるようになりました
・実施したセットの回数・時間・完了状況を履歴に保存し、同期データにも反映します
```

---

## Play Console（ユーザー向け・詳細）

### 概要
スマートフォン用アプリを履歴と設定の 2 タブ構成に整理し、完了したワークアウトの詳細を見返しやすくしました。任意で Bluesky/PDS の設定を保存すると、受信済みの履歴を PDS へ直接同期できます。

### 新機能 / 修正・改善
- **履歴画面の整理** — 履歴一覧、履歴詳細、設定を分け、ボトムナビゲーションで移動できるようにしました。
- **履歴詳細の追加** — ワークアウトのサマリー、心拍、種目ごとの予定と実績を確認できる詳細画面を追加しました。
- **Bluesky/PDS 直接同期** — PDS URL、ハンドル、App Password を設定すると、履歴を `dev.marufeuille.workout.session` record として PDS に同期できます。
- **実施セットの記録** — ウォッチアプリで実施したセットの回数・時間・完了状況を履歴に保存し、詳細表示と PDS 同期データに反映します。
- **設定画面の整理** — Health Connect 連携と Bluesky/PDS 連携を設定タブに集約しました。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 109000 / `versionName` 1.9.0 → `wear:internal` トラック
- companion（phone）: `versionCode` 109001 / `versionName` 1.9.0 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。現行（Wear 108000 / phone 108001）より単調増加。

### 主な変更
- **companion のマルチスクリーン化**（PR #47）。履歴一覧 / 詳細 / 設定の 2 タブ + 詳細 push 構成へ変更し、ViewModel と軽量ナビゲーションを分離。
- **PDS 直接同期**（PR #48）。App Password 認証、ハンドル解決、`com.atproto.repo.putRecord` による `dev.marufeuille.workout.session` への upsert を追加。
- **performed sets の保存・転送**（PR #48）。Wear 側で実施セットを `performedSetRecords` として保持し、Data Layer 経由で companion へ渡す。
- **companion DB v7**。PDS 同期状態と performed sets を追加。内部テスト前提として `fallbackToDestructiveMigration(dropAllTables)` を継続。
- **PDS 認証情報の保存**。PDS URL / ハンドル / App Password を端末内に保存し、App Password は Android Keystore の鍵で暗号化。
- **検証**。`:app` の performed sets 関連ユニットテストと `:companion` の PDS record mapper テストを追加。

### Play 提出時の注意
- **外部送信の扱いが 1.8.0 から変わる**。PDS 設定を保存した場合、運動履歴・心拍・実施セット情報が利用者指定の PDS へ送信される。Play のデータセーフティ申告と公開中のプライバシーポリシーを 1.9.0 の内容に合わせて確認すること。
- **companion の DB は破壊的再作成**（`fallbackToDestructiveMigration(dropAllTables)`）。既存テスターの端末では本バージョン更新時にローカルの完了履歴がリセットされる可能性がある。
- PDS 同期は任意設定。未設定の場合、履歴は端末内保存と Health Connect 書込のみで動作する。
