package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "High Roller",
    val totalXp: Long = 0L,
    val level: Int = 1,
    val vipTitle: String = "Bronze Player",
    val bestWinStreak: Int = 0,
    val biggestWin: Long = 0L,
    val totalRoundsPlayed: Int = 0,
    val royalePoints: Long = 0L,
    val lifetimeRoyalePoints: Long = 0L,
    val loyaltyTier: String = "Ember Member",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
