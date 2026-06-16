package dev.marufeuille.intervo.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CompanionWorkoutHistory::class], version = 5, exportSchema = false)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): CompanionWorkoutHistoryDao

    companion object {
        @Volatile private var INSTANCE: CompanionDatabase? = null

        fun getInstance(context: Context): CompanionDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CompanionDatabase::class.java,
                    "intervo_companion.db"
                )
                    // v5 で BigQuery 同期カラムを撤去。旧スキーマの DB はマイグレーションせず
                    // 破棄して作り直す（履歴はウォッチからの再受信で取り込まれる）。
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
