package com.habitloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScheduleTest {
    @Test
    fun dailyHabitIsScheduledEveryDay() {
        val habit = Habit(scheduleDaysCsv = "1,2,3,4,5,6,7")
        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 7, 27)))
        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 8, 2)))
        assertEquals("Every day", habit.scheduleLabel())
    }

    @Test
    fun weekdayHabitExcludesWeekend() {
        val habit = Habit(scheduleDaysCsv = "1,2,3,4,5")
        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 7, 27)))
        assertFalse(habit.isScheduledOn(LocalDate.of(2026, 8, 1)))
        assertEquals("Weekdays", habit.scheduleLabel())
    }

    @Test
    fun customScheduleHasReadableLabel() {
        val habit = Habit(scheduleDaysCsv = "1,3,5")
        assertEquals("Mon, Wed, Fri", habit.scheduleLabel())
    }
}
