package com.example.ui.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CryptoRateRepository
import com.example.data.CryptoRatesUiState
import com.example.model.CryptoAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CryptoConverterViewModel(
    private val rateRepository: CryptoRateRepository
) : ViewModel() {

    val ratesState: StateFlow<CryptoRatesUiState> = rateRepository.uiState
    val selectedAsset: StateFlow<CryptoAsset> = rateRepository.selectedAsset

    private val _useLiveBankroll = MutableStateFlow(true)
    val useLiveBankroll: StateFlow<Boolean> = _useLiveBankroll.asStateFlow()

    private val _customCreditsInput = MutableStateFlow("100000")
    val customCreditsInput: StateFlow<String> = _customCreditsInput.asStateFlow()

    init {
        refreshRates(force = false)
    }

    fun refreshRates(force: Boolean = true) {
        viewModelScope.launch {
            rateRepository.refreshRates(force = force)
        }
    }

    fun selectAsset(asset: CryptoAsset) {
        rateRepository.selectAsset(asset)
    }

    fun setUseLiveBankroll(useLive: Boolean) {
        _useLiveBankroll.value = useLive
    }

    fun updateCustomCreditsInput(rawInput: String) {
        val digitsOnly = rawInput.filter { it.isDigit() }.take(12)
        _customCreditsInput.value = digitsOnly
        _useLiveBankroll.value = false
    }

    fun applyPresetCredits(amount: Long) {
        _customCreditsInput.value = amount.coerceAtLeast(0L).toString()
        _useLiveBankroll.value = false
    }
}
