package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CommunityVoteOption(val wireValue: String, val label: String) {
  RUNS_GREAT("runs_great", "Runs Great"),
  LAGS("lags", "Lags");

  companion object {
    fun fromWire(value: String?): CommunityVoteOption? =
      entries.firstOrNull { it.wireValue == value }
  }
}

@IgnoreExtraProperties
data class GameVoteSummary(
  @DocumentId val id: String = "",
  val gameId: String = "",
  val runsGreatCount: Long = 0L,
  val lagsCount: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val totalVotes: Long
    get() = (runsGreatCount + lagsCount).coerceAtLeast(0L)

  val runsGreatPercentage: Int
    get() =
      if (totalVotes <= 0L) 0
      else ((runsGreatCount.toDouble() / totalVotes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
}

@IgnoreExtraProperties
data class UserGameVote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val gameId: String = "",
  val verdict: String = "",
  val deviceSoc: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CommunityVerdictState(
  val gameId: String,
  val summary: GameVoteSummary? = null,
  val userVote: CommunityVoteOption? = null,
  val isLoading: Boolean = false,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
)
