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
        if (value == "FREE") {
            ExerciseMode.TIMED
        } else {
            runCatching { ExerciseMode.valueOf(value) }.getOrDefault(ExerciseMode.TIMED)
        }
}

@Database(
    entities = [
        Workout::class,
        Exercise::class,
        WorkoutHistory::class,
        FreeSetRecord::class,
        ExerciseHrRecord::class,
    ],
    version = 8,
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN mode TEXT NOT NULL DEFAULT 'TIMED'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN repsPerSet INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE exercises ADD COLUMN repRestSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_free_set_records_historyId ON free_set_records(historyId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercises SET repsPerSet = 1 WHERE mode = 'REPS' AND repsPerSet = 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercises SET mode = 'TIMED', durationSeconds = -1 WHERE mode = 'FREE'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_history ADD COLUMN startHr INTEGER")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgHr INTEGER")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN maxHr INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_hr_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        historyId TEXT NOT NULL,
                        exerciseIndex INTEGER NOT NULL,
                        exerciseName TEXT NOT NULL,
                        startHr INTEGER NOT NULL,
                        endHr INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(historyId) REFERENCES workout_history(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_hr_records_historyId ON exercise_hr_records(historyId)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN exerciseType TEXT NOT NULL DEFAULT 'OTHER_WORKOUT'")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "interval.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8,
                )
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
