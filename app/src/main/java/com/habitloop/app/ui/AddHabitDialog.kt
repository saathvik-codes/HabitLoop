package com.habitloop.app.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onHabitCreated: (HabitDraft) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a habit") },
        text = {
            LazyColumn(Modifier.heightIn(max = 560.dp)) {
                item {
                    HabitSetupForm(
                        submitLabel = "Add to my loop",
                        onSubmit = onHabitCreated,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
