package com.habitloop.app.ui

sealed class NavRoutes(val route: String) {
    data object Onboarding : NavRoutes("onboarding")
    data object Today : NavRoutes("today")
    data object Habits : NavRoutes("habits")
    data object Insights : NavRoutes("insights")
    data object Profile : NavRoutes("profile")
    data object Community : NavRoutes("community")
    data object Perks : NavRoutes("perks")
    data object Settings : NavRoutes("settings")
    data object AccountSecurity : NavRoutes("account_security")
    data object AppGuide : NavRoutes("app_guide")
    data object Auth : NavRoutes("auth")
    data object EditProfile : NavRoutes("edit_profile")
    data object GrowthLab : NavRoutes("growth_lab")
    data object Notifications : NavRoutes("notifications")
    data object Planner : NavRoutes("planner")
    data object AddPlannerTask : NavRoutes("planner/add")
    data object EditPlannerTask : NavRoutes("planner/edit/{taskId}") {
        fun buildRoute(taskId: Long) = "planner/edit/$taskId"
    }
    data object CircleDetail : NavRoutes("circle/{circleId}") {
        fun buildRoute(circleId: String) = "circle/$circleId"
    }
    data object CircleDiscussion : NavRoutes("circle/{circleId}/discussion") {
        fun buildRoute(circleId: String) = "circle/$circleId/discussion"
    }
    data object CircleCheckIn : NavRoutes("circle/{circleId}/checkin") {
        fun buildRoute(circleId: String) = "circle/$circleId/checkin"
    }
    data object CircleMembers : NavRoutes("circle/{circleId}/members") {
        fun buildRoute(circleId: String) = "circle/$circleId/members"
    }
    data object CircleBoard : NavRoutes("circle/{circleId}/board") {
        fun buildRoute(circleId: String) = "circle/$circleId/board"
    }
    data object CreateCircle : NavRoutes("create_circle")
    data object HabitDetail : NavRoutes("habit/{habitId}") {
        fun buildRoute(habitId: Long) = "habit/$habitId"
    }
}

/** Routes that show the persistent bottom nav bar. */
val BOTTOM_NAV_ROUTES = setOf(
    NavRoutes.Today.route,
    NavRoutes.Habits.route,
    NavRoutes.Insights.route,
    NavRoutes.Community.route,
    NavRoutes.Profile.route
)
