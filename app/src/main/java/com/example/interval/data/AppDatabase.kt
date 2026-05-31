package dev.marufeuille.intervo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExerciseModeConverter {
    @TypeConverter
    fun fromMode(mode: ExerciseMode): String = mode.name

    @TypeConverter
    fun toMode(value: String): ExerciseMode =
        runCatching { ExerciseMode.valueOf(value) }.getOrDefault(ExerciseMode.TIMED)
}

@Database(
    entities = [Workout::class, Exercise::class, WorkoutHistory::class, FreeSetRecord::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(ExerciseModeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        workoutId TEXT NOT NULL,
                        workoutName TEXT NOT NULL,
                        completedAt INTEGER NOT NULL,
                        totalSeconds INTEGER NOT NULL,
                        exerciseCount INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN mode TEXT NOT NULL DEFAULT 'TIMED'")
                database.execSQL("ALTER TABLE exercises ADD COLUMN repsPerSet INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN repRestSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS free_set_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        historyId TEXT NOT NULL,
                        exerciseId TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        setNumber INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        reps INTEGER,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(historyId) REFERENCES workout_history(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_free_set_records_historyId ON free_set_records(historyId)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "interval.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { DefaultWorkouts.insert(it) }
                        }
                    }
                })
                .build()
    }
}
