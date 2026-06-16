# CLAUDE.md

Intervo は Wear OS 向けインターバル・トレーニングタイマー。2 モジュール構成:

- `app` — Wear OS アプリ（ワークアウト計測・心拍取得・Tile/Complication）
- `companion` — スマートフォン用アプリ（ウォッチから受信した完了履歴をローカル保存し Health Connect へ書込）。`app` と `applicationId` を共有するマルチフォームファクタ構成。

## リリース（タグ駆動 CI/CD）

`git tag vX.Y.Z && git push origin vX.Y.Z` で、app/companion を署名ビルドし Play 内部テストへ自動配信する（`.github/workflows/release.yml`）。手順の全体・必要な Secrets・Play 設定は **`docs/release-ci.md`** を参照。

タグを打つ前に必ず:

1. **リリースノートを用意**: `docs/release-notes-TEMPLATE.md` をコピーして `docs/release-notes-<VERSION>.md` を作成・記入。
   「Play Console『最新情報』用」見出し直下のコードフェンス（500字以内・空不可）が Play の「最新情報」になる。
   - 形式は PR 時に `validate-release-notes` ワークフローが検証。ローカル確認: `python3 scripts/check_release_notes.py docs/release-notes-<VERSION>.md`
   - ノートが無い／空のままタグを打つと Release ワークフローは**意図的に失敗**する。
2. リリース対象が main に揃っていること。

仕組みの要点:

- **versionCode は semver から自動採番**（`(major*10000+minor*100+patch)*10 + offset`、app=0 / companion=1）。`build.gradle.kts` を手で編集しない。ローカルビルドは `VERSION_NAME` 未設定でフォールバック。
- **配信トラックはフォームファクタ別**: app(Wear)=`wear:internal` / companion(phone)=`internal`。両 AAB を 1 リリースに混在させない。
- 署名鍵・サービスアカウント鍵は Claude では扱わない。GitHub Secrets はユーザーが登録する。

## CI / 開発フロー

CI は **2 層 + リリース前ゲート**で責務を分ける。「ロジックが壊れてないか」は速い層で、「画面・体験が壊れてないか」は重い層で担保する。

- **軽量層（`ci.yml` の `build`、PR・main push 共通）** — `:app` のコンパイル + Lint + ユニットテストのみ（実機を起動しないので速い）。**PR の高速フィードバックはこの層だけ**。`docs/**` と `*.md` のみの変更ではスキップ（`paths-ignore`）。
- **E2E 層（`ci.yml` の `instrumented-test`、main push のみ）** — Wear OS エミュレータで `connectedDebugAndroidTest`。マージ直後に体験破壊を早期検知するのが目的。**PR では走らない**。
- **リリース前ゲート（`release.yml` の `e2e`、tag push / `workflow_dispatch`）** — 配信ジョブ `release` が `needs: e2e`。E2E が落ちたら**配信されない**。R8 リリースビルドもこの段で初めて通る。
- `validate-release-notes.yml` — リリースノート変更時に形式を検証（変更ファイルのみ対象）。

開発フローと運用ルール:

1. PR は軽量層（build）が緑になればマージ可。**マージは人間が行う**（PR 必須チェックは `build` のみ。E2E を必須にすると PR が永久にマージ不可になる）。
2. **マージ後は main の E2E を必ず確認する**。落ちたら放置せず、原因を分析して修正 PR を出す（main を常にリリース可能な状態に保つ）。
3. リリース前に不安があれば、タグを打つ前に `release.yml` を `workflow_dispatch` で実行し、E2E + R8 ビルドが緑であることを事前確認できる（配信はされない）。

## タスク管理

開発タスク・バックログは **GitHub Issues** で管理する（ラベル: bug/flaky/test/tech-debt/ci/release/enhancement）。
