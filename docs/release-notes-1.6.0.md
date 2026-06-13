# Intervo 1.6.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
心拍数の計測に対応しました。
・ワークアウト中の心拍をリアルタイム表示
・平均/最大、種目ごとの心拍の推移を履歴に記録
・Health Connect 連携で他のヘルスケアアプリとデータを共有（Companion アプリで連携を許可）

そのほかの改善
・画面オフや他アプリ切り替えでもタイマーが止まりません
・アプリ再起動後もワークアウトを途中から再開できます
・タイマーの計測精度を改善しました
```

---

## Play Console（ユーザー向け・詳細）

### 概要
心拍数の計測に対応した、これまでで最大のアップデートです。ワークアウト中の心拍をリアルタイムに表示し、履歴に記録。さらにスマホの Health Connect と連携して、他のヘルスケアアプリとデータを共有できるようになりました。タイマー自体もバックグラウンドで止まらず、より安心して使えるよう改良しています。

### 新機能
- **心拍数の計測** — ワークアウト中の心拍をタイマー画面にリアルタイム表示します。
- **心拍の記録** — 平均・最大に加え、種目ごとの心拍の推移を履歴に残します。
- **Health Connect 連携** — Companion アプリで連携を許可すると、ワークアウトと心拍をスマホの Health Connect に書き出し、他のアプリと共有できます。

### 改善
- 画面を消したり他のアプリに切り替えても、タイマーが止まらなくなりました（常駐表示に対応）。
- アプリが再起動しても、ワークアウトを途中から再開できるようになりました。
- タイマーの計測精度を改善しました。

---

## Internal Notes

### バージョン
- app: `versionCode` 17 / `versionName` 1.6.0
- companion: `versionCode` 18 / `versionName` 1.6.0

### 主な変更
- **タイマーの堅牢化**: health タイプの Foreground Service + Ongoing Activity 化。フェーズ境界でスナップショットを永続化し、プロセス再起動後に再開／破棄時は部分履歴を保存。`delay` ループから `elapsedRealtime` 基準へ変更しドリフトを解消。状態遷移ロジックを純粋な `TimerEngine` に抽出（ユニットテスト整備）。
- **心拍計測**: Health Services `ExerciseClient`（`HEART_RATE_BPM`）。Wear OS 5+ は `android.permission.health.READ_HEART_RATE` が必須。
- **心拍の永続化**: 平均/最大/開始＋種目ごとの開始・終了を Room に保存（app DB v7）。5 秒間隔のサンプル列を Data Layer で Companion へ送信（ウォッチには保持しない forward-only）。
- **Health Connect 連携**: Companion が `ExerciseSessionRecord`（title = ワークアウト名）+ `HeartRateRecord` を書込。`clientRecordId` で冪等化し、処理後に DataItem を削除。Companion DB v4。BigQuery 同期は従来どおり併存。
- **ビルド基盤**: Kotlin 2.0.21 / AGP 8.9.1。app リリースは R8（minify + リソース圧縮）有効。GitHub Actions による CI を導入。

### Play 提出時の注意
- Companion は Health Connect の書き込み権限（`WRITE_EXERCISE` / `WRITE_HEART_RATE`）を使用するため、Play Console で Health Connect の利用宣言（権限の用途説明）が必要。
- R8 を本リリースで初めて有効化したため、製品版へ昇格する前に内部テストトラックで動作確認すること。
