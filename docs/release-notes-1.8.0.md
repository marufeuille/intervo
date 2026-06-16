# Intervo 1.8.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
スマートフォン用アプリを改善しました。
・ワークアウトの完了履歴を端末内とヘルスコネクトにのみ保存する方式へ変更し、外部サーバーへの送信を廃止しました
・これに伴いログイン・同期に関する設定を削除しました
ウォッチアプリの機能に変更はありません。
```

---

## Play Console（ユーザー向け・詳細）

### 概要
スマートフォン用アプリ（companion）から外部バックエンド（BigQuery / Cloud Functions / Firebase）への同期機能を完全に撤去し、ウォッチから受信した完了履歴を端末内に保存して Health Connect（ヘルスコネクト）へ書き込むことに集約しました。データが外部サーバーへ送信されることはありません。

### 修正・改善
- **同期機能の撤去** — ログイン・認証・同期の設定画面（SyncPanel）を削除し、履歴は端末内保存 + Health Connect 書込のみに集約しました。
- **プライバシーの明確化** — 外部送信が一切なくなり、プライバシーポリシーを「外部送信なし」に更新しました。
- **依存・権限の削減** — Firebase / google-services プラグイン / INTERNET 権限を除去しました。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 108000 / `versionName` 1.8.0 → `wear:internal` トラック
- companion（phone）: `versionCode` 108001 / `versionName` 1.8.0 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。現行（Wear 107040 / phone 107041）より単調増加。

### 主な変更
- **BigQuery / Firebase 同期を完全撤去**（PR #44）。`CompanionRepository` から同期・認証・エンドポイントを撤去し、ViewModel/UI の SyncPanel・認証 UI を削除。Header/履歴行を Health Connect 書込状態ベースに変更。
- DAO の同期クエリを削除し `pendingHealthConnectCount` を追加。
- **DB を v5 化**。同期カラムを撤去しマイグレーションを全廃、`fallbackToDestructiveMigration(dropAllTables)` で作り直す方式に変更。
- Firebase 依存 / google-services プラグイン / INTERNET 権限を除去。`firebase.json` / `.firebaserc` / `functions/` を削除。
- `privacy-policy.html` を「外部送信なし」に更新。`README` / `CLAUDE.md` を修正。

### Play 提出時の注意
- **companion の DB は破壊的再作成**（`fallbackToDestructiveMigration(dropAllTables)`）。既存テスターの端末では本バージョン更新時にローカルの完了履歴がリセットされる。アップグレード時のデータ消失に注意。
- companion から **INTERNET 権限が除去**され、外部送信が一切なくなった。Play のデータセーフティ申告（収集・送信なし）を必要に応じて見直すこと。
- 本リリースは companion 主体の変更で、**app（Wear）側の機能変更はなし**。ただし両 AAB とも versionCode が更新され同一リリースで配信される。
