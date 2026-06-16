---
name: cut-release
description: intervo のタグ駆動リリースを安全に実行する手順。バージョン決定 → リリースノート作成・検証 → main の準備確認 → 明示合意 → tag push まで。「リリースして」「v1.8.1 を出して」「次のバージョンを配信して」などで使う。
---

# cut-release — intervo タグ駆動リリース

`git tag vX.Y.Z && git push origin vX.Y.Z` で app/companion を署名ビルドし Play 内部テストへ自動配信する（`.github/workflows/release.yml`）。**tag push は外向き・取り消しづらい操作**なので、下記ゲートを必ず通す。詳細仕様は `CLAUDE.md` と `docs/release-ci.md`。

## 0. 前提の確認
- `git status` がクリーンで、リリース対象が **main に揃っている**こと。
- 現在の最新タグを確認: `git describe --tags --abbrev=0`。次バージョン `vX.Y.Z` をユーザーと合意（patch/minor/major のどれか）。

## 1. リリースノートの草案
- `release-notes-writer` サブエージェントに「次バージョン <VERSION>」を渡して草案を作らせる（直近タグからの `git log` / マージ済み PR を隔離して読み、所定フォーマットの Markdown を返す）。
- 返ってきた草案を `docs/release-notes-<VERSION>.md` として **Write**（ファイル書き込みは本スレで行う）。`docs/release-notes-TEMPLATE.md` の構成に従う。

## 2. フォーマット検証（緑必須）
```
python3 scripts/check_release_notes.py docs/release-notes-<VERSION>.md
```
- 見出し `Play Console「最新情報」用` 直下のコードフェンスが**存在・空でない・500字以内**であること。落ちたら直してから先へ。
- ノートが無い／空のままタグを打つと release ワークフローは**意図的に失敗**する。

## 3. versionCode は触らない
- versionCode は semver から **自動採番**（`(major*10000+minor*100+patch)*10 + offset`、app=0 / companion=1）。`build.gradle.kts` を手編集しない。
- 配信トラックはフォームファクタ別: app(Wear)=`wear:internal` / companion(phone)=`internal`。両 AAB を 1 リリースに混在させない（これは workflow 側が担保）。

## 4. （任意）事前ゲート確認
不安があれば、タグを打つ前に `release.yml` を **`workflow_dispatch`** で実行し、E2E + R8 リリースビルドが緑であることを配信なしで確認できる（`gh workflow run release.yml`）。

## 5. ⚠️ 合意 → tag push
- ここまでの差分（`git log <last-tag>..HEAD --oneline`）と作成したリリースノートの「最新情報」部分をユーザーに**提示**し、「打って」の**明示合意**を得る。
- 合意後にのみ実行:
```
git tag vX.Y.Z
git push origin vX.Y.Z
```
- push 後は `gh run watch`（または `gh run list --workflow release.yml`）で release ワークフローを見届け、`e2e` → `release` が緑で Play 配信まで通ったことを確認する。落ちたら配信されない（`release` は `needs: e2e`）。

## メモ
- マージ自体は人間が行う運用（PR 必須チェックは `build` のみ）。本スキルは**タグを打つ**フェーズに責務を絞る。
- リリースノートのコミットは慣例上 `docs(release): vX.Y.Z リリースノートを追加`。
