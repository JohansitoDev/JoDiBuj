package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class JoDiBujRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfileAndAwardPoints_updatesUserAndLeaderboard() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@jodibuj.test")
        val repo = JoDiBujRepository(firestore, auth)

        val saved = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile(
                username = "AliceArt",
                displayName = "Alice",
                email = "alice@test.com",
                bio = "Me encanta dibujar gatos",
                avatarIcon = "cat",
                avatarColor = "#1CB0F6"
            ).getOrThrow()
        }
        assertEquals(uid, saved.userId)
        assertEquals("AliceArt", saved.username)

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.awardMatchPoints(pointsEarned = 120, wonMatch = true).getOrThrow()
        }
        assertEquals(120L, updated.totalPoints)
        assertEquals(2L, updated.level)
        assertEquals(1L, updated.wins)

        val lb = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeGlobalLeaderboard().first { entries -> entries.any { it.userId == uid } }
        }
        assertTrue(lb.any { it.userId == uid && it.totalPoints == 120L })
    }

    @Test
    fun roomLifecycle_createJoinDrawVote_enforcesPeerVoting() = runBlocking {
        val aliceUid = signInTestUser("alice_room_${UUID.randomUUID()}@jodibuj.test")
        val repo = JoDiBujRepository(firestore, auth)

        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile("AliceHost", "Alice", "", "", "brush", "#58CC02").getOrThrow()
        }
        val roomCode = "RM${UUID.randomUUID().toString().replace("-", "").take(6).uppercase()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom(aliceProfile, DrawingCatalog.prompts.first(), totalRounds = 3, explicitRoomCode = roomCode)
                .getOrThrow()
        }

        val bobUid = signInTestUser("bob_room_${UUID.randomUUID()}@jodibuj.test")
        val bobProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile("BobFriend", "Bob", "", "", "fish", "#1CB0F6").getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.joinRoom(roomCode, bobProfile).getOrThrow()
        }

        val players = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRoomPlayers(roomCode).first { it.size == 2 }
        }
        assertEquals(2, players.size)

        // Bob submits drawing
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitDrawing(
                roomCode = roomCode,
                round = 1,
                promptWord = "Pez",
                strokes = listOf(StrokeData("#1CB0F6", 8.0, listOf(0.1, 0.2, 0.5, 0.5))),
                profile = bobProfile
            ).getOrThrow()
        }

        // Bob cannot vote for himself
        val selfVoteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.castVote(roomCode, 1, bobUid, 50)
        }
        assertTrue(selfVoteResult.isFailure)

        // Bob votes for Alice
        val voteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.castVote(roomCode, 1, aliceUid, 50)
        }
        assertTrue(voteResult.isSuccess)

        val votes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRoomVotes(roomCode).first { it.isNotEmpty() }
        }
        assertEquals(aliceUid, votes.first().targetUserId)
    }

    @Test
    fun observeLeaderboard_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = JoDiBujRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeGlobalLeaderboard().first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = caught!!.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
