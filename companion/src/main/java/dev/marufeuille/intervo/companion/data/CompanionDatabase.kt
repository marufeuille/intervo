package dev.marufeuille.intervo.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CompanionWorkoutHistory::class], version = 3, exportSchema = false)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): CompanionWorkoutHistoryDao

    companion object {
        @Volatile private var INSTANCE: CompanionDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE companion_workout_history ADD COLUMN workoutSnapshotJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE companion_workout_history ADD COLUMN exerciseSnapshotsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE companion_workout_history ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE companion_workout_history ADD COLUMN lastSyncAttemptAt INTEGER")
            }
        }

        fun getInstance(context: Context): CompanionDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CompanionDatabase::class.java,
                    "intervo_companion.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
