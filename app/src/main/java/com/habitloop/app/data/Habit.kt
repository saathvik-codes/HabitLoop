package com.habitloop.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single habit the user is tracking (e.g. "Gym", "Study", "Read").
 * [templateId] drives which icon/reminder copy/widget theme is used —
 * this is what lets one codebase feel like a different app per niche.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val templateId: String = "",  // e.g. "gym", "study", "reading", "meditation", "sobriety"
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedEpochDay: Long? = null,
    val freezeTokensAvailable: Int = 1,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val createdAtEpochDay: Long = 0,
    val scheduleDaysCsv: String = "1,2,3,4,5,6,7",
    val motivation: String = "",
    val pausedUntilEpochDay: Long? = null,
    val isArchived: Boolean = false
)

fun Habit.isScheduledOn(date: java.time.LocalDate): Boolean =
    scheduleDaysCsv.split(",").mapNotNull { it.toIntOrNull() }
        .contains(date.dayOfWeek.value)

fun Habit.isDueOn(date: java.time.LocalDate): Boolean =
    !isArchived &&
        (pausedUntilEpochDay == null || date.toEpochDay() > pausedUntilEpochDay) &&
        isScheduledOn(date)

fun Habit.scheduleLabel(): String {
    val days = scheduleDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    return when (days) {
        setOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
        setOf(1, 2, 3, 4, 5) -> "Weekdays"
        setOf(6, 7) -> "Weekends"
        else -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            .filterIndexed { index, _ -> index + 1 in days }
            .joinToString(", ")
    }
}
