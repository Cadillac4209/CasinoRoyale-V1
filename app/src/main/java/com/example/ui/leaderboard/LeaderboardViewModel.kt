package com.example.ui.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeaderboardRepository
import com.example.model.LeaderboardEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Success(val players: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}

data class PlayerRankSummary(
    val rank: Int? = null,
    val totalXp: Long = 0L,
    val xpToNextRank: Long? = null,
    val nextPlayerName: String? = null
)

class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
    val currentUserId: String
) : ViewModel() {

    val leaderboardState: StateFlow<LeaderboardUiState> = repository.observeTop100Players()
        .map<List<LeaderboardEntry>, LeaderboardUiState> { entries ->
            LeaderboardUiState.Success(entries)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing global leaderboard", rootError)
            emit(LeaderboardUiState.Error(rootError.message ?: "Unable to load global leaderboard"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LeaderboardUiState.Loading
        )

    private val _currentEntryError = MutableStateFlow<String?>(null)
    val currentEntryError: StateFlow<String?> = _currentEntryError.asStateFlow()

    val currentPlayerEntry: StateFlow<LeaderboardEntry?> = repository.observeCurrentPlayerEntry()
        .onEach { _currentEntryError.value = null }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing current player Firestore profile", rootError)
            _currentEntryError.value = rootError.message ?: "Unable to load Firestore profile"
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private val _playerRankSummary = MutableStateFlow(PlayerRankSummary())
    val playerRankSummary: StateFlow<PlayerRankSummary> = _playerRankSummary.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun syncPlayerStats(
        displayName: String,
        totalXp: Long,
        level: Int,
        vipTitle: String,
        bestWinStreak: Int,
        biggestWin: Long,
        totalRoundsPlayed: Int,
        royalePoints: Long = 0L,
        lifetimeRoyalePoints: Long = 0L,
        loyaltyTier: String = "Ember Member"
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.upsertPlayerScore(
                    displayName = displayName,
                    totalXp = totalXp,
                    level = level,
                    vipTitle = vipTitle,
                    bestWinStreak = bestWinStreak,
                    biggestWin = biggestWin,
                    totalRoundsPlayed = totalRoundsPlayed,
                    royalePoints = royalePoints,
                    lifetimeRoyalePoints = lifetimeRoyalePoints,
                    loyaltyTier = loyaltyTier
                )
                refreshRelativeRank(totalXp)
                _syncMessage.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync player stats to leaderboard", e)
                _syncMessage.value = e.localizedMessage ?: "Failed to sync leaderboard rank"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refreshRelativeRank(currentTotalXp: Long) {
        viewModelScope.launch {
            try {
                val rank = repository.getPlayerRelativeRank(currentTotalXp)
                val currentList = (leaderboardState.value as? LeaderboardUiState.Success)?.players.orEmpty()
                val abovePlayer = if (rank > 1 && rank - 2 in currentList.indices) {
                    currentList[rank - 2]
                } else {
                    currentList.lastOrNull { it.totalXp > currentTotalXp }
                }
                val xpGap = abovePlayer?.let { (it.totalXp - currentTotalXp + 1L).coerceAtLeast(1L) }

                _playerRankSummary.value = PlayerRankSummary(
                    rank = rank,
                    totalXp = currentTotalXp,
                    xpToNextRank = xpGap,
                    nextPlayerName = abovePlayer?.displayName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to compute relative rank", e)
            }
        }
    }

    private companion object {
        const val TAG = "LeaderboardVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
