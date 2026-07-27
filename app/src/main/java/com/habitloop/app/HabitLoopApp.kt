package com.habitloop.app

import android.app.Application
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.HabitDatabase
import com.habitloop.app.data.HabitRepository
import com.habitloop.app.data.ReminderPrefs
import com.habitloop.app.data.RewardWallet
import com.habitloop.app.data.NotificationInbox
import com.habitloop.app.data.OnboardingPrefs
import com.habitloop.app.worker.ReminderScheduler
import com.habitloop.app.notifications.HabitLoopMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.habitloop.app.worker.CommunityMessageWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class HabitLoopApp : Application() {

    lateinit var repository: HabitRepository
        private set

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        RewardWallet.initialize(this)
        NotificationInbox.initialize(this)
        HabitLoopMessagingService.createChannels(this)
        repository = HabitRepository(HabitDatabase.getInstance(this).habitDao())
        appScope.launch {
            repository.observeHabits().first()
                .filter { !it.isArchived }
                .forEach { ReminderScheduler.scheduleHabitReminder(this@HabitLoopApp, it.id, it.reminderHour, it.reminderMinute) }
        }
        ReminderScheduler.scheduleDailyReminder(
            this,
            atHour = ReminderPrefs.getHour(this),
            atMinute = ReminderPrefs.getMinute(this)
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "community-message-fallback",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CommunityMessageWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )

        if (!OnboardingPrefs.isExplicitlySignedOut(this)) {
            appScope.launch {
                runCatching {
                    val uid = FirebaseSync.ensureSignedIn()
                    repository.restoreFromCloudIfEmpty(uid)
                    repository.observePlannerTasks().first()
                        .filter { !it.isCompleted && it.dueAtEpochMillis > System.currentTimeMillis() }
                        .forEach { ReminderScheduler.schedulePlannerTask(this@HabitLoopApp, it.id, it.dueAtEpochMillis) }
                }
            }
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                HabitLoopMessagingService.registerToken(this, token)
            }
        }
    }
}
