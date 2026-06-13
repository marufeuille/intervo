package dev.marufeuille.intervo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** 種目ごと（全セット通して）の開始時・終了時の心拍。 */
@Entity(
    tableName = "exercise_hr_records",
    foreignKeys = [ForeignKey(
        entity = WorkoutHistory::class,
        parentColumns = ["id"],
        childColumns = ["historyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("historyId")]
)
data class ExerciseHrRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val historyId: String,
    val exerciseIndex: Int,
    val exerciseName: String,
    val startHr: Int,
    val endHr: Int,
    val sortOrder: Int
)

data class ExerciseHrInput(
    val exerciseIndex: Int,
    val exerciseName: String,
    val startHr: Int,
    val endHr: Int,
    val sortOrder: Int
)
