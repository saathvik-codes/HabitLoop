package com.habitloop.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class HabitCircle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val emoji: String = "🌱",
    val cadence: String = "Daily",
    val memberCount: Long = 0,
    val checkInCount: Long = 0,
    val featured: Boolean = false,
    val ownerId: String = "",
    val leaderName: String = "HabitLoop",
    val durationDays: Int = 21,
    val habitName: String = "",
    val mission: String = "",
    val agenda: String = "",
    val meetingSchedule: String = "Flexible check-ins",
    val guidelines: String = "Be kind, stay relevant, protect privacy, and avoid medical claims.",
    val bannerStyle: String = "sage",
    val createdAt: Timestamp? = null
)

data class CircleCheckIn(
    val id: String = "",
    val circleId: String = "",
    val userId: String = "",
    val displayName: String = "Loop member",
    val message: String = "",
    val mood: String = "✅",
    val createdAt: Timestamp? = null
)

data class CircleMember(
    val userId: String = "",
    val username: String = "Loop member",
    val role: String = "member",
    val joinedAt: Timestamp? = null
)

data class CircleMessage(
    val id: String = "",
    val circleId: String = "",
    val userId: String = "",
    val username: String = "Loop member",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class CircleRank(
    val userId: String,
    val username: String,
    val activeDays: Int,
    val consistencyPercent: Int
)

data class CommunitySnapshot(
    val circles: List<HabitCircle>,
    val joinedCircleIds: Set<String>,
    val recentCheckIns: List<CircleCheckIn>
)

object CommunityRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun load(): CommunitySnapshot {
        val uid = FirebaseSync.ensureSignedIn()
        val circles = db.collection("circles").limit(50).get().await().documents.mapNotNull { doc ->
            doc.toObject(HabitCircle::class.java)?.copy(id = doc.id)
        }.sortedWith(compareByDescending<HabitCircle> { it.featured }.thenByDescending { it.createdAt?.seconds ?: 0 })
        val memberships = db.collection("users").document(uid).collection("circleMemberships").get().await()
            .documents.map { it.id }.toSet()
        val feed = if (memberships.isEmpty()) emptyList() else {
            db.collection("checkIns").whereIn("circleId", memberships.take(10)).limit(40).get().await()
                .documents.mapNotNull { doc -> doc.toObject(CircleCheckIn::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.createdAt?.seconds ?: 0 }
        }
        return CommunitySnapshot(circles, memberships, feed)
    }

    suspend fun circle(circleId: String): HabitCircle? {
        val document = db.collection("circles").document(circleId).get().await()
        return document.toObject(HabitCircle::class.java)?.copy(id = document.id)
    }

    suspend fun createCircle(
        title: String,
        description: String,
        category: String,
        emoji: String,
        cadence: String,
        durationDays: Int,
        habitName: String,
        leaderName: String
    ): String {
        val uid = FirebaseSync.ensureSignedIn()
        val ref = db.collection("circles").document()
        val circle = HabitCircle(
            id = ref.id,
            title = title.trim().take(50),
            description = description.trim().take(240),
            category = category.trim().take(24),
            emoji = emoji.take(4).ifBlank { "🌱" },
            cadence = cadence.trim().take(24),
            memberCount = 1,
            ownerId = uid,
            leaderName = leaderName.trim().take(30).ifBlank { "Loop leader" },
            durationDays = durationDays.coerceIn(7, 90),
            habitName = habitName.trim().take(50),
            mission = description.trim().take(180),
            agenda = "Build consistency around ${habitName.trim().take(50)} through useful check-ins and shared encouragement.",
            createdAt = Timestamp.now()
        )
        require(circle.title.length >= 3 && circle.description.length >= 12 && circle.habitName.length >= 2) {
            "Add a clear title, description and habit."
        }
        db.runBatch { batch ->
            batch.set(ref, circle)
            batch.set(
                db.collection("users").document(uid).collection("circleMemberships").document(ref.id),
                mapOf("circleId" to ref.id, "displayName" to circle.leaderName, "role" to "leader", "joinedAt" to FieldValue.serverTimestamp())
            )
            batch.set(
                ref.collection("members").document(uid),
                mapOf("userId" to uid, "username" to circle.leaderName, "role" to "leader", "joinedAt" to FieldValue.serverTimestamp())
            )
        }.await()
        return ref.id
    }

    suspend fun join(circle: HabitCircle, displayName: String) {
        val uid = FirebaseSync.ensureSignedIn()
        val membership = db.collection("users").document(uid).collection("circleMemberships").document(circle.id)
        if (membership.get().await().exists()) {
            db.collection("circles").document(circle.id).collection("members").document(uid)
                .set(mapOf("userId" to uid, "username" to displayName.take(24), "role" to "member", "joinedAt" to FieldValue.serverTimestamp()))
                .await()
            return
        }
        db.runBatch { batch ->
            batch.set(membership, mapOf("circleId" to circle.id, "displayName" to displayName, "role" to "member", "joinedAt" to FieldValue.serverTimestamp()))
            batch.set(
                db.collection("circles").document(circle.id).collection("members").document(uid),
                mapOf("userId" to uid, "username" to displayName.take(24), "role" to "member", "joinedAt" to FieldValue.serverTimestamp())
            )
            batch.update(db.collection("circles").document(circle.id), "memberCount", FieldValue.increment(1))
        }.await()
    }

    suspend fun updateCircleProfile(
        circleId: String,
        mission: String,
        agenda: String,
        meetingSchedule: String,
        guidelines: String,
        bannerStyle: String
    ) {
        val uid = FirebaseSync.ensureSignedIn()
        val ref = db.collection("circles").document(circleId)
        val existing = ref.get().await().toObject(HabitCircle::class.java)
            ?: error("Community profile was not found.")
        require(existing.ownerId == uid) { "Only the community leader can edit this profile." }
        require(mission.trim().length >= 12 && agenda.trim().length >= 12) {
            "Add a clear mission and agenda."
        }
        ref.update(
            mapOf(
                "mission" to mission.trim().take(300),
                "agenda" to agenda.trim().take(500),
                "meetingSchedule" to meetingSchedule.trim().take(100),
                "guidelines" to guidelines.trim().take(500),
                "bannerStyle" to bannerStyle.takeIf { it in setOf("sage", "sunrise", "ocean", "violet") }.orEmpty().ifBlank { "sage" }
            )
        ).await()
    }

    suspend fun members(circleId: String): List<CircleMember> =
        db.collection("circles").document(circleId).collection("members").limit(100).get().await()
            .documents.mapNotNull { it.toObject(CircleMember::class.java) }
            .sortedWith(compareBy<CircleMember> { it.role != "leader" }.thenBy { it.username.lowercase() })

    suspend fun checkIn(circleId: String, displayName: String, message: String, mood: String) {
        val uid = FirebaseSync.ensureSignedIn()
        val membership = db.collection("users").document(uid).collection("circleMemberships").document(circleId).get().await()
        require(membership.exists()) { "Join the circle before checking in." }
        db.runBatch { batch ->
            batch.set(db.collection("checkIns").document(), mapOf(
                "circleId" to circleId, "userId" to uid,
                "displayName" to displayName.take(30).ifBlank { "Loop member" },
                "message" to message.trim().take(180), "mood" to mood,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            batch.update(db.collection("circles").document(circleId), "checkInCount", FieldValue.increment(1))
        }.await()
    }

    suspend fun messages(circleId: String): List<CircleMessage> {
        val uid = FirebaseSync.ensureSignedIn()
        require(db.collection("users").document(uid).collection("circleMemberships").document(circleId).get().await().exists()) {
            "Join the circle to open its discussion."
        }
        return db.collection("circles").document(circleId).collection("messages")
            .orderBy("createdAt").limitToLast(100).get().await().documents.mapNotNull {
                it.toObject(CircleMessage::class.java)?.copy(id = it.id)
            }
    }

    fun observeMessages(circleId: String): Flow<List<CircleMessage>> = callbackFlow {
        val registration = db.collection("circles").document(circleId).collection("messages")
            .orderBy("createdAt").limitToLast(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull {
                    it.toObject(CircleMessage::class.java)?.copy(id = it.id)
                })
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(circleId: String, username: String, text: String) {
        val uid = FirebaseSync.ensureSignedIn()
        val clean = text.trim().replace(Regex("\\s+"), " ").take(500)
        require(clean.length in 1..500) { "Write a message before sending." }
        require(db.collection("users").document(uid).collection("circleMemberships").document(circleId).get().await().exists()) {
            "Join the circle before posting."
        }
        db.collection("circles").document(circleId).collection("messages").add(
            mapOf(
                "circleId" to circleId,
                "userId" to uid,
                "username" to username.substringBefore("@").trim().take(24).ifBlank { "Loop member" },
                "text" to clean,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun reportMessage(circleId: String, message: CircleMessage, reason: String) {
        val uid = FirebaseSync.ensureSignedIn()
        db.collection("reports").add(
            mapOf(
                "reporterId" to uid,
                "circleId" to circleId,
                "messageId" to message.id,
                "authorId" to message.userId,
                "reason" to reason.take(60),
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun weeklyLeaderboard(circleId: String): List<CircleRank> {
        val uid = FirebaseSync.ensureSignedIn()
        require(db.collection("users").document(uid).collection("circleMemberships").document(circleId).get().await().exists()) {
            "Join the circle to view its weekly board."
        }
        val cutoffSeconds = Timestamp.now().seconds - 7 * 24 * 60 * 60
        val recent = db.collection("checkIns").whereEqualTo("circleId", circleId).limit(250).get().await()
            .documents.mapNotNull { it.toObject(CircleCheckIn::class.java) }
            .filter { (it.createdAt?.seconds ?: 0) >= cutoffSeconds }
        return recent.groupBy { it.userId }.map { (userId, entries) ->
            val days = entries.mapNotNull { item ->
                item.createdAt?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
            }.distinct().size.coerceAtMost(7)
            CircleRank(userId, entries.first().displayName.substringBefore("@").take(24), days, days * 100 / 7)
        }.sortedWith(compareByDescending<CircleRank> { it.activeDays }.thenBy { it.username.lowercase() })
    }

    fun readableError(error: Throwable): String = when {
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Community access is not enabled in Firebase yet. Deploy the included Firestore rules."
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Firebase is temporarily unavailable. Check your connection and retry."
        error.message?.contains("network", ignoreCase = true) == true ->
            "No connection. Your private habits remain available offline."
        else -> error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Community could not refresh."
    }
}
