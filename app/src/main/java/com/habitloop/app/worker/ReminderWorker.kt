package com.habitloop.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitloop.app.MainActivity
import com.habitloop.app.R
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitDatabase
import com.habitloop.app.data.NotificationPrefs
import com.habitloop.app.data.NotificationInbox
import com.habitloop.app.data.isScheduledOn
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!NotificationPrefs.habitReminders(applicationContext)) return Result.success()
        if (NotificationPrefs.quietHours(applicationContext) && NotificationPrefs.isQuietNow()) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        val date = java.time.LocalDate.now()
        val incomplete = HabitDatabase.getInstance(applicationContext).habitDao().observeHabits().first()
            .filter { it.isScheduledOn(date) && it.lastCompletedEpochDay != date.toEpochDay() }
        if (incomplete.isNotEmpty()) notify(incomplete)
        return Result.success()
    }

    private fun notify(incomplete: List<Habit>) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Daily habit reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "One relevant reminder when scheduled habits remain unfinished"
                }
            )
        }
        val revealNames = NotificationPrefs.showHabitNames(applicationContext)
        val variants = when (NotificationPrefs.tone(applicationContext)) {
            "playful" -> listOf(
                "Plot twist: a tiny win still counts.",
                "Your future self sent a very polite ping.",
                "The loop is open. Perfection was not invited.",
                "Small action, suspiciously good momentum."
            )
            "direct" -> listOf(
                "Complete the smallest valid version now.",
                "Your planned check-in is still incomplete.",
                "Open HabitLoop and finish the next action."
            )
            else -> listOf(
                "A small check-in is available when you are.",
                "Return with the smallest useful step.",
                "You still have time for one intentional action."
            )
        }
        val closing = variants[java.time.LocalDate.now().dayOfYear % variants.size]
        val first = if (revealNames) incomplete.first().name.take(36) else "Your next habit"
        val body = when {
            incomplete.size == 1 -> "$first is waiting. $closing"
            revealNames -> "$first and ${incomplete.size - 1} more are waiting. $closing"
            else -> "${incomplete.size} routines remain today. $closing"
        }
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_category", "habit_reminder")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (incomplete.size == 1) "One small loop left" else "${incomplete.size} loops left today")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setVisibility(if (revealNames) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        NotificationInbox.add(
            if (incomplete.size == 1) "One small loop left" else "${incomplete.size} loops left today",
            body,
            "habit_reminder",
            "daily_${java.time.LocalDate.now()}"
        )
        manager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val REMINDER_NOTIFICATION_ID = 1001
    }
}
