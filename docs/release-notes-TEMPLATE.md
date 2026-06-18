<!--
新しいリリースのたびに、このテンプレートを docs/release-notes-<VERSION>.md にコピーして記入する。
（例: docs/release-notes-1.7.3.md）

重要:
- 「Play Console「最新情報」用」見出しの直下のコードフェンス ``` ... ``` の中身を、
  CI がそのまま Play の「最新情報（What's new / ja-JP）」として登録する。
- ファイル名は docs/release-notes-X.Y.Z.md（v なし）。このフェンスは必須・空にしない・500 文字以内。
  ファイル名と形式は CI（validate-release-notes）で検証される。
- ローカル確認: python3 scripts/check_release_notes.py docs/release-notes-<VERSION>.md
-->

# Intervo <VERSION> Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
（ここにユーザー向けの「最新情報」を書く。箇条書き例）
・新機能や修正の要点を簡潔に
・○○を改善しました
```

---

## Play Console（ユーザー向け・詳細）

### 概要
（このリリースの概要を1〜2文で）

### 新機能 / 修正・改善
- **項目** — 説明。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` <CODE> / `versionName` <VERSION> → `wear:internal` トラック
- companion（phone）: `versionCode` <CODE+1> / `versionName` <VERSION> → `internal` トラック

### 主な変更
- （技術的な変更点）

### Play 提出時の注意
- （あれば）
