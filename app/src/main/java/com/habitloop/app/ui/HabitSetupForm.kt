package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.HabitTemplates

data class HabitDraft(
    val name: String,
    val templateId: String,
    val scheduleDaysCsv: String,
    val motivation: String
)

@Composable
fun HabitSetupForm(
    submitLabel: String,
    onSubmit: (HabitDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf(HabitTemplates.ALL.first().id) }
    var selectedDays by remember { mutableStateOf((1..7).toSet()) }
    var motivation by remember { mutableStateOf("") }
    val validName = name.trim().length >= 3

    Column(modifier) {
        Text("What do you want to keep doing?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Write it in your own words. The choices below only control its icon and schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(48) },
            label = { Text("Habit name") },
            placeholder = { Text("e.g. Walk after lunch") },
            singleLine = true,
            isError = name.isNotEmpty() && !validName,
            supportingText = { Text(if (name.isNotEmpty() && !validName) "Use at least 3 characters." else "${name.length}/48") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Choose a category", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(HabitTemplates.ALL) { template ->
                FilterChip(
                    selected = selectedTemplateId == template.id,
                    onClick = { selectedTemplateId = template.id },
                    label = { Text(template.displayName) },
                    leadingIcon = {
                        Image(painterResource(template.iconRes), null, Modifier.size(20.dp))
                    }
                )
            }
        }

        Text("Which days?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(listOf("M", "T", "W", "T", "F", "S", "S").withIndex().toList()) { indexed ->
                val index = indexed.index
                val label = indexed.value
                val day = index + 1
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                    },
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(
            value = motivation,
            onValueChange = { motivation = it.take(100) },
            label = { Text("Why does this matter? (optional)") },
            placeholder = { Text("A reminder for your future self") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        Button(
            onClick = {
                onSubmit(
                    HabitDraft(
                        name = name.trim(),
                        templateId = selectedTemplateId,
                        scheduleDaysCsv = selectedDays.sorted().joinToString(","),
                        motivation = motivation.trim()
                    )
                )
            },
            enabled = validName && selectedDays.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        ) {
            Text(submitLabel)
        }
    }
}
