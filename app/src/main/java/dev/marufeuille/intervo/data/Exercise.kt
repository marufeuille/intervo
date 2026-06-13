package dev.marufeuille.intervo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ExerciseMode {
    TIMED,
    REPS
}

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class Exercise(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val name: String,
    val mode: ExerciseMode = ExerciseMode.TIMED,
    val durationSeconds: Int,
    val sets: Int,
    val restSeconds: Int,
    val repsPerSet: Int = 1,
    val repRestSeconds: Int = 0,
    val sortOrder: Int
)

data class WorkoutExerciseCount(
    @ColumnInfo(name = "workoutId") val workoutId: String,
    @ColumnInfo(name = "count") val count: Int
)

val EXERCISE_PRESET_NAMES = listOf(
    "腕立て伏せ", "スクワット", "腹筋", "プランク", "ランジ",
    "バーピー", "マウンテンクライマー", "ジャンピングジャック", "その他（カスタム）"
)

const val DURATION_MIN = 5
const val DURATION_MAX = 300
const val DURATION_STEP = 5
const val DURATION_UNLIMITED = -1
const val SETS_MIN = 1
const val SETS_MAX = 20
const val REST_MIN = 0
const val REST_MAX = 120
const val REST_STEP = 5
const val REPS_PER_SET_MIN = 1
const val REPS_PER_SET_MAX = 30
const val REPS_OPEN_ENDED = -1
const val REP_REST_MIN = 0
const val REP_REST_MAX = 30
const val REP_REST_STEP = 1

fun Exercise.isOpenEndedReps(): Boolean =
    mode == ExerciseMode.REPS && repsPerSet == REPS_OPEN_ENDED

fun Exercise.isDurationUnlimited(): Boolean =
    mode == ExerciseMode.TIMED && durationSeconds == DURATION_UNLIMITED

fun Exercise.effectiveRepsPerSet(): Int =
    if (isOpenEndedReps()) REPS_OPEN_ENDED else repsPerSet.coerceIn(REPS_PER_SET_MIN, REPS_PER_SET_MAX)
