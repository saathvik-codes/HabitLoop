package com.habitloop.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.habitloop.app.R
import com.habitloop.app.data.HabitTemplate
import com.habitloop.app.data.HabitTemplates

private const val STEP_WELCOME = 0
private const val STEP_TEMPLATE = 1
private const val STEP_WALKTHROUGH = 2
private const val STEP_NOTIFICATIONS = 3
private const val TOTAL_STEPS = 4

/**
 * A real three-step wizard, not two disconnected screens: name → notification
 * permission (with an honest rationale, not a silent OS prompt) → first
 * habit template. Progress dots at the top so it never feels open-ended,
 * animated slide transitions between steps so it feels like one continuous
 * flow instead of a stack of separate pages.
 */
@Composable
fun OnboardingFlow(onFinished: (name: String?, habit: HabitDraft) -> Unit) {
    var step by remember { mutableIntStateOf(STEP_WELCOME) }
    var name by remember { mutableStateOf<String?>(null) }
    var selectedHabit by remember { mutableStateOf<HabitDraft?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        StepProgressDots(currentStep = step, totalSteps = TOTAL_STEPS)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it } togetherWith slideOutHorizontally(tween(300)) { -it })
            },
            modifier = Modifier.fillMaxSize()
        ) { targetStep ->
            when (targetStep) {
                STEP_WELCOME -> WelcomeStep(
                    onContinue = { enteredName ->
                        name = enteredName
                        step = STEP_TEMPLATE
                    }
                )
                STEP_TEMPLATE -> FirstHabitStep(
                    onHabitCreated = { habit ->
                        selectedHabit = habit
                        step = STEP_WALKTHROUGH
                    }
                )
                STEP_WALKTHROUGH -> WalkthroughStep(
                    onContinue = { step = STEP_NOTIFICATIONS }
                )
                STEP_NOTIFICATIONS -> NotificationPermissionStep(
                    onContinue = {
                        selectedHabit?.let { onFinished(name, it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun FirstHabitStep(onHabitCreated: (HabitDraft) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp
        )
    ) {
        item {
            Text("Build your first loop", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Start with something small and specific. You can change or add more routines later.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            HabitSetupForm(
                submitLabel = "Continue with this habit",
                onSubmit = onHabitCreated
            )
        }
    }
}

@Composable
private fun StepProgressDots(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalSteps) { index ->
            val active = index == currentStep
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: (name: String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    val trimmedName = name.trim()
    val validUsername = trimmedName.length in 3..20 &&
        trimmedName.all { it.isLetterOrDigit() || it == '_' }
    val invalidName = name.isNotEmpty() && !validUsername

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_loop_art),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(bottom = 16.dp)
        )
        Text("Welcome to HabitLoop", style = MaterialTheme.typography.displayLarge)
        Text(
            "Choose your private username",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.filter { char -> char.isLetterOrDigit() || char == '_' }.take(20) },
            label = { Text("Username") },
            singleLine = true,
            isError = invalidName,
            supportingText = {
                Text(if (invalidName) "Use 3–20 letters, numbers or underscores." else "${name.length}/20 • shown to communities")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onContinue(trimmedName.ifBlank { null }) },
            enabled = validUsername,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            Text("Continue")
        }
        Text(
            "Your email and phone number are never used as your public community name.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun NotificationPermissionStep(onContinue: () -> Unit) {
    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onContinue() }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Stay on track", style = MaterialTheme.typography.headlineMedium)
        Text(
            "HabitLoop can send you one gentle reminder a day for habits you haven't logged yet — nothing more, no spam. You can change the time later in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
        )
        Button(
            onClick = {
                if (needsRuntimePermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow reminders")
        }
        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Not now")
        }
    }
}

@Composable
private fun WalkthroughStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Your loop, at a glance", style = MaterialTheme.typography.headlineMedium)
        Text(
            "HabitLoop is designed to answer one question quickly: what still needs your attention today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        WalkthroughCard(
            icon = Icons.Filled.CheckCircle,
            title = "Today",
            body = "Check in with one tap. Completed habits move out of the way so your next action stays obvious."
        )
        WalkthroughCard(
            icon = Icons.Filled.LocalFireDepartment,
            title = "Habits",
            body = "Open any habit to see its streak, history, best time and comeback patterns."
        )
        WalkthroughCard(
            icon = Icons.Filled.Shield,
            title = "Streak protection",
            body = "A freeze can cover one missed day. Earn one intentionally from the Perks screen."
        )
        WalkthroughCard(
            icon = Icons.Filled.Insights,
            title = "Insights and Growth Lab",
            body = "Understand your patterns, then practice focus, memory, perspective and reflection from your Profile."
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Set up my reminder")
        }
    }
}

@Composable
private fun WalkthroughCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
