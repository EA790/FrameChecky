package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CommunityVoteOption
import com.example.model.GameVoteSummary
import com.example.model.UserGameVote
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunityVoteRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before voting.")
  }

  fun observeGameVoteSummary(gameId: String): Flow<GameVoteSummary?> = flow {
    val uid = requireUserId()
    val docRef = db.collection(COLLECTION_GAME_VOTES).document(gameId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              GameVoteSummary::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  fun observeUserVote(gameId: String): Flow<UserGameVote?> = flow {
    val uid = requireUserId()
    val docRef =
      db.collection(COLLECTION_GAME_VOTES)
        .document(gameId)
        .collection(SUBCOLLECTION_VOTES)
        .document(uid)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserGameVote::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  suspend fun getGameVoteSummary(gameId: String): GameVoteSummary? {
    requireUserId()
    val docRef = db.collection(COLLECTION_GAME_VOTES).document(gameId)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(
          GameVoteSummary::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun getUserVote(gameId: String): UserGameVote? {
    val uid = requireUserId()
    val docRef =
      db.collection(COLLECTION_GAME_VOTES)
        .document(gameId)
        .collection(SUBCOLLECTION_VOTES)
        .document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(
          UserGameVote::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun submitVote(
    gameId: String,
    option: CommunityVoteOption,
    deviceSoc: String,
  ) {
    val uid = requireUserId()
    val safeSoc = deviceSoc.trim().ifBlank { "Android Device" }.take(120)
    val summaryRef = db.collection(COLLECTION_GAME_VOTES).document(gameId)
    val voteRef = summaryRef.collection(SUBCOLLECTION_VOTES).document(uid)

    try {
      db.runTransaction { transaction ->
          val summarySnap = transaction.get(summaryRef)
          val existingVoteSnap = transaction.get(voteRef)

          val existingVerdict =
            if (existingVoteSnap.exists()) {
              CommunityVoteOption.fromWire(existingVoteSnap.getString("verdict"))
            } else {
              null
            }

          if (existingVerdict == option) {
            return@runTransaction null
          }

          val currentRunsGreat =
            if (summarySnap.exists()) (summarySnap.getLong("runsGreatCount") ?: 0L) else 0L
          val currentLags =
            if (summarySnap.exists()) (summarySnap.getLong("lagsCount") ?: 0L) else 0L

          var nextRunsGreat = currentRunsGreat
          var nextLags = currentLags

          when (existingVerdict) {
            null -> {
              if (option == CommunityVoteOption.RUNS_GREAT) nextRunsGreat += 1L
              else nextLags += 1L
            }
            CommunityVoteOption.RUNS_GREAT -> {
              nextRunsGreat = (nextRunsGreat - 1L).coerceAtLeast(0L)
              nextLags += 1L
            }
            CommunityVoteOption.LAGS -> {
              nextLags = (nextLags - 1L).coerceAtLeast(0L)
              nextRunsGreat += 1L
            }
          }

          if (!summarySnap.exists()) {
            val summaryPayload =
              mapOf(
                "gameId" to gameId,
                "runsGreatCount" to nextRunsGreat,
                "lagsCount" to nextLags,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            transaction.set(summaryRef, summaryPayload)
          } else {
            transaction.update(
              summaryRef,
              mapOf(
                "runsGreatCount" to nextRunsGreat,
                "lagsCount" to nextLags,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }

          if (!existingVoteSnap.exists()) {
            val votePayload =
              mapOf(
                "userId" to uid,
                "gameId" to gameId,
                "verdict" to option.wireValue,
                "deviceSoc" to safeSoc,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            transaction.set(voteRef, votePayload)
          } else {
            transaction.update(
              voteRef,
              mapOf(
                "verdict" to option.wireValue,
                "deviceSoc" to safeSoc,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
          null
        }
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, voteRef.path)
      throw e
    }
  }

  private companion object {
    const val COLLECTION_GAME_VOTES = "game_votes"
    const val SUBCOLLECTION_VOTES = "votes"
  }
}
