package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.habitloop.app.R

@Composable
fun AppGuideScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp
        )
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("How HabitLoop works", style = MaterialTheme.typography.headlineSmall)
            }
            Image(
                painter = painterResource(R.drawable.onboarding_loop_art),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(170.dp)
            )
            Text(
                "Your daily loop",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Create routines in your own words, see only what is scheduled today, check in, and use your history to adjust.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
        }
        item {
            GuideCard(Icons.Filled.CheckCircle, "1. Check Today", "Today is the fastest path: tap the circular control once when a scheduled routine is complete.")
            GuideCard(Icons.Filled.List, "2. Shape your habits", "Create custom routines, choose their weekdays and record why they matter. Open a habit for history and detailed status.")
            GuideCard(Icons.Filled.AutoGraph, "3. Read Trends", "Use weekly activity and 30-day consistency to understand patterns. An unscheduled day is never counted as a miss.")
            GuideCard(Icons.Filled.Groups, "4. Join Together", "Guided challenges create real routines in your private tracker. Joining does not publish your check-ins.")
            GuideCard(Icons.Filled.Shield, "5. Protect a streak", "A freeze automatically covers one missed scheduled occurrence. One completed rewarded ad grants one freeze to the selected habit.")
            GuideCard(Icons.Filled.Person, "6. Control your space", "Profile contains rewards, reminders, cloud status, security controls and this guide.")
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("The simple daily routine", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Open Today → complete what is due → review Trends weekly → adjust schedules when life changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
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
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
