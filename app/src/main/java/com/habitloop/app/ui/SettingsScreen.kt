package com.habitloop.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.ReminderPrefs
import com.habitloop.app.worker.ReminderScheduler
import com.habitloop.app.audio.ThemeMusicController
import com.habitloop.app.audio.ThemeMusicPrefs
import com.habitloop.app.data.NotificationPrefs
import com.habitloop.app.notifications.HabitLoopMessagingService
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenGuide: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val context = LocalContext.current
    var systemNotificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    var hour by remember { mutableIntStateOf(ReminderPrefs.getHour(context)) }
    var minute by remember { mutableIntStateOf(ReminderPrefs.getMinute(context)) }
    var saved by remember { mutableIntStateOf(0) }
    var themeMusic by remember { mutableStateOf(ThemeMusicPrefs.enabled(context)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var circleAlerts by remember { mutableStateOf(NotificationPrefs.circleMessages(context)) }
    var habitAlerts by remember { mutableStateOf(NotificationPrefs.habitReminders(context)) }
    var streakAlerts by remember { mutableStateOf(NotificationPrefs.streakNudges(context)) }
    var quietHours by remember { mutableStateOf(NotificationPrefs.quietHours(context)) }
    var showHabitNames by remember { mutableStateOf(NotificationPrefs.showHabitNames(context)) }
    var notificationTone by remember { mutableStateOf(NotificationPrefs.tone(context)) }
    fun saveNotificationSetting(key: String, value: Boolean) {
        NotificationPrefs.set(context, key, value)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { HabitLoopMessagingService.registerToken(context, it) }
    }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "Current"
    }
    androidx.compose.runtime.LaunchedEffect(showTimePicker) {
        if (showTimePicker) {
            android.app.TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    hour = selectedHour
                    minute = selectedMinute
                    saved = 0
                    showTimePicker = false
                },
                hour,
                minute,
                false
            ).apply {
                setTitle("Choose daily reminder")
                setOnDismissListener { showTimePicker = false }
            }.show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Make HabitLoop work around your day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
                        Text("HabitLoop soundscape", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Four Note Return · pauses outside the app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = themeMusic,
                        onCheckedChange = { enabled ->
                            themeMusic = enabled
                            ThemeMusicPrefs.setEnabled(context, enabled)
                            if (enabled) ThemeMusicController.play(context) else ThemeMusicController.disable(context)
                        }
                    )
                }
            }
        }
        item { SettingsLink(Icons.Filled.NotificationsActive, "Activity inbox", "Review reminders and circle updates", onOpenNotifications) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Notification controls", style = MaterialTheme.typography.titleLarge)
                    Text("Choose what deserves your attention.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NotificationToggle("Daily habit reminder", "Sent around your chosen time only when scheduled habits remain", habitAlerts) {
                        habitAlerts = it
                        saveNotificationSetting("habits", it)
                        if (it) ReminderScheduler.scheduleDailyReminder(context, hour, minute, replaceExisting = true)
                    }
                    NotificationToggle("Circle messages", "Updates from circles you joined", circleAlerts) {
                        circleAlerts = it; saveNotificationSetting("circles", it)
                    }
                    NotificationToggle("Streak and comeback nudges", "Useful progress notices only", streakAlerts) {
                        streakAlerts = it; saveNotificationSetting("streaks", it)
                    }
                    NotificationToggle("Quiet hours", "Hold non-urgent alerts from 10 PM to 7 AM", quietHours) {
                        quietHours = it; saveNotificationSetting("quiet", it)
                    }
                    NotificationToggle("Show habit names", "Allow habit names in lock-screen reminders", showHabitNames) {
                        showHabitNames = it; saveNotificationSetting("show_habit_names", it)
                    }
                    Text("Reminder tone", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("gentle" to "Gentle", "playful" to "Playful", "direct" to "Direct").forEach { (id, label) ->
                            FilterChip(
                                selected = notificationTone == id,
                                onClick = { notificationTone = id; NotificationPrefs.setTone(context, id) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        "Uses your routines and activity—not age, gender or private identity data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Only nudges you while habits remain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (systemNotificationsEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (systemNotificationsEnabled) "System notifications are enabled" else "System notifications are blocked",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (!systemNotificationsEnabled) {
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    })
                                }
                            }) { Text("Enable") }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { showTimePicker = true }
                ) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            java.time.LocalTime.of(hour, minute).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Tap to choose a time · Android may deliver it a little later to save battery", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        ReminderPrefs.setTime(context, hour, minute)
                        ReminderScheduler.scheduleDailyReminder(context, hour, minute, replaceExisting = true)
                        saved++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saved > 0) "Reminder saved" else "Save ${String.format("%02d:%02d", hour, minute)} reminder")
                }
            }
        }
        }
        item {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("About HabitLoop", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A calm, local-first daily habit companion. Your essential tracking works without a connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        }
        item { Text("Account and support", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp)) }
        item { SettingsLink(Icons.Filled.Person, "Profile and avatar", "Photo, character avatar and display name", onEditProfile) }
        item { SettingsLink(Icons.Filled.Security, "Account and security", "Authentication, cloud backup and logout", onOpenAccount) }
        item { SettingsLink(Icons.Filled.HelpCenter, "How HabitLoop works", "Replay the complete app guide", onOpenGuide) }
        item { SettingsLink(Icons.Filled.NotificationsActive, "Rewards and ads", "Optional rewards, freeze tokens and ad transparency", onOpenRewards) }
        item { Text("App information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp)) }
        item { SettingsLink(Icons.Filled.Info, "Version", "HabitLoop $versionName", null) }
        item {
            SettingsLink(
                Icons.Filled.Security,
                "Privacy",
                "Core tracking is local-first; cloud sync is linked to your account",
                null
            )
        }
    }
}

@Composable
private fun NotificationToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun SettingsLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimeStepper(label: String, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = {
            val next = value + step
            onChange(if (next > range.last) range.first else next)
        }) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
        Text(String.format("%02d", value), style = MaterialTheme.typography.headlineMedium)
        IconButton(onClick = {
            val next = value - step
            onChange(if (next < range.first) range.last else next)
        }) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
    }
}
