# リリースノート 1.7.4

## 概要（人間用メモ）

Wear OS 6 (API36) 実機で、スワイプによる前画面への戻り（スワイプ・トゥ・ディスミス）が効かなくなっていた退行を修正した。原因は #30 の `wearCompose 1.3.0 → 1.6.2` 更新で、1.5+ は API36 のスワイプバックを predictive back に統合しており、それが実機で機能していなかった。predictive back 導入前の 1.4.1 に固定して回避（PR #42 / 追跡 #41）。

## Play Console「最新情報」用

```
スワイプで前の画面に戻れなくなっていた問題を修正しました。ワークアウト一覧や詳細などの画面で、これまで通り左端からのスワイプで前の画面に戻れます。
```

## Internal Notes（任意）

- 真因: wearCompose 1.5+ の API36 predictive back 統合による退行。エミュレータ(API36)で 1.4.1↔1.6.2 を実証比較し確定。
- BackHandler / userSwipeEnabled / enableOnBackInvokedCallback=false では回避不可（コード側で直らない）。KEYCODE_BACK は機能していた（スワイプジェスチャーのみ死亡）。
- dependabot で androidx.wear.compose:* の 1.5+ を ignore。1.6+ 再推進と E2E スワイプバックテスト追加は #41 で追跡。
- Ambient(#40) は無関係。
