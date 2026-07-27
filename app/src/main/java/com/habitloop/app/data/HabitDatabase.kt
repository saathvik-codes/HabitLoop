package com.habitloop.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Habit::class, HabitCompletion::class, PlannerTask::class],
    version = 4,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile private var INSTANCE: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habitloop.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleDaysCsv TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
                db.execSQL("ALTER TABLE habits ADD COLUMN motivation TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN pausedUntilEpochDay INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE habits ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS planner_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        dueAtEpochMillis INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
