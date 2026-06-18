# Intervo 1.10.0 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
スマートフォン用アプリの Bluesky/PDS 連携を改善しました。
・連携を設定している場合、ワークアウトの記録を「トレーニングプラン（種目・セット構成）」と「実施したという記録（チェックイン）」に分けて PDS へ保存するようになりました。
・プランは繰り返し使える形で保存され、今後の共有・再利用に活用できます。
・連携していない方の動作・保存内容に変更はありません。
・心拍数データは引き続き PDS へ送信しません。
・以前に同期済みの記録を新形式にしたい場合は、設定または履歴画面から手動で再同期してください。
・ウォッチアプリの機能に変更はありません。
```

---

## Play Console（ユーザー向け・詳細）

### 概要
スマートフォン用アプリ（companion）の Bluesky/PDS 連携の保存形式を見直しました。これまで 1 件のワークアウト記録としてまとめて保存していたデータを、繰り返し使える「トレーニングプラン」と、それを実施したことを表す軽量な「チェックイン」に分けて保存します。連携を設定していない場合の動作に変更はありません。

### 新機能 / 修正・改善
- **プラン／チェックインへの分離保存** — PDS 連携を設定している場合、ワークアウトを「種目・セット構成を表すプラン」と「実施記録（チェックイン）」の 2 種類に分けて保存します。チェックインは保存済みプランを参照する形になり、今後の共有・再利用に向いた構造になります。
- **再同期の整合性向上** — 再同期時はプランを先に書き込み、その結果（プランの参照先）をチェックインに紐づけて保存します。設定／履歴画面からの手動再同期で、既存記録を新しい形式へ書き直せます。
- **心拍数は引き続き送信しません** — PDS へ送るデータに心拍数は含めません。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 110000 / `versionName` 1.10.0 → `wear:internal` トラック
- companion（phone）: `versionCode` 110001 / `versionName` 1.10.0 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。1.9.4（Wear 109040 / phone 109041）より単調増加。

### 主な変更
- **#60 Add PDS workout plan checkins** — companion の PDS ペイロードを、単一の `dev.marufeuille.workout.session` レコードから `dev.marufeuille.workout.plan`（再利用可能なプラン定義）と `dev.marufeuille.workout.checkin`（軽量な実施記録）の 2 レコードに分離。`WorkoutSessionRecordMapper` を `WorkoutPdsRecordMapper` にリネームし、`mapPlan` / `mapCheckin` と plan/checkin の rkey 生成を追加。同期時はプランを先に `putRecord` し、返却された `uri` / `cid` をチェックインの参照（`planRef`）に格納して、実施記録が安定したプランバージョンを指すようにする（`PdsDirectClient` を更新）。心拍数データは引き続きペイロードに含めない。テストは `WorkoutSessionRecordMapperTest` を `WorkoutPdsRecordMapperTest` に置き換え、plan/checkin のマッピングを検証。`docs/build.md` と `docs/privacy-policy.html` の文言を新しい plan/checkin 同期モデルに合わせて更新。
- **#59 Validate release note filenames in CI** — CI 内部の変更。リリースノートのファイル名検証を追加（ユーザー影響なし。「最新情報」には含めない）。

### Play 提出時の注意
- companion のみの機能変更。Wear app（`wear:internal`）に機能差分はないが、両 AAB とも versionCode が更新され同一リリースで配信される。
- DB スキーマの破壊的変更はなし（既存の履歴データを読み取って新形式のレコードを生成するのみ）。
- PDS のレコード形式（lexicon の collection / NSID）が変わるが、送信は利用者が設定した PDS に限られ、新たな外部サーバーへの自動送信や新規パーミッションの追加はない。データセーフティ申告に影響する新規の外部送信・権限追加はなし。
- 既に `pdsSyncedAt` が付いた同期済み記録は自動移行しない。新形式へ揃えるには、設定／履歴画面からの手動再同期で全履歴を再 `putRecord` する（既存運用どおり）。
