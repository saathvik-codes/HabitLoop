package com.habitloop.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitloop.app.data.NotificationInbox
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationInboxScreen(onBack: () -> Unit, onOpenCategory: (String) -> Unit) {
    val notifications by NotificationInbox.items.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Activity", style = MaterialTheme.typography.headlineMedium)
                    Text("Reminders and community updates on this device", style = MaterialTheme.typography.bodyMedium)
                }
                if (notifications.any { !it.read }) TextButton(onClick = NotificationInbox::markAllRead) { Text("Mark read") }
            }
        }
        if (notifications.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Text("All quiet for now", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
                        Text("Useful reminders and circle updates will appear here.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        items(notifications, key = { it.id }) { notification ->
            Card(
                onClick = { onOpenCategory(notification.category) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notification.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(notification.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                    Text(
                        Instant.ofEpochMilli(notification.createdAt).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d · h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
