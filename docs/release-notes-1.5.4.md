# Intervo 1.5.4 Release Notes

## Play Console

- タイマー中に誤ってスワイプしても、ワークアウト画面から戻らないようにしました。
- 時間無制限モードで誤タップしても、記録画面から「再開」でトレーニングへ戻れるようにしました。
- タイマー操作中の意図しない中断を減らし、ワークアウトを続けやすくしました。

## Internal Notes

- `versionCode`: 16
- `versionName`: 1.5.4
- タイマー画面では Wear Compose の戻るスワイプと Activity の `windowSwipeToDismiss` を無効化。
- タイマー中の back 操作は `BackHandler` で吸収。
- 時間無制限セットの記録ダイアログに「再開」ボタンを追加。
