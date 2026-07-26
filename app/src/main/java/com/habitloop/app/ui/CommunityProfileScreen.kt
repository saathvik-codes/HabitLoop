package com.habitloop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.CommunityRepository
import com.habitloop.app.data.HabitCircle
import com.habitloop.app.data.UserPrefs

@Composable
fun CommunityProfileScreen(
    circleId: String,
    onBack: () -> Unit,
    onDiscussion: () -> Unit,
    onCheckIn: () -> Unit,
    onMembers: () -> Unit,
    onBoard: () -> Unit
) {
    var circle by remember(circleId) { mutableStateOf<HabitCircle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(circleId) {
        circle = runCatching { CommunityRepository.circle(circleId) }
            .onFailure { error = CommunityRepository.readableError(it) }.getOrNull()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box {
                val colors = when (circle?.bannerStyle) {
                    "sunrise" -> listOf(Color(0xFFF07858), Color(0xFFFFC49A))
                    "ocean" -> listOf(Color(0xFF31799C), Color(0xFF8BD0DB))
                    "violet" -> listOf(Color(0xFF7056AE), Color(0xFFCABAF6))
                    else -> listOf(Color(0xFF4E775B), Color(0xFFA7CBAE))
                }
                Box(Modifier.fillMaxWidth().height(232.dp).background(Brush.linearGradient(colors)))
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .92f)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.padding(10.dp), tint = Color(0xFF20231F))
                    }
                }
                Surface(
                    shape = CircleShape, color = Color.White, shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 18.dp).size(82.dp)
                ) { Box(contentAlignment = Alignment.Center) { Text(circle?.emoji ?: "🌱", style = MaterialTheme.typography.headlineLarge) } }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 116.dp, end = 18.dp, bottom = 22.dp)) {
                    Text(circle?.title ?: "Community", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(circle?.category.orEmpty(), color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        circle?.let { model ->
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(model.description, style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AssistChip(onClick = onMembers, label = { Text("${model.memberCount} members") }, leadingIcon = { Icon(Icons.Filled.Groups, null) })
                        AssistChip(onClick = onCheckIn, label = { Text("${model.checkInCount} check-ins") })
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (model.tags.ifEmpty { listOf(model.category, model.cadence) }).filter { it.isNotBlank() }.forEach {
                            SuggestionChip(onClick = {}, label = { Text("#${it.replace(" ", "")}") })
                        }
                    }
                    Text("Led by @${model.leaderName.substringBefore("@")}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Community spaces", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 22.dp, bottom = 7.dp))
                    DestinationCard(Icons.Filled.ChatBubble, "Discussion", "Questions, ideas and live conversation", Color(0xFFE9DFFF), onDiscussion)
                    DestinationCard(Icons.Filled.CheckCircle, "Daily check-in", "Share progress without exposing private habits", Color(0xFFDDF3E4), onCheckIn)
                    DestinationCard(Icons.Filled.Groups, "Members", "Meet the people building this habit", Color(0xFFFFE7D4), onMembers)
                    DestinationCard(Icons.Filled.EmojiEvents, "Weekly board", "A friendly seven-day consistency challenge", Color(0xFFDDEEFF), onBoard)
                    Text("About this circle", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
                    Text(model.mission.ifBlank { model.description }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 7.dp))
                    Text("${model.durationDays}-day journey • ${model.cadence}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 9.dp))
                }
            }
        }
        error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)) }
        }
        if (circle == null && error == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp)) }
    }
}

@Composable
private fun DestinationCard(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = tint) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .76f)) { Icon(icon, null, Modifier.padding(11.dp)) }
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, null)
        }
    }
}

@Composable
fun CircleFeatureScreen(
    circleId: String,
    title: String,
    onBack: () -> Unit,
    content: @Composable (String, String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val username = UserPrefs.getName(context)?.substringBefore("@")?.take(24).orEmpty().ifBlank { "Loop member" }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
        content(circleId, username) { error = it }
    }
}
