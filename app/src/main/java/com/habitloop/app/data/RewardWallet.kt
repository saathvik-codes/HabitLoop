package com.habitloop.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RewardWallet {
    private const val PREFS = "habitloop_rewards"
    private const val BALANCE = "loop_coin_balance"
    private const val UNLOCKED = "unlocked_rewards"
    private const val ACTIVE_TITLE = "active_profile_title"
    private const val LAST_DROP_DAY = "last_loop_drop_day"
    const val FREEZE_COST = 100
    private lateinit var appContext: Context
    private val mutableBalance = MutableStateFlow(0)
    val balance = mutableBalance.asStateFlow()
    private val mutableUnlocked = MutableStateFlow<Set<String>>(emptySet())
    val unlocked = mutableUnlocked.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        mutableBalance.value = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(BALANCE, 0)
        mutableUnlocked.value = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(UNLOCKED, emptySet()).orEmpty()
    }

    fun earn(amount: Int) {
        if (amount <= 0) return
        setBalance(mutableBalance.value + amount)
    }

    fun spend(amount: Int): Boolean {
        if (amount <= 0 || mutableBalance.value < amount) return false
        setBalance(mutableBalance.value - amount)
        return true
    }

    fun purchase(itemId: String, cost: Int): Boolean {
        if (itemId in mutableUnlocked.value || !spend(cost)) return false
        mutableUnlocked.value = mutableUnlocked.value + itemId
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(UNLOCKED, mutableUnlocked.value).apply()
        return true
    }

    fun claimDailyDrop(): Int {
        val today = java.time.LocalDate.now().toEpochDay()
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(LAST_DROP_DAY, Long.MIN_VALUE) == today) return 0
        val reward = listOf(5, 10, 15, 20)[kotlin.math.abs(today.hashCode()) % 4]
        prefs.edit().putLong(LAST_DROP_DAY, today).apply()
        earn(reward)
        return reward
    }

    fun dailyDropAvailable(): Boolean =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(LAST_DROP_DAY, Long.MIN_VALUE) != java.time.LocalDate.now().toEpochDay()

    fun activeTitle(): String =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACTIVE_TITLE, "").orEmpty()

    fun equipTitle(title: String) {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ACTIVE_TITLE, title).apply()
    }

    private fun setBalance(value: Int) {
        mutableBalance.value = value.coerceAtLeast(0)
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(BALANCE, mutableBalance.value).apply()
    }
}
