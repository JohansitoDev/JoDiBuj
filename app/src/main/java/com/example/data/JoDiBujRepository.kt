package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class JoDiBujRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private suspend fun requireUserId(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: "guest_${System.currentTimeMillis()}"
        } catch (_: Exception) {
            "guest_${System.currentTimeMillis()}"
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun saveUserProfile(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        avatarIcon: String,
        avatarColor: String,
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val cleanUsername = username.trim().take(24).let {
            if (it.length < 3) "Artista${uid.take(4)}" else it
        }
        val cleanDisplayName = displayName.trim().ifBlank { cleanUsername }.take(40)
        val cleanBio = bio.trim().take(120)
        val cleanEmail = email.trim().take(100)

        val userRef = db.collection("users").document(uid)
        val existingSnap = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path, auth)
            throw e
        }

        val existing = if (existingSnap.exists()) {
            existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
            null
        }

        val totalPoints = existing?.totalPoints ?: 0L
        val level = LevelSystem.levelForPoints(totalPoints).toLong()
        val gamesPlayed = existing?.gamesPlayed ?: 0L
        val wins = existing?.wins ?: 0L
        val streak = existing?.currentStreak ?: 1L

        val batch = db.batch()
        if (existing == null) {
            val createPayload = mapOf(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "email" to cleanEmail,
                "bio" to cleanBio,
                "avatarIcon" to avatarIcon,
                "avatarColor" to avatarColor,
                "totalPoints" to totalPoints,
                "level" to level,
                "gamesPlayed" to gamesPlayed,
                "wins" to wins,
                "currentStreak" to streak,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            batch.set(userRef, createPayload)
        } else {
            val updatePayload = mapOf(
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "email" to cleanEmail,
                "bio" to cleanBio,
                "avatarIcon" to avatarIcon,
                "avatarColor" to avatarColor,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            batch.update(userRef, updatePayload)
        }

        val leaderboardRef = db.collection("leaderboard").document(uid)
        val leaderboardPayload = mapOf(
            "userId" to uid,
            "username" to cleanUsername,
            "avatarIcon" to avatarIcon,
            "avatarColor" to avatarColor,
            "totalPoints" to totalPoints,
            "level" to level,
            "wins" to wins,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        batch.set(leaderboardRef, leaderboardPayload)

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path, auth)
            throw e
        }

        getUserProfile(uid) ?: UserProfile(
            id = uid,
            userId = uid,
            username = cleanUsername,
            displayName = cleanDisplayName,
            email = cleanEmail,
            bio = cleanBio,
            avatarIcon = avatarIcon,
            avatarColor = avatarColor,
            totalPoints = totalPoints,
            level = level,
            gamesPlayed = gamesPlayed,
            wins = wins,
            currentStreak = streak,
        )
    }

    suspend fun awardMatchPoints(pointsEarned: Int, wonMatch: Boolean): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val existingSnap = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path, auth)
            throw e
        }

        val current = existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalStateException("Perfil no encontrado")

        val newPoints = (current.totalPoints + pointsEarned.coerceAtLeast(0)).coerceAtMost(10_000_000L)
        val newLevel = LevelSystem.levelForPoints(newPoints).toLong()
        val newGames = current.gamesPlayed + 1L
        val newWins = if (wonMatch) current.wins + 1L else current.wins
        val newLosses = if (!wonMatch) current.losses + 1L else current.losses
        val newCompletedLvl = if (wonMatch && newLevel > current.completedLevel) newLevel else current.completedLevel
        val newStreak = current.currentStreak.coerceAtLeast(1L) + if (pointsEarned > 0) 1L else 0L

        val batch = db.batch()
        batch.update(
            userRef,
            mapOf(
                "totalPoints" to newPoints,
                "level" to newLevel,
                "completedLevel" to newCompletedLvl,
                "gamesPlayed" to newGames,
                "wins" to newWins,
                "losses" to newLosses,
                "currentStreak" to newStreak,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
        val lbRef = db.collection("leaderboard").document(uid)
        batch.set(
            lbRef,
            mapOf(
                "userId" to uid,
                "username" to current.username,
                "avatarIcon" to current.avatarIcon,
                "avatarColor" to current.avatarColor,
                "totalPoints" to newPoints,
                "level" to newLevel,
                "wins" to newWins,
                "losses" to newLosses,
                "gamesPlayed" to newGames,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            throw e
        }

        current.copy(
            totalPoints = newPoints,
            level = newLevel,
            completedLevel = newCompletedLvl,
            gamesPlayed = newGames,
            wins = newWins,
            losses = newLosses,
            currentStreak = newStreak,
        )
    }

    fun observeGlobalLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        val query = db.collection(path)
            .whereGreaterThanOrEqualTo("totalPoints", 0)
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .limit(50)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeWaitingRooms(): Flow<List<GameRoom>> = flow {
        val path = "rooms"
        val query = db.collection(path).whereEqualTo("status", "waiting")
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        GameRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        val docRef = db.collection("rooms").document(roomCode)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeRoomPlayers(roomCode: String): Flow<List<RoomPlayer>> = flow {
        val path = "rooms/$roomCode/players"
        val query = db.collection("rooms").document(roomCode).collection("players")
            .whereEqualTo("roomId", roomCode)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        RoomPlayer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.joinedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeRoomDrawings(roomCode: String): Flow<List<RoundDrawing>> = flow {
        val path = "rooms/$roomCode/drawings"
        val query = db.collection("rooms").document(roomCode).collection("drawings")
            .whereEqualTo("roomId", roomCode)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        RoundDrawing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeRoomVotes(roomCode: String): Flow<List<RoundVote>> = flow {
        val path = "rooms/$roomCode/votes"
        val query = db.collection("rooms").document(roomCode).collection("votes")
            .whereEqualTo("roomId", roomCode)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        RoundVote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = extractFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(
        profile: UserProfile,
        prompt: DrawingPrompt = DrawingCatalog.randomPrompt(),
        totalRounds: Int = 3,
        maxPlayers: Int = 6,
        roundDurationSec: Int = 60,
        explicitRoomCode: String? = null,
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val roomCode = (explicitRoomCode ?: generateRoomCode()).uppercase()
        val roomRef = db.collection("rooms").document(roomCode)
        val playerRef = roomRef.collection("players").document(uid)

        val roomPayload = mapOf(
            "roomCode" to roomCode,
            "hostId" to uid,
            "hostName" to profile.username.take(30),
            "status" to "waiting",
            "promptWord" to prompt.wordEs,
            "promptKey" to prompt.key,
            "currentRound" to 1,
            "totalRounds" to totalRounds.coerceIn(1, 10),
            "maxPlayers" to maxPlayers.coerceIn(2, 20),
            "roundDurationSec" to roundDurationSec.coerceIn(15, 300),
            "playerUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        val playerPayload = mapOf(
            "userId" to uid,
            "roomId" to roomCode,
            "username" to profile.username.take(30),
            "avatarIcon" to profile.avatarIcon,
            "avatarColor" to profile.avatarColor,
            "hasSubmittedRound" to 0,
            "joinedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        try {
            roomRef.set(roomPayload).await()
            playerRef.set(playerPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, roomRef.path, auth)
            throw e
        }

        roomCode
    }

    suspend fun joinRoom(roomCodeInput: String, profile: UserProfile): Result<String> = runCatching {
        val uid = requireUserId()
        val roomCode = roomCodeInput.trim().uppercase()
        require(roomCode.matches(Regex("^[A-Za-z0-9_-]{4,12}$"))) {
            "El código de sala debe tener entre 4 y 12 caracteres alfanuméricos."
        }
        val roomRef = db.collection("rooms").document(roomCode)
        val snap = try {
            roomRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, roomRef.path, auth)
            throw e
        }

        if (!snap.exists()) {
            throw IllegalStateException("No existe ninguna sala con el código $roomCode.")
        }

        val room = snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalStateException("Sala inválida.")

        if (!room.playerUids.contains(uid)) {
            if (room.status != "waiting") {
                throw IllegalStateException("La partida ya comenzó en esta sala.")
            }
            if (room.playerUids.size >= room.maxPlayers) {
                throw IllegalStateException("La sala ya alcanzó el máximo de 6 jugadores.")
            }
            try {
                roomRef.update(
                    mapOf(
                        "playerUids" to (room.playerUids + uid),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, roomRef.path, auth)
                throw e
            }
        }

        val playerRef = roomRef.collection("players").document(uid)
        val existingPlayer = try {
            playerRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, playerRef.path, auth)
            throw e
        }

        if (!existingPlayer.exists()) {
            val playerPayload = mapOf(
                "userId" to uid,
                "roomId" to roomCode,
                "username" to profile.username.take(30),
                "avatarIcon" to profile.avatarIcon,
                "avatarColor" to profile.avatarColor,
                "hasSubmittedRound" to 0,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            try {
                playerRef.set(playerPayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, playerRef.path, auth)
                throw e
            }
        }

        roomCode
    }

    suspend fun leaveRoom(roomCode: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val roomRef = db.collection("rooms").document(roomCode)
        val snap = try {
            roomRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, roomRef.path, auth)
            throw e
        }
        val room = snap.toObject(GameRoom::class.java) ?: return@runCatching
        if (room.hostId == uid && room.playerUids.size <= 1) {
            try {
                roomRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, roomRef.path, auth)
                throw e
            }
            return@runCatching
        }
        val remaining = room.playerUids.filter { it != uid }
        if (remaining.isNotEmpty() && room.playerUids.contains(uid)) {
            try {
                roomRef.update(
                    mapOf(
                        "playerUids" to remaining,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, roomRef.path, auth)
                throw e
            }
        }
    }

    suspend fun updateRoomState(
        roomCode: String,
        newStatus: String,
        prompt: DrawingPrompt,
        currentRound: Int,
    ): Result<Unit> = runCatching {
        requireUserId()
        val roomRef = db.collection("rooms").document(roomCode)
        try {
            roomRef.update(
                mapOf(
                    "status" to newStatus,
                    "promptWord" to prompt.wordEs,
                    "promptKey" to prompt.key,
                    "currentRound" to currentRound,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path, auth)
            throw e
        }
    }

    suspend fun submitDrawing(
        roomCode: String,
        round: Int,
        promptWord: String,
        strokes: List<StrokeData>,
        profile: UserProfile,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val drawingId = "${round}_$uid"
        val drawingRef = db.collection("rooms").document(roomCode).collection("drawings").document(drawingId)
        val playerRef = db.collection("rooms").document(roomCode).collection("players").document(uid)

        val serializedStrokes = strokes.take(150).map { stroke ->
            mapOf(
                "colorHex" to stroke.colorHex,
                "strokeWidth" to stroke.strokeWidth,
                "points" to stroke.points.take(300),
            )
        }

        val existing = try {
            drawingRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, drawingRef.path, auth)
            throw e
        }

        val batch = db.batch()
        if (existing.exists()) {
            batch.update(
                drawingRef,
                mapOf(
                    "strokes" to serializedStrokes,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        } else {
            batch.set(
                drawingRef,
                mapOf(
                    "userId" to uid,
                    "roomId" to roomCode,
                    "username" to profile.username.take(30),
                    "avatarColor" to profile.avatarColor,
                    "round" to round,
                    "promptWord" to promptWord.take(30),
                    "strokes" to serializedStrokes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }
        batch.update(
            playerRef,
            mapOf(
                "hasSubmittedRound" to round,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, drawingRef.path, auth)
            throw e
        }
    }

    suspend fun castVote(
        roomCode: String,
        round: Int,
        targetUserId: String,
        points: Int = 50,
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(targetUserId != uid) { "No puedes votar por tu propio dibujo." }
        val voteId = "${round}_$uid"
        val voteRef = db.collection("rooms").document(roomCode).collection("votes").document(voteId)
        val payload = mapOf(
            "voterId" to uid,
            "targetUserId" to targetUserId,
            "roomId" to roomCode,
            "round" to round,
            "points" to points.coerceIn(10, 100),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        try {
            voteRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, voteRef.path, auth)
            throw e
        }
    }

    private fun extractFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
