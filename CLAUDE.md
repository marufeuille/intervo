# CLAUDE.md

Intervo は Wear OS 向けインターバル・トレーニングタイマー。2 モジュール構成:

- `app` — Wear OS アプリ（ワークアウト計測・心拍取得・Tile/Complication）
- `companion` — スマートフォン用アプリ（Health Connect 書込・バックエンド同期）。`app` と `applicationId` を共有するマルチフォームファクタ構成。

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

## CI

- `ci.yml` — main / PR で `:app` のビルド・ユニットテスト・R8・Wear エミュレータ E2E。`docs/**` と `*.md` のみの変更ではスキップ（`paths-ignore`）。
- `validate-release-notes.yml` — リリースノート変更時に形式を検証（変更ファイルのみ対象）。

## タスク管理

開発タスク・バックログは **GitHub Issues** で管理する（ラベル: bug/flaky/test/tech-debt/ci/release/enhancement）。
