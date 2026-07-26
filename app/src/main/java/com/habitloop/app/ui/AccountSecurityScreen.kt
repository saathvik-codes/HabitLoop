package com.habitloop.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.UserPrefs
import kotlinx.coroutines.launch

@Composable
fun AccountSecurityScreen(onBack: () -> Unit, onOpenAuth: () -> Unit, onSignedOut: () -> Unit) {
    var connected by remember { mutableStateOf(FirebaseSync.uidOrNull != null) }
    var anonymous by remember { mutableStateOf(FirebaseSync.isAnonymous()) }
    var connecting by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text("Account & security", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Understand exactly where your data lives and control cloud access.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 20.dp)
        )

        SecurityStatusCard(
            icon = Icons.Filled.PhoneAndroid,
            title = "On-device data",
            status = "Available offline",
            body = "Habits and check-ins are stored locally. Signing out of cloud backup does not erase them."
        )
        SecurityStatusCard(
            icon = if (connected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
            title = "Cloud backup",
            status = if (connected) "Connected" else "Disconnected",
            body = if (connected) {
                if (anonymous) {
                    "Temporary backup ID ${FirebaseSync.maskedAccountId() ?: "active"}. Upgrade it to sign in on another device."
                } else {
                    val username = UserPrefs.getName(context)?.substringBefore("@")?.take(24)?.ifBlank { null }
                        ?: "HabitLoop member"
                    "$username · ${FirebaseSync.providerNames().joinToString().ifBlank { "Firebase" }}"
                }
            } else {
                "Your local data still works, but new changes are not mirrored to Firebase."
            }
        )
        SecurityStatusCard(
            icon = Icons.Filled.Lock,
            title = "Transport security",
            status = "Encrypted in transit",
            body = "Cloud communication uses Firebase over HTTPS. HabitLoop never needs your password for anonymous backup."
        )

        if (connected && anonymous) {
            Button(
                onClick = onOpenAuth,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text("Create account or sign in") }
        }

        if (connected) {
            OutlinedButton(
                onClick = { showSignOutConfirmation = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text(if (anonymous) "Disconnect temporary backup" else "Sign out") }
        } else {
            Button(
                onClick = {
                    connecting = true
                    scope.launch {
                        runCatching { FirebaseSync.ensureSignedIn() }
                            .onSuccess { connected = true }
                        connecting = false
                    }
                },
                enabled = !connecting,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text(if (connecting) "Connecting…" else "Reconnect cloud backup") }
        }
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text(if (anonymous) "Disconnect temporary backup?" else "Sign out?") },
            text = {
                Text(
                    if (anonymous) {
                        "Your on-device habits remain available. This temporary cloud identity may not be recoverable after disconnecting."
                    } else {
                        "Your on-device habits remain available. Cloud syncing pauses until you sign in again."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseSync.signOut()
                    connected = false
                    showSignOutConfirmation = false
                    onSignedOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) { Text("Keep connected") }
            }
        )
    }
}

@Composable
private fun SecurityStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}
