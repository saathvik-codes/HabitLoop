package com.habitloop.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

object ReminderScheduler {

    private const val WORK_NAME = "daily_habit_reminder"

    fun scheduleDailyReminder(context: Context, atHour: Int = 19, atMinute: Int = 0, replaceExisting: Boolean = false) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(atHour, atMinute))
        if (target.isBefore(now)) target = target.plusDays(1)
        val initialDelay = Duration.between(now, target)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replaceExisting) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
