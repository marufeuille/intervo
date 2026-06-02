# Intervo 1.5.3 Release Notes

## Play Console

- 時間制限なしで続けられる「自由」タイマーを、通常の時間モード内で選べるようにしました。
- 既存のフリーセットはそのまま引き継がれ、完了時の時間・回数記録も継続して利用できます。
- 回数モードと時間モードの設定を整理し、編集画面で目的に合わせて選びやすくしました。

## Internal Notes

- `versionCode`: 15
- `versionName`: 1.5.3
- `ExerciseMode.FREE` を廃止し、`TIMED + durationSeconds = -1` を時間無制限として扱うように変更。
- DB v6 マイグレーションで既存の `FREE` 種目を `TIMED` / `durationSeconds = -1` に変換。
