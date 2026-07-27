package com.habitloop.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
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

    fun sendTest(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(workDataOf(ReminderWorker.KEY_TEST to true))
                .build()
        )
    }

    fun schedulePlannerTask(context: Context, taskId: Long, dueAtEpochMillis: Long) {
        val delay = (dueAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<PlannerReminderWorker>()
            .setInitialDelay(java.time.Duration.ofMillis(delay))
            .setInputData(workDataOf(PlannerReminderWorker.KEY_TASK_ID to taskId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            plannerWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelPlannerTask(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(plannerWorkName(taskId))
    }

    fun scheduleHabitReminder(context: Context, habitId: Long, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, target))
            .setInputData(workDataOf(HabitReminderWorker.KEY_HABIT_ID to habitId)).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("habit_reminder_$habitId", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelHabitReminder(context: Context, habitId: Long) =
        WorkManager.getInstance(context).cancelUniqueWork("habit_reminder_$habitId")

    private fun plannerWorkName(taskId: Long) = "planner_reminder_$taskId"
}
