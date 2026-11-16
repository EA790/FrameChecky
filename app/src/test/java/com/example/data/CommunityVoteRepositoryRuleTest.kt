package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.CommunityVoteOption
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CommunityVoteRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun submitVote_authenticatedUser_createsSummaryAndUserVote() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = CommunityVoteRepository(firestore, auth)
    val gameId = "genshin_${UUID.randomUUID().toString().take(8)}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitVote(gameId, CommunityVoteOption.RUNS_GREAT, "Snapdragon 8 Gen 3")
    }

    val summary = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getGameVoteSummary(gameId) }
    assertNotNull(summary)
    assertEquals(1L, summary!!.runsGreatCount)
    assertEquals(0L, summary.lagsCount)
    assertEquals(100, summary.runsGreatPercentage)

    val userVote = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUserVote(gameId) }
    assertNotNull(userVote)
    assertEquals(CommunityVoteOption.RUNS_GREAT.wireValue, userVote!!.verdict)
  }

  @Test
  fun submitVote_changingVote_updatesSummaryCountsCorrectly() = runBlocking {
    val gameId = "pubg_${UUID.randomUUID().toString().take(8)}"

    signInTestUser(ALICE_EMAIL)
    val aliceRepo = CommunityVoteRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.submitVote(gameId, CommunityVoteOption.RUNS_GREAT, "Snapdragon 8 Gen 3")
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = CommunityVoteRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.submitVote(gameId, CommunityVoteOption.RUNS_GREAT, "Tensor G4")
    }

    // Alice switches vote to Lags -> 1 Runs Great, 1 Lags -> 50%
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.submitVote(gameId, CommunityVoteOption.LAGS, "Snapdragon 8 Gen 3")
    }

    val summary =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeGameVoteSummary(gameId).first { it != null && it.lagsCount == 1L }
      }
    assertNotNull(summary)
    assertEquals(1L, summary!!.runsGreatCount)
    assertEquals(1L, summary.lagsCount)
    assertEquals(50, summary.runsGreatPercentage)
  }

  @Test
  fun observeGameVoteSummary_unauthenticatedUser_failsWithIllegalStateOrPermissionDenied() =
    runBlocking {
      auth.signOut()
      val repo = CommunityVoteRepository(firestore, auth)
      val gameId = "cod_${UUID.randomUUID().toString().take(8)}"

      try {
        withTimeout(FLOW_TIMEOUT_MS) { repo.observeGameVoteSummary(gameId).first() }
        fail("Expected unauthenticated observeGameVoteSummary to fail")
      } catch (e: Exception) {
        val firestoreEx = e.unwrapFirestoreException()
        assertTrue(
          e is IllegalStateException ||
            firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
      }
    }

  private companion object {
    const val ALICE_EMAIL = "alice_framecheck@test.com"
    const val BOB_EMAIL = "bob_framecheck@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
