package com.example.ui.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CurrencyRepository
import com.example.data.local.entity.CurrencyTransactionEntity
import com.example.data.local.entity.PlayerCurrencyEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CurrencyUiState(
    val bankroll: Long = 10000L,
    val xp: Long = 0L,
    val voidBalance: Long = 10L,
    val voidStaked: Long = 0L,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalWagered: Long = 0L,
    val totalWon: Long = 0L,
    val netProfit: Long = 0L,
    val recentTransactions: List<CurrencyTransactionEntity> = emptyList(),
    val isLowBalance: Boolean = false
)

class CurrencyViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    val uiState: StateFlow<CurrencyUiState> = combine(
        repository.currencyFlow,
        repository.transactionsFlow
    ) { currency, transactions ->
        CurrencyUiState(
            bankroll = currency.bankroll,
            xp = currency.xp,
            voidBalance = currency.voidBalance,
            voidStaked = currency.voidStaked,
            currentWinStreak = currency.currentWinStreak,
            bestWinStreak = currency.bestWinStreak,
            totalWagered = currency.totalWagered,
            totalWon = currency.totalWon,
            netProfit = currency.totalWon - currency.totalWagered,
            recentTransactions = transactions,
            isLowBalance = currency.bankroll < 200L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CurrencyUiState()
    )

    fun placeWager(gameType: String, amount: Long, note: String = "Wager"): Boolean {
        var success = false
        viewModelScope.launch {
            success = repository.recordWager(gameType, amount, note)
        }
        return success
    }

    fun awardPayout(gameType: String, amount: Long, note: String = "Payout") {
        viewModelScope.launch {
            repository.recordPayout(gameType, amount, isWin = amount > 0L, note = note)
        }
    }

    fun recordLoss(gameType: String) {
        viewModelScope.launch {
            repository.recordLoss(gameType)
        }
    }

    fun emergencyRefill() {
        viewModelScope.launch {
            repository.emergencyRefill(2500L)
        }
    }

    fun claimDailyBonus(boostedAmount: Long) {
        viewModelScope.launch {
            repository.claimDailyBonus(boostedAmount)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
