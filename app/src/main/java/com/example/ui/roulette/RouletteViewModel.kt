package com.example.ui.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ActiveRouletteSnapshot
import com.example.data.CasinoRepository
import com.example.data.RouletteBetSnapshot
import com.example.model.GameType
import com.example.ui.components.ChipDenom
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RouletteViewModel(
    private val repository: CasinoRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val activeGameRepository = repository.activeGameRepository

    private val _selectedChip = MutableStateFlow(ChipDenom.TWENTY_FIVE)
    val selectedChip: StateFlow<ChipDenom> = _selectedChip.asStateFlow()

    private val _bets = MutableStateFlow<Map<String, ActiveRouletteBet>>(emptyMap())
    val bets: StateFlow<Map<String, ActiveRouletteBet>> = _bets.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _wheelRotation = MutableStateFlow(0f)
    val wheelRotation: StateFlow<Float> = _wheelRotation.asStateFlow()

    private val _ballRotation = MutableStateFlow(0f)
    val ballRotation: StateFlow<Float> = _ballRotation.asStateFlow()

    private val _winningNumber = MutableStateFlow<Int?>(null)
    val winningNumber: StateFlow<Int?> = _winningNumber.asStateFlow()

    private val _history = MutableStateFlow<List<Int>>(listOf(17, 24, 0, 32, 15))
    val history: StateFlow<List<Int>> = _history.asStateFlow()

    private val _statusMessage = MutableStateFlow("PLACE YOUR CHIPS ON THE TABLE")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastPayout = MutableStateFlow(0L)
    val lastPayout: StateFlow<Long> = _lastPayout.asStateFlow()

    private var previousBets: Map<String, ActiveRouletteBet> = emptyMap()

    val totalBet: Long get() = _bets.value.values.sumOf { it.amount }

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        viewModelScope.launch {
            val saved = activeGameRepository.loadRouletteState() ?: return@launch
            runCatching { ChipDenom.valueOf(saved.selectedChip) }.getOrNull()?.let {
                _selectedChip.value = it
            }
            if (saved.history.isNotEmpty()) {
                _history.value = saved.history
            }
            val restoredBets = saved.bets.associate { it.key to it.toActiveRouletteBet() }
            _bets.value = restoredBets
            if (restoredBets.isNotEmpty()) {
                previousBets = restoredBets
            }

            if (saved.isSpinning && saved.targetNumber != null && restoredBets.isNotEmpty()) {
                settleSpin(
                    targetNumber = saved.targetNumber,
                    wageredAmount = saved.currentTotalBet,
                    resumed = true
                )
            } else if (restoredBets.isNotEmpty()) {
                _statusMessage.value = "RESTORED TABLE BETS • TAP SPIN"
            }
        }
    }

    private fun persistRouletteState(isSpinning: Boolean, targetNumber: Int?) {
        val snapshot = ActiveRouletteSnapshot(
            selectedChip = _selectedChip.value.name,
            bets = _bets.value.entries.map { (k, v) -> RouletteBetSnapshot.fromEntry(k, v) },
            isSpinning = isSpinning,
            targetNumber = targetNumber,
            currentTotalBet = totalBet,
            history = _history.value,
            statusMessage = _statusMessage.value
        )
        viewModelScope.launch {
            if (!isSpinning && _bets.value.isEmpty()) {
                activeGameRepository.clearGameState(GameType.ROULETTE)
            } else {
                activeGameRepository.saveRouletteState(
                    snapshot = snapshot,
                    isInProgress = isSpinning,
                    currentBet = totalBet
                )
            }
        }
    }

    fun selectChip(denom: ChipDenom) {
        if (!_isSpinning.value) {
            _selectedChip.value = denom
            soundManager.playChipClick()
        }
    }

    fun placeBet(key: String, type: RouletteBetType) {
        if (_isSpinning.value) return
        val denomVal = _selectedChip.value.value
        val currentBetForCell = _bets.value[key]?.amount ?: 0L
        val newCellBet = currentBetForCell + denomVal
        val newTotal = totalBet + denomVal

        if (repository.canWager(newTotal)) {
            val updated = _bets.value.toMutableMap()
            updated[key] = ActiveRouletteBet(type = type, amount = newCellBet)
            _bets.value = updated
            soundManager.playChipClick(denomVal)
            persistRouletteState(isSpinning = false, targetNumber = null)
        } else {
            _statusMessage.value = "INSUFFICIENT FUNDS"
            soundManager.playLoss()
        }
    }

    fun clearBets() {
        if (!_isSpinning.value) {
            _bets.value = emptyMap()
            soundManager.playChipClear()
            persistRouletteState(isSpinning = false, targetNumber = null)
        }
    }

    fun doubleBets() {
        if (!_isSpinning.value && _bets.value.isNotEmpty()) {
            val doubled = _bets.value.mapValues { (_, bet) ->
                bet.copy(amount = bet.amount * 2)
            }
            val newTotal = doubled.values.sumOf { it.amount }
            if (repository.canWager(newTotal)) {
                _bets.value = doubled
                soundManager.playChipClick(newTotal)
                persistRouletteState(isSpinning = false, targetNumber = null)
            }
        }
    }

    fun rebet() {
        if (!_isSpinning.value && previousBets.isNotEmpty()) {
            val total = previousBets.values.sumOf { it.amount }
            if (repository.canWager(total)) {
                _bets.value = previousBets
                soundManager.playChipClick(total)
                persistRouletteState(isSpinning = false, targetNumber = null)
            }
        }
    }

    fun spin() {
        if (_isSpinning.value) return
        val currentTotalBet = totalBet
        if (currentTotalBet <= 0) {
            _statusMessage.value = "PLEASE PLACE AT LEAST ONE BET"
            return
        }

        if (!repository.placeWager(currentTotalBet, GameType.ROULETTE)) {
            _statusMessage.value = "INSUFFICIENT FUNDS"
            soundManager.playLoss()
            return
        }

        previousBets = _bets.value

        viewModelScope.launch {
            _isSpinning.value = true
            _winningNumber.value = null
            _lastPayout.value = 0L
            _statusMessage.value = "NO MORE BETS! WHEEL SPINNING..."

            soundManager.playChipClick(currentTotalBet)
            soundManager.playRouletteSpin()

            // Pick winning number and immediately persist in-flight spin to Room
            val targetNumber = RouletteData.WHEEL_NUMBERS[Random.nextInt(RouletteData.WHEEL_NUMBERS.size)]
            val spinSnapshot = ActiveRouletteSnapshot(
                selectedChip = _selectedChip.value.name,
                bets = _bets.value.entries.map { (k, v) -> RouletteBetSnapshot.fromEntry(k, v) },
                isSpinning = true,
                targetNumber = targetNumber,
                currentTotalBet = currentTotalBet,
                history = _history.value,
                statusMessage = _statusMessage.value
            )
            activeGameRepository.saveRouletteState(
                snapshot = spinSnapshot,
                isInProgress = true,
                currentBet = currentTotalBet
            )

            // Wheel spins clockwise, ball spins counter-clockwise
            val totalSpins = 5 + Random.nextFloat() * 2
            val durationMs = repository.scaleDelay(3800L)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < durationMs) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easeOut = 1f - (1f - progress) * (1f - progress)

                _wheelRotation.value = (easeOut * totalSpins * 360f) % 360f
                _ballRotation.value = (-(1f - easeOut) * (totalSpins + 2) * 360f) % 360f

                if (progress > 0.6f && (elapsed % 180 < 30)) {
                    soundManager.playBallBounce()
                }
                delay(30)
            }

            soundManager.playBallBounce()
            delay(repository.scaleDelay(300))

            settleSpin(
                targetNumber = targetNumber,
                wageredAmount = currentTotalBet,
                resumed = false
            )
        }
    }

    private fun settleSpin(
        targetNumber: Int,
        wageredAmount: Long,
        resumed: Boolean
    ) {
        _winningNumber.value = targetNumber
        val color = RouletteData.getColor(targetNumber)
        val colorName = when (color) {
            RouletteColor.RED -> "RED"
            RouletteColor.BLACK -> "BLACK"
            RouletteColor.GREEN -> "GREEN"
        }

        val isFrench = repository.profile.value.selectedRouletteVariant == com.example.model.RouletteVariant.FRENCH_LA_PARTAGE
        var wonAmount = 0L
        for (bet in _bets.value.values) {
            if (bet.type.isWinning(targetNumber)) {
                val profit = bet.amount * bet.type.payoutRatio
                wonAmount += (bet.amount + profit)
            } else if (targetNumber == 0 && isFrench && (bet.type is RouletteBetType.Red || bet.type is RouletteBetType.Black || bet.type is RouletteBetType.Even || bet.type is RouletteBetType.Odd || bet.type is RouletteBetType.Low || bet.type is RouletteBetType.High)) {
                wonAmount += (bet.amount / 2)
            }
        }

        _history.value = listOf(targetNumber) + _history.value.take(9)
        val prefix = if (resumed) "RESUMED SPIN • " else ""

        if (wonAmount > 0) {
            _lastPayout.value = wonAmount
            repository.awardPayout(GameType.ROULETTE, wonAmount, isJackpot = wonAmount >= wageredAmount * 30)
            _statusMessage.value = "$prefix$targetNumber $colorName! YOU WON $${wonAmount}!"
            val currentStreak = repository.profile.value.stats.currentWinStreak
            soundManager.playWinStreak(currentStreak, wonAmount)
            soundManager.playChipCollect()
        } else {
            repository.recordLoss(GameType.ROULETTE)
            _statusMessage.value = "$prefix$targetNumber $colorName. NO WIN"
            soundManager.playLoss()
        }

        _isSpinning.value = false
        persistRouletteState(isSpinning = false, targetNumber = null)
    }
}
