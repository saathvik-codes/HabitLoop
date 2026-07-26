package com.habitloop.app.data

import android.content.Context

object NotificationPrefs {
    private const val PREFS = "notification_preferences"
    fun habitReminders(context: Context) = get(context, "habits", true)
    fun circleMessages(context: Context) = get(context, "circles", true)
    fun jamUpdates(context: Context) = get(context, "jams", true)
    fun streakNudges(context: Context) = get(context, "streaks", true)
    fun quietHours(context: Context) = get(context, "quiet", true)
    fun showHabitNames(context: Context) = get(context, "show_habit_names", false)
    fun playfulTone(context: Context) = get(context, "playful_tone", true)
    fun tone(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tone", "playful") ?: "playful"
    fun setTone(context: Context, value: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("tone", value.takeIf { it in setOf("gentle", "playful", "direct") } ?: "playful").apply()
    fun set(context: Context, key: String, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, enabled).apply()
    private fun get(context: Context, key: String, default: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, default)
    fun isQuietNow(): Boolean {
        val hour = java.time.LocalTime.now().hour
        return hour >= 22 || hour < 7
    }
}
