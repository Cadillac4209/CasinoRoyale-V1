package com.example.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class CryptoAsset(
    val symbol: String,
    val displayName: String,
    val coinGeckoId: String,
    val coinbasePair: String,
    val accentHex: Long,
    val badgeSymbol: String,
    val displayDecimals: Int
) {
    BTC(
        symbol = "BTC",
        displayName = "Bitcoin",
        coinGeckoId = "bitcoin",
        coinbasePair = "BTC-USD",
        accentHex = 0xFFF7931A,
        badgeSymbol = "₿",
        displayDecimals = 6
    ),
    ETH(
        symbol = "ETH",
        displayName = "Ethereum",
        coinGeckoId = "ethereum",
        coinbasePair = "ETH-USD",
        accentHex = 0xFF627EEA,
        badgeSymbol = "Ξ",
        displayDecimals = 5
    ),
    SOL(
        symbol = "SOL",
        displayName = "Solana",
        coinGeckoId = "solana",
        coinbasePair = "SOL-USD",
        accentHex = 0xFF14F195,
        badgeSymbol = "◎",
        displayDecimals = 4
    ),
    LTC(
        symbol = "LTC",
        displayName = "Litecoin",
        coinGeckoId = "litecoin",
        coinbasePair = "LTC-USD",
        accentHex = 0xFF38BDF8,
        badgeSymbol = "Ł",
        displayDecimals = 4
    );

    companion object {
        fun fromSymbol(symbol: String?): CryptoAsset {
            return entries.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: BTC
        }
    }
}

data class CryptoQuote(
    val asset: CryptoAsset,
    val usdPrice: Double,
    val change24hPercent: Double? = null,
    val fetchedAtMillis: Long,
    val sourceName: String
)

data class CryptoConversionSummary(
    val virtualCredits: Long,
    val virtualUsdValue: Double,
    val selectedAsset: CryptoAsset,
    val convertedAmount: Double,
    val formattedCryptoAmount: String,
    val creditsPerUsd: Long
)

object CryptoConversionCalculator {

    /**
     * Read-only visualization benchmark:
     * 1,000 Virtual Casino Credits = $1.00 Simulated Reference Value.
     * Note: Virtual credits have zero real-world monetary value and cannot be traded or redeemed.
     */
    const val DEFAULT_CREDITS_PER_USD: Long = 1_000L

    fun convertCreditsToCrypto(
        virtualCredits: Long,
        quote: CryptoQuote,
        creditsPerUsd: Long = DEFAULT_CREDITS_PER_USD
    ): CryptoConversionSummary {
        val safeCredits = virtualCredits.coerceAtLeast(0L)
        val safeCreditsPerUsd = creditsPerUsd.coerceAtLeast(1L)
        val usdReference = safeCredits.toDouble() / safeCreditsPerUsd.toDouble()
        val convertedAmount = if (quote.usdPrice > 0.0 && quote.usdPrice.isFinite()) {
            usdReference / quote.usdPrice
        } else {
            0.0
        }
        return CryptoConversionSummary(
            virtualCredits = safeCredits,
            virtualUsdValue = usdReference,
            selectedAsset = quote.asset,
            convertedAmount = convertedAmount,
            formattedCryptoAmount = formatCryptoAmount(convertedAmount, quote.asset),
            creditsPerUsd = safeCreditsPerUsd
        )
    }

    fun formatCryptoAmount(amount: Double, asset: CryptoAsset): String {
        val safeAmount = if (amount.isFinite() && amount > 0.0) amount else 0.0
        val maxDecimals = if (safeAmount > 0.0 && safeAmount < 0.0001) {
            maxOf(asset.displayDecimals, 8)
        } else {
            asset.displayDecimals
        }
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = buildString {
            append("#,##0.")
            repeat(asset.displayDecimals) { append('0') }
            repeat((maxDecimals - asset.displayDecimals).coerceAtLeast(0)) { append('#') }
        }
        val formatter = DecimalFormat(pattern, symbols)
        return "${formatter.format(safeAmount)} ${asset.symbol}"
    }

    fun formatUsdPrice(usdPrice: Double): String {
        if (!usdPrice.isFinite() || usdPrice <= 0.0) return "$0.00"
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = if (usdPrice < 1.0) 4 else 2
        }
        return formatter.format(usdPrice)
    }

    fun formatUsdReference(usdReference: Double): String {
        val safeValue = if (usdReference.isFinite() && usdReference >= 0.0) usdReference else 0.0
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(safeValue)
    }

    fun formatCredits(credits: Long): String {
        val safeCredits = credits.coerceAtLeast(0L)
        return NumberFormat.getNumberInstance(Locale.US).format(safeCredits)
    }

    fun format24hChange(changePercent: Double?): String {
        if (changePercent == null || !changePercent.isFinite()) return "24h N/A"
        val sign = if (changePercent >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s%.2f%% (24h)", sign, abs(changePercent))
    }
}
