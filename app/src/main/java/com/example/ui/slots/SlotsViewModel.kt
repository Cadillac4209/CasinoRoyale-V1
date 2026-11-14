package com.example.ui.slots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ActiveSlotsSnapshot
import com.example.data.CasinoRepository
import com.example.model.GameType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class SlotsViewModel(
    private val repository: CasinoRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val activeGameRepository = repository.activeGameRepository

    // 9 Standard Paylines: list of row indices (0, 1, or 2) for reels 0 to 4
    val paylinePatterns = listOf(
        listOf(1, 1, 1, 1, 1), // Line 1: Center
        listOf(0, 0, 0, 0, 0), // Line 2: Top
        listOf(2, 2, 2, 2, 2), // Line 3: Bottom
        listOf(0, 1, 2, 1, 0), // Line 4: V
        listOf(2, 1, 0, 1, 2), // Line 5: Inverted V
        listOf(0, 0, 1, 2, 2), // Line 6: Zigzag top
        listOf(2, 2, 1, 0, 0), // Line 7: Zigzag bottom
        listOf(1, 0, 0, 0, 1), // Line 8: Plateau top
        listOf(1, 2, 2, 2, 1)  // Line 9: Valley bottom
    )

    // Current 5 reels x 3 rows matrix: grid[col][row]
    private val _grid = MutableStateFlow(
        List(5) {
            List(3) { randomSymbol() }
        }
    )
    val grid: StateFlow<List<List<SlotSymbol>>> = _grid.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _reelsSpinning = MutableStateFlow(List(5) { false })
    val reelsSpinning: StateFlow<List<Boolean>> = _reelsSpinning.asStateFlow()

    private val _activeLines = MutableStateFlow(9) // 1 to 9
    val activeLines: StateFlow<Int> = _activeLines.asStateFlow()

    private val _betPerLine = MutableStateFlow(10L) // $10 per line
    val betPerLine: StateFlow<Long> = _betPerLine.asStateFlow()

    private val _winningLines = MutableStateFlow<List<WinningLine>>(emptyList())
    val winningLines: StateFlow<List<WinningLine>> = _winningLines.asStateFlow()

    private val _lastWin = MutableStateFlow(0L)
    val lastWin: StateFlow<Long> = _lastWin.asStateFlow()

    private val _freeSpinsRemaining = MutableStateFlow(0)
    val freeSpinsRemaining: StateFlow<Int> = _freeSpinsRemaining.asStateFlow()

    private val _isAutoSpin = MutableStateFlow(false)
    val isAutoSpin: StateFlow<Boolean> = _isAutoSpin.asStateFlow()

    private val _celebrationWin = MutableStateFlow<Long?>(null)
    val celebrationWin: StateFlow<Long?> = _celebrationWin.asStateFlow()

    private val _statusText = MutableStateFlow("SELECT BET & SPIN!")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    val totalBet: Long get() = _activeLines.value * _betPerLine.value

    private var autoSpinJob: Job? = null

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        viewModelScope.launch {
            val saved = activeGameRepository.loadSlotsState() ?: return@launch
            _activeLines.value = saved.activeLines.coerceIn(1, 9)
            _betPerLine.value = saved.betPerLine
            _freeSpinsRemaining.value = saved.freeSpinsRemaining.coerceAtLeast(0)
            _lastWin.value = saved.lastWin

            if (saved.isSpinning && saved.finalGrid.size == 5) {
                val targetGrid = saved.toFinalGridSymbols()
                _grid.value = targetGrid
                _reelsSpinning.value = List(5) { false }
                settleSpin(finalGrid = targetGrid, isFreeSpin = saved.isFreeSpinActive, resumed = true)
            } else if (saved.freeSpinsRemaining > 0 && saved.grid.size == 5) {
                _grid.value = saved.toGridSymbols()
                _statusText.value = "RESUMED • ${saved.freeSpinsRemaining} FREE SPINS LEFT!"
            }
        }
    }

    private suspend fun persistSlotsState(
        finalGrid: List<List<SlotSymbol>>,
        isSpinning: Boolean,
        isFreeSpinActive: Boolean,
        costPaid: Long
    ) {
        val shouldStayActive = isSpinning || _freeSpinsRemaining.value > 0
        if (!shouldStayActive) {
            activeGameRepository.clearGameState(GameType.SLOTS)
            return
        }
        val snapshot = ActiveSlotsSnapshot(
            grid = ActiveSlotsSnapshot.symbolsToNames(_grid.value),
            finalGrid = ActiveSlotsSnapshot.symbolsToNames(finalGrid),
            activeLines = _activeLines.value,
            betPerLine = _betPerLine.value,
            freeSpinsRemaining = _freeSpinsRemaining.value,
            lastWin = _lastWin.value,
            isSpinning = isSpinning,
            isFreeSpinActive = isFreeSpinActive,
            costPaid = costPaid,
            statusText = _statusText.value
        )
        activeGameRepository.saveSlotsState(
            snapshot = snapshot,
            isInProgress = true,
            currentBet = totalBet
        )
    }

    private fun randomSymbol(): SlotSymbol {
        val roll = Random.nextInt(100)
        return when {
            roll < 4 -> SlotSymbol.WILD
            roll < 9 -> SlotSymbol.SEVEN
            roll < 16 -> SlotSymbol.BELL
            roll < 26 -> SlotSymbol.BAR
            roll < 40 -> SlotSymbol.HORSESHOE
            roll < 60 -> SlotSymbol.CLOVER
            roll < 90 -> SlotSymbol.CHERRY
            else -> SlotSymbol.SCATTER
        }
    }

    fun setLines(lines: Int) {
        if (!_isSpinning.value && _freeSpinsRemaining.value == 0) {
            _activeLines.value = lines.coerceIn(1, 9)
            soundManager.playChipClick()
        }
    }

    fun setBetPerLine(amount: Long) {
        if (!_isSpinning.value && _freeSpinsRemaining.value == 0) {
            _betPerLine.value = amount
            soundManager.playChipClick()
        }
    }

    fun toggleAutoSpin() {
        if (_isAutoSpin.value) {
            _isAutoSpin.value = false
            autoSpinJob?.cancel()
        } else {
            _isAutoSpin.value = true
            spin()
        }
    }

    fun dismissCelebration() {
        _celebrationWin.value = null
    }

    fun spin() {
        if (_isSpinning.value) return

        val isFreeSpin = _freeSpinsRemaining.value > 0
        val cost = if (isFreeSpin) 0L else totalBet

        if (!isFreeSpin && !repository.placeWager(cost, GameType.SLOTS)) {
            _statusText.value = "INSUFFICIENT FUNDS FOR SPIN"
            soundManager.playLoss()
            _isAutoSpin.value = false
            return
        }

        viewModelScope.launch {
            _isSpinning.value = true
            _winningLines.value = emptyList()
            _celebrationWin.value = null
            _statusText.value = if (isFreeSpin) "FREE SPIN (${_freeSpinsRemaining.value} LEFT)!" else "SPINNING..."

            // Set all reels spinning
            _reelsSpinning.value = List(5) { true }
            soundManager.playChipClick(cost)
            soundManager.playSpinReels()

            // Generate target final symbols for all reels & persist immediately in Room
            val finalGrid = List(5) { List(3) { randomSymbol() } }
            persistSlotsState(
                finalGrid = finalGrid,
                isSpinning = true,
                isFreeSpinActive = isFreeSpin,
                costPaid = cost
            )

            // Stagger stop of each reel
            val activeGrid = _grid.value.map { it.toMutableList() }.toMutableList()

            for (reelIndex in 0 until 5) {
                // Blur spin reel scaled by GameSpeed
                val reelSpinDuration = repository.scaleDelay(250L + (reelIndex * 150L))
                val stepDelay = repository.scaleDelay(50L)
                val iterations = (reelSpinDuration / stepDelay.coerceAtLeast(15L)).toInt().coerceAtLeast(2)
                for (iter in 0 until iterations) {
                    activeGrid[reelIndex] = mutableListOf(randomSymbol(), randomSymbol(), randomSymbol())
                    _grid.value = activeGrid.map { it.toList() }
                    soundManager.playReelClick()
                    delay(stepDelay)
                }
                // Lock reel with final symbols
                activeGrid[reelIndex] = finalGrid[reelIndex].toMutableList()
                _grid.value = activeGrid.map { it.toList() }

                val currentSpinning = _reelsSpinning.value.toMutableList()
                currentSpinning[reelIndex] = false
                _reelsSpinning.value = currentSpinning
                soundManager.playReelStop(reelIndex)
                delay(repository.scaleDelay(60))
            }

            settleSpin(finalGrid = finalGrid, isFreeSpin = isFreeSpin, resumed = false)

            // Check if AutoSpin should continue
            if (_isAutoSpin.value) {
                delay(repository.scaleDelay(1200))
                if (_isAutoSpin.value) {
                    spin()
                }
            } else if (_freeSpinsRemaining.value > 0) {
                delay(repository.scaleDelay(1200))
                spin()
            }
        }
    }

    private suspend fun settleSpin(
        finalGrid: List<List<SlotSymbol>>,
        isFreeSpin: Boolean,
        resumed: Boolean
    ) {
        val results = evaluateSpins(finalGrid)
        _winningLines.value = results.winningLines
        _lastWin.value = results.totalWon

        // Check scatter triggers for Free Spins
        var scatterCount = 0
        for (col in 0 until 5) {
            for (row in 0 until 3) {
                if (finalGrid[col][row].isScatter) scatterCount++
            }
        }

        if (scatterCount >= 3) {
            val awarded = 10
            _freeSpinsRemaining.value += awarded
            _statusText.value = "BONUS! $scatterCount SCATTERS = $awarded FREE SPINS!"
            soundManager.playJackpot()
        }

        if (isFreeSpin) {
            _freeSpinsRemaining.value = (_freeSpinsRemaining.value - 1).coerceAtLeast(0)
        }

        if (results.totalWon > 0) {
            val finalPayout = if (isFreeSpin) results.totalWon * 2 else results.totalWon
            val isJackpot = finalPayout >= totalBet * 50
            repository.awardPayout(GameType.SLOTS, finalPayout, isJackpot = isJackpot)
            _statusText.value = if (resumed) "RESUMED SPIN • WIN $${finalPayout}!" else "WIN $${finalPayout}!"

            val currentStreak = repository.profile.value.stats.currentWinStreak
            soundManager.playWinStreak(currentStreak, finalPayout)
            soundManager.playChipCollect()

            if (finalPayout >= totalBet * 20) {
                _celebrationWin.value = finalPayout
            }
        } else {
            if (!isFreeSpin) {
                repository.recordLoss(GameType.SLOTS)
                _statusText.value = if (resumed) "RESUMED SPIN • NO WIN" else "NO WIN - SPIN AGAIN"
            } else if (_freeSpinsRemaining.value > 0) {
                _statusText.value = "FREE SPINS LEFT: ${_freeSpinsRemaining.value}"
            }
        }

        _isSpinning.value = false
        persistSlotsState(
            finalGrid = finalGrid,
            isSpinning = false,
            isFreeSpinActive = false,
            costPaid = 0L
        )
    }

    private data class SpinEvaluation(
        val winningLines: List<WinningLine>,
        val totalWon: Long
    )

    private fun evaluateSpins(grid: List<List<SlotSymbol>>): SpinEvaluation {
        val winners = mutableListOf<WinningLine>()
        var total = 0L

        val linesToEvaluate = _activeLines.value.coerceIn(1, 9)

        for (lineIdx in 0 until linesToEvaluate) {
            val pattern = paylinePatterns[lineIdx]
            val symbolsOnLine = (0 until 5).map { col ->
                val row = pattern[col]
                grid[col][row]
            }

            // Find matching symbol from left to right (Wilds substitute!)
            val firstNonWild = symbolsOnLine.firstOrNull { !it.isWild && !it.isScatter } ?: SlotSymbol.WILD
            var matchCount = 0
            for (sym in symbolsOnLine) {
                if (sym == firstNonWild || sym.isWild) {
                    matchCount++
                } else {
                    break
                }
            }

            if (matchCount >= 3) {
                val baseMultiplier = firstNonWild.payoutForCount(matchCount)
                if (baseMultiplier > 0) {
                    val hasWild = symbolsOnLine.take(matchCount).any { it.isWild }
                    val variantMultiplier = if (hasWild && repository.profile.value.selectedSlotsVariant == com.example.model.SlotsVariant.NEON_FRENZY) 3 else 1
                    val linePayout = _betPerLine.value * baseMultiplier * variantMultiplier
                    winners.add(
                        WinningLine(
                            lineIndex = lineIdx + 1,
                            symbol = firstNonWild,
                            matchCount = matchCount,
                            winAmount = linePayout
                        )
                    )
                    total += linePayout
                }
            }
        }

        return SpinEvaluation(winners, total)
    }
}
