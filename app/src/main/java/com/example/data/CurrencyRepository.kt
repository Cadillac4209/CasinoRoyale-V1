package com.example.data

import com.example.data.local.dao.CurrencyDao
import com.example.data.local.entity.CurrencyTransactionEntity
import com.example.data.local.entity.PlayerCurrencyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurrencyRepository(private val currencyDao: CurrencyDao) {

    val currencyFlow: Flow<PlayerCurrencyEntity> = currencyDao.getPlayerCurrencyFlow()
        .map { it ?: PlayerCurrencyEntity() }

    val transactionsFlow: Flow<List<CurrencyTransactionEntity>> =
        currencyDao.getRecentTransactions(50)

    suspend fun getCurrency(): PlayerCurrencyEntity {
        return currencyDao.getPlayerCurrency() ?: run {
            val default = PlayerCurrencyEntity()
            currencyDao.insertOrUpdateCurrency(default)
            default
        }
    }

    suspend fun seedInitialIfEmpty(initial: PlayerCurrencyEntity) {
        val existing = currencyDao.getPlayerCurrency()
        if (existing == null) {
            currencyDao.insertOrUpdateCurrency(initial)
        }
    }

    suspend fun recordWager(gameType: String, amount: Long, note: String = "Bet placed"): Boolean {
        if (amount <= 0L) return true
        val current = getCurrency()
        if (current.bankroll < amount) return false

        val newBalance = current.bankroll - amount
        val updated = current.copy(
            bankroll = newBalance,
            totalWagered = current.totalWagered + amount,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        currencyDao.insertOrUpdateCurrency(updated)

        currencyDao.insertTransaction(
            CurrencyTransactionEntity(
                gameType = gameType,
                amountDelta = -amount,
                balanceAfter = newBalance,
                note = note
            )
        )
        return true
    }

    suspend fun recordPayout(
        gameType: String,
        amount: Long,
        isWin: Boolean = true,
        note: String = "Payout"
    ): Long {
        val current = getCurrency()
        val newStreak = if (isWin && amount > 0L) current.currentWinStreak + 1 else 0
        val newBestStreak = maxOf(current.bestWinStreak, newStreak)
        val newBalance = current.bankroll + amount

        val updated = current.copy(
            bankroll = newBalance,
            currentWinStreak = newStreak,
            bestWinStreak = newBestStreak,
            totalWon = current.totalWon + amount,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        currencyDao.insertOrUpdateCurrency(updated)

        if (amount > 0L) {
            currencyDao.insertTransaction(
                CurrencyTransactionEntity(
                    gameType = gameType,
                    amountDelta = amount,
                    balanceAfter = newBalance,
                    note = "$note (Streak: $newStreak)"
                )
            )
        }
        return newBalance
    }

    suspend fun recordLoss(gameType: String) {
        val current = getCurrency()
        val updated = current.copy(
            currentWinStreak = 0,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        currencyDao.insertOrUpdateCurrency(updated)
    }

    suspend fun emergencyRefill(minBankroll: Long = 2500L): Long {
        val current = getCurrency()
        if (current.bankroll >= minBankroll) return current.bankroll

        val delta = minBankroll - current.bankroll
        val updated = current.copy(
            bankroll = minBankroll,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        currencyDao.insertOrUpdateCurrency(updated)

        currencyDao.insertTransaction(
            CurrencyTransactionEntity(
                gameType = "EMERGENCY_REFILL",
                amountDelta = delta,
                balanceAfter = minBankroll,
                note = "Floor Manager Complimentary Refill"
            )
        )
        return minBankroll
    }

    suspend fun claimDailyBonus(boostedAmount: Long): Long {
        val current = getCurrency()
        val newBalance = current.bankroll + boostedAmount
        val updated = current.copy(
            bankroll = newBalance,
            lastDailyBonusTime = System.currentTimeMillis(),
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        currencyDao.insertOrUpdateCurrency(updated)

        currencyDao.insertTransaction(
            CurrencyTransactionEntity(
                gameType = "DAILY_WHEEL",
                amountDelta = boostedAmount,
                balanceAfter = newBalance,
                note = "Daily Bonus Lucky Wheel"
            )
        )
        return newBalance
    }

    suspend fun updateVoidTokens(balance: Long, staked: Long) {
        currencyDao.updateVoidBalance(balance, staked)
    }

    suspend fun clearHistory() {
        currencyDao.clearTransactions()
    }
}
