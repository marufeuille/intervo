package dev.marufeuille.intervo.companion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companion_workout_history")
data class CompanionWorkoutHistory(
    @PrimaryKey val id: String,
    val workoutId: String,
    val workoutName: String,
    val completedAt: Long,
    val totalSeconds: Int,
    val exerciseCount: Int,
    val workoutSnapshotJson: String = "",
    val exerciseSnapshotsJson: String = "[]",
    val startHr: Int? = null,
    val avgHr: Int? = null,
    val maxHr: Int? = null,
    val exerciseHrJson: String = "[]",
    val hrSamplesJson: String = "[]",
    val performedSetsJson: String = "[]",
    val receivedAt: Long = System.currentTimeMillis(),
    val healthConnectWrittenAt: Long? = null,
)
