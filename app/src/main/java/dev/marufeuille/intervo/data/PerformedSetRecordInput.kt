package dev.marufeuille.intervo.data

/**
 * PDS の performed.sets[] に載せるセット単位実績。
 * Room には保存せず、完了時に Wear -> Companion の DataLayer payload へ同梱する。
 */
data class PerformedSetRecordInput(
    val exerciseIndex: Int,
    val exerciseName: String,
    val setIndex: Int,
    val durationSeconds: Int?,
    val reps: Int?,
    val completed: Boolean,
    val sortOrder: Int,
)
