# Intervo 1.6.0 Release Notes

## Play Console

- ワークアウト中の心拍数を計測し、タイマー画面に表示するようにしました。
- 心拍（平均・最大・種目ごとの推移）をワークアウト履歴に記録するようにしました。
- スマホの Health Connect に、ワークアウトと心拍を書き出せるようにしました（Companion アプリで連携を許可）。
- 画面を消したり他アプリに切り替えてもタイマーが止まらないよう、常駐表示（Ongoing Activity）に対応しました。
- アプリが再起動してもワークアウトを途中から再開できるようにしました。
- タイマーの計測精度を改善しました。

## Internal Notes

- app `versionCode`: 17 / `versionName`: 1.6.0
- companion `versionCode`: 13 / `versionName`: 1.6.0
- タイマーを health タイプの Foreground Service + Ongoing Activity 化。フェーズ境界でスナップショット永続化し、プロセス再起動後に再開／部分履歴保存。
- 心拍は Health Services ExerciseClient（`HEART_RATE_BPM`）。Wear OS 5+ は `android.permission.health.READ_HEART_RATE` が必須。
- 心拍は平均/最大/開始＋種目ごとの開始・終了を Room に保存（DB v7）。5 秒間隔のサンプル列を Data Layer で Companion へ送信。
- Companion が ExerciseSessionRecord（title=ワークアウト名）+ HeartRateRecord を Health Connect に書込（clientRecordId で冪等、処理後に DataItem 削除）。Companion DB v4。
- ビルド基盤: Kotlin 2.0.21 / AGP 8.9.1。app リリースは R8（minify + resource shrink）有効。GitHub Actions CI 導入。

## Play 提出時の注意

- Companion は Health Connect の書き込み権限（`WRITE_EXERCISE` / `WRITE_HEART_RATE`）を使用するため、Play Console で Health Connect の利用宣言（権限の用途説明）が必要。
