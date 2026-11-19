package com.example.data

import android.content.Context
import com.example.R
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.model.DailyLoginRecord
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DailyLoginRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing daily login records.")
    }

    fun observeDailyLoginRecord(): Flow<DailyLoginRecord?> = flow {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_DAILY_LOGINS).document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            DailyLoginRecord::class.java,
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

    suspend fun recordSessionLogin(): Result<Unit> {
        var docPath = "$COLLECTION_DAILY_LOGINS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COLLECTION_DAILY_LOGINS).document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "currentStreak" to 0,
                        "bestStreak" to 0,
                        "totalCheckIns" to 0,
                        "lastClaimedChips" to 0L,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordDailyBonusClaim(
        currentStreak: Int,
        bestStreak: Int,
        totalCheckIns: Int,
        claimedChips: Long
    ): Result<Unit> {
        var docPath = "$COLLECTION_DAILY_LOGINS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val safeStreak = currentStreak.coerceIn(0, MAX_STREAK)
            val safeBestStreak = bestStreak.coerceIn(safeStreak, MAX_STREAK)
            val safeTotalCheckIns = totalCheckIns.coerceIn(0, MAX_TOTAL_CHECK_INS)
            val safeChips = claimedChips.coerceIn(0L, MAX_CLAIMED_CHIPS)

            val docRef = db.collection(COLLECTION_DAILY_LOGINS).document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "currentStreak" to safeStreak,
                        "bestStreak" to safeBestStreak,
                        "totalCheckIns" to safeTotalCheckIns,
                        "lastClaimedChips" to safeChips,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "lastBonusClaimAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "currentStreak" to safeStreak,
                        "bestStreak" to safeBestStreak,
                        "totalCheckIns" to safeTotalCheckIns,
                        "lastClaimedChips" to safeChips,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "lastBonusClaimAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordWheelSpinClaim(): Result<Unit> {
        var docPath = "$COLLECTION_DAILY_LOGINS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COLLECTION_DAILY_LOGINS).document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "lastWheelSpinAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "currentStreak" to 0,
                        "bestStreak" to 0,
                        "totalCheckIns" to 0,
                        "lastClaimedChips" to 0L,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "lastWheelSpinAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resetCloudStreakForTesting(): Result<Unit> {
        var docPath = "$COLLECTION_DAILY_LOGINS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COLLECTION_DAILY_LOGINS).document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "currentStreak" to 0,
                        "lastClaimedChips" to 0L,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_DAILY_LOGINS = "dailyLogins"
        const val MAX_STREAK = 10_000
        const val MAX_TOTAL_CHECK_INS = 100_000
        const val MAX_CLAIMED_CHIPS = 10_000_000L
    }
}
