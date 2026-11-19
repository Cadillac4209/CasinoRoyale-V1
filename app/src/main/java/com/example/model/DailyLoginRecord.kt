package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class DailyLoginRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCheckIns: Int = 0,
    val lastClaimedChips: Long = 0L,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    val lastBonusClaimAt: Timestamp? = null,
    val lastWheelSpinAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
