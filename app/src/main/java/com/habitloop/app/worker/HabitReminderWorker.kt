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
import com.habitloop.app.data.HabitDatabase
import com.habitloop.app.data.NotificationInbox
import com.habitloop.app.data.isDueOn
import java.time.LocalDate

class HabitReminderWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val habit = HabitDatabase.getInstance(applicationContext).habitDao()
            .getHabit(inputData.getLong(KEY_HABIT_ID, -1)) ?: return Result.success()
        val today = LocalDate.now()
        if (!habit.isDueOn(today) || habit.lastCompletedEpochDay == today.toEpochDay() ||
            !NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val body = "Time for ${habit.name}. A small check-in is enough."
        NotificationInbox.add("Habit reminder", body, "habit_reminder", "habit_${habit.id}_${today}")
        val pending = PendingIntent.getActivity(applicationContext, habit.id.hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_category", "habit_reminder")
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((10_000 + habit.id).toInt(), NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Time for ${habit.name}")
            .setContentText(body).setContentIntent(pending).setAutoCancel(true).build())
        return Result.success()
    }
    companion object { const val KEY_HABIT_ID = "habit_id"; const val CHANNEL_ID = "habit_time_reminders" }
}
