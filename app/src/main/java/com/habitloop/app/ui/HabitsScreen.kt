package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.scheduleLabel
import java.time.LocalDate

@Composable
fun HabitsScreen(viewModel: HabitViewModel, onOpenHabit: (Long) -> Unit) {
    val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

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
                else "${habits.size} routine${if (habits.size == 1) "" else "s"} in your daily loop",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit) { onOpenHabit(habit.id) }
                }
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
private fun HabitCard(habit: Habit, onClick: () -> Unit) {
    val template = HabitTemplates.byId(habit.templateId)
    val doneToday = habit.lastCompletedEpochDay == LocalDate.now().toEpochDay()
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
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = template.accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        "${habit.scheduleLabel()}  •  ${habit.currentStreak} day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = template.accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            if (doneToday) {
                Icon(Icons.Filled.CheckCircle, "Completed today", tint = MaterialTheme.colorScheme.primary)
            } else {
                Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
