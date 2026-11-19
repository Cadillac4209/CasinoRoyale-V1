package com.example.ui.lobby

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyLoginRepository
import com.example.model.DailyLoginRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailyLoginViewModel(
    private val repository: DailyLoginRepository
) : ViewModel() {

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val dailyLoginRecord: StateFlow<DailyLoginRecord?> = repository.observeDailyLoginRecord()
        .onEach { _syncError.value = null }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Failed to observe daily login Firestore record", rootError)
            _syncError.value = rootError.message ?: "Unable to load daily login streak from Firestore"
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    fun syncLoginSession() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.recordSessionLogin()
                result.exceptionOrNull()?.let { err ->
                    _syncError.value = err.localizedMessage ?: "Failed to save last login timestamp"
                } ?: run {
                    _syncError.value = null
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun recordDailyClaim(
        currentStreak: Int,
        bestStreak: Int,
        totalCheckIns: Int,
        claimedChips: Long
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.recordDailyBonusClaim(
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    totalCheckIns = totalCheckIns,
                    claimedChips = claimedChips
                )
                result.exceptionOrNull()?.let { err ->
                    _syncError.value = err.localizedMessage ?: "Failed to sync daily bonus claim"
                } ?: run {
                    _syncError.value = null
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun recordWheelSpin() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = repository.recordWheelSpinClaim()
                result.exceptionOrNull()?.let { err ->
                    _syncError.value = err.localizedMessage ?: "Failed to sync wheel spin timestamp"
                } ?: run {
                    _syncError.value = null
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun resetCloudStreak() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.resetCloudStreakForTesting()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private companion object {
        const val TAG = "DailyLoginVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
