package com.habitloop.app.data

import android.content.Context

/**
 * Local-only display name for the greeting header ("Good morning, Saathvik").
 * Deliberately not real auth — there's no backend to authenticate against
 * yet, so a login screen here would be theater. Real account auth (Google
 * Sign-In) is a Firebase-dependent feature, tracked in the roadmap, built
 * only once there's an actual account-linked feature that needs it.
 */
object UserPrefs {
    private const val PREFS_NAME = "habitloop_prefs"
    private const val KEY_NAME = "display_name"
    private const val KEY_AVATAR_COLOR = "avatar_color"
    private const val KEY_AVATAR_SYMBOL = "avatar_symbol"
    private const val KEY_PROFILE_PHOTO_URI = "profile_photo_uri"
    private const val KEY_INSTAGRAM = "instagram_handle"
    private const val KEY_DISCORD = "discord_handle"
    private const val KEY_SHARE_SOCIALS = "share_socials_in_jams"

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_NAME, null)

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name)
            .apply()
    }

    fun getAvatarColor(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_AVATAR_COLOR, 0xFF84A98C)

    fun setAvatarColor(context: Context, color: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_AVATAR_COLOR, color).apply()
    }

    fun getAvatarSymbol(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR_SYMBOL, "initial") ?: "initial"

    fun setAvatarSymbol(context: Context, symbol: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_AVATAR_SYMBOL, symbol).apply()
    }

    fun getProfilePhotoUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_PHOTO_URI, null)

    fun setProfilePhotoUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                if (uri == null) remove(KEY_PROFILE_PHOTO_URI) else putString(KEY_PROFILE_PHOTO_URI, uri)
            }.apply()
    }

    fun getInstagram(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_INSTAGRAM, "").orEmpty()

    fun getDiscord(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DISCORD, "").orEmpty()

    fun sharesSocialsInJams(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SHARE_SOCIALS, false)

    fun setJamSocials(context: Context, instagram: String, discord: String, share: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_INSTAGRAM, instagram.trim().removePrefix("@").take(30))
            .putString(KEY_DISCORD, discord.trim().take(40))
            .putBoolean(KEY_SHARE_SOCIALS, share)
            .apply()
    }

}
