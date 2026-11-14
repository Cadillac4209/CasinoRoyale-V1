package com.example.data

import android.content.Context
import com.example.R
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.model.LeaderboardEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.AggregateSource
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

class LeaderboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing leaderboard.")
    }

    fun observeTop100Players(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection(COLLECTION_LEADERBOARD)
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(TOP_LIMIT)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_LEADERBOARD)
                    }
                    throw error
                }
        )
    }

    fun observeCurrentPlayerEntry(): Flow<LeaderboardEntry?> = flow {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_LEADERBOARD).document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            LeaderboardEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
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

    suspend fun getCurrentPlayerEntry(): LeaderboardEntry? {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_LEADERBOARD).document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
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

    suspend fun getPlayerRelativeRank(totalXp: Long): Int {
        requireUserId()
        val safeXp = totalXp.coerceIn(0L, MAX_XP)
        return try {
            val higherCount = db.collection(COLLECTION_LEADERBOARD)
                .whereGreaterThan("totalXp", safeXp)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
            (higherCount + 1L).toInt().coerceAtLeast(1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_LEADERBOARD)
            throw e
        }
    }

    suspend fun upsertPlayerScore(
        displayName: String,
        totalXp: Long,
        level: Int,
        vipTitle: String,
        bestWinStreak: Int,
        biggestWin: Long,
        totalRoundsPlayed: Int
    ) {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Ember Roller" }.take(30)
        val safeVipTitle = vipTitle.trim().ifBlank { "Bronze Player" }.take(40)
        val safeXp = totalXp.coerceIn(0L, MAX_XP)
        val safeLevel = level.coerceIn(1, 500)
        val safeStreak = bestWinStreak.coerceIn(0, 1_000_000)
        val safeBiggestWin = biggestWin.coerceIn(0L, 10_000_000_000L)
        val safeRounds = totalRoundsPlayed.coerceIn(0, 1_000_000_000)

        val docRef = db.collection(COLLECTION_LEADERBOARD).document(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf<String, Any>(
                    "displayName" to safeName,
                    "totalXp" to safeXp,
                    "level" to safeLevel,
                    "vipTitle" to safeVipTitle,
                    "bestWinStreak" to safeStreak,
                    "biggestWin" to safeBiggestWin,
                    "totalRoundsPlayed" to safeRounds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "totalXp" to safeXp,
                    "level" to safeLevel,
                    "vipTitle" to safeVipTitle,
                    "bestWinStreak" to safeStreak,
                    "biggestWin" to safeBiggestWin,
                    "totalRoundsPlayed" to safeRounds,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    companion object {
        const val COLLECTION_LEADERBOARD = "leaderboard"
        const val TOP_LIMIT = 100L
        const val MAX_XP = 1_000_000_000L
    }
}
