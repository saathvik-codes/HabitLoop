package com.habitloop.app.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitCompletion
import com.habitloop.app.data.HabitInsights
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.isScheduledOn
import com.habitloop.app.data.isDueOn
import com.habitloop.app.data.scheduleLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun HabitDetailScreen(
    habit: Habit,
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onComplete: () -> Unit,
    onWatchAdForFreeze: () -> Unit
) {
    val template = HabitTemplates.byId(habit.templateId)
    val context = LocalContext.current
    val completions by viewModel.observeCompletions(habit.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val completedDays = completions.associateBy { it.epochDay }
    val today = LocalDate.now()
    val isDoneToday = habit.lastCompletedEpochDay == today.toEpochDay()
    val isScheduledToday = habit.isDueOn(today)
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var monthDirection by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var editSchedule by remember { mutableStateOf(false) }
    var scheduleSaved by remember { mutableStateOf(false) }
    var showPauseOptions by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    if (editSchedule) {
        ScheduleEditorDialog(
            initialDays = habit.scheduleDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet(),
            onDismiss = { editSchedule = false },
            onSave = { days ->
                viewModel.updateSchedule(habit.id, days.sorted().joinToString(","))
                scheduleSaved = true
                editSchedule = false
            }
        )
    }

    if (showPauseOptions) {
        AlertDialog(
            onDismissRequest = { showPauseOptions = false },
            title = { Text("Pause without losing progress") },
            text = {
                Column {
                    Text("Paused days do not appear on Today and do not count as missed.")
                    TextButton(onClick = {
                        viewModel.pauseHabit(habit.id, today.plusDays(1).toEpochDay())
                        showPauseOptions = false
                    }) { Text("Pause for 1 day") }
                    TextButton(onClick = {
                        viewModel.pauseHabit(habit.id, today.plusDays(7).toEpochDay())
                        showPauseOptions = false
                    }) { Text("Pause for 1 week") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPauseOptions = false }) { Text("Cancel") } }
        )
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(if (habit.isArchived) "Restore this habit?" else "Archive this habit?") },
            text = {
                Text(
                    if (habit.isArchived) "It will return to your active habits."
                    else "It will leave Today, but its streak and check-in history stay safe."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setArchived(habit.id, !habit.isArchived)
                    confirmArchive = false
                }) { Text(if (habit.isArchived) "Restore" else "Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Habit details", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Schedule", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (scheduleSaved) "Saved · ${habit.scheduleLabel()}" else "${habit.scheduleLabel()} · Tap to change",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { scheduleSaved = false; editSchedule = true }) { Text("Edit") }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Reminder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Daily at ${LocalTime.of(habit.reminderHour, habit.reminderMinute).format(DateTimeFormatter.ofPattern("h:mm a"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute -> viewModel.updateHabitReminder(habit.id, hour, minute) }, habit.reminderHour, habit.reminderMinute, false).show()
                    }) { Text("Change") }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (habit.pausedUntilEpochDay != null &&
                            habit.pausedUntilEpochDay >= today.toEpochDay()
                        ) viewModel.pauseHabit(habit.id, null) else showPauseOptions = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !habit.isArchived
                ) {
                    Icon(
                        if (habit.pausedUntilEpochDay != null) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                        null,
                        Modifier.padding(end = 6.dp)
                    )
                    Text(if (habit.pausedUntilEpochDay != null) "Resume" else "Pause")
                }
                OutlinedButton(
                    onClick = { confirmArchive = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Archive, null, Modifier.padding(end = 6.dp))
                    Text(if (habit.isArchived) "Restore" else "Archive")
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(30.dp),
                color = template.accentColor.copy(alpha = .12f)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(62.dp).clip(RoundedCornerShape(20.dp))
                                .background(template.accentColor.copy(alpha = .2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(template.iconRes), null, Modifier.size(38.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(habit.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                habit.scheduleLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = template.accentColor)
                        Text(habit.currentStreak.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                    if (habit.motivation.isNotBlank()) {
                        Text(
                            "“${habit.motivation}”",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(Modifier.weight(1f), habit.currentStreak.toString(), "Current")
                MetricTile(Modifier.weight(1f), habit.longestStreak.toString(), "Personal best")
                MetricTile(Modifier.weight(1f), habit.freezeTokensAvailable.toString(), "Freezes")
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Button(
                    onClick = onComplete,
                    enabled = !isDoneToday && isScheduledToday,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDoneToday) Icon(Icons.Filled.CheckCircle, null, Modifier.padding(end = 8.dp))
                    Text(
                        when {
                            isDoneToday -> "Completed for today"
                            !isScheduledToday -> "Rest day · not scheduled"
                            else -> "Complete today"
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.IosShare, null, Modifier.padding(end = 6.dp))
                        Text("Share")
                    }
                    OutlinedButton(onClick = onWatchAdForFreeze, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AcUnit, null, Modifier.padding(end = 6.dp))
                        Text("Earn freeze")
                    }
                }
            }
        }
        item {
            HabitCalendar(
                habit = habit,
                month = selectedMonth,
                monthDirection = monthDirection,
                completions = completedDays,
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                onPrevious = {
                    monthDirection = -1
                    selectedMonth = selectedMonth.minusMonths(1)
                    selectedDate = null
                },
                onNext = if (selectedMonth < YearMonth.now()) {
                    {
                        monthDirection = 1
                        selectedMonth = selectedMonth.plusMonths(1)
                        selectedDate = null
                    }
                } else null
            )
        }
        item {
            InsightsSection(completions)
        }
    }
}

@Composable
private fun ScheduleEditorDialog(
    initialDays: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    var selected by remember(initialDays) { mutableStateOf(initialDays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }) }
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When should this habit appear?") },
        text = {
            Column {
                Text("Only selected days count toward streaks. Unselected days appear as rest days and cannot be marked missed.")
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    labels.forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = day in selected,
                            onClick = { selected = if (day in selected) selected - day else selected + day },
                            label = { Text(label.take(1)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    when {
                        selected.size == 7 -> "Every day"
                        selected == setOf(1, 2, 3, 4, 5) -> "Weekdays"
                        selected.isEmpty() -> "Choose at least one day"
                        else -> selected.sorted().joinToString(" · ") { labels[it - 1] }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(selected) }, enabled = selected.isNotEmpty()) { Text("Save schedule") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MetricTile(modifier: Modifier, value: String, label: String) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitCalendar(
    habit: Habit,
    month: YearMonth,
    monthDirection: Int,
    completions: Map<Long, HabitCompletion>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp).animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge)
                    Text("Tap a date for its status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                    Icon(Icons.Filled.ChevronRight, "Next month")
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (monthDirection >= 0) {
                        (slideInHorizontally(tween(220)) { it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(180)) { -it / 5 } + fadeOut())
                    } else {
                        (slideInHorizontally(tween(220)) { -it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(180)) { it / 5 } + fadeOut())
                    }
                },
                label = "calendar month"
            ) { visibleMonth ->
                CalendarMonthGrid(habit, visibleMonth, completions, selectedDate, onSelectDate)
            }
            CalendarLegend()
            selectedDate?.let { date ->
                val status = dateStatus(habit, date, completions[date.toEpochDay()])
                Surface(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} · $status",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    habit: Habit,
    month: YearMonth,
    completions: Map<Long, HabitCompletion>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column(Modifier.padding(top = 8.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = row * 7 + column - leading + 1
                    if (day !in 1..month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).padding(3.dp).size(38.dp))
                    } else {
                        val date = month.atDay(day)
                        CalendarDayCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            habit = habit,
                            completion = completions[date.toEpochDay()],
                            selected = date == selectedDate,
                            onClick = { onSelectDate(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier,
    date: LocalDate,
    habit: Habit,
    completion: HabitCompletion?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val created = LocalDate.ofEpochDay(habit.createdAtEpochDay)
    val scheduled = habit.isScheduledOn(date)
    val background = when {
        completion?.usedFreezeToken == true -> Color(0xFFBDE0FE)
        completion != null -> MaterialTheme.colorScheme.primary
        date > today -> MaterialTheme.colorScheme.surface
        date < created -> MaterialTheme.colorScheme.surface
        scheduled -> MaterialTheme.colorScheme.error.copy(alpha = .12f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        completion != null && !completion.usedFreezeToken -> MaterialTheme.colorScheme.onPrimary
        date < created -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier.padding(3.dp).size(38.dp).clip(CircleShape)
            .background(background)
            .then(
                if (selected || date == today) Modifier.background(
                    if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = .5f)
                    else Color.Transparent
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (completion != null) {
            Icon(Icons.Filled.Check, null, tint = textColor, modifier = Modifier.size(17.dp))
        } else {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, color = textColor)
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(MaterialTheme.colorScheme.primary, "Done")
        LegendItem(MaterialTheme.colorScheme.error.copy(alpha = .16f), "Missed")
        LegendItem(Color(0xFFBDE0FE), "Protected")
        LegendItem(MaterialTheme.colorScheme.surfaceVariant, "Rest")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
    }
}

private fun dateStatus(habit: Habit, date: LocalDate, completion: HabitCompletion?): String {
    val created = LocalDate.ofEpochDay(habit.createdAtEpochDay)
    return when {
        date < created -> "before this habit started"
        completion?.usedFreezeToken == true -> "completed with streak protection"
        completion != null -> "completed"
        date > LocalDate.now() && habit.isScheduledOn(date) -> "scheduled"
        date > LocalDate.now() -> "rest day"
        habit.isScheduledOn(date) -> "missed scheduled day"
        else -> "rest day"
    }
}

@Composable
private fun InsightsSection(completions: List<HabitCompletion>) {
    val bestHour = HabitInsights.bestTimeOfDay(completions)
    val comebacks = HabitInsights.comebackCount(completions)
    if (bestHour == null && comebacks == 0) return

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Patterns", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                if (bestHour != null) {
                    IconText(
                        Icons.Filled.Schedule,
                        "Most check-ins happen around ${HabitInsights.formatHour(bestHour)}",
                        MaterialTheme.typography.bodyLarge
                    )
                }
                if (comebacks > 0) {
                    IconText(
                        Icons.Filled.TrendingUp,
                        "$comebacks strong comeback${if (comebacks == 1) "" else "s"} after a gap",
                        MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = if (bestHour != null) 10.dp else 0.dp)
                    )
                }
            }
        }
    }
}
