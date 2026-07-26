package com.habitloop.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.*
import kotlinx.coroutines.launch

private enum class CircleSection(val label: String) {
    Discussion("Discussion"), CheckIn("Check-in"), Members("Members"), Board("Weekly board")
}

@Composable
fun CircleDetailScreen(circleId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val username = UserPrefs.getName(context)?.substringBefore("@")?.take(24).orEmpty().ifBlank { "Loop member" }
    var circle by remember(circleId) { mutableStateOf<HabitCircle?>(null) }
    var section by remember { mutableStateOf(CircleSection.Discussion) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(circleId) {
        circle = runCatching { CommunityRepository.circle(circleId) }
            .onFailure { error = CommunityRepository.readableError(it) }.getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(circle?.title ?: "Circle", style = MaterialTheme.typography.headlineSmall)
                circle?.let { Text("${it.memberCount} members · ${it.cadence}", style = MaterialTheme.typography.bodySmall) }
            }
        }
        circle?.let {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${it.emoji}  ${it.habitName}", style = MaterialTheme.typography.titleLarge)
                    Text(it.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
                    Text("Led by ${it.leaderName.substringBefore("@")}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircleSection.entries.forEach { item ->
                FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.label) })
            }
        }
        error?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp)) {
                Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (circle == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        when (section) {
            CircleSection.Discussion -> CircleDiscussionContent(circleId, username, onError = { error = it })
            CircleSection.CheckIn -> CircleCheckInContent(circleId, username, onError = { error = it })
            CircleSection.Members -> CircleMembersContent(circleId, onError = { error = it })
            CircleSection.Board -> CircleBoardContent(circleId, onError = { error = it })
        }
    }
}

@Composable
private fun CircleDiscussionContent(circleId: String, username: String, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var messages by remember(circleId) { mutableStateOf<List<CircleMessage>?>(null) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<CircleMessage?>(null) }
    val myUid = FirebaseSync.uidOrNull

    LaunchedEffect(circleId) {
        runCatching {
            CommunityRepository.observeMessages(circleId).collect { messages = it }
        }.onFailure { onError(CommunityRepository.readableError(it)) }
    }
    reportTarget?.let { message ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report this message?") },
            text = { Text("Use reporting for harmful, threatening, hateful, explicit, spam or privacy-violating content.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching { CommunityRepository.reportMessage(circleId, message, "Community safety") }
                            .onFailure { onError(CommunityRepository.readableError(it)) }
                        reportTarget = null
                    }
                }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Cancel") } }
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (messages == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (messages?.isEmpty() == true) item { Text("Start with a useful question, tip or encouragement.") }
            items(messages.orEmpty(), key = { it.id }) { message ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (message.userId == myUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(message.username.substringBefore("@"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 3.dp))
                        if (message.userId != myUid) TextButton(onClick = { reportTarget = message }, modifier = Modifier.align(Alignment.End)) { Text("Report") }
                    }
                }
            }
        }
        OutlinedTextField(
            draft, { if (it.length <= 500) draft = it },
            label = { Text("Message the circle") },
            supportingText = { Text("${draft.length}/500") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            trailingIcon = {
                IconButton(enabled = draft.isNotBlank() && !sending, onClick = {
                    sending = true
                    scope.launch {
                        runCatching { CommunityRepository.sendMessage(circleId, username, draft) }
                            .onSuccess { draft = "" }
                            .onFailure { onError(CommunityRepository.readableError(it)) }
                        sending = false
                    }
                }) { Icon(Icons.Filled.Send, "Send") }
            }
        )
    }
}

@Composable
private fun CircleCheckInContent(circleId: String, username: String, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("✅") }
    var posted by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Share today’s progress", style = MaterialTheme.typography.titleLarge)
        Text("Short, useful and honest. Your private habit history stays private.", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("✅", "🔥", "🌱", "💪").forEach { option ->
                FilterChip(selected = mood == option, onClick = { mood = option }, label = { Text(option) })
            }
        }
        OutlinedTextField(note, { note = it.take(180) }, label = { Text("What helped today?") }, supportingText = { Text("${note.length}/180") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    runCatching { CommunityRepository.checkIn(circleId, username, note, mood) }
                        .onSuccess { note = ""; posted = true }
                        .onFailure { onError(CommunityRepository.readableError(it)) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) { Text(if (posted) "Check-in posted" else "Post check-in") }
    }
}

@Composable
private fun CircleMembersContent(circleId: String, onError: (String) -> Unit) {
    var members by remember(circleId) { mutableStateOf<List<CircleMember>?>(null) }
    LaunchedEffect(circleId) {
        members = runCatching { CommunityRepository.members(circleId) }
            .onFailure { onError(CommunityRepository.readableError(it)) }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Usernames only. Contact details and private habit histories are hidden.", style = MaterialTheme.typography.bodyMedium) }
        if (members == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(members.orEmpty(), key = { it.userId }) { member ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(member.username.take(1).uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(member.username.substringBefore("@"), style = MaterialTheme.typography.titleMedium)
                        Text(if (member.role == "leader") "Circle leader" else "Member", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleBoardContent(circleId: String, onError: (String) -> Unit) {
    var ranks by remember(circleId) { mutableStateOf<List<CircleRank>?>(null) }
    LaunchedEffect(circleId) {
        ranks = runCatching { CommunityRepository.weeklyLeaderboard(circleId) }
            .onFailure { onError(CommunityRepository.readableError(it)) }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Seven-day consistency", style = MaterialTheme.typography.titleLarge)
                    Text("Distinct check-in days—not spending, messages or lifetime streak age.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (ranks == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(ranks.orEmpty(), key = { it.userId }) { rank ->
            val index = ranks.orEmpty().indexOf(rank)
            Surface(shape = RoundedCornerShape(18.dp), color = if (index < 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index < 3) listOf("🥇", "🥈", "🥉")[index] else "${index + 1}", style = MaterialTheme.typography.titleLarge)
                    Text(rank.username, Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                    Text("${rank.activeDays}/7 · ${rank.consistencyPercent}%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
