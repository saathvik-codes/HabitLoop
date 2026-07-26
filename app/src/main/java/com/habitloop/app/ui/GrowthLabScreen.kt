package com.habitloop.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.habitloop.app.data.GrowthRepository
import com.habitloop.app.data.JamRepository
import com.habitloop.app.data.CommunityJam
import com.habitloop.app.data.JamParticipant
import com.habitloop.app.data.UserPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GrowthLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(GrowthRepository.progress(context)) }
    fun completed(skill: String, score: Int, seconds: Int, reflection: String? = null) {
        GrowthRepository.complete(context, skill, score, seconds, reflection)
        progress = GrowthRepository.progress(context)
        mode = null
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Growth Lab", style = MaterialTheme.typography.headlineSmall)
        }
        when (mode) {
            "reset" -> FocusReset { completed("Focus reset", 15, 60) }
            "quiz" -> PerspectiveQuiz { completed("Perspective", 20, 90) }
            "memory" -> MemorySequence { score -> completed("Memory", score, 75) }
            "filter" -> DistractionFilter { score -> completed("Focus filter", score, 60) }
            "reflect" -> GuidedReflection { mood, note -> completed("Reflection", 15, 120, "$mood: $note") }
            "impulse" -> ImpulseControl { score -> completed("Impulse control", score, 45) }
            else -> GrowthLabHome(progress.sessions, progress.skillPoints, progress.lastSkill, onOpen = { mode = it })
        }
    }
}

