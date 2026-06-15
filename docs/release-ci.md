# タグ駆動リリース CI/CD

`git tag vX.Y.Z` を push すると、GitHub Actions（`.github/workflows/release.yml`）が
app / companion の 2 つの AAB を署名ビルドし、Google Play の**内部テスト**トラックへ自動配信する。

手作業（versionCode の手採番・2 つの AAB の手動ビルド・Play Console への手動アップロード）はゼロになる。
既存の `ci.yml`（main / PR で :app のテスト・E2E）は無改変でそのまま動く。

## 全体像

```
git tag v1.7.2 && git push origin v1.7.2
        │
        ▼
release.yml (push: tags ['v*'])
  1. checkout / Java 17 / Gradle
  2. VERSION_NAME=1.7.2 を解決（タグ名から v を除去）
  3. Secrets から keystore / google-services.json を復元
  4. :app:testDebugUnitTest（ガード）
  5. :app:bundleRelease :companion:bundleRelease
  6. Play 内部テストへ配信（app=Wear / companion=phone を**フォームファクタ別リリース**で 2 回）
  7. AAB を artifact 保存（保険）
```

`workflow_dispatch`（手動実行）では配信手前（復元・テスト・ビルド）までを検証できる。
Secrets が未整備でも署名前のステップで切り分けできる。配信は tag push 時のみ実行される。

## versionCode / versionName の自動採番

`app/build.gradle.kts` / `companion/build.gradle.kts` の冒頭ヘルパーで semver から決定論的に算出する。

- `versionName` = 環境変数 `VERSION_NAME`（CI がタグ `vX.Y.Z` → `X.Y.Z` を渡す）。
  未設定（ローカルビルド）時はフォールバック `"1.7.1"`。
- `versionCode` = `(major*10000 + minor*100 + patch) * 10 + offset`。**app は offset=0 / companion は offset=1**。
  - 例: `1.7.2` → app `107020` / companion `107021`
  - 現行 21 / 22 より大きく単調増加するため、Play の versionCode 衝突・逆行は起きない。

ローカル単体確認:

```bash
VERSION_NAME=1.7.2 ./gradlew :app:bundleRelease
# → versionCode が 107020 になる
```

## 必要な GitHub Secrets

鍵類は GitHub の **Settings → Secrets and variables → Actions** に登録する。

| Secret 名 | 内容 | 作り方 |
| --- | --- | --- |
| `KEYSTORE_BASE64` | リリース署名鍵 `keystore-release.jks` を base64 化した文字列 | `base64 -i keystore-release.jks \| pbcopy`（macOS） |
| `KEYSTORE_PASSWORD` | keystore のストアパスワード | ローカル `keystore.properties` の `storePassword` |
| `KEY_ALIAS` | 署名鍵のエイリアス | ローカル `keystore.properties` の `keyAlias` |
| `KEY_PASSWORD` | 署名鍵のパスワード | ローカル `keystore.properties` の `keyPassword` |
| `GOOGLE_SERVICES_JSON_BASE64` | companion の `google-services.json`（release variant）を base64 化 | `base64 -i companion/src/release/google-services.json \| pbcopy` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play API サービスアカウント鍵 JSON の中身（全文） | 下記「Play サービスアカウント発行」参照 |

> base64 化のメモ
> - macOS: `base64 -i <file>`（改行なし）。`-i` を付けないと標準入力待ちになる。
> - Linux: `base64 -w0 <file>`（`-w0` で改行を抑止）。
> CI 側は `base64 --decode` で復元するため、改行が混ざっても decode 自体は通るが、無改行で登録するのが無難。

CI は復元時に以下を生成する（`build.gradle.kts` の signingConfig が読む `keystore.properties` 形式）:

```properties
storeFile=$GITHUB_WORKSPACE/keystore-release.jks
storePassword=$KEYSTORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
```

> `storeFile` は絶対パスで指定する。`build.gradle.kts` の `file(...)` は各モジュール
> （app/ companion/）基準で相対パスを解決するため、相対パスだと `app/keystore-release.jks`
> を探して失敗する。CI では `$GITHUB_WORKSPACE` 直下に復元し絶対パスで参照する。

`GOOGLE_SERVICES_JSON_BASE64` は `companion/src/release/google-services.json` に復元される
（ローカルと同じ配置）。

## Play Console サービスアカウント発行手順

`PLAY_SERVICE_ACCOUNT_JSON` 用のサービスアカウントを作る。

1. **GCP でサービスアカウント作成**
   - Google Cloud Console → 対象プロジェクト → IAM と管理 → サービスアカウント → 「サービスアカウントを作成」
   - 作成後、鍵タブ →「鍵を追加」→「新しい鍵を作成」→ JSON を選択 → ダウンロード
   - このダウンロードした JSON の**中身全文**を `PLAY_SERVICE_ACCOUNT_JSON` に登録する
2. **Play Console と連携**
   - Google Play Console → 左下「ユーザーと権限」または「API アクセス」（Setup → API access）
   - 上記サービスアカウントを Play Console にリンク（GCP プロジェクトが未連携なら先にプロジェクトをリンク）
3. **権限付与**
   - そのサービスアカウントに、対象アプリへの「リリース」権限を付与する
     （最低限: リリースの作成・編集、内部テストへのアップロード）

> 反映に時間がかかることがある。配信が 403 等で失敗する場合は権限伝播待ちを疑う。

## 運用フロー

1. 機能をマージし、リリース対象が main に揃った状態にする。
2. タグを打って push:

   ```bash
   git tag v1.7.2
   git push origin v1.7.2
   ```

3. GitHub Actions の **Release** ワークフローが走り、内部テストへ配信される。
4. Play Console の内部テストで実機確認 → 問題なければ製品版トラックへ昇格（Play Console 上で手動）。

### 配信前だけ検証したいとき

GitHub Actions → Release → 「Run workflow」（workflow_dispatch）。
`version` 入力（例 `1.7.2`）は任意。配信はされず、復元・テスト・ビルドまでを確認できる。

## 注意

- 初回 API 配信は「内部テストに既存リリースがある（＝既にアプリが Play 上に存在）」前提。
  本プロジェクトは既に内部テスト運用中のため API 配信可能。
- 1.7.1（versionCode 21 / 22）はコミット済み。CI 化後の次リリースから semver 採番（107020〜）に切り替わる。
- 署名鍵・サービスアカウント鍵は私（Claude）は扱わない。ユーザーが GitHub Secrets に登録する。
- app(Wear) と companion(phone) は applicationId を共有するマルチフォームファクタ構成で、Play 上は
  **フォームファクタ別トラック**に分かれる。Play Developer API ではフォームファクタ別トラックを
  `<prefix>:<defaultTrack>` で表す（Wear OS は `wear:` プレフィックス）。本 CI は
  **app(Wear)=`wear:internal` / companion(phone)=`internal`** にそれぞれ配信する。
  - 両 AAB を 1 リリースに混在 → `requires the Wear OS system feature android.hardware.type.watch` で失敗。
  - Wear AAB をデフォルトの `internal`（=phone 用）に出す → `does not allow any existing users to upgrade` で失敗。
  - 参考: [Manage form factor releases on dedicated tracks](https://support.google.com/googleplay/android-developer/answer/13295490) / [APKs and Tracks](https://developers.google.com/android-publisher/tracks)
- GCP プロジェクト（`intervo-app`）で **Android Publisher API**（`androidpublisher.googleapis.com`）の
  有効化が必要。未有効だと配信ステップが `API has not been used ... or it is disabled` で失敗する。
  `gcloud services enable androidpublisher.googleapis.com --project=intervo-app`
