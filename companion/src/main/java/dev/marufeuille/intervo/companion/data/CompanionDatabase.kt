package dev.marufeuille.intervo.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CompanionWorkoutHistory::class], version = 7, exportSchema = false)
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
                    // v6/v7 は PDS 同期状態と performed sets を追加。まだ内部テスト前の
                    // Companion 履歴なので同じ方針を継続する。
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
