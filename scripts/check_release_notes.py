#!/usr/bin/env python3
"""リリースノート（docs/release-notes-<VERSION>.md）のフォーマット検証。

CI がリリース時に Play の「最新情報（What's new）」として抽出するブロックが
必ず存在し、空でなく、Play の上限（500 文字）以内であることを保証する。

抽出仕様（release.yml の awk と一致させること）:
  「Play Console「最新情報」用」見出し以降の最初のコードフェンス（``` ... ```）の中身。

使い方:
  python3 scripts/check_release_notes.py [FILE ...]
  - 引数があればそのファイルのみ検証（CI は変更されたファイルを渡す）。
  - 引数が無ければ docs/release-notes-*.md を全件検証（ローカル確認用）。
終了コード: 0=OK / 1=NG
"""
import glob
import re
import sys

MARKER = "Play Console「最新情報」用"
LIMIT = 500  # Play の「最新情報」は 1 言語あたり 500 文字まで


def extract_whatsnew(text: str):
    """見出し以降の最初のコードフェンス内テキストを返す。無ければ None。"""
    idx = text.find(MARKER)
    if idx == -1:
        return None, "missing-marker"
    after = text[idx:]
    m = re.search(r"```[^\n]*\n(.*?)```", after, re.S)
    if not m:
        return None, "missing-fence"
    return m.group(1).strip(), None


def check(path: str) -> bool:
    try:
        text = open(path, encoding="utf-8").read()
    except OSError as e:
        print(f"::error file={path}::読み込めません: {e}")
        return False

    body, err = extract_whatsnew(text)
    if err == "missing-marker":
        print(f"::error file={path}::『{MARKER}』見出しがありません")
        return False
    if err == "missing-fence":
        print(f"::error file={path}::最新情報のコードフェンス（``` ... ```）がありません")
        return False
    if not body:
        print(f"::error file={path}::最新情報（コードフェンス内）が空です")
        return False

    n = len(body)
    if n > LIMIT:
        print(f"::error file={path}::最新情報が {n} 文字で上限 {LIMIT} 文字を超えています")
        return False

    print(f"OK: {path} (最新情報 {n} 文字)")
    return True


def main(argv):
    files = argv[1:] or sorted(glob.glob("docs/release-notes-*.md"))
    if not files:
        print("検証対象のリリースノートがありません（スキップ）")
        return 0
    ok = all(check(f) for f in files)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
