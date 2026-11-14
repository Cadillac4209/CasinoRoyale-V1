package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_game_states")
data class ActiveGameStateEntity(
    @PrimaryKey
    val gameType: String,
    val isInProgress: Boolean,
    val stageLabel: String,
    val currentBet: Long,
    val statePayloadJson: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
)
