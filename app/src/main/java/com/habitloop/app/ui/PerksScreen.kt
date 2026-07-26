package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.habitloop.app.R
import com.habitloop.app.data.Habit
import com.habitloop.app.data.HabitTemplates
import com.habitloop.app.data.RewardWallet
import com.habitloop.app.ads.RewardedAdState

/**
 * The perk economy screen — where the ad-watching mechanic and the
 * streak-freeze reward actually meet the user. Kept as its own destination
 * (not buried in habit detail) so it reads as "a place with things to earn,"
 * which is what makes the daily-return habit form in the first place.
 */
@Composable
fun PerksScreen(
    habits: List<Habit>,
    adState: RewardedAdState,
    onWatchAdForFreeze: (Long) -> Unit,
    onRedeemCoins: (Long) -> Unit,
    onRetryAd: () -> Unit
) {
    val coinBalance by RewardWallet.balance.collectAsStateWithLifecycle()
    val unlocked by RewardWallet.unlocked.collectAsStateWithLifecycle()
    var dropResult by remember { mutableIntStateOf(0) }
    var dropAvailable by remember { mutableStateOf(RewardWallet.dailyDropAvailable()) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Image(
                painter = painterResource(R.drawable.streak_protection_art),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Text("Streak protection", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Complete habits to earn Loop Coins. Redeem 100 coins or choose an optional rewarded ad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (habits.isEmpty()) {
            Text(
                "Add a habit first to start earning perks.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("🪙 $coinBalance Loop Coins", style = MaterialTheme.typography.headlineSmall)
                            Text("Habit check-in +10 · every 7-day milestone +25 bonus", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Daily Loop Drop", style = MaterialTheme.typography.titleLarge)
                            Text("One free reveal each day. No coins or payment required.", style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = {
                                    dropResult = RewardWallet.claimDailyDrop()
                                    dropAvailable = false
                                },
                                enabled = dropAvailable,
                                modifier = Modifier.padding(top = 10.dp)
                            ) { Text(if (dropAvailable) "Reveal today’s gift" else if (dropResult > 0) "+$dropResult Loop Coins" else "Come back tomorrow") }
                        }
                    }
                }
                item {
                    Text("Loop Store", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    Text("Clear prices, permanent unlocks and no randomized purchases.", style = MaterialTheme.typography.bodyMedium)
                }
                items(
                    listOf(
                        Triple("title_steady", "Steady Builder title", 120),
                        Triple("title_comeback", "Comeback Champion title", 180),
                        Triple("title_focus", "Focus Gardener title", 220)
                    )
                ) { item ->
                    val owned = item.first in unlocked
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🎁", style = MaterialTheme.typography.headlineSmall)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.second, style = MaterialTheme.typography.titleMedium)
                                Text(if (owned) "Permanent unlock" else "${item.third} Loop Coins", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    if (owned) RewardWallet.equipTitle(item.second.removeSuffix(" title"))
                                    else if (RewardWallet.purchase(item.first, item.third)) RewardWallet.equipTitle(item.second.removeSuffix(" title"))
                                },
                                enabled = owned || coinBalance >= item.third
                            ) { Text(if (owned) "Equip" else "Unlock") }
                        }
                    }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AcUnit, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("One ad = one freeze", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "A freeze automatically protects a streak when exactly one day is missed.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                items(habits) { habit ->
                    PerkCard(
                        habit = habit,
                        adState = adState,
                        onWatchAd = { onWatchAdForFreeze(habit.id) },
                        onRedeem = { onRedeemCoins(habit.id) },
                        canRedeem = coinBalance >= RewardWallet.FREEZE_COST,
                        onRetryAd = onRetryAd
                    )
                }
            }
        }
    }
}

@Composable
private fun PerkCard(
    habit: Habit,
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onRedeem: () -> Unit,
    canRedeem: Boolean,
    onRetryAd: () -> Unit
) {
    val template = HabitTemplates.byId(habit.templateId)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(template.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(template.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${habit.freezeTokensAvailable} freeze token${if (habit.freezeTokensAvailable == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onRedeem, enabled = canRedeem) { Text("100 🪙") }
                Button(
                    onClick = if (adState == RewardedAdState.Unavailable) onRetryAd else onWatchAd,
                    enabled = adState == RewardedAdState.Ready || adState == RewardedAdState.Unavailable
                ) {
                    Icon(if (adState == RewardedAdState.Unavailable) Icons.Filled.Refresh else Icons.Filled.PlayCircle, null)
                    Text(if (adState == RewardedAdState.Unavailable) "Retry" else "Ad", Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
