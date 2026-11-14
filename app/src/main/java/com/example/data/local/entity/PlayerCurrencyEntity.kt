package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_currency")
data class PlayerCurrencyEntity(
    @PrimaryKey
    val id: Int = 1,
    val bankroll: Long = 10000L,
    val xp: Long = 0L,
    val voidBalance: Long = 10L,
    val voidStaked: Long = 0L,
    val lastDailyBonusTime: Long = 0L,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val checkInStreak: Int = 0,
    val bestCheckInStreak: Int = 0,
    val lastCheckInTimestamp: Long = 0L,
    val totalCheckIns: Int = 0,
    val totalWagered: Long = 0L,
    val totalWon: Long = 0L,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
