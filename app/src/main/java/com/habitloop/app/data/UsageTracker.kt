package com.habitloop.app.data

import android.content.Context
import java.time.LocalDate

object UsageTracker {
    private const val PREFS = "usage_progress"
    private const val DAYS = "active_days"

    fun recordOpen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val days = prefs.getStringSet(DAYS, emptySet()).orEmpty().toMutableSet()
        days += LocalDate.now().toString()
        prefs.edit().putStringSet(DAYS, days.sorted().takeLast(120).toSet()).apply()
    }

    fun activeDays(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(DAYS, emptySet()).orEmpty().size
}
