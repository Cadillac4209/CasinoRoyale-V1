package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_transactions")
data class CurrencyTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val transactionId: Long = 0,
    val gameType: String,
    val amountDelta: Long,
    val balanceAfter: Long,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
