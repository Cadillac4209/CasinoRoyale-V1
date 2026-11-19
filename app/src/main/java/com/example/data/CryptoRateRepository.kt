package com.example.data

import android.content.Context
import com.example.data.remote.CoinGeckoApiService
import com.example.data.remote.CoinbaseApiService
import com.example.data.remote.CryptoRateParser
import com.example.model.CryptoAsset
import com.example.model.CryptoQuote
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed interface CryptoRatesUiState {
    data object Loading : CryptoRatesUiState

    data class Success(
        val quotes: Map<CryptoAsset, CryptoQuote>,
        val lastUpdatedMillis: Long,
        val isStaleFromCache: Boolean = false,
        val isRefreshing: Boolean = false,
        val lastRefreshError: String? = null
    ) : CryptoRatesUiState

    data class Error(
        val message: String
    ) : CryptoRatesUiState
}

@JsonClass(generateAdapter = true)
internal data class CachedCryptoQuoteDto(
    val symbol: String,
    val usdPrice: Double,
    val change24hPercent: Double?,
    val fetchedAtMillis: Long,
    val sourceName: String
)

class CryptoRateRepository(
    context: Context,
    private val coinGeckoService: CoinGeckoApiService = defaultCoinGeckoService(),
    private val coinbaseService: CoinbaseApiService = defaultCoinbaseService()
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val refreshMutex = Mutex()
    private val moshi: Moshi = Moshi.Builder().build()
    private val cacheListType = Types.newParameterizedType(List::class.java, CachedCryptoQuoteDto::class.java)
    private val cacheAdapter = moshi.adapter<List<CachedCryptoQuoteDto>>(cacheListType)

    private val _selectedAsset = MutableStateFlow(
        CryptoAsset.fromSymbol(prefs.getString(KEY_SELECTED_ASSET, CryptoAsset.BTC.symbol))
    )
    val selectedAsset: StateFlow<CryptoAsset> = _selectedAsset.asStateFlow()

    private val _uiState = MutableStateFlow<CryptoRatesUiState>(
        loadCachedQuotes()?.let { cached ->
            val newestTimestamp = cached.values.maxOfOrNull { it.fetchedAtMillis } ?: 0L
            CryptoRatesUiState.Success(
                quotes = cached,
                lastUpdatedMillis = newestTimestamp,
                isStaleFromCache = true,
                isRefreshing = false
            )
        } ?: CryptoRatesUiState.Loading
    )
    val uiState: StateFlow<CryptoRatesUiState> = _uiState.asStateFlow()

    fun selectAsset(asset: CryptoAsset) {
        _selectedAsset.value = asset
        prefs.edit().putString(KEY_SELECTED_ASSET, asset.symbol).apply()
    }

    suspend fun refreshRates(force: Boolean = false) {
        refreshMutex.withLock {
            val current = _uiState.value
            val now = System.currentTimeMillis()
            if (!force && current is CryptoRatesUiState.Success && !current.isStaleFromCache) {
                val ageMs = now - current.lastUpdatedMillis
                if (ageMs in 0 until REFRESH_COOLDOWN_MS) {
                    return
                }
            }

            if (current is CryptoRatesUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true, lastRefreshError = null)
            } else {
                _uiState.value = CryptoRatesUiState.Loading
            }

            val fetchResult = fetchLiveQuotesFromNetwork()
            fetchResult.fold(
                onSuccess = { quotes ->
                    saveCachedQuotes(quotes)
                    val updatedAt = quotes.values.maxOfOrNull { it.fetchedAtMillis }
                        ?: System.currentTimeMillis()
                    _uiState.value = CryptoRatesUiState.Success(
                        quotes = quotes,
                        lastUpdatedMillis = updatedAt,
                        isStaleFromCache = false,
                        isRefreshing = false,
                        lastRefreshError = null
                    )
                },
                onFailure = { throwable ->
                    val existingQuotes = when (val existing = _uiState.value) {
                        is CryptoRatesUiState.Success -> existing.quotes
                        else -> loadCachedQuotes()
                    }
                    val reason = throwable.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Network request unavailable"
                    if (!existingQuotes.isNullOrEmpty()) {
                        val newestTimestamp = existingQuotes.values.maxOfOrNull { it.fetchedAtMillis } ?: now
                        _uiState.value = CryptoRatesUiState.Success(
                            quotes = existingQuotes,
                            lastUpdatedMillis = newestTimestamp,
                            isStaleFromCache = true,
                            isRefreshing = false,
                            lastRefreshError = "Live feed unreachable ($reason). Showing last synced rates."
                        )
                    } else {
                        _uiState.value = CryptoRatesUiState.Error(
                            "Unable to fetch live crypto exchange rates. Check your internet connection and tap Retry."
                        )
                    }
                }
            )
        }
    }

    private suspend fun fetchLiveQuotesFromNetwork(): Result<Map<CryptoAsset, CryptoQuote>> =
        withContext(Dispatchers.IO) {
            val coinGeckoAttempt = runCatching {
                val body = coinGeckoService.getSimplePrices().use { it.string() }
                val parsed = CryptoRateParser.parseCoinGeckoResponse(body, System.currentTimeMillis())
                check(parsed.isNotEmpty()) { "Empty CoinGecko response" }
                parsed
            }
            if (coinGeckoAttempt.isSuccess) {
                return@withContext coinGeckoAttempt
            }

            // Fallback to Coinbase public spot endpoint if CoinGecko is rate-limited or unreachable
            val coinbaseAttempt = runCatching {
                val now = System.currentTimeMillis()
                val fetchedList = coroutineScope {
                    CryptoAsset.entries.map { asset ->
                        async {
                            runCatching {
                                val dto = coinbaseService.getSpotPrice(asset.coinbasePair)
                                CryptoRateParser.parseCoinbaseResponse(asset, dto, now)
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                val map = linkedMapOf<CryptoAsset, CryptoQuote>()
                fetchedList.filterNotNull().forEach { quote ->
                    map[quote.asset] = quote
                }
                check(map.isNotEmpty()) { "Empty Coinbase fallback response" }
                map.toMap()
            }
            coinbaseAttempt
        }

    private fun saveCachedQuotes(quotes: Map<CryptoAsset, CryptoQuote>) {
        val dtos = quotes.values.map { quote ->
            CachedCryptoQuoteDto(
                symbol = quote.asset.symbol,
                usdPrice = quote.usdPrice,
                change24hPercent = quote.change24hPercent,
                fetchedAtMillis = quote.fetchedAtMillis,
                sourceName = quote.sourceName
            )
        }
        runCatching {
            prefs.edit().putString(KEY_CACHED_QUOTES_JSON, cacheAdapter.toJson(dtos)).apply()
        }
    }

    private fun loadCachedQuotes(): Map<CryptoAsset, CryptoQuote>? {
        val json = prefs.getString(KEY_CACHED_QUOTES_JSON, null) ?: return null
        val dtos = runCatching { cacheAdapter.fromJson(json) }.getOrNull() ?: return null
        val map = linkedMapOf<CryptoAsset, CryptoQuote>()
        for (dto in dtos) {
            if (!dto.usdPrice.isFinite() || dto.usdPrice <= 0.0) continue
            val asset = CryptoAsset.entries.firstOrNull {
                it.symbol.equals(dto.symbol, ignoreCase = true)
            } ?: continue
            map[asset] = CryptoQuote(
                asset = asset,
                usdPrice = dto.usdPrice,
                change24hPercent = dto.change24hPercent?.takeIf { it.isFinite() },
                fetchedAtMillis = dto.fetchedAtMillis,
                sourceName = dto.sourceName
            )
        }
        return map.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val PREFS_NAME = "fire_kern_crypto_rates"
        private const val KEY_SELECTED_ASSET = "selected_crypto_asset"
        private const val KEY_CACHED_QUOTES_JSON = "cached_quotes_json"
        private const val REFRESH_COOLDOWN_MS = 15_000L

        private val sharedOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        private fun defaultCoinGeckoService(): CoinGeckoApiService {
            return Retrofit.Builder()
                .baseUrl("https://api.coingecko.com/")
                .client(sharedOkHttpClient)
                .build()
                .create(CoinGeckoApiService::class.java)
        }

        private fun defaultCoinbaseService(): CoinbaseApiService {
            val moshi = Moshi.Builder().build()
            return Retrofit.Builder()
                .baseUrl("https://api.coinbase.com/")
                .client(sharedOkHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(CoinbaseApiService::class.java)
        }
    }
}
