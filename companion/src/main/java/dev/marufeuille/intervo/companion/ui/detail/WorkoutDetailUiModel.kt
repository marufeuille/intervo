package dev.marufeuille.intervo.companion.ui.detail

/** 種目の計測モード。intervo の ExerciseMode TIMED/REPS に対応（companion 内では文字列から復元）。 */
enum class ExerciseModeUi { TIMED, REPS }

/** 履歴詳細画面に出す 1 種目分の予実。数値は表示側で整形する。 */
data class ExerciseDetail(
    val name: String,
    val mode: ExerciseModeUi?,
    val sets: Int?,
    val reps: Int?,
    val durationSeconds: Int?,
    val restSeconds: Int?,
    val repRestSeconds: Int?,
    val startHr: Int?,
    val endHr: Int?,
)

/** 履歴詳細画面のセッション単位の表示モデル。 */
data class WorkoutDetailUiModel(
    val title: String,
    val completedAt: Long,
    val totalSeconds: Int,
    val exerciseCount: Int,
    val totalPlannedSets: Int,
    val startHr: Int?,
    val avgHr: Int?,
    val maxHr: Int?,
    val healthConnectWritten: Boolean,
    val pdsSynced: Boolean,
    val exercises: List<ExerciseDetail>,
) {
    val hasHeartRate: Boolean get() = startHr != null || avgHr != null || maxHr != null
}
