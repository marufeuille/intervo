# CLAUDE.md

Intervo は Wear OS 向けインターバル・トレーニングタイマー。2 モジュール構成:

- `app` — Wear OS アプリ（ワークアウト計測・心拍取得・Tile/Complication）
- `companion` — スマートフォン用アプリ（ウォッチから受信した完了履歴をローカル保存し Health Connect へ書込）。`app` と `applicationId` を共有するマルチフォームファクタ構成。

## リリース（タグ駆動 CI/CD）

`git tag vX.Y.Z && git push origin vX.Y.Z` で、app/companion を署名ビルドし Play 内部テストへ自動配信する（`.github/workflows/release.yml`）。手順の全体・必要な Secrets・Play 設定は **`docs/release-ci.md`** を参照。

タグを打つ前に必ず:

1. **リリースノートを用意**: `docs/release-notes-TEMPLATE.md` をコピーして `docs/release-notes-<VERSION>.md` を作成・記入。
   「Play Console『最新情報』用」見出し直下のコードフェンス（500字以内・空不可）が Play の「最新情報」になる。
   - ファイル名と形式は PR 時に `validate-release-notes` ワークフローが検証。ローカル確認: `python3 scripts/check_release_notes.py docs/release-notes-<VERSION>.md`
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
- `validate-release-notes.yml` — リリースノート変更時にファイル名と形式を検証（変更ファイルのみ対象）。

開発フローと運用ルール:

1. PR は軽量層（build）が緑になればマージ可。**マージは人間が行う**（PR 必須チェックは `build` のみ。E2E を必須にすると PR が永久にマージ不可になる）。
2. **マージ後は main の E2E を必ず確認する**。落ちたら放置せず、原因を分析して修正 PR を出す（main を常にリリース可能な状態に保つ）。
3. リリース前に不安があれば、タグを打つ前に `release.yml` を `workflow_dispatch` で実行し、E2E + R8 ビルドが緑であることを事前確認できる（配信はされない）。

## 開発用コマンドラッパー（権限プロンプト省略）

`adb` / `gradlew` / `git` は権限チェックが都度走って開発に支障が出るため、PR 作成までの日常操作だけを**ホワイトリスト方式**で通す薄ラッパーを `scripts/` に置いている。許可エントリは `.claude/settings.local.json` に登録済みで、プロンプトなしで実行できる。

| スクリプト | 役割 | 例 |
|---|---|---|
| `scripts/g` | gradlew（許可タスクのみ） | `scripts/g assembleDebug` / `scripts/g :app:testDebugUnitTest` |
| `scripts/a` | adb（読み系・インストール系のみ） | `scripts/a devices` / `scripts/a shell ...` / `scripts/a logcat` |
| `scripts/gitw` | git（PR作成までの作業系） | `scripts/gitw status` / `scripts/gitw add -A` / `scripts/gitw push -u origin HEAD` |
| `scripts/watch-adb` | 実機向け adb 統合（既存） | `scripts/watch-adb reinstall`（ビルド+インストール+起動） |

**AI エージェントが守るルール:**

1. `adb` / `./gradlew` / `git` を直接叩く前に、対応するラッパーで代用できないか必ず確認する。
2. ラッパーが拒否した（＝ホワイトリスト外・破壊的操作）場合は、**直接生コマンドを実行してよい**（ask プロンプトが走るのが意図通り）。
3. 拒否される代表例:
   - `g`: `clean`, `publish` 系, 未知タスク → `./gradlew clean` を直接
   - `a`: `uninstall`, `reboot`, `root`, `shell pm uninstall`, `shell am force-stop`, `push` → `adb uninstall ...` を直接
   - `gitw`: `reset --hard`, `clean`, `merge`（--ff-only 以外）, `tag` 作成, `push --force`, `push origin main` → `git reset --hard` 等を直接（push/merge/rebase は ask 設定が効く）

**設計意図:** 「完全オープン」ではなく、PR を作るところまでの操作は摩擦ゼロ、破壊的操作は従来通り `ask` プロンプトで確認を挟む。`git push` は feature ブランチへの初回 push（`-u origin HEAD`）のみ許可し、`main`/`master` と `--force` 系は生 git に誘導して ask を効かせる。

各ラッパーの冒頭に usage がある: `scripts/g help` / `scripts/a help` / `scripts/gitw help`。

## タスク管理

開発タスク・バックログは **GitHub Issues** で管理する（ラベル: bug/flaky/test/tech-debt/ci/release/enhancement）。

## Cursor Cloud specific instructions

Cloud VM は起動時に update script（JDK 17 + Android SDK のセットアップ）が実行済みで、`~/.bashrc` に `JAVA_HOME`（JDK 17）と `ANDROID_HOME`（`~/Android/Sdk`）が export される。**非対話 shell（`Shell` ツールの単発コマンド）では `~/.bashrc` が読まれない**ため、Gradle/adb を直接叩くコマンドの先頭で毎回 `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=$HOME/Android/Sdk` を明示するか、`scripts/g`（JAVA_HOME を自前解決する）経由で実行すること。**JDK 21 が system default なので JAVA_HOME を渡さないと AGP が失敗する**。

- 日常の検証（速い層）: `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`（CI の `build` ジョブと同じ）。companion は `./gradlew :companion:testDebugUnitTest :companion:assembleDebug`。標準タスク一覧は本ファイル上部と `docs/build.md` を参照。
- `local.properties`（`sdk.dir`）は gitignore 対象。update script 側では作らないので、無ければ `echo "sdk.dir=$ANDROID_HOME" > local.properties` を作る（未作成でも `ANDROID_HOME` が効いていればビルドは通る）。
- **Wear OS / phone エミュレータ（`connectedDebugAndroidTest` 等の instrumented / E2E）はこの Cloud VM では実用にならない**。`/dev/kvm` が無く（ネスト仮想化が露出していない）、エミュレータが純ソフトウェア（QEMU TCG）で動くため極端に遅い。ブートは完了しアプリの起動・画面遷移までは確認できるが、`system_server` が高頻度で ANR/一時停止し、Gradle の APK 再インストール工程が `Can't find service: package` で失敗する。instrumented テストの検証は CI（GitHub Actions の `instrumented-test` / `release.yml` の `e2e`、KVM 有効）に委ねる。手元では unit テスト＋`assembleDebug`＋`lintDebug` で担保する。
- GUI 目視が必要なときはエミュレータを headless（`-no-window -gpu swiftshader_indirect -accel off`）で起動し、`adb exec-out screencap -p` でスクショを取る（computer use は headless エミュレータを映せない）。起動直後は Wear のセットアップウィザードが前面に出るので `settings put secure user_setup_complete 1` / `settings put global device_provisioned 1` を設定し、権限は `pm grant` で付与しておくと良い。
