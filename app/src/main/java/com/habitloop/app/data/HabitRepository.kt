package com.habitloop.app.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Owns the streak math. Kept out of the DAO/UI layers so the "what counts as
 * keeping a streak alive" rule lives in exactly one place.
 *
 * Room is the source of truth for the UI — every read here is local and
 * instant. Firestore pushes (via [FirebaseSync]) are fire-and-forget
 * write-throughs for cloud backup; the UI never waits on them, and if
 * they're offline they just silently retry later via Firestore's own
 * offline persistence.
 */
class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<Habit>> = dao.observeHabits()

    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>> =
        dao.observeCompletions(habitId)

    suspend fun createHabit(
        name: String,
        templateId: String,
        scheduleDaysCsv: String = "1,2,3,4,5,6,7",
        motivation: String = ""
    ): Long {
        val id = dao.insertHabit(
            Habit(
                name = name,
                templateId = templateId,
                scheduleDaysCsv = scheduleDaysCsv,
                motivation = motivation,
                createdAtEpochDay = LocalDate.now().toEpochDay()
            )
        )
        pushHabitById(id)
        return id
    }

    /**
     * Marks today complete for [habitId]. If yesterday was missed, spends a
     * freeze token to keep the streak alive instead of resetting it to zero —
     * the "grace day" mechanic: loss-aversion without the guilt-trip.
     */
    suspend fun completeToday(habitId: Long): CompletionResult {
        val habit = dao.getHabit(habitId) ?: return CompletionResult.HabitNotFound
        val today = LocalDate.now().toEpochDay()

        if (dao.hasCompletionOn(habitId, today) > 0) {
            return CompletionResult.AlreadyCompletedToday
        }

        val previousScheduledDay = generateSequence(LocalDate.now().minusDays(1)) { it.minusDays(1) }
            .first { habit.isScheduledOn(it) }
            .toEpochDay()
        val scheduledDayBeforeThat = generateSequence(
            LocalDate.ofEpochDay(previousScheduledDay).minusDays(1)
        ) { it.minusDays(1) }
            .first { habit.isScheduledOn(it) }
            .toEpochDay()
        val usedFreeze: Boolean
        val newStreak: Int = when {
            habit.lastCompletedEpochDay == null -> {
                usedFreeze = false
                1
            }
            habit.lastCompletedEpochDay == previousScheduledDay -> {
                usedFreeze = false
                habit.currentStreak + 1
            }
            habit.lastCompletedEpochDay == scheduledDayBeforeThat && habit.freezeTokensAvailable > 0 -> {
                // exactly one day was missed — spend a grace token, keep the streak
                usedFreeze = true
                habit.currentStreak + 1
            }
            else -> {
                usedFreeze = false
                1
            }
        }

        val completion = HabitCompletion(
            habitId = habitId,
            epochDay = today,
            completedAtEpochMillis = System.currentTimeMillis(),
            usedFreezeToken = usedFreeze
        )
        dao.insertCompletion(completion)
        FirebaseSync.uidOrNull?.let { FirebaseSync.pushCompletion(it, completion) }

        val updatedHabit = habit.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, habit.longestStreak),
            lastCompletedEpochDay = today,
            freezeTokensAvailable = if (usedFreeze) habit.freezeTokensAvailable - 1 else habit.freezeTokensAvailable
        )
        dao.updateHabit(updatedHabit)
        FirebaseSync.uidOrNull?.let { FirebaseSync.pushHabit(it, updatedHabit) }
        // Ten verified check-ins earn one Recovery Pass. The wallet stores
        // tenths internally for backward-compatible migration from Loop Coins.
        RewardWallet.earn(10)

        return if (usedFreeze) CompletionResult.CompletedWithFreeze(newStreak) else CompletionResult.Completed(newStreak)
    }

    /** Grants one freeze token, earned by watching a rewarded ad. */
    suspend fun grantFreezeToken(habitId: Long) {
        val habit = dao.getHabit(habitId) ?: return
        val updated = habit.copy(freezeTokensAvailable = habit.freezeTokensAvailable + 1)
        dao.updateHabit(updated)
        FirebaseSync.uidOrNull?.let { FirebaseSync.pushHabit(it, updated) }
    }

    suspend fun updateSchedule(habitId: Long, scheduleDaysCsv: String) {
        val habit = dao.getHabit(habitId) ?: return
        val days = scheduleDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.distinct().sorted()
        require(days.isNotEmpty()) { "Choose at least one day." }
        val updated = habit.copy(scheduleDaysCsv = days.joinToString(","))
        dao.updateHabit(updated)
        FirebaseSync.uidOrNull?.let { FirebaseSync.pushHabit(it, updated) }
    }

    /**
     * Restores habits + their completions from Firestore if the local DB is
     * empty — the actual value cloud sync provides: reinstall the app, sign
     * back in anonymously (same device = same anonymous UID unless app data
     * was fully wiped), get your streaks back instead of starting at zero.
     */
    suspend fun restoreFromCloudIfEmpty(uid: String) {
        if (observeHabits().first().isNotEmpty()) return
        val cloudHabits = FirebaseSync.pullAllHabits(uid)
        for (habit in cloudHabits) {
            dao.insertHabit(habit)
            val completions = FirebaseSync.pullCompletions(uid, habit.id)
            completions.forEach { dao.insertCompletion(it) }
        }
    }

    private suspend fun pushHabitById(id: Long) {
        val habit = dao.getHabit(id) ?: return
        FirebaseSync.uidOrNull?.let { FirebaseSync.pushHabit(it, habit) }
    }

    /** All completions across every habit within [startEpochDay, endEpochDay] — used for the monthly recap. */
    suspend fun completionsInRange(startEpochDay: Long, endEpochDay: Long): List<HabitCompletion> =
        dao.getCompletionsInRange(startEpochDay, endEpochDay)

    /** All completions across every habit, full history — used for cross-habit correlation. */
    suspend fun allCompletions(): List<HabitCompletion> = dao.getAllCompletions()
}

sealed class CompletionResult {
    data object HabitNotFound : CompletionResult()
    data object AlreadyCompletedToday : CompletionResult()
    data class Completed(val newStreak: Int) : CompletionResult()
    data class CompletedWithFreeze(val newStreak: Int) : CompletionResult()
}
