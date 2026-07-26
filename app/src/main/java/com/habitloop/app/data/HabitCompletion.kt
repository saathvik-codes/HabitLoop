package com.habitloop.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completion record per (habit, day). Kept as its own table rather than
 * just bumping a counter on [Habit] so the heatmap/weekly-recap features
 * have real history to render, not just a derived streak number.
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long = 0,
    val epochDay: Long = 0,       // java.time.LocalDate.toEpochDay()
    val completedAtEpochMillis: Long = 0,
    val usedFreezeToken: Boolean = false
)
