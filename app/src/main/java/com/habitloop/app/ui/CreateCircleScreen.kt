package com.habitloop.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.CommunityRepository
import com.habitloop.app.data.UserPrefs
import kotlinx.coroutines.launch

@Composable
fun CreateCircleScreen(viewModel: HabitViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val leader = UserPrefs.getName(context)?.substringBefore("@").orEmpty().ifBlank { "Loop leader" }
    var title by remember { mutableStateOf("") }
    var habit by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wellbeing") }
    var cadence by remember { mutableStateOf("Daily") }
    var emoji by remember { mutableStateOf("🌱") }
    var tagDraft by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }
    var bannerStyle by remember { mutableStateOf("sage") }
    var days by remember { mutableIntStateOf(21) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = title.trim().length >= 3 && habit.trim().length >= 2 && description.trim().length >= 12

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column {
                Text("Create a circle", style = MaterialTheme.typography.headlineSmall)
                Text("One clear habit, one supportive room", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("You’ll lead this circle. Make the goal easy to understand before asking people to join.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(title, { title = it.take(50) }, label = { Text("Circle name") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            OutlinedTextField(habit, { habit = it.take(50) }, label = { Text("Habit to practice") }, placeholder = { Text("Walk for 20 minutes") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            OutlinedTextField(description, { description = it.take(240) }, label = { Text("Why is this circle useful?") }, supportingText = { Text("${description.length}/240") }, minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            Text("Category", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Wellbeing", "Focus", "Fitness", "Learning", "Daily life").forEach {
                    FilterChip(category == it, { category = it }, label = { Text(it) })
                }
            }
            Text("Circle identity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("🌱", "🎯", "🏃", "📚", "🤝").forEach {
                    FilterChip(emoji == it, { emoji = it }, label = { Text(it) })
                }
            }
            Text("Banner colour", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    "sage" to Color(0xFF78A083), "sunrise" to Color(0xFFF08A65),
                    "ocean" to Color(0xFF4B91B5), "violet" to Color(0xFF8A75C9)
                ).forEach { (style, color) ->
                    FilterChip(
                        selected = bannerStyle == style,
                        onClick = { bannerStyle = style },
                        label = { Text(style.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = { Surface(color = color, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(14.dp)) {} }
                    )
                }
            }
            Text("Discovery tags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
            Text("Add up to five topics so the right people can find your circle.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = tagDraft,
                onValueChange = { tagDraft = it.filter { ch -> ch.isLetterOrDigit() || ch == ' ' }.take(18) },
                label = { Text("Add a tag") },
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        enabled = tagDraft.trim().length >= 2 && tags.size < 5,
                        onClick = {
                            val clean = tagDraft.trim()
                            if (tags.none { it.equals(clean, true) }) tags = tags + clean
                            tagDraft = ""
                        }
                    ) { Text("Add") }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    InputChip(selected = false, onClick = { tags = tags - tag }, label = { Text("#$tag  ×") })
                }
            }
            Text("Rhythm", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Daily", "Weekdays", "3× weekly", "Flexible").forEach {
                    FilterChip(cadence == it, { cadence = it }, label = { Text(it) })
                }
            }
            Text("Challenge length: $days days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Slider(days.toFloat(), { days = (it / 7).toInt() * 7 }, valueRange = 7f..84f, steps = 10)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching {
                            val circleId = CommunityRepository.createCircle(
                                title, description, category, emoji, cadence, days, habit, leader, tags, bannerStyle
                            )
                            viewModel.addHabit(habit, "custom", "1,2,3,4,5,6,7", "Created for $title")
                            circleId
                        }.onSuccess { id ->
                            busy = false
                            if (id != null) onCreated(id) else onBack()
                        }.onFailure {
                            busy = false
                            error = CommunityRepository.readableError(it)
                        }
                    }
                },
                enabled = valid && !busy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) { Text(if (busy) "Creating…" else "Create circle") }
        }
    }
}
