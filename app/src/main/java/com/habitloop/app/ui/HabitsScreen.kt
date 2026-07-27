package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.scheduleLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitsScreen(viewModel: HabitViewModel, onOpenHabit: (Long) -> Unit) {
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HabitListFilter.Active) }
    val today = LocalDate.now().toEpochDay()
    val visibleHabits = habits.filter { habit ->
        val matchesState = when (filter) {
            HabitListFilter.Active -> !habit.isArchived &&
                (habit.pausedUntilEpochDay == null || habit.pausedUntilEpochDay < today)
            HabitListFilter.Paused -> !habit.isArchived &&
                habit.pausedUntilEpochDay != null && habit.pausedUntilEpochDay >= today
            HabitListFilter.Archived -> habit.isArchived
        }
        matchesState && habit.name.contains(query.trim(), ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add habit") },
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Your habits", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (habits.isEmpty()) "Start with one routine worth returning to."
                else "Build routines that can flex when life changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Find a habit") },
                shape = RoundedCornerShape(18.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitListFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) }
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (visibleHabits.isEmpty()) {
                    item { EmptyHabitList(filter, query) }
                }
                items(visibleHabits, key = { it.id }) { habit ->
                    HabitCard(habit) { onOpenHabit(habit.id) }
                }
                item { Spacer(Modifier.size(88.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onHabitCreated = {
                viewModel.addHabit(it.name, it.templateId, it.scheduleDaysCsv, it.motivation)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyHabitList(filter: HabitListFilter, query: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            when {
                query.isNotBlank() -> "No habits match “${query.trim()}”."
                filter == HabitListFilter.Paused -> "No paused habits"
                filter == HabitListFilter.Archived -> "No archived habits"
                else -> "Your first habit starts here"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            if (filter == HabitListFilter.Active) "Add one small routine you can repeat."
            else "Habits you ${filter.label.lowercase()} will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private enum class HabitListFilter(val label: String) {
    Active("Active"),
    Paused("Paused"),
    Archived("Archived")
}

@Composable
private fun HabitCard(habit: Habit, onClick: () -> Unit) {
    val template = HabitTemplates.byId(habit.templateId)
    val doneToday = habit.lastCompletedEpochDay == LocalDate.now().toEpochDay()
    val pausedUntil = habit.pausedUntilEpochDay?.let(LocalDate::ofEpochDay)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                    .background(template.accentColor.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(template.iconRes), null, Modifier.size(30.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = template.accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        when {
                            habit.isArchived -> "History preserved"
                            pausedUntil != null -> "Paused until ${pausedUntil.format(DateTimeFormatter.ofPattern("MMM d"))}"
                            else -> "${habit.scheduleLabel()} • ${habit.currentStreak} day streak"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = template.accentColor,
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            when {
                habit.isArchived -> Text("Archived", style = MaterialTheme.typography.labelLarge)
                pausedUntil != null -> Text("Paused", style = MaterialTheme.typography.labelLarge)
                doneToday -> Icon(Icons.Filled.CheckCircle, "Completed today", tint = MaterialTheme.colorScheme.primary)
                else -> Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
