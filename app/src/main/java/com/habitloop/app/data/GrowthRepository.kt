package com.habitloop.app.data

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class GrowthProgress(val sessions: Int, val skillPoints: Int, val lastSkill: String?)

object GrowthRepository {
    private const val PREFS = "growth_progress"
    private const val SESSIONS = "sessions"
    private const val POINTS = "points"
    private const val LAST_SKILL = "last_skill"
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun progress(context: Context): GrowthProgress {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GrowthProgress(
            sessions = prefs.getInt(SESSIONS, 0),
            skillPoints = prefs.getInt(POINTS, 0),
            lastSkill = prefs.getString(LAST_SKILL, null)
        )
    }

    fun complete(context: Context, skill: String, score: Int, durationSeconds: Int, reflection: String? = null) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val points = score.coerceIn(1, 100)
        prefs.edit()
            .putInt(SESSIONS, prefs.getInt(SESSIONS, 0) + 1)
            .putInt(POINTS, prefs.getInt(POINTS, 0) + points)
            .putString(LAST_SKILL, skill)
            .apply()

        val uid = FirebaseSync.uidOrNull ?: return
        val record = mutableMapOf<String, Any>(
            "skill" to skill,
            "score" to points,
            "durationSeconds" to durationSeconds.coerceAtLeast(0),
            "completedAt" to FieldValue.serverTimestamp()
        )
        reflection?.trim()?.takeIf { it.isNotBlank() }?.let { record["reflection"] = it.take(300) }
        db.collection("users").document(uid).collection("growthSessions").add(record)
    }
}
