package com.example.data.remote

import com.example.model.CryptoAsset
import com.example.model.CryptoQuote
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class CoinGeckoAssetPriceDto(
    @Json(name = "usd") val usd: Double? = null,
    @Json(name = "usd_24h_change") val usd24hChange: Double? = null,
    @Json(name = "last_updated_at") val lastUpdatedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class CoinbaseSpotResponseDto(
    @Json(name = "data") val data: CoinbaseSpotDataDto? = null
)

@JsonClass(generateAdapter = true)
data class CoinbaseSpotDataDto(
    @Json(name = "base") val base: String? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "amount") val amount: String? = null
)

interface CoinGeckoApiService {
    @GET("api/v3/simple/price")
    suspend fun getSimplePrices(
        @Query("ids") ids: String = CryptoAsset.entries.joinToString(",") { it.coinGeckoId },
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") include24hrChange: Boolean = true,
        @Query("include_last_updated_at") includeLastUpdatedAt: Boolean = true
    ): ResponseBody
}

interface CoinbaseApiService {
    @GET("v2/prices/{pair}/spot")
    suspend fun getSpotPrice(
        @Path("pair") pair: String
    ): CoinbaseSpotResponseDto
}

object CryptoRateParser {
    private val moshi: Moshi = Moshi.Builder().build()

    private val coinGeckoMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        CoinGeckoAssetPriceDto::class.java
    )
    private val coinGeckoMapAdapter = moshi.adapter<Map<String, CoinGeckoAssetPriceDto>>(coinGeckoMapType)
    private val coinbaseAdapter = moshi.adapter(CoinbaseSpotResponseDto::class.java)

    fun parseCoinGeckoResponse(
        json: String,
        fallbackFetchedAtMillis: Long = System.currentTimeMillis()
    ): Map<CryptoAsset, CryptoQuote> {
        val rawMap = runCatching { coinGeckoMapAdapter.fromJson(json) }.getOrNull().orEmpty()
        val quotes = linkedMapOf<CryptoAsset, CryptoQuote>()
        for (asset in CryptoAsset.entries) {
            val dto = rawMap[asset.coinGeckoId] ?: continue
            val price = dto.usd ?: continue
            if (!price.isFinite() || price <= 0.0) continue
            val updatedAtMs = dto.lastUpdatedAt?.takeIf { it > 0L }?.times(1000L)
                ?: fallbackFetchedAtMillis
            quotes[asset] = CryptoQuote(
                asset = asset,
                usdPrice = price,
                change24hPercent = dto.usd24hChange?.takeIf { it.isFinite() },
                fetchedAtMillis = updatedAtMs,
                sourceName = "CoinGecko Live"
            )
        }
        return quotes
    }

    fun parseCoinbaseResponse(
        asset: CryptoAsset,
        dto: CoinbaseSpotResponseDto,
        fetchedAtMillis: Long = System.currentTimeMillis()
    ): CryptoQuote? {
        val amount = dto.data?.amount?.toDoubleOrNull() ?: return null
        if (!amount.isFinite() || amount <= 0.0) return null
        return CryptoQuote(
            asset = asset,
            usdPrice = amount,
            change24hPercent = null,
            fetchedAtMillis = fetchedAtMillis,
            sourceName = "Coinbase Live"
        )
    }

    fun parseCoinbaseJson(
        asset: CryptoAsset,
        json: String,
        fetchedAtMillis: Long = System.currentTimeMillis()
    ): CryptoQuote? {
        val dto = runCatching { coinbaseAdapter.fromJson(json) }.getOrNull() ?: return null
        return parseCoinbaseResponse(asset, dto, fetchedAtMillis)
    }
}
