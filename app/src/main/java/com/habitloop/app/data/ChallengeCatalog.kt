package com.habitloop.app.data

import android.content.Context

data class HabitChallenge(
    val id: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val habitName: String,
    val templateId: String,
    val scheduleDaysCsv: String,
    val motivation: String
)

object ChallengeCatalog {
    val all = listOf(
        HabitChallenge(
            "walk_21", "21-day walking reset",
            "Build a dependable walking rhythm without chasing step-count perfection.",
            21, "Take a purposeful walk", "gym", "1,2,3,4,5,6,7",
            "Create daily movement that feels sustainable."
        ),
        HabitChallenge(
            "read_14", "Two weeks of reading",
            "Replace a small pocket of scrolling with focused reading.",
            14, "Read for 15 minutes", "reading", "1,2,3,4,5,6,7",
            "Protect a quiet part of the day for learning."
        ),
        HabitChallenge(
            "focus_10", "10 focused workdays",
            "Start weekdays with one distraction-free block before reactive work.",
            14, "Complete one focus block", "coding", "1,2,3,4,5",
            "Make progress on important work before urgent work takes over."
        ),
        HabitChallenge(
            "reset_7", "Seven-day evening reset",
            "Create a short closing ritual so tomorrow starts with less friction.",
            7, "Do my evening reset", "meditation", "1,2,3,4,5,6,7",
            "End the day intentionally and make tomorrow easier."
        )
    )
}

object ChallengePrefs {
    private const val PREFS = "habitloop_challenges"
    fun joined(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("joined", emptySet())
            ?.toSet()
            ?: emptySet()

    fun markJoined(context: Context, id: String) {
        val updated = joined(context) + id
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet("joined", updated).apply()
    }
}
