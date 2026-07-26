package com.habitloop.app.data

import android.content.Context

object OnboardingPrefs {
    private const val PREFS_NAME = "habitloop_prefs"
    private const val KEY_ONBOARDED = "has_onboarded"
    private const val KEY_EXPLICITLY_SIGNED_OUT = "explicitly_signed_out"

    fun hasOnboarded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDED, false)

    fun shouldShowOnboarding(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_EXPLICITLY_SIGNED_OUT, false) ||
            !preferences.getBoolean(KEY_ONBOARDED, false)
    }

    fun isExplicitlySignedOut(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXPLICITLY_SIGNED_OUT, false)

    fun markOnboarded(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDED, true)
            .putBoolean(KEY_EXPLICITLY_SIGNED_OUT, false)
            .commit()
    }

    /**
     * This must be a synchronous write. A user commonly signs out and
     * immediately swipes the app from Recents; apply() can be lost if Android
     * kills the process before the asynchronous disk write finishes.
     */
    fun markExplicitlySignedOut(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDED, false)
            .putBoolean(KEY_EXPLICITLY_SIGNED_OUT, true)
            .commit()
    }
}
