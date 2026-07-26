package com.habitloop.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CorrelationInsight(val habitAId: Long, val habitBId: Long, val liftPercent: Int)

/**
 * Pure computation over completion history — no server, no ML API, just
 * honest stats from data already sitting in Room. This is what makes a
 * habit tracker feel smart instead of being a glorified counter.
 */
object HabitInsights {

    /** Most common hour-of-day this habit gets completed, if there's enough history to say anything meaningful. */
    fun bestTimeOfDay(completions: List<HabitCompletion>): Int? {
        if (completions.size < 3) return null
        return completions
            .map { Instant.ofEpochMilli(it.completedAtEpochMillis).atZone(ZoneId.systemDefault()).hour }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    /**
     * Counts "comebacks": a gap of one or more missed days (not covered by a
     * freeze token) followed by at least 3 consecutive days of completion.
     * Framed positively on purpose — most habit trackers only show streak
     * breaks as failures; this rewards the resilience of coming back instead.
     */
    fun comebackCount(completions: List<HabitCompletion>): Int {
        if (completions.size < 4) return 0
        val sortedDays = completions.map { it.epochDay }.distinct().sorted()

        var comebacks = 0
        for (i in 1 until sortedDays.size) {
            val gap = sortedDays[i] - sortedDays[i - 1]
            if (gap > 1) {
                // a real gap ended the previous run — check if the new run reaches 3+ days
                val newRunLength = countConsecutiveFrom(sortedDays, i)
                if (newRunLength >= 3) comebacks++
            }
        }
        return comebacks
    }

    private fun countConsecutiveFrom(sortedDays: List<Long>, startIndex: Int): Int {
        var length = 1
        var i = startIndex
        while (i + 1 < sortedDays.size && sortedDays[i + 1] - sortedDays[i] == 1L) {
            length++
            i++
        }
        return length
    }

    /**
     * A single blended 0-100 number across all habits — each habit's current
     * streak scored against a 30-day "solid habit" baseline, then averaged.
     * Deliberately not just "average streak length" (a single 90-day streak
     * would drown out three struggling habits); capping each habit's
     * contribution at 30 days means someone with many habits going okay
     * scores similarly to someone with one habit going great, which is the
     * honest comparison this is meant to give people something to improve.
     */
    fun momentumScore(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        val perHabitScore = habits.map { minOf(it.currentStreak, 30) / 30.0 }
        return (perHabitScore.average() * 100).toInt()
    }

    /**
     * Finds the strongest "you complete A more often on days you also do B"
     * relationship across all habit pairs. Deliberately conservative: skips
     * any pair sharing fewer than 14 overlapping days or where B has fewer
     * than 5 completions in that window — better to show nothing than a
     * correlation built on 3 data points that reads as statistically
     * confident but isn't. Only surfaces lifts of 20%+ so the claim is worth
     * saying out loud.
     */
    fun bestCorrelation(habits: List<Habit>, allCompletions: List<HabitCompletion>): CorrelationInsight? {
        if (habits.size < 2) return null
        val daysByHabit: Map<Long, Set<Long>> = allCompletions
            .groupBy { it.habitId }
            .mapValues { (_, list) -> list.map { it.epochDay }.toSet() }

        val today = LocalDate.now().toEpochDay()
        var best: CorrelationInsight? = null

        for (a in habits) {
            for (b in habits) {
                if (a.id == b.id) continue
                val rangeStart = maxOf(a.createdAtEpochDay, b.createdAtEpochDay)
                val totalDays = today - rangeStart + 1
                if (totalDays < 14) continue

                val daysA = daysByHabit[a.id] ?: emptySet()
                val daysB = daysByHabit[b.id] ?: emptySet()
                val daysBInRange = daysB.filter { it in rangeStart..today }
                if (daysBInRange.size < 5) continue

                val baselineRate = daysA.count { it in rangeStart..today }.toDouble() / totalDays
                if (baselineRate <= 0.0) continue

                val conditionalRate = daysBInRange.count { it in daysA }.toDouble() / daysBInRange.size
                val liftPercent = (((conditionalRate - baselineRate) / baselineRate) * 100).toInt()

                val current = best
                if (liftPercent >= 20 && (current == null || liftPercent > current.liftPercent)) {
                    best = CorrelationInsight(habitAId = a.id, habitBId = b.id, liftPercent = liftPercent)
                }
            }
        }
        return best
    }

    fun formatHour(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:00 $period"
    }
}
