package com.habitloop.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.habitloop.app.MainActivity
import com.habitloop.app.R
import com.habitloop.app.data.CircleMessage
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.NotificationPrefs
import com.habitloop.app.notifications.HabitLoopMessagingService
import kotlinx.coroutines.tasks.await

class CommunityMessageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!NotificationPrefs.circleMessages(applicationContext)) return Result.success()
        if (NotificationPrefs.quietHours(applicationContext) && NotificationPrefs.isQuietNow()) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        val uid = runCatching { FirebaseSync.ensureSignedIn() }.getOrElse { return Result.retry() }
        val db = FirebaseFirestore.getInstance()
        val memberships = runCatching {
            db.collection("users").document(uid).collection("circleMemberships").get().await().documents
        }.getOrElse { return Result.retry() }
        val prefs = applicationContext.getSharedPreferences("community_message_watermarks", Context.MODE_PRIVATE)
        memberships.forEach { membership ->
            val circleId = membership.id
            val latest = runCatching {
                db.collection("circles").document(circleId).collection("messages")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1).get().await().documents.firstOrNull()
            }.getOrNull() ?: return@forEach
            val message = latest.toObject(CircleMessage::class.java) ?: return@forEach
            val created = message.createdAt?.seconds ?: return@forEach
            val key = "last_$circleId"
            val seen = prefs.getLong(key, 0)
            prefs.edit().putLong(key, created).apply()
            if (seen == 0L || created <= seen || message.userId == uid) return@forEach
            notify(circleId, message)
        }
        return Result.success()
    }

    private fun notify(circleId: String, message: CircleMessage) {
        HabitLoopMessagingService.createChannels(applicationContext)
        val intent = PendingIntent.getActivity(
            applicationContext, circleId.hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("notification_category", "circle_message")
                putExtra("circle_id", circleId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = "${message.username.substringBefore("@")}: ${message.text.take(120)}"
        NotificationManagerCompat.from(applicationContext).notify(
            "fallback_${message.id}".hashCode(),
            NotificationCompat.Builder(applicationContext, HabitLoopMessagingService.CHANNEL_COMMUNITY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New circle message")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        )
    }
}
