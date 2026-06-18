# ビルド・インストール手順

## 必要なもの

- Android SDK（compileSdk / targetSdk 36）
- Java 17 以上（`gradle.properties` で `org.gradle.java.home` を指定済み）
- Pixel Watch 4 実機

## パッケージ名

| ビルド | パッケージ名 |
|--------|-------------|
| Debug（ADB）| `dev.marufeuille.intervo.debug` |
| Release（Play ストア）| `dev.marufeuille.intervo` |

Debug と Release は別アプリとしてウォッチに共存できる。

---

## デバッグビルド（ADB 経由）

### ビルド

```bash
./gradlew assembleDebug
```

### Companion の PDS 直接同期（任意）

`companion` は標準 PDS の XRPC に直接書き込む。設定画面で以下を保存すると、受信済み履歴を
`dev.marufeuille.workout.session` record として `com.atproto.repo.putRecord` する。
PDS へ送る record には実施日時、合計時間、種目、予定セット、実施セットを含める。心拍数データは含めず、
Health Connect 側にのみ書き込む。
設定画面や履歴画面の再同期は同じ `sourceRef` / rkey で全履歴を再 `putRecord` し、PDS 上の既存 record を
現在の payload で上書きする。

- PDS URL: `https://pds.example.com` など
- ハンドル: `you.example.com` など
- App Password: Bluesky/PDS 側で発行した App Password

App Password は Android Keystore の鍵で暗号化して端末内に保存する。将来 OAuth 化する場合は、この
App Password 認証部分を OAuth token provider に置き換える。

### Emulator で実 PDS 書き込みを確認する

設定画面で PDS 設定を保存してから、Debug 専用 receiver でダミー履歴を投入できる。
ダミー履歴には `performed.sets[]` の完了セット、実レップが少ない未完了セット、時間セットの途中終了例を含めている。

```bash
./gradlew :companion:installDebug

adb shell am broadcast \
  -n dev.marufeuille.intervo.debug/dev.marufeuille.intervo.companion.debug.DebugWorkoutHistoryReceiver \
  -a dev.marufeuille.intervo.DEBUG_SEED_WORKOUT_HISTORY \
  --es sourceRef emulator-test-001
```

ADB だけで一度に設定と送信を行う場合は、Debug 専用 extra を使える（App Password はログに出さない）。

```bash
adb shell am broadcast \
  -n dev.marufeuille.intervo.debug/dev.marufeuille.intervo.companion.debug.DebugWorkoutHistoryReceiver \
  -a dev.marufeuille.intervo.DEBUG_SEED_WORKOUT_HISTORY \
  --es sourceRef emulator-test-001 \
  --es pdsUrl https://pds.example.com \
  --es identifier you.example.com \
  --es appPassword '<APP_PASSWORD>'
```

同じ `sourceRef` で再実行すると、PDS では同一 rkey へのべき等 upsert になる。Room へ入れるだけで
同期しない場合は `--ez sync false` を付ける。

### Wi-Fi ADB で接続・インストール

1. ウォッチ側: 設定 → 一般 → 開発者向けオプション → ADB デバッグ ON → Wi-Fi 経由のデバッグ ON
2. 表示されたIPアドレス:ポートで接続（初回はペアリングが必要）

初回だけペアリングする:

```bash
scripts/watch-adb pair <IPアドレス>:<ペアリングポート> <ペアリングコード>
```

接続先を保存する:

```bash
scripts/watch-adb connect <IPアドレス>:<デバッグポート>
```

以後はIP/ポート指定なしで、保存済み接続先またはmDNS自動検出を使える:

```bash
scripts/watch-adb reinstall
```

`reinstall` は `assembleDebug` → `adb install -r` → アプリ起動まで実行する。
接続だけ、インストールだけ、起動だけ行う場合はそれぞれ `connect` / `install` / `run` を使う。
接続先が変わっても、ペアリング済みのウォッチが1台だけ見つかれば自動で `.adb-device` を更新する。

自動検出できる接続先だけ確認する場合:

```bash
scripts/watch-adb discover
```

手動で実行する場合:

```bash
adb connect <IPアドレス>:<ポート>
adb -s <IPアドレス>:<ポート> install -r app/build/outputs/apk/debug/app-debug.apk
```

### 旧パッケージ（com.example.interval）を削除する場合

```bash
adb -s <IPアドレス>:<ポート> uninstall com.example.interval
```

---

## リリースビルド（Play ストア）

### 前提：`keystore.properties`

プロジェクトルートに作成（git 管理外）：

```properties
storeFile=/path/to/your.jks
storePassword=xxxx
keyAlias=xxxx
keyPassword=xxxx
```

### AAB をビルド

```bash
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

### バージョンを上げる場合

`app/build.gradle.kts` の `versionCode` を +1 してから再ビルド。

### Play Console へのアップロード

1. [play.google.com/console](https://play.google.com/console) を開く
2. Intervo → テスト → 内部テスト → 新しいリリースを作成
3. `app-release.aab` をアップロード → リリース
