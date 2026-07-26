package com.habitloop.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Cloud backup/sync layer. Room stays the source of truth for the UI (fast,
 * offline-first, zero network dependency for normal use) — Firestore is a
 * write-through mirror plus a one-time restore path for reinstalls.
 *
 * Uses Firebase Anonymous Auth deliberately: it gives every install a real,
 * stable authenticated user ID without a login screen. This is not "fake"
 * auth — it's a genuine Firebase user — it's just invisible to the user,
 * which matches the IA decision to not force account creation before
 * there's a feature (this one) that actually needs an identity.
 */
object FirebaseSync {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    @Volatile var uidOrNull: String? = null
        private set

    suspend fun ensureSignedIn(): String {
        val existing = auth.currentUser
        if (existing != null) {
            uidOrNull = existing.uid
            return existing.uid
        }
        val result = auth.signInAnonymously().await()
        val uid = result.user?.uid ?: error("Anonymous sign-in returned no user")
        uidOrNull = uid
        return uid
    }

    fun signOut() {
        auth.signOut()
        uidOrNull = null
    }

    fun maskedAccountId(): String? =
        uidOrNull?.let { uid ->
            if (uid.length <= 8) uid else "${uid.take(4)}••••${uid.takeLast(4)}"
        }

    fun refreshSession() {
        uidOrNull = auth.currentUser?.uid
    }

    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous != false

    fun accountEmail(): String? = auth.currentUser?.email

    fun providerNames(): List<String> =
        auth.currentUser?.providerData
            ?.mapNotNull { it.providerId.takeIf { id -> id != "firebase" } }
            ?.distinct()
            ?: emptyList()

    fun pushHabit(uid: String, habit: Habit) {
        db.collection("users").document(uid)
            .collection("habits").document(habit.id.toString())
            .set(habit)
    }

    fun pushCompletion(uid: String, completion: HabitCompletion) {
        db.collection("users").document(uid)
            .collection("habits").document(completion.habitId.toString())
            .collection("completions").document(completion.epochDay.toString())
            .set(completion)
    }

    fun pushProfile(displayName: String, avatarStyle: String = "", avatarColor: Long? = null) {
        val uid = auth.currentUser?.uid ?: return
        val profile = mutableMapOf<String, Any>(
            "displayName" to displayName.trim().take(24),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        if (avatarStyle.isNotBlank()) profile["avatarStyle"] = avatarStyle
        avatarColor?.let { profile["avatarColor"] = it }
        db.collection("users").document(uid)
            .set(profile, com.google.firebase.firestore.SetOptions.merge())
    }

    suspend fun pullAllHabits(uid: String): List<Habit> {
        val snapshot = db.collection("users").document(uid).collection("habits").get().await()
        return snapshot.documents.mapNotNull { it.toObject(Habit::class.java) }
    }

    suspend fun pullCompletions(uid: String, habitId: Long): List<HabitCompletion> {
        val snapshot = db.collection("users").document(uid)
            .collection("habits").document(habitId.toString())
            .collection("completions").get().await()
        return snapshot.documents.mapNotNull { it.toObject(HabitCompletion::class.java) }
    }
}
