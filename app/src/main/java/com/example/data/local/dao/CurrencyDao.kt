package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CurrencyTransactionEntity
import com.example.data.local.entity.PlayerCurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM player_currency WHERE id = 1")
    fun getPlayerCurrencyFlow(): Flow<PlayerCurrencyEntity?>

    @Query("SELECT * FROM player_currency WHERE id = 1")
    suspend fun getPlayerCurrency(): PlayerCurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCurrency(currency: PlayerCurrencyEntity)

    @Query("UPDATE player_currency SET bankroll = :newBankroll, lastUpdatedTimestamp = :timestamp WHERE id = 1")
    suspend fun updateBankroll(newBankroll: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE player_currency SET bankroll = :newBankroll, currentWinStreak = :winStreak, bestWinStreak = max(bestWinStreak, :winStreak), lastUpdatedTimestamp = :timestamp WHERE id = 1")
    suspend fun updateBankrollAndStreak(newBankroll: Long, winStreak: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE player_currency SET voidBalance = :voidBalance, voidStaked = :voidStaked, lastUpdatedTimestamp = :timestamp WHERE id = 1")
    suspend fun updateVoidBalance(voidBalance: Long, voidStaked: Long, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CurrencyTransactionEntity)

    @Query("SELECT * FROM currency_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 30): Flow<List<CurrencyTransactionEntity>>

    @Query("DELETE FROM currency_transactions")
    suspend fun clearTransactions()
}
