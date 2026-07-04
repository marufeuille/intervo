---
name: watch-deploy
description: 実機の Wear OS ウォッチに app（:app）をビルド→転送→起動する。mDNS でデバッグポートが毎回変わる問題を scripts/watch-adb で吸収する。「ウォッチで動かして」「実機に入れて」「ウォッチに reinstall して」などで使う。エミュレータではなく実機向け。
---

# watch-deploy — 実機ウォッチへデプロイ

Wear OS 実機は無線デバッグの**ポートが毎回変わる**ため、`scripts/watch-adb`（mDNS 自動探索＋`.adb-device` キャッシュ）経由で扱う。`adb connect <ip:port>` をベタ書きしない。

- 対象パッケージ: `dev.marufeuille.intervo.debug`
- 直前に成功したターゲットは `.adb-device` にキャッシュされる。`INTERVO_ADB_SERIAL=ip:port` で上書きも可。
- `adb` は `~/Library/Android/sdk/platform-tools` を PATH に（無ければ `ADB=...` で明示）。

## 標準フロー（ペア済みウォッチ）
1. デバッグビルド:
   ```
   ./gradlew :app:assembleDebug
   ```
2. 接続（キャッシュ or mDNS 自動探索）:
   ```
   scripts/watch-adb connect      # .adb-device を再利用
   scripts/watch-adb discover     # 見つからないとき mDNS で探索
   ```
3. 入れ直して起動:
   ```
   scripts/watch-adb reinstall    # アンインストール→install
   scripts/watch-adb run          # 起動
   ```
   `scripts/watch-adb devices` で接続状態を確認できる。

## 初回ペアリング（未ペアの場合）
無線デバッグの **pairing-port / ペアリングコード**はウォッチ画面の値が必要なので**ユーザーに尋ねる**（こちらでは取得できない）。
```
scripts/watch-adb pair <ip:pairing-port> <pairing-code>
scripts/watch-adb connect <ip:debug-port>
```

## つまずいたら
- `connect`/`discover` が失敗 → ウォッチの開発者設定で「ワイヤレスデバッグ」が ON か、同一 LAN か、画面が起きているかを確認。再ペアリングが要ることもある。
- `adb not found` → Android SDK の platform-tools を PATH に通す。
- 2〜3回試して繋がらなければ、同じコマンドを繰り返さずユーザーに状況を確認する。
