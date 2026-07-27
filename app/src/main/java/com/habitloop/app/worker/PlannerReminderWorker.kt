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

class PlannerReminderWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1)
        val task = HabitDatabase.getInstance(applicationContext).habitDao().getPlannerTask(taskId)
            ?: return Result.success()
        if (task.isCompleted || !NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Planner reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders for one-time plans and important tasks"
                }
            )
        }
        val intent = PendingIntent.getActivity(
            applicationContext,
            task.id.hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_category", "planner")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = task.note.ifBlank { "You asked HabitLoop to remind you now." }
        NotificationInbox.add(task.title, body, "planner", "planner_${task.id}")
        manager.notify(
            NOTIFICATION_BASE_ID + (task.id % 100_000).toInt(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "planner_task_id"
        const val CHANNEL_ID = "planner_reminders"
        const val NOTIFICATION_BASE_ID = 5000
    }
}
