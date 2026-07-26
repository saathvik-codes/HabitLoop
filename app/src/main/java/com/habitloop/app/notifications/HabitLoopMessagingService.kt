package com.habitloop.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.habitloop.app.MainActivity
import com.habitloop.app.R
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.NotificationPrefs
import com.habitloop.app.data.NotificationInbox
import java.security.MessageDigest

class HabitLoopMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        registerToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val category = message.data["category"] ?: "community"
        val enabled = when (category) {
            "circle_message" -> NotificationPrefs.circleMessages(this)
            "jam" -> NotificationPrefs.jamUpdates(this)
            "streak" -> NotificationPrefs.streakNudges(this)
            else -> true
        }
        if (!enabled || (NotificationPrefs.quietHours(this) && NotificationPrefs.isQuietNow())) return
        createChannels(this)
        val title = message.data["title"] ?: message.notification?.title ?: "HabitLoop"
        val body = message.data["body"] ?: message.notification?.body ?: "There’s something new in your loop."
        NotificationInbox.add(title, body, category, message.messageId ?: "${category}_${System.currentTimeMillis()}")
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_category", category)
            message.data["circleId"]?.let { putExtra("circle_id", it) }
        }
        val pending = PendingIntent.getActivity(
            this, body.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (category == "circle_message") CHANNEL_COMMUNITY else CHANNEL_PROGRESS
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify("${category}_${message.messageId}".hashCode(), notification)
    }

    companion object {
        const val CHANNEL_COMMUNITY = "community_updates"
        const val CHANNEL_PROGRESS = "progress_reminders"
        fun registerToken(context: Context, token: String) {
        val uid = FirebaseSync.uidOrNull ?: return
        val id = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(32)
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("devices").document(id).set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "circleMessages" to NotificationPrefs.circleMessages(context),
                    "jamUpdates" to NotificationPrefs.jamUpdates(context),
                    "quietHours" to NotificationPrefs.quietHours(context),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        }
        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_COMMUNITY, "Circle messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Messages and activity from circles you joined"
                    },
                    NotificationChannel(CHANNEL_PROGRESS, "Habit progress", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Habit reminders, Jam updates and streak notices"
                    }
                )
            )
        }
    }
}
