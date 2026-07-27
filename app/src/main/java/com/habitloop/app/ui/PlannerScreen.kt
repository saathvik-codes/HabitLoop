package com.habitloop.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.PlannerTask
import com.habitloop.app.worker.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlannerScreen(
    viewModel: HabitViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    val tasks by viewModel.plannerTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val open = tasks.filterNot { it.isCompleted }
    val completed = tasks.filter { it.isCompleted }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add reminder") },
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("Planner", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "One-time plans, remembered at the right moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (tasks.isEmpty()) {
                item {
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Nothing to remember yet", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                            Text(
                                "Add an appointment, bill, errand, call, or anything you cannot afford to forget.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
            if (open.isNotEmpty()) {
                item { PlannerSectionTitle("Coming up", open.size) }
                items(open, key = { it.id }) { task ->
                    PlannerTaskCard(
                        task = task,
                        onToggle = {
                            viewModel.setPlannerTaskCompleted(task.id, true)
                            ReminderScheduler.cancelPlannerTask(context, task.id)
                        },
                        onDelete = {
                            ReminderScheduler.cancelPlannerTask(context, task.id)
                            viewModel.deletePlannerTask(task.id)
                        },
                        onEdit = { onEditTask(task.id) }
                    )
                }
            }
            if (completed.isNotEmpty()) {
                item { PlannerSectionTitle("Completed", completed.size) }
                items(completed, key = { it.id }) { task ->
                    PlannerTaskCard(
                        task = task,
                        onToggle = {
                            viewModel.setPlannerTaskCompleted(task.id, false)
                            if (task.dueAtEpochMillis > System.currentTimeMillis()) {
                                ReminderScheduler.schedulePlannerTask(context, task.id, task.dueAtEpochMillis)
                            }
                        },
                        onDelete = { viewModel.deletePlannerTask(task.id) },
                        onEdit = { onEditTask(task.id) }
                    )
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 72.dp)) }
        }
    }
}

@Composable
private fun PlannerSectionTitle(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlannerTaskCard(task: PlannerTask, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val due = Instant.ofEpochMilli(task.dueAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val overdue = !task.isCompleted && task.dueAtEpochMillis < System.currentTimeMillis()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    if (task.isCompleted) "Mark incomplete" else "Mark complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        task.isCompleted -> "Completed"
                        overdue -> "Overdue • ${formatPlannerDate(due)}"
                        else -> formatPlannerDate(due)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (task.note.isNotBlank()) {
                    Text(
                        task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit reminder") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Delete reminder") }
        }
    }
}

@Composable
fun AddPlannerTaskScreen(
    viewModel: HabitViewModel,
    existingTask: PlannerTask? = null,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val initialDue = remember(existingTask) { existingTask?.let { Instant.ofEpochMilli(it.dueAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime() } ?: LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0) }
    var title by remember(existingTask) { mutableStateOf(existingTask?.title.orEmpty()) }
    var note by remember(existingTask) { mutableStateOf(existingTask?.note.orEmpty()) }
    var dueDate by remember { mutableStateOf(initialDue.toLocalDate()) }
    var dueTime by remember { mutableStateOf(initialDue.toLocalTime()) }
    var attempted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val due = LocalDateTime.of(dueDate, dueTime)
    val valid = title.trim().length >= 2 && due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > System.currentTimeMillis()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(if (existingTask == null) "New reminder" else "Edit reminder", style = MaterialTheme.typography.headlineMedium)
                    Text("Capture it now. HabitLoop will bring it back on time.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What should I remember?") },
                    supportingText = { Text("${title.length}/80") },
                    isError = attempted && title.trim().length < 2,
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 240) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (optional)") },
                    supportingText = { Text("${note.length}/240") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> dueDate = LocalDate.of(year, month + 1, day) },
                                dueDate.year, dueDate.monthValue - 1, dueDate.dayOfMonth
                            ).apply { datePicker.minDate = System.currentTimeMillis() - 1_000 }.show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(dueDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))) }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> dueTime = LocalTime.of(hour, minute) },
                                dueTime.hour, dueTime.minute, false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(dueTime.format(DateTimeFormatter.ofPattern("h:mm a"))) }
                }
                if (attempted && !valid) {
                    Text(
                        "Enter at least 2 characters and choose a future time.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        attempted = true
                        if (!valid) return@Button
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val dueMillis = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        if (existingTask == null) {
                            viewModel.addPlannerTask(title, note, dueMillis) { taskId ->
                                ReminderScheduler.schedulePlannerTask(context, taskId, dueMillis)
                                onSaved()
                            }
                        } else {
                            val updated = existingTask.copy(title = title.trim(), note = note.trim(), dueAtEpochMillis = dueMillis)
                            viewModel.updatePlannerTask(updated)
                            if (!updated.isCompleted) ReminderScheduler.schedulePlannerTask(context, updated.id, dueMillis)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, Modifier.padding(end = 8.dp))
                    Text(if (existingTask == null) "Save and remind me" else "Save changes")
                }
            }
        }
    }
}

private fun formatPlannerDate(dateTime: LocalDateTime): String {
    val date = dateTime.toLocalDate()
    val prefix = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    return "$prefix • ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
}
