package com.habitloop.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.R
import com.habitloop.app.data.CorrelationInsight
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitInsights
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.UserPrefs
import com.habitloop.app.data.NotificationInbox
import com.habitloop.app.data.isScheduledOn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    viewModel: HabitViewModel,
    onOpenHabit: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenGrowthLab: () -> Unit,
    onOpenCommunity: () -> Unit
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    val inboxItems by NotificationInbox.items.collectAsStateWithLifecycle()
    val unreadCount = inboxItems.count { !it.read }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val today = LocalDate.now().toEpochDay()
    val scheduledToday = habits.filter { it.isScheduledOn(LocalDate.now()) }
    val done = scheduledToday.filter { it.lastCompletedEpochDay == today }
    val remaining = scheduledToday.filterNot { it.lastCompletedEpochDay == today }
    val completion = if (scheduledToday.isEmpty()) 0f else done.size.toFloat() / scheduledToday.size
    var insight by remember { mutableStateOf<CorrelationInsight?>(null) }
    var visible by remember { mutableStateOf(false) }
    var pendingCompletion by remember { mutableStateOf<Habit?>(null) }

    pendingCompletion?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingCompletion = null },
            title = { Text("Complete ${habit.name}?") },
            text = {
                Column {
                    Text("Confirm only after doing the activity. HabitLoop records an honest check-in—not just a button tap.")
                    Text(
                        "This will update today’s progress, streak and add 10 Loop Coins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.completeToday(habit.id)
                    pendingCompletion = null
                }) { Text("Yes, I did it") }
            },
            dismissButton = { TextButton(onClick = { pendingCompletion = null }) { Text("Not yet") } }
        )
    }

    LaunchedEffect(habits) {
        visible = true
        insight = if (habits.size >= 2) HabitInsights.bestCorrelation(habits, viewModel.allCompletions()) else null
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 12 }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, top = 18.dp, end = 20.dp, bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TodayHeader(
                    name = UserPrefs.getName(context),
                    unreadCount = unreadCount,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSettings = onOpenSettings
                )
            }

            if (habits.isEmpty()) {
                item { EmptyTodayState() }
            } else if (scheduledToday.isEmpty()) {
                item { RestDayState() }
            } else {
                item {
                    DailyProgressCard(
                        completed = done.size,
                        total = scheduledToday.size,
                        progress = completion,
                        momentum = HabitInsights.momentumScore(habits)
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeShortcut(
                            Modifier.weight(1f),
                            Icons.Filled.Psychology,
                            "Quick reset",
                            "1-minute practice",
                            onOpenGrowthLab
                        )
                        HomeShortcut(
                            Modifier.weight(1f),
                            Icons.Filled.Groups,
                            "Join a circle",
                            "Build together",
                            onOpenCommunity
                        )
                    }
                }

                if (remaining.isNotEmpty()) {
                    item { SectionTitle("Up next", "${remaining.size} remaining") }
                    items(remaining, key = { it.id }) { habit ->
                        DailyHabitRow(
                            habit = habit,
                            isDone = false,
                            onToggle = {
                                pendingCompletion = habit
                            },
                            onOpen = { onOpenHabit(habit.id) }
                        )
                    }
                }

                if (done.isNotEmpty()) {
                    item { SectionTitle("Completed", "${done.size} today") }
                    items(done, key = { it.id }) { habit ->
                        DailyHabitRow(
                            habit = habit,
                            isDone = true,
                            onToggle = null,
                            onOpen = { onOpenHabit(habit.id) }
                        )
                    }
                }

                insight?.let { value ->
                    item { InsightCard(value, habits) }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayHeader(
    name: String?,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                if (name.isNullOrBlank()) greetingForNow() else "${greetingForNow()}, ${name.substringBefore("@")}",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                IconButton(onClick = onOpenNotifications) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) Badge {
                                Text(if (unreadCount > 9) "9+" else unreadCount.toString())
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Activity inbox")
                    }
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun DailyProgressCard(completed: Int, total: Int, progress: Float, momentum: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (completed == total) {
                Image(
                    painter = painterResource(R.drawable.day_complete_art),
                    contentDescription = "Completed day celebration",
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (completed == total) "Day complete" else "Today's progress",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        if (completed == total) "Everything is checked off. Nice work."
                        else "$completed of $total habits completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = .65f)
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("$momentum momentum", style = MaterialTheme.typography.labelLarge)
                Text("  •  Keep the loop moving", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, meta: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyHabitRow(habit: Habit, isDone: Boolean, onToggle: (() -> Unit)?, onOpen: () -> Unit) {
    val template = HabitTemplates.byId(habit.templateId)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(template.accentColor.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(template.iconRes), null, Modifier.size(30.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (habit.currentStreak == 0) "Start your streak today"
                    else "${habit.currentStreak} day streak  •  Best ${habit.longestStreak}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = template.accentColor
                )
            }
            Surface(
                modifier = Modifier.size(44.dp).then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier),
                shape = CircleShape,
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                border = if (isDone) null else androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDone) Icon(Icons.Filled.Check, "Completed", tint = MaterialTheme.colorScheme.onPrimary)
                    else Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: CorrelationInsight, habits: List<Habit>) {
    val a = habits.firstOrNull { it.id == insight.habitAId } ?: return
    val b = habits.firstOrNull { it.id == insight.habitBId } ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = .10f)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Insights, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("A pattern worth keeping", style = MaterialTheme.typography.labelLarge)
                Text(
                    "You complete ${a.name} ${insight.liftPercent}% more often on days you also do ${b.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmptyTodayState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.empty_habits_v2), null, Modifier.size(190.dp))
        Text("Your daily loop starts here", style = MaterialTheme.typography.titleLarge)
        Text(
            "Add your first habit from the Habits tab.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RestDayState() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Nothing scheduled today", style = MaterialTheme.typography.titleLarge)
            Text(
                "Rest is part of a sustainable loop. Your next habits will appear automatically.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun greetingForNow() = when (LocalTime.now().hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
