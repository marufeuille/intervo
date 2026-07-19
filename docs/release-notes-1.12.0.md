# Intervo 1.12.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
・ウォッチ: ワークアウトの記録時間を修正しました。これまで一時停止した時間が合計時間に含まれず、実際より短く記録されることがありましたが、開始から完了までの実際の経過時間で記録されるようになります。
・スマートフォン用アプリ: Bluesky/PDS 連携機能を終了しました。ワークアウト記録の同期先は Health Connect のみになります。端末内の履歴や Health Connect に保存済みのデータはそのまま利用できます。ワークアウト内容を Bluesky 投稿用の文章として共有する機能は引き続き利用できます。
```

---

## Play Console（ユーザー向け・詳細）

### 概要
ウォッチ側でワークアウトの記録時間が実際より短くなる不具合を修正しました。あわせて、スマートフォン用アプリの Bluesky/PDS 連携機能を終了し、記録の同期先を Health Connect のみに整理しました。

### 新機能 / 修正・改善
- **一時停止した時間も記録時間に含めるよう修正（ウォッチ）** — ワークアウト中に一時停止（休憩を延ばすためのポーズなど）をすると、その時間が合計時間から抜け落ちて実際より短く記録されていました。記録時間を「開始から完了までの実際の経過時間」に変更し、ウォッチの履歴・スマートフォンでの表示・Health Connect のセッション時間が実態と一致するようになります。一時停止中に残り時間のカウントダウンが止まる動作はこれまでどおりです。
- **Bluesky/PDS 連携の終了（スマートフォン用アプリ）** — ワークアウト記録を利用者指定の PDS へ保存する機能を終了しました。設定画面の連携設定と履歴画面の同期表示がなくなり、同期先は Health Connect のみになります。端末内の履歴や Health Connect に保存済みのデータには影響ありません。ワークアウト内容を Bluesky 投稿用の文章として共有する機能は引き続き利用できます。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 112000 / `versionName` 1.12.0 → `wear:internal` トラック
- companion（phone）: `versionCode` 112001 / `versionName` 1.12.0 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。1.11.0（Wear 111000 / phone 111001）より単調増加。

### 主な変更
- **#75 fix(timer): ポーズ時間をワークアウト記録時間に含める** — `elapsedSecondsNow()` がポーズ累積を差し引いていたため、ポーズ時間が `totalSeconds`（Wear 履歴・companion 表示・Health Connect セッション長の唯一のワークアウト時間）から丸ごと消えていた。記録時間を「開始〜完了の実時計」に変更し、不要になったポーズ会計（`pausedAtElapsedMillis` / `totalPausedMillis` / `foldPauseTime`）を削除。副作用として Health Connect の開始時刻ズレも解消。各フェーズのカウントダウン凍結（ポーズ中の正しい挙動）は不変。変更は `TimerService.kt` のみ。
- **#76 feat(companion): PDS連携を削除する (sc-30)** — Bluesky/PDS への直接同期（XRPC client・認証情報ストア・record mapper・設定画面の PDS カード・同期状態カラム）を撤去し、companion の同期先を Health Connect のみに。`pds` パッケージ（`PdsCredentialsStore` / `PdsDirectClient` / `WorkoutPdsRecordMapper`）とユニットテストを削除、`CompanionWorkoutHistory` から `pdsSyncedAt` を削除して Room を v8 へ（既存方針どおり destructive migration）。履歴一覧/詳細の PDS チップ（`ChipKind.Pds`）撤去、`SyncWorker` / Debug receiver / E2E / docs を Health Connect のみの構成へ更新。Bluesky 投稿文の共有機能（`social` パッケージ）は PDS へ書き込まないため残す（約 -1,100 行）。

### Play 提出時の注意
- **companion の DB は破壊的変更（Room v7 → v8）**。更新時にローカル履歴 DB は破棄・再作成される（`fallbackToDestructiveMigration(dropAllTables = true)`、内部テスト段階の既存方針を継続。履歴はウォッチからの再受信で取り込まれる）。
- **`docs/privacy-policy.html` に PDS 連携の記述が残っている**（#76 で未更新）。機能自体は削除済みで外部送信は減る方向のため申告上の追加リスクはないが、提出前にプライバシーポリシーから PDS/Bluesky 同期の記述を削除する更新を推奨（Bluesky 投稿文の共有機能に関する記述は要否を確認）。
- 権限の追加はなし。companion の `INTERNET` 権限は Health Connect 以外の用途がなくなったが宣言は残存（削除は任意・別途検討）。データセーフティ申告に影響する新規の外部送信はなし（むしろ PDS への送信経路が消滅）。
- 過去に保存された PDS 認証情報（App Password 等）を読み書きするコードは削除済みだが、端末内の保存領域を明示的に消去する処理は入れていない。
- 両モジュールに機能変更があるリリース。両 AAB とも versionCode が更新され、app=`wear:internal` / companion=`internal` の各トラックへ配信される。