@Composable
private fun GrowthLabHome(sessions: Int, points: Int, lastSkill: String?, onOpen: (String) -> Unit) {
    val level = points / 100 + 1
    val levelProgress = (points % 100) / 100f
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Train the skills behind the habit", style = MaterialTheme.typography.headlineMedium)
            Text("Two-minute practices designed to help you pause, choose and return.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Mind Level $level", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
                        Text("${points % 100}/100", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    LinearProgressIndicator(progress = { levelProgress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp))
                    Text("Complete sessions to unlock harder patterns and longer sequences.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GrowthStat(Modifier.weight(1f), sessions.toString(), "Sessions")
                GrowthStat(Modifier.weight(1f), points.toString(), "Skill points")
            }
            lastSkill?.let { Text("Last practiced: $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        }
        item { LabCard(Icons.Filled.SelfImprovement, "60-second focus reset", "Follow one breathing loop before your next task.", "reset", onOpen) }
        item { LabCard(Icons.Filled.Lightbulb, "Perspective challenge", "Practice choosing a useful response to an everyday setback.", "quiz", onOpen) }
        item { LabCard(Icons.Filled.Memory, "Memory loop", "Watch a short sequence, then rebuild it from memory.", "memory", onOpen) }
        item { LabCard(Icons.Filled.FilterCenterFocus, "Distraction filter", "Ignore the word and identify its ink color.", "filter", onOpen) }
        item { LabCard(Icons.Filled.Psychology, "Impulse pause", "Respond to GO signals and resist false prompts under time pressure.", "impulse", onOpen) }
        item { LabCard(Icons.Filled.EditNote, "Guided reflection", "Name the day, notice what helped and choose one next step.", "reflect", onOpen) }
        item {
            Text("HabitLoop practices support reflection and learning; they are not diagnosis, therapy or emergency support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun GrowthStat(modifier: Modifier, value: String, label: String) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LabCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, id: String, onOpen: (String) -> Unit) {
    Card(onClick = { onOpen(id) }, shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FocusReset(onDone: () -> Unit) {
    val context = LocalContext.current
    val detector = remember { BreathMotionDetector(context) }
    var sensedMode by remember { mutableStateOf(false) }
    DisposableEffect(sensedMode) {
        if (sensedMode) detector.start()
        onDispose { detector.stop() }
    }
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(0.72f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "breathScale")
    var seconds by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (seconds > 0) { kotlinx.coroutines.delay(1000); seconds-- }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (seconds > 0) if (seconds % 8 >= 4) "Breathe out" else "Breathe in" else "Reset complete", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sensed motion", style = MaterialTheme.typography.labelLarge)
            Switch(checked = sensedMode, onCheckedChange = { sensedMode = it }, modifier = Modifier.padding(start = 8.dp))
        }
        Surface(Modifier.size(190.dp).scale(scale).padding(18.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {}
        if (sensedMode) {
            LinearProgressIndicator(progress = { detector.signal }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Text("${detector.breathCount} motion cycles · ${detector.quality}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Wellness estimate only. Raw motion stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(if (seconds > 0) "$seconds seconds" else "Notice what deserves your attention next.", style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 30.dp)) { Text(if (seconds > 0) "Finish early" else "Return to Growth Lab") }
    }
}

@Composable
private fun MemorySequence(onComplete: (Int) -> Unit) {
    var level by remember { mutableIntStateOf(1) }
    var attempt by remember { mutableIntStateOf(0) }
    val sequence = remember(level, attempt) { List(2 + level) { kotlin.random.Random.nextInt(4) } }
    var lit by remember { mutableIntStateOf(-1) }
    var accepting by remember { mutableStateOf(false) }
    var userStep by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Watch the loop") }
    LaunchedEffect(sequence, attempt) {
        accepting = false
        userStep = 0
        message = "Watch the loop"
        delay(600)
        sequence.forEach { value -> lit = value; delay(550); lit = -1; delay(220) }
        accepting = true
        message = "Your turn"
    }
    val colors = listOf(Color(0xFF84A98C), Color(0xFFF6B89E), Color(0xFFBDE0FE), Color(0xFFFFE8A3))
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Memory loop", style = MaterialTheme.typography.headlineMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                repeat(2) { column ->
                    val index = row * 2 + column
                    Surface(
                        modifier = Modifier.size(112.dp).clickable(enabled = accepting) {
                            if (sequence[userStep] == index) {
                                userStep++
                                if (userStep == sequence.size) {
                                    accepting = false
                                    if (level == 5) {
                                        message = "Five loops mastered"
                                        onComplete(100)
                                    } else {
                                        message = "Level $level complete"
                                        level++
                                    }
                                }
                            } else {
                                accepting = false
                                message = "Almost—watch once more"
                                attempt++
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        color = colors[index].copy(alpha = if (lit == index) 1f else .38f)
                    ) {}
                }
            }
        }
        Text("Level $level of 5 · ${sequence.size} steps · $userStep remembered", style = MaterialTheme.typography.labelLarge)
    }
}

private data class ColorChoice(val name: String, val color: Color)

@Composable
private fun DistractionFilter(onComplete: (Int) -> Unit) {
    val choices = remember { listOf(ColorChoice("Sage", Color(0xFF52796F)), ColorChoice("Peach", Color(0xFFE88968)), ColorChoice("Blue", Color(0xFF5286A5))) }
    val questions = remember {
        List(12) {
            val wordIndex = kotlin.random.Random.nextInt(choices.size)
            var inkIndex = kotlin.random.Random.nextInt(choices.size)
            if (inkIndex == wordIndex) inkIndex = (inkIndex + 1) % choices.size
            wordIndex to inkIndex
        }
    }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var totalReactionMs by remember { mutableStateOf(0L) }
    var roundStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    val ink = choices[questions[round].second]
    val word = choices[questions[round].first].name
    val answerOrder = remember(round) { choices.shuffled() }
    val averageReaction = if (round == 0) 0 else totalReactionMs / round
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Distraction filter", style = MaterialTheme.typography.headlineMedium)
        Text("Tap the ink color—not the written word.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        Text(word, style = MaterialTheme.typography.displayLarge, color = ink.color, modifier = Modifier.padding(vertical = 54.dp))
        answerOrder.forEach { choice ->
            OutlinedButton(
                onClick = {
                    totalReactionMs += System.currentTimeMillis() - roundStartedAt
                    if (choice.name == ink.name) score++
                    if (round == questions.lastIndex) {
                        val finalCorrect = score + if (choice.name == ink.name) 1 else 0
                        val avg = (totalReactionMs / questions.size).coerceAtLeast(1)
                        val speedBonus = ((1800L - avg).coerceAtLeast(0) / 45).toInt()
                        onComplete((finalCorrect * 6 + speedBonus).coerceAtMost(100))
                    } else {
                        round++
                        roundStartedAt = System.currentTimeMillis()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) { Text(choice.name) }
        }
        Text(
            "Round ${round + 1}/${questions.size} · $score correct${if (averageReaction > 0) " · ${averageReaction}ms avg" else ""}",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun GuidedReflection(onComplete: (String, String) -> Unit) {
    var mood by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Guided reflection", style = MaterialTheme.typography.headlineMedium)
        Text("How did today feel?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Calm", "Heavy", "Proud", "Mixed").forEach { value ->
                FilterChip(mood == value, { mood = value }, label = { Text(value) })
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(300) },
            label = { Text("What helped, and what is one kind next step?") },
            supportingText = { Text("${note.length}/300") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onComplete(mood, note) },
            enabled = mood.isNotBlank() && note.trim().length >= 10,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        ) { Text("Save reflection") }
        Text("Reflections sync to your private Firebase account space.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun ImpulseControl(onComplete: (Int) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var tapped by remember { mutableStateOf(false) }
    var showSignal by remember { mutableStateOf(false) }
    val isGo = remember(round) { kotlin.random.Random.nextInt(100) < 62 }
    val finished = round >= 10

    LaunchedEffect(round) {
        if (!finished) {
            tapped = false
            showSignal = false
            delay(kotlin.random.Random.nextLong(450, 1050))
            showSignal = true
            delay(900)
            if (!tapped && !isGo) score += 10
            showSignal = false
            delay(260)
            round++
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Impulse pause", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tap only when the loop says GO. Hold still on PAUSE.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        LinearProgressIndicator(progress = { round.coerceAtMost(10) / 10f }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(220.dp).clickable(enabled = showSignal && !tapped) {
                    tapped = true
                    if (isGo) score += 10 else score = (score - 5).coerceAtLeast(0)
                },
                shape = CircleShape,
                color = when {
                    !showSignal -> MaterialTheme.colorScheme.surfaceVariant
                    isGo -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (!showSignal) "WAIT" else if (isGo) "GO" else "PAUSE",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (showSignal && isGo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Text("Round ${round.coerceAtMost(10)} of 10 · $score points", style = MaterialTheme.typography.titleMedium)
        if (finished) {
            Text(
                if (score >= 80) "Excellent response control." else "Good training. Repeat later and aim for calmer accuracy.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button({ onComplete(score.coerceAtLeast(10)) }, Modifier.fillMaxWidth().padding(top = 18.dp)) { Text("Save session") }
        }
    }
}

@Composable
private fun PerspectiveQuiz(onDone: () -> Unit) {
    data class Scenario(val prompt: String, val choices: List<String>, val best: Int, val explanation: String)
    val scenarios = remember {
        listOf(
            Scenario(
                "You miss two planned workouts after a difficult week. What response best supports the habit?",
                listOf("Double every workout next week", "Drop it until next month", "Choose one smaller session and restart today"),
                2, "A smaller restart protects continuity without using punishment."
            ),
            Scenario(
                "A friend checks in for seven days while you complete only four. What is the most useful interpretation?",
                listOf("I am falling behind", "Their result is information, not my score", "I need a harder target immediately"),
                1, "Separating another person’s progress from your worth keeps community supportive."
            ),
            Scenario(
                "Your focus session is interrupted after ten minutes. What should count?",
                listOf("Nothing—it was incomplete", "The ten focused minutes plus a planned restart", "Only the distraction"),
                1, "Recognizing partial progress makes the next intentional action easier."
            )
        )
    }
    var round by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf<Int?>(null) }
    val scenario = scenarios[round]
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Perspective challenge · ${round + 1}/${scenarios.size}", style = MaterialTheme.typography.headlineMedium)
        Text(scenario.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
        scenario.choices.forEachIndexed { index, option ->
            Surface(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(enabled = answer == null) {
                    answer = index
                    if (index == scenario.best) correct++
                },
                shape = RoundedCornerShape(18.dp),
                color = when {
                    answer == null -> MaterialTheme.colorScheme.surfaceVariant
                    index == scenario.best -> MaterialTheme.colorScheme.primaryContainer
                    answer == index -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) { Text(option, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
        }
        answer?.let {
            Text(scenario.explanation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            Button(
                onClick = {
                    if (round == scenarios.lastIndex) onDone()
                    else { round++; answer = null }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) { Text(if (round == scenarios.lastIndex) "Complete · $correct/${scenarios.size}" else "Next scenario") }
        }
    }
}

@Composable
private fun CommunityJams(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jams by remember { mutableStateOf<List<CommunityJam>?>(null) }
    var creating by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf<CommunityJam?>(null) }
    var peopleJam by remember { mutableStateOf<CommunityJam?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val username = UserPrefs.getName(context)?.substringBefore("@")?.take(24) ?: "Loop member"
    val shareSocials = UserPrefs.sharesSocialsInJams(context)
    val instagram = if (shareSocials) UserPrefs.getInstagram(context) else ""
    val discord = if (shareSocials) UserPrefs.getDiscord(context) else ""
    fun refresh() { scope.launch { jams = runCatching { JamRepository.active() }.onFailure { error = "Jams need Firebase community access and deployed rules." }.getOrDefault(emptyList()) } }
    LaunchedEffect(Unit) { refresh() }
    peopleJam?.let { jam -> JamPeopleDialog(jam) { peopleJam = null } }
    active?.let { jam ->
        JamTimer(jam) {
            GrowthRepository.complete(context, "Community ${jam.skill}", 25, jam.durationMinutes * 60)
            active = null
        }
        return
    }
    if (creating) CreateJamDialog({ creating = false }) { title, skill, minutes ->
        scope.launch {
            runCatching { JamRepository.create(title, skill, minutes, username, instagram, discord) }
                .onSuccess { creating = false; active = it; refresh() }
                .onFailure { error = it.message ?: "Could not create this Jam." }
        }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Community Jams", style = MaterialTheme.typography.headlineSmall)
                    Text("Practice beside people, not against them.", style = MaterialTheme.typography.bodyMedium)
                }
                Button({ creating = true }) { Text("Create") }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (jams == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (jams?.isEmpty() == true) item { Text("No active Jams. Start one and become the first host.") }
        jams?.forEach { jam ->
            item(key = jam.id) {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(jam.title, style = MaterialTheme.typography.titleLarge)
                        Text("${jam.skill} · ${jam.durationMinutes} min · led by ${jam.leaderName}", style = MaterialTheme.typography.bodyMedium)
                        Text("${jam.participantCount} practicing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { peopleJam = jam }, modifier = Modifier.align(Alignment.End)) {
                            Text("See participants")
                        }
                        Button({
                            scope.launch {
                                runCatching { JamRepository.join(jam, username, instagram, discord) }
                                    .onSuccess { active = jam }
                                    .onFailure { error = it.message ?: "Could not join this Jam." }
                            }
                        }, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Join Jam") }
                    }
                }
            }
        }
    }
}

@Composable
private fun JamPeopleDialog(jam: CommunityJam, onDismiss: () -> Unit) {
    var people by remember(jam.id) { mutableStateOf<List<JamParticipant>?>(null) }
    LaunchedEffect(jam.id) {
        people = runCatching { JamRepository.participants(jam.id) }.getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${jam.title} participants") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        "Only handles people explicitly chose to share inside Jams are shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (people == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                items(people?.size ?: 0) { index ->
                    val person = people!![index]
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(person.username.substringBefore("@"), style = MaterialTheme.typography.titleMedium)
                            if (person.instagram.isNotBlank()) Text("Instagram · @${person.instagram}", style = MaterialTheme.typography.bodyMedium)
                            if (person.discord.isNotBlank()) Text("Discord · ${person.discord}", style = MaterialTheme.typography.bodyMedium)
                            if (person.instagram.isBlank() && person.discord.isBlank()) {
                                Text("Contact links private", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun CreateJamDialog(onDismiss: () -> Unit, onCreate: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("Focus") }
    var minutes by remember { mutableIntStateOf(10) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create a Community Jam") }, text = {
        Column {
            OutlinedTextField(title, { title = it.take(50) }, label = { Text("Jam name") }, singleLine = true)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Focus", "Memory", "Reflect").forEach { FilterChip(skill == it, { skill = it }, label = { Text(it) }) }
            }
            Text("Duration: $minutes minutes", modifier = Modifier.padding(top = 10.dp))
            Slider(minutes.toFloat(), { minutes = it.toInt() }, valueRange = 2f..30f)
        }
    }, confirmButton = { Button({ onCreate(title, skill, minutes) }, enabled = title.trim().length >= 3) { Text("Start Jam") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun JamTimer(jam: CommunityJam, onDone: () -> Unit) {
    var seconds by remember(jam.id) { mutableIntStateOf(jam.durationMinutes * 60) }
    LaunchedEffect(jam.id) { while (seconds > 0) { delay(1000); seconds-- } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(jam.title, style = MaterialTheme.typography.headlineMedium)
        Text("${jam.participantCount} people practicing ${jam.skill.lowercase()} together", style = MaterialTheme.typography.bodyLarge)
        Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 30.dp))
        LinearProgressIndicator(progress = { 1f - seconds.toFloat() / (jam.durationMinutes * 60) }, modifier = Modifier.fillMaxWidth())
        Button(onDone, Modifier.fillMaxWidth().padding(top = 24.dp)) { Text(if (seconds == 0) "Complete Jam" else "Finish my session") }
    }
}
