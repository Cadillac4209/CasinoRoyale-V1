package com.example.ui.videopoker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ActiveVideoPokerSnapshot
import com.example.data.CardSnapshot
import com.example.data.CasinoRepository
import com.example.data.DoubleUpGameSnapshot
import com.example.model.Card
import com.example.model.Deck
import com.example.model.GameType
import com.example.ui.components.ChipDenom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VideoPokerState {
    BETTING,
    DRAW_READY,
    ROUND_OVER,
    DOUBLE_UP_READY,
    DOUBLE_UP_ACTIVE
}

data class DoubleUpGame(
    val dealerCard: Card,
    val options: List<Card>,
    val selectedIndex: Int? = null,
    val resultText: String = "",
    val isWin: Boolean? = null
)

class VideoPokerViewModel(
    private val repository: CasinoRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val activeGameRepository = repository.activeGameRepository
    private val deck = Deck(1)

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _heldIndices = MutableStateFlow<Set<Int>>(emptySet())
    val heldIndices: StateFlow<Set<Int>> = _heldIndices.asStateFlow()

    private val _coinsBet = MutableStateFlow(1) // 1 to 5
    val coinsBet: StateFlow<Int> = _coinsBet.asStateFlow()

    private val _chipDenom = MutableStateFlow(ChipDenom.TEN)
    val chipDenom: StateFlow<ChipDenom> = _chipDenom.asStateFlow()

    private val _gameState = MutableStateFlow(VideoPokerState.BETTING)
    val gameState: StateFlow<VideoPokerState> = _gameState.asStateFlow()

    private val _handRank = MutableStateFlow<PokerHandRank?>(null)
    val handRank: StateFlow<PokerHandRank?> = _handRank.asStateFlow()

    private val _winAmount = MutableStateFlow(0L)
    val winAmount: StateFlow<Long> = _winAmount.asStateFlow()

    private val _statusMessage = MutableStateFlow("CHOOSE BET & DEAL")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _doubleUpGame = MutableStateFlow<DoubleUpGame?>(null)
    val doubleUpGame: StateFlow<DoubleUpGame?> = _doubleUpGame.asStateFlow()

    val totalBet: Long get() = _chipDenom.value.value * _coinsBet.value

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        viewModelScope.launch {
            val saved = activeGameRepository.loadVideoPokerState() ?: return@launch
            val savedState = runCatching { VideoPokerState.valueOf(saved.gameState) }.getOrNull() ?: return@launch
            if (savedState == VideoPokerState.BETTING || savedState == VideoPokerState.ROUND_OVER) {
                return@launch
            }

            _coinsBet.value = saved.coinsBet.coerceIn(1, 5)
            runCatching { ChipDenom.valueOf(saved.chipDenom) }.getOrNull()?.let {
                _chipDenom.value = it
            }
            _cards.value = saved.cards.map { it.toCard() }
            _heldIndices.value = saved.heldIndices.toSet()
            _winAmount.value = saved.winAmount
            _doubleUpGame.value = saved.doubleUpGame?.toDoubleUpGame()
            deck.restoreCards(saved.remainingDeckCards.map { it.toCard() })

            val variant = repository.profile.value.selectedPokerVariant
            _handRank.value = if (_cards.value.size == 5 && savedState != VideoPokerState.DRAW_READY) {
                PokerHandEvaluator.evaluate(_cards.value, variant)
            } else {
                null
            }

            _gameState.value = savedState
            _statusMessage.value = when (savedState) {
                VideoPokerState.DRAW_READY -> "SESSION RESUMED • HOLD CARDS & DRAW"
                VideoPokerState.DOUBLE_UP_READY -> "SESSION RESUMED • DOUBLE UP OR COLLECT?"
                VideoPokerState.DOUBLE_UP_ACTIVE -> "SESSION RESUMED • PICK A HIGHER CARD!"
                else -> saved.statusMessage
            }
        }
    }

    private fun persistActiveState() {
        val state = _gameState.value
        val inProgress = state == VideoPokerState.DRAW_READY ||
            state == VideoPokerState.DOUBLE_UP_READY ||
            state == VideoPokerState.DOUBLE_UP_ACTIVE
        if (!inProgress) return

        val snapshot = ActiveVideoPokerSnapshot(
            gameState = state.name,
            cards = _cards.value.map { CardSnapshot.fromCard(it) },
            heldIndices = _heldIndices.value.toList(),
            coinsBet = _coinsBet.value,
            chipDenom = _chipDenom.value.name,
            winAmount = _winAmount.value,
            statusMessage = _statusMessage.value,
            doubleUpGame = _doubleUpGame.value?.let { DoubleUpGameSnapshot.fromDoubleUpGame(it) },
            remainingDeckCards = deck.exportRemainingCards().map { CardSnapshot.fromCard(it) }
        )
        viewModelScope.launch {
            activeGameRepository.saveVideoPokerState(
                snapshot = snapshot,
                isInProgress = true,
                currentBet = totalBet
            )
        }
    }

    private fun clearSavedState() {
        viewModelScope.launch {
            activeGameRepository.clearGameState(GameType.VIDEO_POKER)
        }
    }

    fun setChipDenom(denom: ChipDenom) {
        if (_gameState.value == VideoPokerState.BETTING || _gameState.value == VideoPokerState.ROUND_OVER) {
            _chipDenom.value = denom
            soundManager.playChipClick(denom.value)
        }
    }

    fun betOne() {
        if (_gameState.value == VideoPokerState.BETTING || _gameState.value == VideoPokerState.ROUND_OVER) {
            _coinsBet.value = if (_coinsBet.value < 5) _coinsBet.value + 1 else 1
            soundManager.playChipClick(_chipDenom.value.value)
        }
    }

    fun betMax() {
        if (_gameState.value == VideoPokerState.BETTING || _gameState.value == VideoPokerState.ROUND_OVER) {
            _coinsBet.value = 5
            soundManager.playChipClick(_chipDenom.value.value * 5)
            deal()
        }
    }

    fun toggleHold(index: Int) {
        if (_gameState.value != VideoPokerState.DRAW_READY) return
        val current = _heldIndices.value.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        _heldIndices.value = current
        soundManager.playButtonClick()
        persistActiveState()
    }

    fun deal() {
        if (_gameState.value != VideoPokerState.BETTING && _gameState.value != VideoPokerState.ROUND_OVER) return

        val bet = totalBet
        if (!repository.placeWager(bet, GameType.VIDEO_POKER)) {
            _statusMessage.value = "INSUFFICIENT FUNDS"
            soundManager.playLoss()
            return
        }

        viewModelScope.launch {
            _gameState.value = VideoPokerState.BETTING
            _handRank.value = null
            _winAmount.value = 0L
            _heldIndices.value = emptySet()
            _doubleUpGame.value = null

            soundManager.playChipClick(bet)
            soundManager.playShuffleCards()
            delay(repository.scaleDelay(220))

            deck.reset()

            // Pre-draw all 5 cards and persist immediately so closing mid-deal resumes cleanly
            val preDealt = List(5) { deck.draw() }
            val preDealSnapshot = ActiveVideoPokerSnapshot(
                gameState = VideoPokerState.DRAW_READY.name,
                cards = preDealt.map { CardSnapshot.fromCard(it) },
                heldIndices = emptyList(),
                coinsBet = _coinsBet.value,
                chipDenom = _chipDenom.value.name,
                winAmount = 0L,
                statusMessage = "HOLD CARDS & DRAW",
                doubleUpGame = null,
                remainingDeckCards = deck.exportRemainingCards().map { CardSnapshot.fromCard(it) }
            )
            activeGameRepository.saveVideoPokerState(
                snapshot = preDealSnapshot,
                isInProgress = true,
                currentBet = bet
            )

            val dealtCards = mutableListOf<Card>()
            _statusMessage.value = "DEALING..."
            for (card in preDealt) {
                dealtCards.add(card)
                _cards.value = dealtCards.toList()
                soundManager.playCardDeal()
                delay(repository.scaleDelay(120))
            }

            _gameState.value = VideoPokerState.DRAW_READY
            _statusMessage.value = "HOLD CARDS & DRAW"
        }
    }

    fun draw() {
        if (_gameState.value != VideoPokerState.DRAW_READY) return

        viewModelScope.launch {
            _statusMessage.value = "DRAWING..."
            val currentCards = _cards.value.toMutableList()
            for (i in 0 until 5) {
                if (!(_heldIndices.value.contains(i))) {
                    val newCard = deck.draw()
                    currentCards[i] = newCard
                    _cards.value = currentCards.toList()
                    soundManager.playCardFlip()
                    delay(repository.scaleDelay(140))
                }
            }

            // Evaluate hand with current active variant (Jacks or Better or Deuces Wild)
            val variant = repository.profile.value.selectedPokerVariant
            val rank = PokerHandEvaluator.evaluate(currentCards, variant)
            _handRank.value = rank

            if (rank != PokerHandRank.NOTHING) {
                // If royal flush with 5 coins bet, bonus payout
                val multiplier = if ((rank == PokerHandRank.ROYAL_FLUSH || rank == PokerHandRank.NATURAL_ROYAL_FLUSH) && _coinsBet.value == 5) 800 else rank.multiplier
                val payout = totalBet * multiplier
                _winAmount.value = payout
                val isJackpot = rank == PokerHandRank.ROYAL_FLUSH || rank == PokerHandRank.NATURAL_ROYAL_FLUSH || rank == PokerHandRank.FOUR_DEUCES
                repository.awardPayout(GameType.VIDEO_POKER, payout, isJackpot = isJackpot)
                _statusMessage.value = "${rank.displayName} - WIN $$payout!"

                val currentStreak = repository.profile.value.stats.currentWinStreak
                soundManager.playWinStreak(currentStreak, payout)
                soundManager.playChipCollect()
                _gameState.value = VideoPokerState.DOUBLE_UP_READY
                persistActiveState()
            } else {
                repository.recordLoss(GameType.VIDEO_POKER)
                _statusMessage.value = "GAME OVER"
                soundManager.playLoss()
                _gameState.value = VideoPokerState.ROUND_OVER
                clearSavedState()
            }
        }
    }

    fun startDoubleUp() {
        if (_gameState.value != VideoPokerState.DOUBLE_UP_READY) return
        val gambleDeck = Deck(1)
        val dealer = gambleDeck.draw().copy(isFaceUp = true)
        val options = listOf(
            gambleDeck.draw().copy(isFaceUp = false),
            gambleDeck.draw().copy(isFaceUp = false),
            gambleDeck.draw().copy(isFaceUp = false),
            gambleDeck.draw().copy(isFaceUp = false)
        )
        _doubleUpGame.value = DoubleUpGame(dealerCard = dealer, options = options)
        _gameState.value = VideoPokerState.DOUBLE_UP_ACTIVE
        _statusMessage.value = "PICK A HIGHER CARD TO DOUBLE!"
        soundManager.playCardDeal()
        persistActiveState()
    }

    fun selectDoubleUpCard(index: Int) {
        val game = _doubleUpGame.value ?: return
        if (game.selectedIndex != null) return

        val revealedOptions = game.options.map { card ->
            card.copy(isFaceUp = true)
        }
        val chosen = revealedOptions[index]
        val dealerRank = game.dealerCard.rank.pokerValue
        val chosenRank = chosen.rank.pokerValue

        val isWin = chosenRank > dealerRank
        val isPush = chosenRank == dealerRank

        soundManager.playCardFlip()

        if (isWin) {
            val doubled = _winAmount.value * 2
            val profitAdded = _winAmount.value
            _winAmount.value = doubled
            repository.awardPayout(GameType.VIDEO_POKER, profitAdded)
            _doubleUpGame.value = game.copy(
                options = revealedOptions,
                selectedIndex = index,
                resultText = "YOU WIN! $$doubled",
                isWin = true
            )
            _statusMessage.value = "YOU WON! DOUBLE AGAIN OR COLLECT?"
            soundManager.playWin()
            _gameState.value = VideoPokerState.DOUBLE_UP_READY
            persistActiveState()
        } else if (isPush) {
            _doubleUpGame.value = game.copy(
                options = revealedOptions,
                selectedIndex = index,
                resultText = "PUSH - TIE GAME",
                isWin = null
            )
            _statusMessage.value = "PUSH! TRY AGAIN OR COLLECT"
            _gameState.value = VideoPokerState.DOUBLE_UP_READY
            persistActiveState()
        } else {
            _doubleUpGame.value = game.copy(
                options = revealedOptions,
                selectedIndex = index,
                resultText = "DEALER WINS",
                isWin = false
            )
            _winAmount.value = 0L
            _statusMessage.value = "LOST DOUBLE UP - GAME OVER"
            soundManager.playLoss()
            _gameState.value = VideoPokerState.ROUND_OVER
            clearSavedState()
        }
    }

    fun collectWin() {
        _gameState.value = VideoPokerState.ROUND_OVER
        _doubleUpGame.value = null
        _statusMessage.value = "COLLECTED ${_winAmount.value}! PLACE BET"
        soundManager.playChipCollect()
        clearSavedState()
    }
}
