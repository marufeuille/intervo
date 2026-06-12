# Wear OS app keep rules.
# Room / Compose / coroutines ship consumer rules; add app-specific rules here as needed.

# TimerSnapshot は org.json で手書きシリアライズしており、リフレクションは不使用。
# 現状追加の keep ルールは不要。R8 で問題が出たクラスはここに追記する。
