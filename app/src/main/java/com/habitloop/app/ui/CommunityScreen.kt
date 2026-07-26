package com.habitloop.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitloop.app.ads.BannerAd
import com.habitloop.app.data.*
import kotlinx.coroutines.launch

private enum class TogetherTab(val label: String) { Circles("Discover"), MyCircles("My circles"), Feed("Check-ins") }

@Composable
fun CommunityScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<CommunitySnapshot?>(null) }
    var selected by remember { mutableStateOf(TogetherTab.Circles) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkInCircle by remember { mutableStateOf<HabitCircle?>(null) }
    var memberCircle by remember { mutableStateOf<HabitCircle?>(null) }
    var chatCircle by remember { mutableStateOf<HabitCircle?>(null) }
    var rankingCircle by remember { mutableStateOf<HabitCircle?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val displayName = remember { UserPrefs.getName(context).orEmpty().ifBlank { "Loop member" } }

    fun refresh() {
        scope.launch {
            error = null
            snapshot = runCatching { CommunityRepository.load() }
                .onFailure { error = CommunityRepository.readableError(it) }
                .getOrElse { CommunitySnapshot(emptyList(), emptySet(), emptyList()) }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    memberCircle?.let { circle ->
        CircleMembersDialog(circle, onDismiss = { memberCircle = null })
    }
    chatCircle?.let { circle ->
        CircleDiscussionDialog(circle, displayName, onDismiss = { chatCircle = null })
    }
    rankingCircle?.let { circle -> CircleLeaderboardDialog(circle) { rankingCircle = null } }

    if (showCreate) {
        CreateCircleDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, description, category, emoji, cadence, days, habitName ->
                scope.launch {
                    busyId = "create"
                    runCatching {
                        CommunityRepository.createCircle(title, description, category, emoji, cadence, days, habitName, displayName)
                        viewModel.addHabit(habitName, "custom", "1,2,3,4,5,6,7", "Created for $title")
                    }.onFailure { error = CommunityRepository.readableError(it) }
                    busyId = null
                    showCreate = false
                    selected = TogetherTab.MyCircles
                    refresh()
                }
            }
        )
    }

    checkInCircle?.let { circle ->
        CheckInDialog(circle, { checkInCircle = null }) { message, mood ->
            scope.launch {
                busyId = circle.id
                runCatching { CommunityRepository.checkIn(circle.id, displayName, message, mood) }
                    .onFailure { error = it.message ?: "Check-in failed. Please try again." }
                busyId = null
                checkInCircle = null
                refresh()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Together", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Text("Create", Modifier.padding(start = 6.dp))
                }
            }
            Text("Small circles, shared momentum—without follower counts or a noisy public feed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TogetherTab.entries.forEach { tab -> FilterChip(selected == tab, { selected = tab }, label = { Text(tab.label) }) }
            }
        }
        error?.let { message -> item { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) { Text(message, Modifier.padding(14.dp)) } } }
        if (snapshot == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else {
            val state = snapshot!!
            when (selected) {
                TogetherTab.Circles -> {
                    item { PrivacyPromise() }
                    if (state.circles.isEmpty()) item {
                        CommunityEmpty("Build the first circle", "HabitLoop challenges live in Firebase—not in the app code. Create a useful challenge, invite people and lead by checking in.")
                    }
                    items(state.circles, key = { it.id }) { circle ->
                        CircleCard(circle, circle.id in state.joinedCircleIds, busyId == circle.id, {
                            scope.launch {
                                busyId = circle.id
                                runCatching {
                                    CommunityRepository.join(circle, displayName)
                                    if (circle.habitName.isNotBlank()) {
                                        viewModel.addHabit(circle.habitName, "custom", "1,2,3,4,5,6,7", "Joined ${circle.title}")
                                    }
                                }.onFailure { error = CommunityRepository.readableError(it) }
                                busyId = null
                                refresh()
                            }
                        }, { checkInCircle = circle }, { memberCircle = circle }, { chatCircle = circle }, { rankingCircle = circle })
                    }
                    item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sponsored", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp)); BannerAd()
                    } }
                }
                TogetherTab.MyCircles -> {
                    val mine = state.circles.filter { it.id in state.joinedCircleIds }
                    if (mine.isEmpty()) item { CommunityEmpty("No circles yet", "Join one focused group and make your first check-in.") }
                    items(mine, key = { it.id }) { CircleCard(it, true, busyId == it.id, {}, { checkInCircle = it }, { memberCircle = it }, { chatCircle = it }, { rankingCircle = it }) }
                }
                TogetherTab.Feed -> {
                    if (state.recentCheckIns.isEmpty()) item { CommunityEmpty("The room is quiet", "Check in to a joined circle. Short, useful updates appear here.") }
                    items(state.recentCheckIns, key = { it.id }) { CheckInCard(it) }
                }
            }
        }
    }
}

