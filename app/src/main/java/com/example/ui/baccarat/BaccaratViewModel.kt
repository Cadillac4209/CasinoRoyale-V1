package com.example.ui.baccarat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ActiveBaccaratSnapshot
import com.example.data.CardSnapshot
import com.example.data.CasinoRepository
import com.example.model.BaccaratBetSpot
import com.example.model.BaccaratOutcome
import com.example.model.BaccaratRules
import com.example.model.Card
import com.example.model.Deck
import com.example.model.GameType
import com.example.ui.components.ChipDenom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BaccaratViewModel(
    private val repository: CasinoRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val activeGameRepository = repository.activeGameRepository
    private val deck = Deck(6) // Standard 6-deck Macau shoe

    private val _selectedChip = MutableStateFlow(ChipDenom.TWENTY_FIVE)
    val selectedChip: StateFlow<ChipDenom> = _selectedChip.asStateFlow()

    private val _bets = MutableStateFlow<Map<BaccaratBetSpot, Long>>(emptyMap())
    val bets: StateFlow<Map<BaccaratBetSpot, Long>> = _bets.asStateFlow()

    private val _isDealing = MutableStateFlow(false)
    val isDealing: StateFlow<Boolean> = _isDealing.asStateFlow()

    private val _playerCards = MutableStateFlow<List<Card>>(emptyList())
    val playerCards: StateFlow<List<Card>> = _playerCards.asStateFlow()

    private val _bankerCards = MutableStateFlow<List<Card>>(emptyList())
    val bankerCards: StateFlow<List<Card>> = _bankerCards.asStateFlow()

    private val _history = MutableStateFlow<List<BaccaratOutcome>>(emptyList())
    val history: StateFlow<List<BaccaratOutcome>> = _history.asStateFlow()

    private val _statusMessage = MutableStateFlow("PLACE CHIPS ON PLAYER, BANKER, TIE, OR PAIR")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastPayout = MutableStateFlow(0L)
    val lastPayout: StateFlow<Long> = _lastPayout.asStateFlow()

    private val _lastOutcome = MutableStateFlow<BaccaratOutcome?>(null)
    val lastOutcome: StateFlow<BaccaratOutcome?> = _lastOutcome.asStateFlow()

    private var previousBets: Map<BaccaratBetSpot, Long> = emptyMap()

    val totalTableBet: Long
        get() = _bets.value.values.sum()

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        viewModelScope.launch {
            val saved = activeGameRepository.loadBaccaratState() ?: return@launch
            runCatching { ChipDenom.valueOf(saved.selectedChip) }.getOrNull()?.let {
                _selectedChip.value = it
            }
            val restoredBets = linkedMapOf<BaccaratBetSpot, Long>().apply {
                if (saved.playerBet > 0L) put(BaccaratBetSpot.PLAYER, saved.playerBet)
                if (saved.bankerBet > 0L) put(BaccaratBetSpot.BANKER, saved.bankerBet)
                if (saved.tieBet > 0L) put(BaccaratBetSpot.TIE, saved.tieBet)
                if (saved.pairBet > 0L) put(BaccaratBetSpot.DRAGON_PAIR, saved.pairBet)
            }
            _bets.value = restoredBets
            previousBets = linkedMapOf<BaccaratBetSpot, Long>().apply {
                if (saved.previousPlayerBet > 0L) put(BaccaratBetSpot.PLAYER, saved.previousPlayerBet)
                if (saved.previousBankerBet > 0L) put(BaccaratBetSpot.BANKER, saved.previousBankerBet)
                if (saved.previousTieBet > 0L) put(BaccaratBetSpot.TIE, saved.previousTieBet)
                if (saved.previousPairBet > 0L) put(BaccaratBetSpot.DRAGON_PAIR, saved.previousPairBet)
            }
            _history.value = saved.historyCodes.mapNotNull { code ->
                BaccaratOutcome.entries.firstOrNull { it.shortCode == code }
            }
            deck.restoreCards(saved.remainingShoeCards.map { it.toCard() })

            if (saved.isDealing && saved.playerCards.size >= 2 && saved.bankerCards.size >= 2 && restoredBets.isNotEmpty()) {
                _playerCards.value = saved.playerCards.map { it.toCard() }
                _bankerCards.value = saved.bankerCards.map { it.toCard() }
                _statusMessage.value = "RESUMED BACCARAT HAND..."
                finishDealSequence(
                    placedBets = restoredBets,
                    initialPlayer = saved.playerCards.take(2).map { it.toCard() },
                    initialBanker = saved.bankerCards.take(2).map { it.toCard() }
                )
            } else {
                _statusMessage.value = saved.statusMessage
            }
        }
    }

    fun selectChip(denom: ChipDenom) {
        if (_isDealing.value) return
        soundManager.playChipClick()
        _selectedChip.value = denom
    }

    fun addBetToSpot(spot: BaccaratBetSpot) {
        if (_isDealing.value) return
        val profile = repository.profile.value
        val chipAmount = _selectedChip.value.value
        val currentTotal = _bets.value.values.sum()

        if (currentTotal + chipAmount > profile.bankroll) {
            _statusMessage.value = "INSUFFICIENT CHIPS"
            return
        }
        if (currentTotal + chipAmount > profile.maxBetAllowed) {
            _statusMessage.value = "TABLE MAX LIMIT: $${profile.maxBetAllowed}"
            return
        }

        soundManager.playChipClick()
        val updated = _bets.value.toMutableMap()
        updated[spot] = (updated[spot] ?: 0L) + chipAmount
        _bets.value = updated
        _lastPayout.value = 0L
        _lastOutcome.value = null
        _statusMessage.value = "TOTAL TABLE BET: $${updated.values.sum()}"
    }

    fun clearBets() {
        if (_isDealing.value) return
        soundManager.playButtonClick()
        _bets.value = emptyMap()
        _lastPayout.value = 0L
        _lastOutcome.value = null
        _statusMessage.value = "PLACE CHIPS ON PLAYER, BANKER, TIE, OR PAIR"
    }

    fun rebetPrevious() {
        if (_isDealing.value || previousBets.isEmpty()) return
        val needed = previousBets.values.sum()
        val profile = repository.profile.value
        if (needed > profile.bankroll) {
            _statusMessage.value = "NOT ENOUGH CHIPS TO REBET"
            return
        }
        if (needed > profile.maxBetAllowed) {
            _statusMessage.value = "REBET EXCEEDS TABLE MAX ($${profile.maxBetAllowed})"
            return
        }
        soundManager.playChipClick()
        _bets.value = previousBets
        _lastPayout.value = 0L
        _lastOutcome.value = null
        _statusMessage.value = "REBET PLACED: $$needed"
    }

    fun dealRound() {
        if (_isDealing.value) return
        val placedBets = _bets.value.filterValues { it > 0L }
        val totalBet = placedBets.values.sum()
        if (totalBet <= 0L) {
            _statusMessage.value = "TAP A BETTING ZONE FIRST!"
            return
        }

        if (!repository.placeWager(totalBet, GameType.BACCARAT)) {
            _statusMessage.value = "INSUFFICIENT CHIPS"
            return
        }

        previousBets = placedBets
        _isDealing.value = true
        _lastPayout.value = 0L
        _lastOutcome.value = null
        _playerCards.value = emptyList()
        _bankerCards.value = emptyList()

        viewModelScope.launch {
            if (deck.remainingCards() < 20) {
                deck.reset()
            }
            _statusMessage.value = "DEALING INITIAL HANDS..."
            val p1 = deck.draw().copy(isFaceUp = true)
            soundManager.playCardDeal()
            _playerCards.value = listOf(p1)
            delay(repository.scaleDelay(220L))

            val b1 = deck.draw().copy(isFaceUp = true)
            soundManager.playCardDeal()
            _bankerCards.value = listOf(b1)
            delay(repository.scaleDelay(220L))

            val p2 = deck.draw().copy(isFaceUp = true)
            soundManager.playCardDeal()
            val initialPlayer = listOf(p1, p2)
            _playerCards.value = initialPlayer
            delay(repository.scaleDelay(220L))

            val b2 = deck.draw().copy(isFaceUp = true)
            soundManager.playCardDeal()
            val initialBanker = listOf(b1, b2)
            _bankerCards.value = initialBanker

            persistInProgressState(placedBets, totalBet)
            delay(repository.scaleDelay(320L))

            finishDealSequence(placedBets, initialPlayer, initialBanker)
        }
    }

    private suspend fun finishDealSequence(
        placedBets: Map<BaccaratBetSpot, Long>,
        initialPlayer: List<Card>,
        initialBanker: List<Card>
    ) {
        val deal = BaccaratRules.resolveFromCards(
            playerInitial = initialPlayer,
            bankerInitial = initialBanker,
            drawNextCard = { deck.draw().copy(isFaceUp = true) }
        )

        if (deal.isNatural) {
            _statusMessage.value = "NATURAL ${maxOf(deal.playerFinalScore, deal.bankerFinalScore)}!"
            delay(repository.scaleDelay(350L))
        } else {
            if (deal.playerThirdCard != null) {
                _statusMessage.value = "PLAYER DRAWS THIRD CARD..."
                soundManager.playCardDeal()
                _playerCards.value = initialPlayer + deal.playerThirdCard
                delay(repository.scaleDelay(400L))
            }
            if (deal.bankerThirdCard != null) {
                _statusMessage.value = "BANKER DRAWS THIRD CARD..."
                soundManager.playCardDeal()
                _bankerCards.value = initialBanker + deal.bankerThirdCard
                delay(repository.scaleDelay(400L))
            }
        }

        val totalBet = placedBets.values.sum()
        val breakdown = BaccaratRules.calculatePayouts(
            bets = placedBets,
            outcome = deal.outcome,
            hasAnyPair = deal.hasAnyPair
        )

        _history.value = (listOf(deal.outcome) + _history.value).take(18)
        _lastOutcome.value = deal.outcome

        if (breakdown.totalReturned > 0L) {
            val isPairJackpot = deal.hasAnyPair && (placedBets[BaccaratBetSpot.DRAGON_PAIR] ?: 0L) > 0L
            val isBigWin = breakdown.totalReturned >= totalBet * 4L || isPairJackpot
            if (!breakdown.isPushOnly) {
                repository.awardPayout(GameType.BACCARAT, breakdown.totalReturned, isJackpot = isPairJackpot)
            } else {
                // Push returns wager cleanly without counting as a loss
                repository.awardPayout(GameType.BACCARAT, breakdown.totalReturned, isJackpot = false)
            }

            if (isBigWin) {
                soundManager.playBigWin()
            } else {
                soundManager.playWin()
            }

            val bonusTags = if (breakdown.winningLabels.isNotEmpty()) {
                " (${breakdown.winningLabels.joinToString(" + ")})"
            } else ""

            _lastPayout.value = if (breakdown.isPushOnly) 0L else breakdown.totalReturned
            _statusMessage.value = if (breakdown.isPushOnly) {
                "TIE • STAKES PUSHED BACK ($${breakdown.totalReturned})"
            } else {
                "${deal.outcome.displayTitle} +$${breakdown.totalReturned}$bonusTags"
            }
        } else {
            repository.recordLoss(GameType.BACCARAT)
            soundManager.playLoss()
            _statusMessage.value = "${deal.outcome.displayTitle} • P ${deal.playerFinalScore} vs B ${deal.bankerFinalScore}"
        }

        _isDealing.value = false
        activeGameRepository.clearGameState(GameType.BACCARAT)
    }

    fun dismissWinCelebration() {
        _lastPayout.value = 0L
    }

    private fun persistInProgressState(placedBets: Map<BaccaratBetSpot, Long>, totalBet: Long) {
        viewModelScope.launch {
            activeGameRepository.saveBaccaratState(
                snapshot = ActiveBaccaratSnapshot(
                    selectedChip = _selectedChip.value.name,
                    playerBet = placedBets[BaccaratBetSpot.PLAYER] ?: 0L,
                    bankerBet = placedBets[BaccaratBetSpot.BANKER] ?: 0L,
                    tieBet = placedBets[BaccaratBetSpot.TIE] ?: 0L,
                    pairBet = placedBets[BaccaratBetSpot.DRAGON_PAIR] ?: 0L,
                    previousPlayerBet = previousBets[BaccaratBetSpot.PLAYER] ?: 0L,
                    previousBankerBet = previousBets[BaccaratBetSpot.BANKER] ?: 0L,
                    previousTieBet = previousBets[BaccaratBetSpot.TIE] ?: 0L,
                    previousPairBet = previousBets[BaccaratBetSpot.DRAGON_PAIR] ?: 0L,
                    isDealing = true,
                    playerCards = _playerCards.value.map { CardSnapshot.fromCard(it) },
                    bankerCards = _bankerCards.value.map { CardSnapshot.fromCard(it) },
                    historyCodes = _history.value.map { it.shortCode },
                    statusMessage = _statusMessage.value,
                    remainingShoeCards = deck.exportRemainingCards().map { CardSnapshot.fromCard(it) }
                ),
                isInProgress = true,
                currentBet = totalBet
            )
        }
    }
}
