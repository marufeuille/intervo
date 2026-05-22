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
    val receivedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val syncError: String? = null,
    val syncAttempts: Int = 0,
    val lastSyncAttemptAt: Long? = null,
)