@Composable private fun PrivacyPromise() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Progress, not popularity", style = MaterialTheme.typography.titleMedium)
                Text("No follower counts, DMs or public habit history. Share only a short check-in and chosen name.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable private fun CircleCard(
    circle: HabitCircle,
    joined: Boolean,
    busy: Boolean,
    onJoin: () -> Unit,
    onCheckIn: () -> Unit,
    onMembers: () -> Unit,
    onDiscussion: () -> Unit,
    onLeaderboard: () -> Unit
) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(circle.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(circle.title, style = MaterialTheme.typography.titleLarge)
                    Text("${circle.category} · ${circle.cadence}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(circle.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Text(
                "${if (circle.featured) "HabitLoop official" else "Led by ${circle.leaderName}"} · ${circle.durationDays} days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text("${circle.memberCount.coerceAtLeast(0)} members · ${circle.checkInCount.coerceAtLeast(0)} check-ins", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onMembers) { Text("Members") }
                if (joined) {
                    IconButton(onClick = onLeaderboard) { Icon(Icons.Filled.EmojiEvents, "Weekly board") }
                    TextButton(onClick = onDiscussion) {
                        Icon(Icons.Filled.ChatBubble, null, Modifier.size(17.dp))
                        Text("Discussion", Modifier.padding(start = 6.dp))
                    }
                }
            }
            Button(if (joined) onCheckIn else onJoin, Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !busy) {
                Text(if (busy) "Working…" else if (joined) "Check in" else "Join circle")
            }
        }
    }
}

