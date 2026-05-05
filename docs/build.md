# ビルド・インストール手順

## 必要なもの

- Android Studio（最新版推奨）
- Android SDK（compileSdk / targetSdk 36）
- Wear OS エミュレータ または Pixel Watch 4 実機

## デバッグビルド

```bash
./gradlew assembleDebug
```

## リリースビルド

署名用の `keystore.properties` をプロジェクトルートに用意する（リポジトリ非管理）。

```properties
storeFile=/path/to/your.jks
storePassword=xxxx
keyAlias=xxxx
keyPassword=xxxx
```

```bash
./gradlew assembleRelease
```

## エミュレータで実行

AVD Manager で以下の構成を作成して起動：

| 項目 | 値 |
|------|-----|
| API | android-36 |
| System Image | android-wear-signed / arm64-v8a |

Android Studio の Run ボタン、または：

```bash
./gradlew installDebug
```

## 実機インストール（Pixel Watch 4 / Wi-Fi ADB）

```bash
# Watch 側: 設定 → システム → 開発者向けオプション → ADBデバッグ ON

adb tcpip 5555
adb connect <WatchのIPアドレス>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```
