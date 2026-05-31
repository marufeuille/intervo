package dev.marufeuille.intervo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "free_set_records",
    foreignKeys = [ForeignKey(
        entity = WorkoutHistory::class,
        parentColumns = ["id"],
        childColumns = ["historyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("historyId")]
)
data class FreeSetRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val historyId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val durationSeconds: Int,
    val reps: Int?,
    val sortOrder: Int
)

data class FreeSetRecordInput(
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val durationSeconds: Int,
    val reps: Int?,
    val sortOrder: Int
)
