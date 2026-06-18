# Intervo 1.9.4 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
スマートフォン用アプリを改善しました。
・ワークアウト履歴の詳細画面に「Bluesky 投稿」の下書きを追加しました。完了したトレーニングの種目・セット・回数・時間をまとめた投稿文が自動で作られます。
・「共有」ボタンから、お使いの Bluesky アプリや他のアプリへ下書きをそのまま送れます。投稿前に内容を確認・編集できます。
・投稿文が長くなる場合は複数ページに分割し、「前へ／次へ」で切り替えられます。
・自動で投稿することはありません。共有するかどうかは毎回ご自身で選べます。
・ウォッチアプリの機能に変更はありません。
```

---

## Play Console（ユーザー向け・詳細）

### 概要
スマートフォン用アプリ（companion）のワークアウト詳細画面に、Bluesky 向けの投稿下書きを表示する機能を追加しました。完了したトレーニング内容から投稿文を自動生成し、Android の共有メニュー経由で任意のアプリへ送れます。自動投稿は行いません。

### 新機能 / 修正・改善
- **Bluesky 投稿の下書き生成** — ワークアウト詳細から、トレーニング名・各種目の実施セット数・回数・時間をまとめた投稿文を自動で作成します。末尾に `#Intervo` ハッシュタグを付与します。
- **長文の自動ページ分割** — Bluesky の文字数上限（300 グラフェム）に収まるよう投稿文を複数ページに分割し、「前へ／次へ」で切り替えられます。
- **共有メニューからの送信** — 「共有」ボタンで現在のページの下書きテキストを Android の共有シートへ渡し、Bluesky アプリなどへそのまま送れます。送信前に内容を確認できます。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 109040 / `versionName` 1.9.4 → `wear:internal` トラック
- companion（phone）: `versionCode` 109041 / `versionName` 1.9.4 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。1.9.3（Wear 109030 / phone 109031）より単調増加。

### 主な変更
- **#56 Add Bluesky post draft sharing** — companion に `SessionPostDraftComposer` を新設。`CompanionWorkoutHistory`（種目スナップショット JSON・実施セット JSON・ワークアウト名）から Bluesky 風の投稿下書きを生成。種目ごとに「実施/予定セット・実施回数・実施時間」を整形し、未実施種目は「未実施（N セット予定）」と表示。投稿は BreakIterator によるグラフェム計測で 300 グラフェム上限に収め、超過時は「（続き）」付きで複数ポストにページ分割（1 ページ目のみ `#Intervo` 付与、単一行が上限超なら末尾省略）。`HistoryDetailViewModel` の `UiState.Loaded` に `postDraft` を追加し、`HistoryDetailScreen` に `PostDraftCard`（下書きプレビュー＋前へ/次へ＋共有）を追加。共有は `Intent.ACTION_SEND`（text/plain）の共有シート経由で、アプリ自身からの自動投稿・ネットワーク送信は行わない。`SessionPostDraftComposerTest` でページ分割・整形を検証。

### Play 提出時の注意
- companion のみの変更。Wear app（`wear:internal`）に機能差分はないが、両 AAB とも versionCode が更新され同一リリースで配信される。
- 投稿は Android 標準の共有シート経由でユーザーが手動送信するのみ。アプリから外部サーバーへ自動送信する処理は追加していないため、データセーフティ申告に影響する新たな外部送信・権限追加はなし（新規パーミッションなし）。
- DB スキーマの破壊的変更はなし（既存の履歴データを読み取って投稿文を生成するのみ）。
