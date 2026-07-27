package com.habitloop.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.google.android.gms.ads.MobileAds
import com.habitloop.app.ads.RewardedAdManager
import com.habitloop.app.ads.RewardedAdState
import com.habitloop.app.data.OnboardingPrefs
import com.habitloop.app.data.UserPrefs
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.share.StreakCardGenerator
import com.habitloop.app.ui.BOTTOM_NAV_ROUTES
import com.habitloop.app.ui.HabitDetailScreen
import com.habitloop.app.ui.HabitViewModel
import com.habitloop.app.ui.HabitsScreen
import com.habitloop.app.ui.InsightsScreen
import com.habitloop.app.ui.ProfileScreen
import com.habitloop.app.ui.AccountSecurityScreen
import com.habitloop.app.ui.CommunityScreen
import com.habitloop.app.ui.AppGuideScreen
import com.habitloop.app.ui.AuthScreen
import com.habitloop.app.ui.EditProfileScreen
import com.habitloop.app.ui.NavRoutes
import com.habitloop.app.ui.OnboardingFlow
import com.habitloop.app.ui.PerksScreen
import com.habitloop.app.ui.SettingsScreen
import com.habitloop.app.ui.GrowthLabScreen
import com.habitloop.app.ui.TodayScreen
import com.habitloop.app.ui.LaunchScreen
import com.habitloop.app.ui.NotificationInboxScreen
import com.habitloop.app.ui.CommunityProfileScreen
import com.habitloop.app.ui.CircleFeatureScreen
import com.habitloop.app.ui.CircleDiscussionContent
import com.habitloop.app.ui.CircleCheckInContent
import com.habitloop.app.ui.CircleMembersContent
import com.habitloop.app.ui.CircleBoardContent
import com.habitloop.app.ui.CreateCircleScreen
import com.habitloop.app.ui.PlannerScreen
import com.habitloop.app.ui.AddPlannerTaskScreen
import com.habitloop.app.ui.theme.HabitLoopTheme
import com.habitloop.app.audio.ThemeMusicController
import com.habitloop.app.audio.ThemeMusicPrefs
import com.habitloop.app.data.UsageTracker
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var notificationCategory by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationCategory = intent.getStringExtra("notification_category")
    }
    override fun onStart() {
        super.onStart()
        UsageTracker.recordOpen(this)
        if (ThemeMusicPrefs.enabled(this)) ThemeMusicController.play(this)
    }

    override fun onStop() {
        ThemeMusicController.pause()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        notificationCategory = intent?.getStringExtra("notification_category")
        val repository = (application as HabitLoopApp).repository

        MobileAds.initialize(this)
        RewardedAdManager.preload(this)

        setContent {
            HabitLoopTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel = HabitViewModel(repository)
                    val navController = rememberNavController()
                    var showLaunch by rememberSaveable { mutableStateOf(true) }
                    val startDestination =
                        if (OnboardingPrefs.shouldShowOnboarding(this)) {
                            NavRoutes.Onboarding.route
                        } else {
                            NavRoutes.Today.route
                        }

                    LaunchedEffect(Unit) {
                        delay(1500)
                        showLaunch = false
                    }

                    if (showLaunch) {
                        LaunchScreen()
                    } else {
                        AppRoot(
                            navController = navController,
                            startDestination = startDestination,
                            viewModel = viewModel,
                            activity = this,
                            notificationCategory = notificationCategory,
                            onNotificationConsumed = { notificationCategory = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    navController: NavHostController,
    startDestination: String,
    viewModel: HabitViewModel,
    activity: MainActivity,
    notificationCategory: String?,
    onNotificationConsumed: () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in BOTTOM_NAV_ROUTES
    val rewardedAdState by RewardedAdManager.state.collectAsStateWithLifecycle()
    LaunchedEffect(notificationCategory) {
        if (notificationCategory != null && !OnboardingPrefs.shouldShowOnboarding(activity)) {
            navController.navigate(
                when (notificationCategory) {
                    "circle_message" -> NavRoutes.Community.route
                    "jam" -> NavRoutes.GrowthLab.route
                    "planner" -> NavRoutes.Planner.route
                    else -> NavRoutes.Today.route
                }
            ) { launchSingleTop = true }
            onNotificationConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) HabitLoopBottomNav(navController, currentRoute)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavRoutes.Onboarding.route) {
                OnboardingFlow(
                    onFinished = { name, habit ->
                        if (name != null) {
                            UserPrefs.setName(activity, name)
                            FirebaseSync.pushProfile(name)
                        }
                        viewModel.addHabit(
                            name = habit.name,
                            templateId = habit.templateId,
                            scheduleDaysCsv = habit.scheduleDaysCsv,
                            motivation = habit.motivation
                        )
                        OnboardingPrefs.markOnboarded(activity)
                        navController.navigate(NavRoutes.Today.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSignIn = {
                        navController.navigate(NavRoutes.Auth.route) { launchSingleTop = true }
                    }
                )
            }

            composable(NavRoutes.Today.route) {
                TodayScreen(
                    viewModel = viewModel,
                    onOpenHabit = { habitId -> navController.navigate(NavRoutes.HabitDetail.buildRoute(habitId)) },
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) },
                    onOpenNotifications = { navController.navigate(NavRoutes.Notifications.route) },
                    onOpenGrowthLab = { navController.navigate(NavRoutes.GrowthLab.route) },
                    onOpenCommunity = { navController.navigate(NavRoutes.Community.route) },
                    onOpenPlanner = { navController.navigate(NavRoutes.Planner.route) }
                )
            }

            composable(NavRoutes.Planner.route) {
                PlannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddTask = { navController.navigate(NavRoutes.AddPlannerTask.route) }
                )
            }

            composable(NavRoutes.AddPlannerTask.route) {
                AddPlannerTaskScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAccount = { navController.navigate(NavRoutes.AccountSecurity.route) },
                    onOpenGuide = { navController.navigate(NavRoutes.AppGuide.route) },
                    onEditProfile = { navController.navigate(NavRoutes.EditProfile.route) },
                    onOpenRewards = { navController.navigate(NavRoutes.Perks.route) },
                    onOpenNotifications = { navController.navigate(NavRoutes.Notifications.route) }
                )
            }

            composable(NavRoutes.Habits.route) {
                HabitsScreen(
                    viewModel = viewModel,
                    onOpenHabit = { habitId -> navController.navigate(NavRoutes.HabitDetail.buildRoute(habitId)) }
                )
            }

            composable(NavRoutes.Perks.route) {
                val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
                PerksScreen(
                    habits = habits,
                    adState = rewardedAdState,
                    onWatchAdForFreeze = { habitId ->
                        RewardedAdManager.showForFreezeToken(
                            activity = activity,
                            onEarned = {
                                viewModel.grantFreezeToken(habitId)
                                val habitName = habits.firstOrNull { it.id == habitId }?.name ?: "your habit"
                                android.widget.Toast.makeText(
                                    activity,
                                    "Freeze earned for $habitName",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            },
                            onUnavailable = {
                                android.widget.Toast.makeText(
                                    activity,
                                    "The reward is not ready yet. Check your connection and try again.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    onRedeemCoins = { habitId ->
                        if (com.habitloop.app.data.RewardWallet.spend(com.habitloop.app.data.RewardWallet.FREEZE_COST)) {
                            viewModel.grantFreezeToken(habitId)
                            android.widget.Toast.makeText(activity, "Recovery Pass applied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRetryAd = { RewardedAdManager.preload(activity) }
                )
            }

            composable(NavRoutes.Insights.route) {
                InsightsScreen(
                    viewModel = viewModel,
                    onOpenHabit = { habitId -> navController.navigate(NavRoutes.HabitDetail.buildRoute(habitId)) }
                )
            }

            composable(NavRoutes.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onOpenPerks = { navController.navigate(NavRoutes.Perks.route) },
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) },
                    onOpenSecurity = { navController.navigate(NavRoutes.AccountSecurity.route) },
                    onOpenGuide = { navController.navigate(NavRoutes.AppGuide.route) },
                    onEditProfile = { navController.navigate(NavRoutes.EditProfile.route) },
                    onOpenGrowthLab = { navController.navigate(NavRoutes.GrowthLab.route) }
                )
            }
            composable(NavRoutes.GrowthLab.route) {
                GrowthLabScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Notifications.route) {
                NotificationInboxScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { category ->
                        navController.navigate(
                            when (category) {
                                "circle_message" -> NavRoutes.Community.route
                                "jam" -> NavRoutes.GrowthLab.route
                                "planner" -> NavRoutes.Planner.route
                                else -> NavRoutes.Today.route
                            }
                        )
                    }
                )
            }

            composable(NavRoutes.AccountSecurity.route) {
                AccountSecurityScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAuth = { navController.navigate(NavRoutes.Auth.route) },
                    onSignedOut = {
                        OnboardingPrefs.markExplicitlySignedOut(activity)
                        // Rebuild the navigation graph from the durable signed-out
                        // state so Back or restored state cannot reopen private screens.
                        activity.recreate()
                    }
                )
            }

            composable(NavRoutes.Community.route) {
                CommunityScreen(
                    viewModel,
                    onOpenCircle = { navController.navigate(NavRoutes.CircleDetail.buildRoute(it)) },
                    onCreateCircle = { navController.navigate(NavRoutes.CreateCircle.route) }
                )
            }

            composable(
                route = NavRoutes.CircleDetail.route,
                deepLinks = listOf(navDeepLink { uriPattern = "habitloop://circle/{circleId}" })
            ) { entry ->
                val circleId = entry.arguments?.getString("circleId").orEmpty()
                CommunityProfileScreen(
                    circleId = circleId,
                    onBack = { navController.popBackStack() },
                    onDiscussion = { navController.navigate(NavRoutes.CircleDiscussion.buildRoute(circleId)) },
                    onCheckIn = { navController.navigate(NavRoutes.CircleCheckIn.buildRoute(circleId)) },
                    onMembers = { navController.navigate(NavRoutes.CircleMembers.buildRoute(circleId)) },
                    onBoard = { navController.navigate(NavRoutes.CircleBoard.buildRoute(circleId)) }
                )
            }
            composable(NavRoutes.CircleDiscussion.route) { entry ->
                CircleFeatureScreen(entry.arguments?.getString("circleId").orEmpty(), "Discussion", { navController.popBackStack() }) {
                    id, username, onError -> CircleDiscussionContent(id, username, onError)
                }
            }
            composable(NavRoutes.CircleCheckIn.route) { entry ->
                CircleFeatureScreen(entry.arguments?.getString("circleId").orEmpty(), "Daily check-in", { navController.popBackStack() }) {
                    id, username, onError -> CircleCheckInContent(id, username, onError)
                }
            }
            composable(NavRoutes.CircleMembers.route) { entry ->
                CircleFeatureScreen(entry.arguments?.getString("circleId").orEmpty(), "Members", { navController.popBackStack() }) {
                    id, _, onError -> CircleMembersContent(id, onError)
                }
            }
            composable(NavRoutes.CircleBoard.route) { entry ->
                CircleFeatureScreen(entry.arguments?.getString("circleId").orEmpty(), "Weekly board", { navController.popBackStack() }) {
                    id, _, onError -> CircleBoardContent(id, onError)
                }
            }

            composable(NavRoutes.CreateCircle.route) {
                CreateCircleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onCreated = { circleId ->
                        navController.navigate(NavRoutes.CircleDetail.buildRoute(circleId)) {
                            popUpTo(NavRoutes.CreateCircle.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.AppGuide.route) {
                AppGuideScreen(onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.Auth.route) {
                AuthScreen(
                    onBack = { navController.popBackStack() },
                    onAuthenticated = {
                        OnboardingPrefs.markOnboarded(activity)
                        navController.navigate(NavRoutes.Today.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.HabitDetail.route) { backStackEntry ->
                val habits by viewModel.habits.collectAsStateWithLifecycle(initialValue = emptyList())
                val habit = habits.firstOrNull { it.id.toString() == habitIdArg(backStackEntry) }
                if (habit != null) {
                    HabitDetailScreen(
                        habit = habit,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onShare = { StreakCardGenerator.shareStreak(activity, habit) },
                        onComplete = { viewModel.completeToday(habit.id) },
                        onWatchAdForFreeze = {
                            RewardedAdManager.showForFreezeToken(
                                activity = activity,
                                onEarned = {
                                    viewModel.grantFreezeToken(habit.id)
                                    android.widget.Toast.makeText(
                                        activity,
                                        "Freeze earned for ${habit.name}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                },
                                onUnavailable = {
                                    android.widget.Toast.makeText(
                                        activity,
                                        "The reward is not ready yet. Try again shortly.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitLoopBottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).clip(RoundedCornerShape(30.dp)),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Today.route,
            onClick = { navController.navigateSingleTop(NavRoutes.Today.route) },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = "Today") },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Habits.route,
            onClick = { navController.navigateSingleTop(NavRoutes.Habits.route) },
            icon = { Icon(Icons.Filled.List, contentDescription = "Habits") },
            label = { Text("Habits") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Insights.route,
            onClick = { navController.navigateSingleTop(NavRoutes.Insights.route) },
            icon = { Icon(Icons.Filled.Insights, contentDescription = "Insights") },
            label = { Text("Trends") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Community.route,
            onClick = { navController.navigateSingleTop(NavRoutes.Community.route) },
            icon = { Icon(Icons.Filled.Groups, contentDescription = "Challenges") },
            label = { Text("Together") }
        )
        NavigationBarItem(
            selected = currentRoute == NavRoutes.Profile.route,
            onClick = { navController.navigateSingleTop(NavRoutes.Profile.route) },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("You") }
        )
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun habitIdArg(backStackEntry: NavBackStackEntry): String? =
    backStackEntry.arguments?.getString("habitId")
