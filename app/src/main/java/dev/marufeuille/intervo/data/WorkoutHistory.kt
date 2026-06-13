package dev.marufeuille.intervo.data

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "workout_history")
data class WorkoutHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val workoutName: String,
    val completedAt: Long = System.currentTimeMillis(),
    val totalSeconds: Int,
    val exerciseCount: Int,
    val startHr: Int? = null,
    val avgHr: Int? = null,
    val maxHr: Int? = null
)

data class WorkoutHistoryWithFreeSetRecords(
    @Embedded val history: WorkoutHistory,
    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val freeSetRecords: List<FreeSetRecord>,
    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val exerciseHrRecords: List<ExerciseHrRecord>
)