@Composable
private fun CircleLeaderboardDialog(circle: HabitCircle, onDismiss: () -> Unit) {
    var ranks by remember(circle.id) { mutableStateOf<List<CircleRank>?>(null) }
    var error by remember(circle.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(circle.id) {
        ranks = runCatching { CommunityRepository.weeklyLeaderboard(circle.id) }
            .onFailure { error = CommunityRepository.readableError(it) }
            .getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weekly consistency") },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("A fair seven-day board based on distinct check-in days—not streak age, coins, messages, or spending.", style = MaterialTheme.typography.bodySmall)
                }
                if (ranks == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                items(ranks?.size ?: 0) { index ->
                    val rank = ranks!![index]
                    Surface(shape = RoundedCornerShape(16.dp), color = if (index < 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (index < 3) listOf("🥇", "🥈", "🥉")[index] else "${index + 1}", style = MaterialTheme.typography.titleLarge)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(rank.username, style = MaterialTheme.typography.titleMedium)
                                Text("${rank.activeDays}/7 active days", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${rank.consistencyPercent}%", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (ranks?.isEmpty() == true) item { Text("No check-ins in this circle during the last seven days.") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun CircleDiscussionDialog(circle: HabitCircle, username: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var messages by remember(circle.id) { mutableStateOf<List<CircleMessage>?>(null) }
    var draft by remember(circle.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reportTarget by remember { mutableStateOf<CircleMessage?>(null) }
    val myUid = FirebaseSync.uidOrNull

    fun refreshMessages() {
        scope.launch {
            messages = runCatching { CommunityRepository.messages(circle.id) }
                .onFailure { error = CommunityRepository.readableError(it) }
                .getOrDefault(emptyList())
        }
    }
    LaunchedEffect(circle.id) {
        CommunityRepository.observeMessages(circle.id).collect {
            messages = it
            error = null
        }
    }

    reportTarget?.let { message ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report message?") },
            text = { Text("Report content that is harmful, hateful, threatening, sexually explicit, spam, or exposes private information.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching { CommunityRepository.reportMessage(circle.id, message, "Community safety") }
                            .onFailure { error = CommunityRepository.readableError(it) }
                        reportTarget = null
                    }
                }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Cancel") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(circle.title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Circle members only · usernames only · be useful and kind", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                if (messages == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (messages!!.isEmpty()) item { Text("Start with a useful question, tip, or encouragement.") }
                        items(messages!!, key = { it.id }) { message ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (message.userId == myUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text(message.username.substringBefore("@"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(message.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                                    if (message.userId != myUid) {
                                        TextButton(onClick = { reportTarget = message }, modifier = Modifier.align(Alignment.End)) { Text("Report") }
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    draft,
                    { if (it.length <= 500) draft = it },
                    label = { Text("Message") },
                    supportingText = { Text("${draft.length}/500") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    trailingIcon = {
                        IconButton(
                            enabled = draft.isNotBlank() && !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    runCatching { CommunityRepository.sendMessage(circle.id, username, draft) }
                                        .onSuccess { draft = "" }
                                        .onFailure { error = CommunityRepository.readableError(it) }
                                    busy = false
                                }
                            }
                        ) { Icon(Icons.Filled.Send, "Send") }
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun CircleMembersDialog(circle: HabitCircle, onDismiss: () -> Unit) {
    var members by remember(circle.id) { mutableStateOf<List<CircleMember>?>(null) }
    var error by remember(circle.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(circle.id) {
        members = runCatching { CommunityRepository.members(circle.id) }
            .onFailure { error = CommunityRepository.readableError(it) }
            .getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${circle.title} members") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("Usernames only. Contact details and private habit history are never shown.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (members == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                members?.forEach { member ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(member.username.take(1).uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(member.username.substringBefore("@"), style = MaterialTheme.typography.titleMedium)
                            Text(if (member.role == "leader") "Circle leader" else "Member", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (members?.isEmpty() == true) Text("Member profiles will appear as people join.", modifier = Modifier.padding(top = 14.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable private fun CheckInCard(checkIn: CircleCheckIn) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(checkIn.mood, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.padding(start = 12.dp)) {
                Text(checkIn.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (checkIn.message.isNotBlank()) Text(checkIn.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable private fun CommunityEmpty(title: String, body: String) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Groups, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable private fun CheckInDialog(circle: HabitCircle, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("✅") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Check in to ${circle.title}") }, text = {
        Column {
            Text("What helped today? Keep it useful and kind.")
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("✅", "🔥", "🌱", "💪").forEach { option -> FilterChip(mood == option, { mood = option }, label = { Text(option) }) }
            }
            OutlinedTextField(message, { if (it.length <= 180) message = it }, label = { Text("Optional note") }, supportingText = { Text("${message.length}/180") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button({ onSubmit(message, mood) }) { Text("Post check-in") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun CreateCircleDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var habitName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wellbeing") }
    var emoji by remember { mutableStateOf("🌱") }
    var cadence by remember { mutableStateOf("Daily") }
    var duration by remember { mutableStateOf(21) }
    val valid = title.trim().length >= 3 && description.trim().length >= 12 && habitName.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a challenge circle") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("You’ll be the group leader. Set one clear habit people can understand immediately.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(title, { title = it.take(50) }, label = { Text("Circle name") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                OutlinedTextField(habitName, { habitName = it.take(50) }, label = { Text("Habit to track") }, placeholder = { Text("Example: Walk for 20 minutes") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(description, { description = it.take(240) }, label = { Text("What makes this circle useful?") }, supportingText = { Text("${description.length}/240") }, minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Wellbeing", "Focus", "Fitness", "Learning").forEach { value ->
                        FilterChip(category == value, { category = value }, label = { Text(value) })
                    }
                }
                Text("Identity", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("🌱", "🎯", "🏃", "📚").forEach { value ->
                        FilterChip(emoji == value, { emoji = value }, label = { Text(value) })
                    }
                }
                Text("Rhythm", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Daily", "Weekdays", "3× weekly", "Flexible").forEach { value ->
                        FilterChip(cadence == value, { cadence = value }, label = { Text(value) })
                    }
                }
                Text("Duration: $duration days", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = (it / 7).toInt() * 7 },
                    valueRange = 7f..84f,
                    steps = 10
                )
                Text("By creating, you agree to lead respectfully. HabitLoop can remove harmful or misleading circles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, category, emoji, cadence, duration, habitName) },
                enabled = valid
            ) { Text("Create circle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
