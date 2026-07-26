package com.habitloop.app.data

import android.content.Context

object OnboardingPrefs {
    private const val PREFS_NAME = "habitloop_prefs"
    private const val KEY_ONBOARDED = "has_onboarded"

    fun hasOnboarded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDED, false)

    fun markOnboarded(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDED, true)
            .apply()
    }
}
