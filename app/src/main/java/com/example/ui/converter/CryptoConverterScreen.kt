package com.example.ui.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundManager
import com.example.data.CryptoRatesUiState
import com.example.model.CryptoAsset
import com.example.model.CryptoConversionCalculator
import com.example.model.CryptoQuote
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CryptoConverterScreen(
    viewModel: CryptoConverterViewModel,
    liveBankroll: Long,
    soundManager: SoundManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratesState by viewModel.ratesState.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val useLiveBankroll by viewModel.useLiveBankroll.collectAsStateWithLifecycle()
    val customCreditsInput by viewModel.customCreditsInput.collectAsStateWithLifecycle()

    val displayedCredits = if (useLiveBankroll) {
        liveBankroll.coerceAtLeast(0L)
    } else {
        customCreditsInput.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FireKernEmberDark, FireKernObsidian, FireKernVoid)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sticky Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = FireKernVoid.copy(alpha = 0.96f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("converter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Lobby",
                            tint = GoldLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CRYPTO RATE VISUALIZER",
                            color = GoldPrimary,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "LIVE PUBLIC SPOT EXCHANGE FEED",
                            color = NeonCyan,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            soundManager.playButtonClick()
                            viewModel.refreshRates(force = true)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("converter_refresh_button")
                    ) {
                        if (ratesState is CryptoRatesUiState.Success &&
                            (ratesState as CryptoRatesUiState.Success).isRefreshing
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Live Crypto Rates",
                                tint = GoldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dual Balance Hero Card
            DualHeroConversionCard(
                displayedCredits = displayedCredits,
                isLiveBalance = useLiveBankroll,
                selectedAsset = selectedAsset,
                ratesState = ratesState,
                timeFormat = timeFormat,
                onRetry = {
                    soundManager.playButtonClick()
                    viewModel.refreshRates(force = true)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Token Selector Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "SELECT CRYPTO TOKEN",
                    color = GoldLight,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CryptoAsset.entries.forEach { asset ->
                        val isSelected = asset == selectedAsset
                        val accent = Color(asset.accentHex)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.22f)
                                    else FireKernCard
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else FireKernCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    soundManager.playChipClick()
                                    viewModel.selectAsset(asset)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("converter_select_${asset.symbol.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = asset.badgeSymbol,
                                    color = accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = asset.symbol,
                                    color = if (isSelected) Color.White else FireKernAsh,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance Source & What-If Simulator Card
            BalanceSimulatorCard(
                liveBankroll = liveBankroll,
                useLiveBankroll = useLiveBankroll,
                customCreditsInput = customCreditsInput,
                onSelectLive = {
                    soundManager.playButtonClick()
                    viewModel.setUseLiveBankroll(true)
                },
                onUpdateCustomInput = { viewModel.updateCustomCreditsInput(it) },
                onSelectPreset = { amount ->
                    soundManager.playChipClick()
                    viewModel.applyPresetCredits(amount)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Full Portfolio Breakdown Across All Supported Tokens
            AllTokensBreakdownSection(
                displayedCredits = displayedCredits,
                selectedAsset = selectedAsset,
                ratesState = ratesState,
                onSelectAsset = { asset ->
                    soundManager.playChipClick()
                    viewModel.selectAsset(asset)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Read-Only Non-Monetary Disclaimer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FireKernCard)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "READ-ONLY BALANCE VISUALIZATION",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spot rates are fetched live from public market APIs using a simulated benchmark of 1,000 Virtual Credits = \$1.00 reference. Fire Kern Royale credits are for adult entertainment only and have zero real-world monetary value — no deposits, purchases, withdrawals, or crypto transfers occur.",
                            color = FireKernAsh,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DualHeroConversionCard(
    displayedCredits: Long,
    isLiveBalance: Boolean,
    selectedAsset: CryptoAsset,
    ratesState: CryptoRatesUiState,
    timeFormat: SimpleDateFormat,
    onRetry: () -> Unit
) {
    val accent = Color(selectedAsset.accentHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(GoldPrimary, accent)),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FireKernCardElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLiveBalance) "LIVE CASINO BALANCE" else "SIMULATED CREDIT PREVIEW",
                    color = GoldPrimary,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.7.sp
                )

                val statusLabel = when (ratesState) {
                    is CryptoRatesUiState.Loading -> "SYNCING..."
                    is CryptoRatesUiState.Error -> "OFFLINE"
                    is CryptoRatesUiState.Success -> {
                        val quote = ratesState.quotes[selectedAsset]
                        val updatedStr = timeFormat.format(Date(ratesState.lastUpdatedMillis))
                        val source = quote?.sourceName ?: "Live Feed"
                        if (ratesState.isStaleFromCache) "CACHED • $updatedStr" else "$source • $updatedStr"
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FireKernVoid.copy(alpha = 0.75f))
                        .border(1.dp, FireKernCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = if (ratesState is CryptoRatesUiState.Error) NeonPink else NeonCyan,
                        fontFamily = MonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Virtual Credits Display
            Text(
                text = "$${CryptoConversionCalculator.formatCredits(displayedCredits)} Credits",
                color = GoldLight,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("converter_virtual_credits_value")
            )
            Text(
                text = "Simulated Reference: ${
                    CryptoConversionCalculator.formatUsdReference(
                        displayedCredits.toDouble() / CryptoConversionCalculator.DEFAULT_CREDITS_PER_USD
                    )
                } (1,000 Credits = \$1.00 ref)",
                color = FireKernAsh.copy(alpha = 0.85f),
                fontFamily = MonoFontFamily,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FireKernCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Converted Crypto Token Display
            when (ratesState) {
                is CryptoRatesUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = accent,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Fetching real-time ${selectedAsset.displayName} spot price...",
                            color = FireKernAsh,
                            fontSize = 13.sp
                        )
                    }
                }

                is CryptoRatesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FireKernVoid.copy(alpha = 0.7f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ratesState.message,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("converter_retry_button")
                        ) {
                            Text("RETRY LIVE FETCH", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                is CryptoRatesUiState.Success -> {
                    val quote = ratesState.quotes[selectedAsset]
                    if (quote != null) {
                        val summary = CryptoConversionCalculator.convertCreditsToCrypto(
                            virtualCredits = displayedCredits,
                            quote = quote
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selectedAsset.badgeSymbol} ${selectedAsset.displayName.uppercase()} EQUIVALENT",
                                    color = accent,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = summary.formattedCryptoAmount,
                                    color = Color.White,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("converter_crypto_amount_value")
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CryptoConversionCalculator.formatUsdPrice(quote.usdPrice),
                                    color = GoldLight,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                quote.change24hPercent?.let { change ->
                                    val isPositive = change >= 0.0
                                    val changeColor = if (isPositive) Color(0xFF14F195) else NeonPink
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPositive) {
                                                Icons.AutoMirrored.Filled.TrendingUp
                                            } else {
                                                Icons.AutoMirrored.Filled.TrendingDown
                                            },
                                            contentDescription = null,
                                            tint = changeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = CryptoConversionCalculator.format24hChange(change),
                                            color = changeColor,
                                            fontFamily = MonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        ratesState.lastRefreshError?.let { warning ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = warning,
                                color = FireKernEmber,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSimulatorCard(
    liveBankroll: Long,
    useLiveBankroll: Boolean,
    customCreditsInput: String,
    onSelectLive: () -> Unit,
    onUpdateCustomInput: (String) -> Unit,
    onSelectPreset: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(1.dp, FireKernCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FireKernCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "VISUALIZE BALANCE OR CUSTOM AMOUNT",
                color = GoldLight,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val liveLabel = "MY BALANCE ($${CryptoConversionCalculator.formatCredits(liveBankroll)})"
                Button(
                    onClick = onSelectLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useLiveBankroll) GoldPrimary else FireKernCardElevated,
                        contentColor = if (useLiveBankroll) Color.Black else GoldLight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("converter_mode_live")
                ) {
                    Text(
                        text = liveLabel,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                listOf(10_000L to "10K", 100_000L to "100K", 1_000_000L to "1M").forEach { (amt, label) ->
                    OutlinedButton(
                        onClick = { onSelectPreset(amt) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("converter_preset_${label.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            color = GoldLight,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = if (useLiveBankroll) liveBankroll.toString() else customCreditsInput,
                onValueChange = onUpdateCustomInput,
                label = { Text("Virtual Credits to Preview", fontSize = 12.sp) },
                prefix = { Text("$ ", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = FireKernCardBorder,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = FireKernAsh,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("converter_credits_input")
            )
        }
    }
}

@Composable
private fun AllTokensBreakdownSection(
    displayedCredits: Long,
    selectedAsset: CryptoAsset,
    ratesState: CryptoRatesUiState,
    onSelectAsset: (CryptoAsset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ALL SUPPORTED CRYPTO RATES",
            color = GoldLight,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp
        )

        CryptoAsset.entries.forEach { asset ->
            val quote: CryptoQuote? = (ratesState as? CryptoRatesUiState.Success)?.quotes?.get(asset)
            val isSelected = asset == selectedAsset
            val accent = Color(asset.accentHex)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accent else FireKernCardBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectAsset(asset) }
                    .testTag("converter_row_${asset.symbol.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) FireKernCardElevated else FireKernCard
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.2f))
                            .border(1.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = asset.badgeSymbol,
                            color = accent,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = asset.symbol,
                                color = accent,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                        val priceSubtitle = if (quote != null) {
                            "Spot: ${CryptoConversionCalculator.formatUsdPrice(quote.usdPrice)} • ${
                                CryptoConversionCalculator.format24hChange(quote.change24hPercent)
                            }"
                        } else if (ratesState is CryptoRatesUiState.Loading) {
                            "Loading live price..."
                        } else {
                            "Rate unavailable"
                        }
                        Text(
                            text = priceSubtitle,
                            color = FireKernAsh,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (quote != null) {
                            CryptoConversionCalculator.convertCreditsToCrypto(
                                virtualCredits = displayedCredits,
                                quote = quote
                            ).formattedCryptoAmount
                        } else {
                            "-- ${asset.symbol}"
                        },
                        color = if (isSelected) accent else GoldLight,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
