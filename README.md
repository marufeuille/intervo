<div align="center">
  <img src="design/icon_b_retro_poster.png" width="128" alt="Intervo icon" />
  <h1>Intervo</h1>
  <p><strong>Wear OS インターバルタイマー</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Wear%20OS-6.0-4285F4?logo=wear-os&logoColor=white" alt="Wear OS 6.0" />
    <img src="https://img.shields.io/badge/version-1.5.4-E8560A" alt="version 1.5.4" />
    <img src="https://img.shields.io/badge/license-MIT-lightgrey" alt="MIT" />
  </p>
</div>

---

<div align="center">
  <img src="design/feature_graphic.png" alt="Intervo feature graphic" width="100%" />
</div>

<br/>

## 機能

| | |
|---|---|
| **ワークアウト管理** | 「上半身」「脚の日」など複数メニューを作成・保存 |
| **エクササイズ編集** | 種目名・運動時間・セット数・休憩時間をウォッチ単体で設定 |
| **インターバルタイマー** | 運動 → 休憩を自動進行。残り3秒からカウントダウンバイブ |
| **音声ガイド** | 「始め」「休憩」「完了」を日本語で読み上げ。画面を見なくてもOK |
| **休憩スキップ** | 休憩中にタップするだけで次のセットへ即移行 |
| **次の種目プレビュー** | タイマー中に次の種目名を表示 |
| **ワークアウト履歴** | 完了したトレーニングを日付・所要時間付きで自動記録 |
| **Ambient Mode 対応** | スリープ中も残り時間を常時表示。バッテリー節約 |

## スクリーンショット

<div align="center">
  <img src="design/screenshots/01_workout_select.png"  width="180" alt="ワークアウト選択" />
  <img src="design/screenshots/02_workout_detail.png"  width="180" alt="ワークアウト詳細" />
  <img src="design/screenshots/03_timer_exercise.png"  width="180" alt="タイマー（運動中）" />
</div>
<div align="center">
  <img src="design/screenshots/04_timer_rest.png"      width="180" alt="タイマー（休憩中）" />
  <img src="design/screenshots/05_completion.png"      width="180" alt="完了画面" />
  <img src="design/screenshots/06_history.png"         width="180" alt="履歴" />
</div>

## ドキュメント

- [ビルド・実機インストール手順](docs/build.md)
- [実装計画](docs/plan/wear-os-interval-timer.md)
- [E2E テスト（シナリオベース）](docs/e2e-testing.md)

## 開発

`adb` / `gradlew` / `git` のよく使う操作を、権限プロンプトなしで通す薄ラッパーを `scripts/` に用意している（ホワイトリスト方式。破壊的操作は拒否して生コマンドへ誘導）。AI エージェントも含め、直接 `./gradlew` / `adb` / `git` を叩く前に対応ラッパーを優先する。

| スクリプト | 役割 | 例 |
|---|---|---|
| `scripts/g` | gradlew（許可タスクのみ） | `scripts/g assembleDebug` |
| `scripts/a` | adb（読み系・インストール系のみ） | `scripts/a devices` |
| `scripts/gitw` | git（PR 作成までの作業系） | `scripts/gitw push -u origin HEAD` |
| `scripts/watch-adb` | 実機 adb 統合（ビルド+インストール+起動） | `scripts/watch-adb reinstall` |

詳細と守るべきルールは [`CLAUDE.md`](CLAUDE.md) の「開発用コマンドラッパー」節、各スクリプトの `help`（`scripts/g help` 等）を参照。

## ライセンス

MIT
