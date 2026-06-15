package dev.marufeuille.intervo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sortOrder: Int,
    /** ExerciseCategory の enum 名。Health Connect の種別へ変換するために保持する。 */
    val exerciseType: String = ExerciseCategory.DEFAULT.name
)

data class WorkoutWithCount(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val exerciseCount: Int
)
