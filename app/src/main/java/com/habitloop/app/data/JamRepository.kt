package com.habitloop.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class CommunityJam(
    val id: String = "",
    val title: String = "",
    val skill: String = "Focus",
    val durationMinutes: Int = 10,
    val leaderName: String = "Loop member",
    val ownerId: String = "",
    val participantCount: Long = 1,
    val active: Boolean = true,
    val createdAt: Timestamp? = null
)

data class JamParticipant(
    val username: String = "Loop member",
    val instagram: String = "",
    val discord: String = "",
    val joinedAt: Timestamp? = null
)

object JamRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun active(): List<CommunityJam> =
        db.collection("jams").whereEqualTo("active", true).limit(25).get().await()
            .documents.mapNotNull { it.toObject(CommunityJam::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.createdAt?.seconds ?: 0 }

    suspend fun create(title: String, skill: String, minutes: Int, leaderName: String, instagram: String = "", discord: String = ""): CommunityJam {
        val uid = FirebaseSync.ensureSignedIn()
        val ref = db.collection("jams").document()
        val jam = CommunityJam(
            id = ref.id,
            title = title.trim().take(50),
            skill = skill.take(24),
            durationMinutes = minutes.coerceIn(2, 60),
            leaderName = leaderName.take(24).ifBlank { "Loop member" },
            ownerId = uid,
            createdAt = Timestamp.now()
        )
        require(jam.title.length >= 3) { "Use at least 3 characters for the Jam name." }
        db.runBatch { batch ->
            batch.set(ref, jam)
            batch.set(ref.collection("participants").document(uid), participantData(jam.leaderName, instagram, discord))
        }.await()
        return jam
    }

    suspend fun join(jam: CommunityJam, username: String, instagram: String = "", discord: String = "") {
        val uid = FirebaseSync.ensureSignedIn()
        val ref = db.collection("jams").document(jam.id)
        val participant = ref.collection("participants").document(uid)
        if (participant.get().await().exists()) return
        db.runBatch { batch ->
            batch.set(participant, participantData(username, instagram, discord))
            batch.update(ref, "participantCount", FieldValue.increment(1))
        }.await()
    }

    suspend fun participants(jamId: String): List<JamParticipant> =
        db.collection("jams").document(jamId).collection("participants").limit(100).get().await()
            .documents.mapNotNull { it.toObject(JamParticipant::class.java) }

    private fun participantData(username: String, instagram: String, discord: String): Map<String, Any> =
        mapOf(
            "username" to username.substringBefore("@").take(24),
            "instagram" to instagram.removePrefix("@").take(30),
            "discord" to discord.take(40),
            "joinedAt" to FieldValue.serverTimestamp()
        )
}
