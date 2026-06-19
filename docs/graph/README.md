# アーキテクチャグラフ（graphify）

[graphify](https://github.com/safishamsi/graphify) で生成した Intervo のアーキテクチャ知識グラフ。
GitHub Pages（`main/docs` legacy）で公開される。

## 公開 URL

- インタラクティブグラフ: <https://marufeuille.github.io/intervo/graph/graph.html>
- 監査レポート: <https://marufeuille.github.io/intervo/graph/GRAPH_REPORT.md>

## ファイル

| ファイル | 内容 |
|---|---|
| `graph.html` | インタラクティブな依存グラフ。ブラウザで開く（ノード 1,097 / エッジ 1,827 / コミュニティ 75）。 |
| `GRAPH_REPORT.md` | 監査レポート（God Nodes / Surprising Connections / Suggested Questions / トークンコスト）。 |

## 見方

`graph.html` をブラウザで開く。コミュニティ（色分けされたクラスタ）単位で全体構造を俯瞰し、
ノードをクリックすると接続が見える。**God Nodes**（最も接続の多い中核）が依存の中心。

## 更新手順

graphify のフルビルド（セマンティック抽出）は Claude サブエージェント経由が前提のため、
CI（headless）ではなく**ローカルの Claude Code 上で実行**して成果物をコミットする。

1. リポジトリルートで Claude Code を開き `/graphify` を実行（差分更新なら `/graphify --update`）。
   - 抽出範囲は対話で選べる。アプリアイコン類は除外、スクリーンショットとドキュメントを含めると UI 画面構造まで把握できる。
2. 生成物をこのディレクトリへコピー:
   ```sh
   cp graphify-out/graph.html      docs/graph/graph.html
   cp graphify-out/GRAPH_REPORT.md docs/graph/GRAPH_REPORT.md
   ```
3. コミット & プッシュ。Pages が自動で更新する。

更新の目安: 大きなリファクタリング、新モジュール追加、リリースの節目。コード構造（コールグラフ）は
AST 抽出で自動追従するが、コミュニティ再分割や God Nodes の変化を反映したいときに再生成する。

## CI 自動化しない理由

graphify のフルビルドはホスト LLM（Claude サブエージェント）を前提としており、GitHub Actions の
headless 環境では `graphify update`（AST 差分のみ・LLM 不要）しか素直に動かない。また
`graph.html`（≈1 MB）/ `graph.json`（≈1 MB）を毎 push で自動コミットするとリポジトリ履歴が膨らむ。
そのため人間のタイミングで更新する運用にしている。将来 `graph.json` を CI に食わせて `graph.html`
を再生成・デプロイする半自動化も可能（その際は Pages を Actions デプロイに移行する）。

## 既知の限界：グラフの「見えない橋」

ソースコードの静的解析ベースのため、**実行時に繋がるがコード上で離れたコンポーネント**の橋は
エッジにならない。代表例:

- **Wear Data Layer** — `app` の `WorkoutHistorySyncClient.send()` と `companion` の
  `WorkoutHistoryListenerService.onDataChanged()` は Wear OS のメッセージ通信で繋がるが、
  graphify の `path` では到達不可（別コミュニティに孤立する）。
- **Intent / IPC / WorkManager の遅延実行** も同様。

これらの end-to-end データフローを追うときは、graphify の構造情報を出発点にしつつ該当ファイルを
直接読んで補う。セマンティック抽出で「送受信ペア」を INFERRED エッジとして補強すれば橋を生やすことも
できる（`/graphify --mode deep` や抽出プロンプトの調整）。
