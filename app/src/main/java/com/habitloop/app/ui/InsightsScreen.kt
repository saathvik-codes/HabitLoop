package com.habitloop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.HabitCompletion
import com.habitloop.app.data.HabitInsights
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.isScheduledOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(viewModel: HabitViewModel, onOpenHabit: (Long) -> Unit) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    var completions by remember { mutableStateOf<List<HabitCompletion>>(emptyList()) }
    LaunchedEffect(habits) { completions = viewModel.allCompletions() }

    val start = LocalDate.now().minusDays(6).toEpochDay()
    val completedThisWeek = completions.count { it.epochDay >= start }
    val momentum = HabitInsights.momentumScore(habits)
    val best = habits.maxByOrNull { it.longestStreak }
    val comebacks = completions.groupBy { it.habitId }.values.sumOf(HabitInsights::comebackCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Patterns that help you adjust—not judge—your routines.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Bolt,
                    value = "$momentum%",
                    label = "Momentum"
                )
                InsightMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.AutoGraph,
                    value = completedThisWeek.toString(),
                    label = "Check-ins · 7d"
                )
            }
        }
        item {
            WeeklyActivityChart(completions)
        }
        best?.let { habit ->
            item {
                InsightFeatureCard(
                    title = "Your strongest loop",
                    body = "${habit.name} has reached a ${habit.longestStreak}-day best streak.",
                    icon = Icons.Filled.LocalFireDepartment,
                    onClick = { onOpenHabit(habit.id) }
                )
            }
        }
        if (comebacks > 0) {
            item {
                InsightFeatureCard(
                    title = "Recovery matters",
                    body = "You returned after a missed day $comebacks time${if (comebacks == 1) "" else "s"}. Consistency includes coming back.",
                    icon = Icons.Filled.Replay,
                    onClick = null
                )
            }
        }
        item {
            Text("Routine status", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        if (habits.isEmpty()) {
            item {
                Text(
                    "Your insights will appear after you create and check in with a routine.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(habits, key = { it.id }) { habit ->
            val template = HabitTemplates.byId(habit.templateId)
            val last30Start = LocalDate.now().minusDays(29)
            val planned = (0L..29L).count { offset -> habit.isScheduledOn(last30Start.plusDays(offset)) }
            val completed = completions.count {
                it.habitId == habit.id && it.epochDay >= last30Start.toEpochDay()
            }
            val consistency = if (planned == 0) 0f else (completed.toFloat() / planned).coerceIn(0f, 1f)
            Card(
                onClick = { onOpenHabit(habit.id) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).background(template.accentColor.copy(.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${habit.currentStreak}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${(consistency * 100).toInt()}% consistency · ${habit.currentStreak} current",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { consistency },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                            color = template.accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${habit.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityChart(completions: List<HabitCompletion>) {
    val dates = (6L downTo 0L).map { LocalDate.now().minusDays(it) }
    val values = dates.map { date -> completions.count { it.epochDay == date.toEpochDay() } }
    val max = maxOf(1, values.maxOrNull() ?: 1)
    var selectedIndex by remember { mutableStateOf(6) }
    val selectedDate = dates[selectedIndex]
    val selectedValue = values[selectedIndex]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
            Text(
                "${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} · $selectedValue check-in${if (selectedValue == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(172.dp).padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dates.forEachIndexed { index, date ->
                    val value = values[index]
                    val barHeight by animateDpAsState(
                        targetValue = (14 + (82 * value.toFloat() / max)).dp,
                        animationSpec = spring(),
                        label = "activityBar"
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                            .background(
                                if (selectedIndex == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                                else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (value == 0) "–" else value.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            Modifier.height(96.dp).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier.width(24.dp).height(barHeight).background(
                                    if (value > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                                )
                            )
                        }
                        Text(
                            date.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsightFeatureCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
