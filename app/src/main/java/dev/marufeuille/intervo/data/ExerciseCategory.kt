package dev.marufeuille.intervo.data

/**
 * ワークアウトの種別。enum 名（key）はそのまま永続化・Wear→Companion 転送に使い、
 * Companion 側の HealthConnectWriter で Health Connect の ExerciseSessionRecord.EXERCISE_TYPE_*
 * 定数（Int）へ変換する。Wear 側に health-connect 依存を持ち込まないため文字列キーで扱う。
 */
enum class ExerciseCategory(val label: String) {
    OTHER_WORKOUT("その他のワークアウト"),
    STRENGTH_TRAINING("筋力トレーニング"),
    HIGH_INTENSITY_INTERVAL_TRAINING("HIIT"),
    STRETCHING("ストレッチ"),
    CALISTHENICS("自重トレーニング"),
    YOGA("ヨガ"),
    RUNNING("ランニング"),
    WALKING("ウォーキング");

    companion object {
        val DEFAULT = OTHER_WORKOUT

        /** 不明・空のキーは DEFAULT にフォールバックする。 */
        fun fromKey(key: String?): ExerciseCategory =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
