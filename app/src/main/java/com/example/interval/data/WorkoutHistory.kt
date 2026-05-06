package dev.marufeuille.intervo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workout_history")
data class WorkoutHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val workoutName: String,
    val completedAt: Long = System.currentTimeMillis(),
    val totalSeconds: Int,
    val exerciseCount: Int
)
