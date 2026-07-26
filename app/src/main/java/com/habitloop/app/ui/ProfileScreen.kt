package com.habitloop.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.UserPrefs
import com.habitloop.app.data.UsageTracker
import com.habitloop.app.data.RewardWallet

@Composable
fun ProfileScreen(
    viewModel: HabitViewModel,
    onOpenPerks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenGuide: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenGrowthLab: () -> Unit
) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val name = UserPrefs.getName(context)?.takeIf { it.isNotBlank() } ?: "Habit builder"
    val totalBest = habits.maxOfOrNull { it.longestStreak } ?: 0
    val freezes = habits.sumOf { it.freezeTokensAvailable }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = name,
                    avatarStyle = UserPrefs.getAvatarSymbol(context),
                    color = UserPrefs.getAvatarColor(context),
                    photoUri = UserPrefs.getProfilePhotoUri(context),
                    size = 72.dp
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text(name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        RewardWallet.activeTitle().ifBlank { "${habits.size} active routine${if (habits.size == 1) "" else "s"}" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat(Modifier.weight(1f), totalBest.toString(), "Best streak")
                ProfileStat(Modifier.weight(1f), freezes.toString(), "Freezes")
                ProfileStat(Modifier.weight(1f), UsageTracker.activeDays(context).toString(), "Active days")
            }
        }
        item {
            Text("Your space", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            ProfileAction(
                Icons.Filled.Psychology,
                "Growth Lab",
                "Practice focus, reflection and everyday mental skills",
                onOpenGrowthLab
            )
        }
        item {
            ProfileAction(Icons.Filled.Edit, "Customize profile", "Display name, avatar style and color", onEditProfile)
        }
        item {
            ProfileAction(
                Icons.Filled.Share,
                "Share my progress",
                "Invite an accountability partner with your current momentum"
            ) {
                val shareText = buildString {
                    append("$name is building ${habits.size} routine")
                    append(if (habits.size == 1) "" else "s")
                    append(" on HabitLoop")
                    if (totalBest > 0) append(" with a best streak of $totalBest days")
                    append(". Want to keep each other accountable?")
                }
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Join my HabitLoop")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        },
                        "Share your progress"
                    )
                )
            }
        }
        item {
            ProfileAction(
                Icons.Filled.HelpCenter,
                "How HabitLoop works",
                "Replay the complete daily-use guide",
                onOpenGuide
            )
        }
        item {
            ProfileAction(
                Icons.Filled.Security,
                "Account & security",
                "Cloud session, local data and sign-out controls",
                onOpenSecurity
            )
        }
        item {
            ProfileAction(
                Icons.Filled.Shield,
                "Streak protection",
                "Earn and manage freeze tokens",
                onOpenPerks
            )
        }
        item {
            ProfileAction(
                Icons.Filled.Notifications,
                "Reminders and preferences",
                "Choose when HabitLoop should nudge you",
                onOpenSettings
            )
        }
        item {
            ProfileAction(
                Icons.Filled.CloudDone,
                "Cloud backup",
                if (FirebaseSync.uidOrNull != null) "Connected and syncing in the background" else "Preparing secure backup",
                null
            )
        }
        item {
            ProfileAction(Icons.Filled.Settings, "App information", "Privacy, version and local-first behavior", onOpenSettings)
        }
    }
}

@Composable
private fun ProfileStat(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
