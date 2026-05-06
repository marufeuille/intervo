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

### Wi-Fi ADB でインストール

1. ウォッチ側: 設定 → 一般 → 開発者向けオプション → ADB デバッグ ON → Wi-Fi 経由のデバッグ ON
2. 表示されたIPアドレス:ポートで接続（初回はペアリングが必要）

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
