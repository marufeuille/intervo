# Intervo 1.7.3 Release Notes

## Play Console「最新情報」用（コピペ可・500字以内）

```
不具合修正を行いました。
・運動中の常時表示（Ambient）が正しく切り替わらない不具合を修正しました
```

---

## Play Console（ユーザー向け・詳細）

### 概要
運動中の常時表示（Ambient）に関する不具合を修正したメンテナンスリリースです。

### 修正・改善
- **常時表示（Ambient）の修正** — ウォッチが常時表示モードへ切り替わる際、タイマー画面が常時表示用の表示に切り替わらないことがある不具合を修正しました。

---

## Internal Notes

### バージョン
- app（Wear）: `versionCode` 107030 / `versionName` 1.7.3 → `wear:internal` トラック
- companion（phone）: `versionCode` 107031 / `versionName` 1.7.3 → `internal` トラック
- semver からの自動採番: `(major*10000 + minor*100 + patch)*10 + offset`（app offset=0 / companion offset=1）。現行（Wear 107020 / phone 107021）より単調増加。

### 主な変更
- **Ambient 状態のスコープ不整合を解消**（#9 / PR #40）。Ambient フラグが Activity スコープの `TimerViewModel` に設定される一方、`TimerScreen` の `viewModel()` は NavBackStackEntry スコープの別インスタンスを参照していたため、`isAmbient` が UI に届かず `AmbientTimerContent` へ切り替わらなかった。
- Ambient 状態を `TimerViewModel` から外し、`MainActivity` を単一ソースとして `StateFlow` で `AppNavigation` → `TimerScreen` へ降下させる方式へ変更。ViewModel のスコープ（= サービス bind 挙動）は変更していない。副次的に `MainActivity` から `TimerViewModel` 依存が消えた。

### Play 提出時の注意
- 本修正は「Ambient コールバックが UI state に届く経路」を保証するもの。**`onEnterAmbient` の実発火は実機/エミュレータで未検証**であり、main の E2E でも Ambient 遷移はエミュレートされないため保証されない。製品版へ昇格する前に、実機で常時表示モードへの遷移と表示切り替えを必ず確認すること。
- 特に `TimerScreen` の `view.keepScreenOn = true`（運動中の画面維持・意図的な実装）は端末によっては Ambient 遷移自体を抑止する可能性がある。実機で発火しない場合の第一容疑として確認すること。
