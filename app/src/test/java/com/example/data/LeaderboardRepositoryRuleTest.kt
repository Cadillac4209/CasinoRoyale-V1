package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LeaderboardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertPlayerScore_authenticatedOwner_createsAndUpdatesLeaderboardEntry() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LeaderboardRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPlayerScore(
                displayName = "AliceEmber",
                totalXp = 4200L,
                level = 7,
                vipTitle = "Platinum Ace",
                bestWinStreak = 4,
                biggestWin = 8500L,
                totalRoundsPlayed = 35
            )
        }

        val entry = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCurrentPlayerEntry()
        }
        assertNotNull(entry)
        assertEquals(uid, entry?.userId)
        assertEquals("AliceEmber", entry?.displayName)
        assertEquals(4200L, entry?.totalXp)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPlayerScore(
                displayName = "AliceInferno",
                totalXp = 6400L,
                level = 9,
                vipTitle = "Platinum Ace",
                bestWinStreak = 6,
                biggestWin = 12500L,
                totalRoundsPlayed = 50
            )
        }

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCurrentPlayerEntry()
        }
        assertEquals("AliceInferno", updated?.displayName)
        assertEquals(6400L, updated?.totalXp)
        assertEquals(9, updated?.level)
    }

    @Test
    fun observeTop100Players_andRelativeRank_orderedByTotalXpDescending() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LeaderboardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertPlayerScore(
                displayName = "AliceEmber",
                totalXp = 2100L,
                level = 5,
                vipTitle = "Gold High Roller",
                bestWinStreak = 3,
                biggestWin = 4000L,
                totalRoundsPlayed = 20
            )
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = LeaderboardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.upsertPlayerScore(
                displayName = "BobVolcano",
                totalXp = 9900L,
                level = 12,
                vipTitle = "Diamond Royale",
                bestWinStreak = 9,
                biggestWin = 25000L,
                totalRoundsPlayed = 88
            )
        }

        val topPlayers = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeTop100Players().first { entries ->
                entries.any { it.userId == aliceUid } && entries.any { it.userId == bobUid }
            }
        }
        val bobIdx = topPlayers.indexOfFirst { it.userId == bobUid }
        val aliceIdx = topPlayers.indexOfFirst { it.userId == aliceUid }
        assertTrue(bobIdx in 0 until aliceIdx)

        val aliceRank = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPlayerRelativeRank(2100L)
        }
        assertTrue(aliceRank >= 2)
    }

    @Test
    fun crossUserWrite_failsWithPermissionDenied() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = LeaderboardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.upsertPlayerScore(
                displayName = "BobVolcano",
                totalXp = 3000L,
                level = 5,
                vipTitle = "Gold High Roller",
                bestWinStreak = 2,
                biggestWin = 3000L,
                totalRoundsPlayed = 15
            )
        }

        signInTestUser(ALICE_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(LeaderboardRepository.COLLECTION_LEADERBOARD)
                    .document(bobUid)
                    .update("totalXp", 999999L)
                    .await()
            }
            fail("Expected PERMISSION_DENIED when Alice updates Bob's leaderboard document")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeTop100Players_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val unauthCollection = firestore.collection(LeaderboardRepository.COLLECTION_LEADERBOARD)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                unauthCollection.limit(10L).get().await()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_casino@test.com"
        const val BOB_EMAIL = "bob_casino@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
