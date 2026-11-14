package com.example.ui.blackjack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ActiveBlackjackSnapshot
import com.example.data.BlackjackHandSnapshot
import com.example.data.CardSnapshot
import com.example.data.CasinoRepository
import com.example.model.Card
import com.example.model.Deck
import com.example.model.GameType
import com.example.model.Rank
import com.example.ui.components.ChipDenom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BlackjackStage {
    BETTING,
    DEALING,
    INSURANCE_OFFER,
    PLAYER_TURN,
    DEALER_TURN,
    ROUND_OVER
}

class BlackjackViewModel(
    private val repository: CasinoRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val activeGameRepository = repository.activeGameRepository
    private val deck = Deck(4) // 4-deck shoe

    private val _stage = MutableStateFlow(BlackjackStage.BETTING)
    val stage: StateFlow<BlackjackStage> = _stage.asStateFlow()

    private val _selectedChip = MutableStateFlow(ChipDenom.TWENTY_FIVE)
    val selectedChip: StateFlow<ChipDenom> = _selectedChip.asStateFlow()

    private val _currentBet = MutableStateFlow(0L)
    val currentBet: StateFlow<Long> = _currentBet.asStateFlow()

    private val _dealerHand = MutableStateFlow<List<Card>>(emptyList())
    val dealerHand: StateFlow<List<Card>> = _dealerHand.asStateFlow()

    private val _playerHands = MutableStateFlow<List<BlackjackHand>>(listOf(BlackjackHand()))
    val playerHands: StateFlow<List<BlackjackHand>> = _playerHands.asStateFlow()

    private val _activeHandIndex = MutableStateFlow(0)
    val activeHandIndex: StateFlow<Int> = _activeHandIndex.asStateFlow()

    private val _insuranceBet = MutableStateFlow(0L)
    val insuranceBet: StateFlow<Long> = _insuranceBet.asStateFlow()

    private val _statusMessage = MutableStateFlow("PLACE YOUR BETS")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastPayout = MutableStateFlow(0L)
    val lastPayout: StateFlow<Long> = _lastPayout.asStateFlow()

    private var previousBet = 25L

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        viewModelScope.launch {
            val saved = activeGameRepository.loadBlackjackState() ?: return@launch
            val savedStage = runCatching { BlackjackStage.valueOf(saved.stage) }.getOrNull() ?: return@launch
            if (savedStage == BlackjackStage.BETTING || savedStage == BlackjackStage.ROUND_OVER) {
                return@launch
            }

            runCatching { ChipDenom.valueOf(saved.selectedChip) }.getOrNull()?.let {
                _selectedChip.value = it
            }
            _currentBet.value = saved.currentBet
            previousBet = saved.previousBet
            _dealerHand.value = saved.dealerHand.map { it.toCard() }
            val restoredHands = saved.playerHands.map { it.toBlackjackHand() }
            if (restoredHands.isEmpty()) return@launch
            _playerHands.value = restoredHands
            _activeHandIndex.value = saved.activeHandIndex.coerceIn(0, restoredHands.lastIndex)
            _insuranceBet.value = saved.insuranceBet
            deck.restoreCards(saved.remainingShoeCards.map { it.toCard() })

            when (savedStage) {
                BlackjackStage.DEALING -> {
                    val initialHand = restoredHands[0]
                    val isNaturalBlackjack = initialHand.totalScore == 21 && initialHand.cards.size == 2
                    val dealerUpCard = _dealerHand.value.firstOrNull()
                    if (isNaturalBlackjack) {
                        _playerHands.value = listOf(initialHand.copy(isBlackjack = true))
                        playDealerTurn()
                    } else if (dealerUpCard?.rank == Rank.ACE) {
                        _stage.value = BlackjackStage.INSURANCE_OFFER
                        _statusMessage.value = "SESSION RESUMED • TAKE INSURANCE?"
                        persistActiveState()
                    } else {
                        _stage.value = BlackjackStage.PLAYER_TURN
                        _statusMessage.value = "SESSION RESUMED • HIT OR STAND?"
                        persistActiveState()
                    }
                }
                BlackjackStage.INSURANCE_OFFER -> {
                    _stage.value = BlackjackStage.INSURANCE_OFFER
                    _statusMessage.value = "SESSION RESUMED • TAKE INSURANCE?"
                }
                BlackjackStage.PLAYER_TURN -> {
                    _stage.value = BlackjackStage.PLAYER_TURN
                    val prefix = if (restoredHands.size > 1) "HAND ${_activeHandIndex.value + 1}: " else ""
                    _statusMessage.value = "SESSION RESUMED • ${prefix}HIT OR STAND?"
                }
                BlackjackStage.DEALER_TURN -> {
                    playDealerTurn()
                }
                else -> Unit
            }
        }
    }

    private fun persistActiveState() {
        val currentStage = _stage.value
        val inProgress = currentStage == BlackjackStage.DEALING ||
            currentStage == BlackjackStage.INSURANCE_OFFER ||
            currentStage == BlackjackStage.PLAYER_TURN ||
            currentStage == BlackjackStage.DEALER_TURN
        if (!inProgress) return

        val snapshot = ActiveBlackjackSnapshot(
            stage = currentStage.name,
            selectedChip = _selectedChip.value.name,
            currentBet = _currentBet.value,
            previousBet = previousBet,
            dealerHand = _dealerHand.value.map { CardSnapshot.fromCard(it) },
            playerHands = _playerHands.value.map { BlackjackHandSnapshot.fromBlackjackHand(it) },
            activeHandIndex = _activeHandIndex.value,
            insuranceBet = _insuranceBet.value,
            statusMessage = _statusMessage.value,
            remainingShoeCards = deck.exportRemainingCards().map { CardSnapshot.fromCard(it) }
        )
        viewModelScope.launch {
            activeGameRepository.saveBlackjackState(
                snapshot = snapshot,
                isInProgress = true,
                currentBet = _playerHands.value.sumOf { it.bet }.coerceAtLeast(_currentBet.value)
            )
        }
    }

    private fun clearSavedState() {
        viewModelScope.launch {
            activeGameRepository.clearGameState(GameType.BLACKJACK)
        }
    }

    fun selectChip(denom: ChipDenom) {
        if (_stage.value == BlackjackStage.BETTING || _stage.value == BlackjackStage.ROUND_OVER) {
            _selectedChip.value = denom
            soundManager.playChipClick()
        }
    }

    fun addChipBet() {
        if (_stage.value != BlackjackStage.BETTING && _stage.value != BlackjackStage.ROUND_OVER) return
        val denomVal = _selectedChip.value.value
        val newBet = _currentBet.value + denomVal
        if (repository.canWager(newBet)) {
            _currentBet.value = newBet
            soundManager.playChipClick(denomVal)
        } else {
            soundManager.playLoss()
            _statusMessage.value = "INSUFFICIENT BALANCE"
        }
    }

    fun clearBet() {
        if (_stage.value == BlackjackStage.BETTING || _stage.value == BlackjackStage.ROUND_OVER) {
            _currentBet.value = 0L
            soundManager.playChipClear()
        }
    }

    fun rebet() {
        if (_stage.value == BlackjackStage.BETTING || _stage.value == BlackjackStage.ROUND_OVER) {
            if (repository.canWager(previousBet)) {
                _currentBet.value = previousBet
                soundManager.playChipClick(previousBet)
            }
        }
    }

    fun deal() {
        if (_stage.value != BlackjackStage.BETTING && _stage.value != BlackjackStage.ROUND_OVER) return
        val bet = _currentBet.value
        if (bet <= 0L) {
            _statusMessage.value = "PLEASE PLACE A BET"
            return
        }

        if (!repository.placeWager(bet, GameType.BLACKJACK)) {
            _statusMessage.value = "INSUFFICIENT FUNDS"
            soundManager.playLoss()
            return
        }

        previousBet = bet
        viewModelScope.launch {
            _stage.value = BlackjackStage.DEALING
            _lastPayout.value = 0L
            _insuranceBet.value = 0L
            _activeHandIndex.value = 0
            _statusMessage.value = "DEALING CARDS..."

            soundManager.playChipClick(bet)
            soundManager.playShuffleCards()
            delay(repository.scaleDelay(240))

            deck.reset()

            val pCard1 = deck.draw().copy(isFaceUp = true)
            val dCard1 = deck.draw().copy(isFaceUp = true)
            val pCard2 = deck.draw().copy(isFaceUp = true)
            val dCard2 = deck.draw().copy(isFaceUp = false) // Dealer hole card

            // Persist all 4 dealt cards & remaining shoe immediately before reveal animation
            val initialSnapshot = ActiveBlackjackSnapshot(
                stage = BlackjackStage.DEALING.name,
                selectedChip = _selectedChip.value.name,
                currentBet = bet,
                previousBet = previousBet,
                dealerHand = listOf(dCard1, dCard2).map { CardSnapshot.fromCard(it) },
                playerHands = listOf(
                    BlackjackHandSnapshot.fromBlackjackHand(
                        BlackjackHand(cards = listOf(pCard1, pCard2), bet = bet)
                    )
                ),
                activeHandIndex = 0,
                insuranceBet = 0L,
                statusMessage = "DEALING CARDS...",
                remainingShoeCards = deck.exportRemainingCards().map { CardSnapshot.fromCard(it) }
            )
            activeGameRepository.saveBlackjackState(
                snapshot = initialSnapshot,
                isInProgress = true,
                currentBet = bet
            )

            _dealerHand.value = listOf(dCard1)
            _playerHands.value = listOf(BlackjackHand(cards = listOf(pCard1), bet = bet))
            soundManager.playCardDeal()
            delay(repository.scaleDelay(150))

            _playerHands.value = listOf(BlackjackHand(cards = listOf(pCard1, pCard2), bet = bet))
            soundManager.playCardDeal()
            delay(repository.scaleDelay(150))

            _dealerHand.value = listOf(dCard1, dCard2)
            soundManager.playCardDeal()
            delay(repository.scaleDelay(200))

            val initialHand = _playerHands.value[0]
            val isNaturalBlackjack = initialHand.totalScore == 21 && initialHand.cards.size == 2

            if (isNaturalBlackjack) {
                _playerHands.value = listOf(initialHand.copy(isBlackjack = true))
            }

            // Check if dealer up-card is ACE (Offer Insurance)
            if (dCard1.rank == Rank.ACE && !isNaturalBlackjack) {
                _stage.value = BlackjackStage.INSURANCE_OFFER
                _statusMessage.value = "DEALER SHOWS ACE: TAKE INSURANCE?"
                persistActiveState()
            } else {
                if (isNaturalBlackjack) {
                    playDealerTurn()
                } else {
                    _stage.value = BlackjackStage.PLAYER_TURN
                    _statusMessage.value = "HIT OR STAND?"
                    persistActiveState()
                }
            }
        }
    }

    fun takeInsurance(take: Boolean) {
        if (_stage.value != BlackjackStage.INSURANCE_OFFER) return
        if (take) {
            val insCost = _currentBet.value / 2
            if (repository.placeWager(insCost)) {
                _insuranceBet.value = insCost
                soundManager.playChipClick()
            }
        }
        _stage.value = BlackjackStage.PLAYER_TURN
        _statusMessage.value = "HIT OR STAND?"
        persistActiveState()
    }

    fun hit() {
        if (_stage.value != BlackjackStage.PLAYER_TURN) return
        viewModelScope.launch {
            val activeIdx = _activeHandIndex.value
            val hands = _playerHands.value.toMutableList()
            val currentHand = hands[activeIdx]

            val newCard = deck.draw().copy(isFaceUp = true)
            val updatedCards = currentHand.cards + newCard
            soundManager.playCardDeal()

            val tempHand = currentHand.copy(cards = updatedCards)
            val isBust = tempHand.totalScore > 21

            hands[activeIdx] = tempHand.copy(isBusted = isBust)
            _playerHands.value = hands
            persistActiveState()

            if (isBust) {
                soundManager.playLoss()
                _statusMessage.value = "BUST! (${tempHand.totalScore})"
                delay(repository.scaleDelay(600))
                advanceToNextHandOrDealer()
            } else if (tempHand.totalScore == 21) {
                advanceToNextHandOrDealer()
            }
        }
    }

    fun stand() {
        if (_stage.value != BlackjackStage.PLAYER_TURN) return
        soundManager.playButtonClick()
        advanceToNextHandOrDealer()
    }

    fun doubleDown() {
        if (_stage.value != BlackjackStage.PLAYER_TURN) return
        val activeIdx = _activeHandIndex.value
        val hands = _playerHands.value.toMutableList()
        val currentHand = hands[activeIdx]

        if (!currentHand.canDouble) return

        if (!repository.placeWager(currentHand.bet)) {
            _statusMessage.value = "NOT ENOUGH BALANCE TO DOUBLE"
            soundManager.playLoss()
            return
        }

        viewModelScope.launch {
            soundManager.playChipClick()
            val newCard = deck.draw().copy(isFaceUp = true)
            val updatedCards = currentHand.cards + newCard
            soundManager.playCardDeal()

            val tempHand = currentHand.copy(
                cards = updatedCards,
                bet = currentHand.bet * 2,
                isDoubled = true
            )
            val isBust = tempHand.totalScore > 21
            hands[activeIdx] = tempHand.copy(isBusted = isBust, isStanding = true)
            _playerHands.value = hands
            persistActiveState()

            delay(repository.scaleDelay(600))
            advanceToNextHandOrDealer()
        }
    }

    fun split() {
        if (_stage.value != BlackjackStage.PLAYER_TURN) return
        val hands = _playerHands.value
        if (hands.size > 1) return // only 1 split allowed
        val hand = hands[0]
        if (!hand.canSplit) return

        if (!repository.placeWager(hand.bet)) {
            _statusMessage.value = "INSUFFICIENT FUNDS TO SPLIT"
            soundManager.playLoss()
            return
        }

        viewModelScope.launch {
            soundManager.playChipClick()
            val card1 = hand.cards[0]
            val card2 = hand.cards[1]

            val newCard1 = deck.draw().copy(isFaceUp = true)
            val newCard2 = deck.draw().copy(isFaceUp = true)

            val hand1 = BlackjackHand(cards = listOf(card1, newCard1), bet = hand.bet)
            val hand2 = BlackjackHand(cards = listOf(card2, newCard2), bet = hand.bet)

            _playerHands.value = listOf(hand1, hand2)
            _activeHandIndex.value = 0
            soundManager.playCardDeal()
            _statusMessage.value = "HAND 1: HIT OR STAND?"
            persistActiveState()
        }
    }

    fun surrender() {
        if (_stage.value != BlackjackStage.PLAYER_TURN) return
        val hand = _playerHands.value[0]
        if (hand.cards.size != 2) return

        val returnedAmount = hand.bet / 2
        repository.awardPayout(GameType.BLACKJACK, returnedAmount)
        _statusMessage.value = "SURRENDER: HALF BET RETURNED ($$returnedAmount)"
        _stage.value = BlackjackStage.ROUND_OVER
        soundManager.playButtonClick()
        clearSavedState()
    }

    private fun advanceToNextHandOrDealer() {
        val hands = _playerHands.value
        if (_activeHandIndex.value < hands.size - 1) {
            _activeHandIndex.value = _activeHandIndex.value + 1
            _statusMessage.value = "HAND 2: HIT OR STAND?"
            persistActiveState()
        } else {
            // Check if all player hands are busted
            val allBusted = hands.all { it.isBusted }
            if (allBusted) {
                revealHoleCard()
                finishRound()
            } else {
                playDealerTurn()
            }
        }
    }

    private fun revealHoleCard() {
        val dCards = _dealerHand.value.map { it.copy(isFaceUp = true) }
        _dealerHand.value = dCards
    }

    private fun playDealerTurn() {
        viewModelScope.launch {
            _stage.value = BlackjackStage.DEALER_TURN
            _statusMessage.value = "DEALER'S TURN..."

            revealHoleCard()
            persistActiveState()
            soundManager.playCardFlip()
            delay(repository.scaleDelay(500))

            var dCards = _dealerHand.value.toMutableList()
            var dealerTotal = calculateScore(dCards)

            while (dealerTotal < 17) {
                val newCard = deck.draw().copy(isFaceUp = true)
                dCards.add(newCard)
                _dealerHand.value = dCards.toList()
                persistActiveState()
                soundManager.playCardDeal()
                dealerTotal = calculateScore(dCards)
                delay(repository.scaleDelay(500))
            }

            finishRound()
        }
    }

    private fun finishRound() {
        val dealerCards = _dealerHand.value
        val dealerTotal = calculateScore(dealerCards)
        val dealerBust = dealerTotal > 21
        val dealerHasBlackjack = dealerTotal == 21 && dealerCards.size == 2

        var totalPayout = 0L

        // Evaluate Insurance
        if (_insuranceBet.value > 0L) {
            if (dealerHasBlackjack) {
                val insPayout = _insuranceBet.value * 3 // 2:1 profit + bet
                totalPayout += insPayout
            }
        }

        // Evaluate Player Hands
        val results = mutableListOf<String>()

        for ((idx, hand) in _playerHands.value.withIndex()) {
            val label = if (_playerHands.value.size > 1) "H${idx + 1}" else ""
            when {
                hand.isBusted -> {
                    results.add("$label BUST")
                    repository.recordLoss(GameType.BLACKJACK)
                }
                hand.isBlackjack && !dealerHasBlackjack -> {
                    val isSuited = hand.cards.size == 2 && hand.cards[0].suit == hand.cards[1].suit
                    val isHighRoller = repository.profile.value.selectedBlackjackVariant == com.example.model.BlackjackVariant.HIGH_ROLLER_VIP
                    val payout = if (isHighRoller && isSuited) {
                        hand.bet * 3 // 2:1 profit + bet
                    } else {
                        hand.bet + (hand.bet * 3 / 2) // standard 3:2
                    }
                    totalPayout += payout
                    val text = if (isHighRoller && isSuited) "$label SUITED BJ 2:1! (+$payout)" else "$label BLACKJACK! (+$payout)"
                    results.add(text)
                }
                hand.isBlackjack && dealerHasBlackjack -> {
                    totalPayout += hand.bet
                    results.add("$label PUSH")
                }
                dealerHasBlackjack -> {
                    results.add("$label DEALER BLACKJACK")
                    repository.recordLoss(GameType.BLACKJACK)
                }
                dealerBust -> {
                    val payout = hand.bet * 2
                    totalPayout += payout
                    results.add("$label DEALER BUST! (+$payout)")
                }
                hand.totalScore > dealerTotal -> {
                    val payout = hand.bet * 2
                    totalPayout += payout
                    results.add("$label WIN! (+$payout)")
                }
                hand.totalScore == dealerTotal -> {
                    totalPayout += hand.bet
                    results.add("$label PUSH")
                }
                else -> {
                    results.add("$label LOSE")
                    repository.recordLoss(GameType.BLACKJACK)
                }
            }
        }

        _lastPayout.value = totalPayout
        if (totalPayout > 0) {
            repository.awardPayout(GameType.BLACKJACK, totalPayout)
            val currentStreak = repository.profile.value.stats.currentWinStreak
            soundManager.playWinStreak(currentStreak, totalPayout)
            soundManager.playChipCollect()
        } else {
            soundManager.playLoss()
        }

        _statusMessage.value = results.joinToString(" • ")
        _stage.value = BlackjackStage.ROUND_OVER
        clearSavedState()
    }

    fun calculateScore(cards: List<Card>): Int {
        var sum = 0
        var aces = 0
        for (c in cards) {
            if (c.rank == Rank.ACE) {
                aces++
                sum += 11
            } else {
                sum += c.rank.blackjackValue
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10
            aces--
        }
        return sum
    }
}
