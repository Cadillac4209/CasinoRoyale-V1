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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CryptoRatesUiState
import com.example.model.CryptoAsset
import com.example.model.CryptoConversionCalculator
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink

@Composable
fun DualBalanceVisualizerCard(
    virtualCredits: Long,
    selectedAsset: CryptoAsset,
    ratesState: CryptoRatesUiState,
    onSelectAsset: (CryptoAsset) -> Unit,
    onRefreshRates: () -> Unit,
    onOpenFullConverter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(selectedAsset.accentHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(GoldDark, accentColor)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onOpenFullConverter() }
            .testTag("lobby_crypto_converter_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FireKernCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(accentColor, FireKernEmberDark))
                            )
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Live Crypto Balance Visualizer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CREDIT ↔ CRYPTO VISUALIZER",
                            color = GoldLight,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Live spot rate preview • Read-only",
                            color = FireKernAsh.copy(alpha = 0.78f),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshRates,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("lobby_crypto_refresh_button")
                ) {
                    if (ratesState is CryptoRatesUiState.Success && ratesState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Live Crypto Rates",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Balance Panel: Virtual Credits + Selected Crypto Token
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FireKernCardElevated)
                    .border(1.dp, FireKernCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VIRTUAL CREDITS",
                        color = FireKernAsh,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${CryptoConversionCalculator.formatCredits(virtualCredits)}",
                        color = GoldPrimary,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "≈ ${
                            CryptoConversionCalculator.formatUsdReference(
                                virtualCredits.toDouble() / CryptoConversionCalculator.DEFAULT_CREDITS_PER_USD
                            )
                        } ref",
                        color = FireKernAsh.copy(alpha = 0.7f),
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${selectedAsset.displayName.uppercase()} EQUIV.",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    when (ratesState) {
                        is CryptoRatesUiState.Loading -> {
                            Text(
                                text = "Fetching...",
                                color = Color.LightGray,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        is CryptoRatesUiState.Error -> {
                            Text(
                                text = "Rate offline",
                                color = NeonPink,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap refresh",
                                color = FireKernAsh.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                        is CryptoRatesUiState.Success -> {
                            val quote = ratesState.quotes[selectedAsset]
                            if (quote != null) {
                                val conversion = CryptoConversionCalculator.convertCreditsToCrypto(
                                    virtualCredits = virtualCredits,
                                    quote = quote
                                )
                                Text(
                                    text = conversion.formattedCryptoAmount,
                                    color = Color.White,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "1 ${selectedAsset.symbol} = ${
                                        CryptoConversionCalculator.formatUsdPrice(quote.usdPrice)
                                    }",
                                    color = NeonCyan,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp
                                )
                            } else {
                                Text(
                                    text = "Unavailable",
                                    color = FireKernAsh,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick token switcher row + Open converter button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CryptoAsset.entries.forEach { asset ->
                    val isSelected = asset == selectedAsset
                    val chipAccent = Color(asset.accentHex)
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) chipAccent.copy(alpha = 0.22f)
                                else FireKernCardElevated
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipAccent else FireKernCardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectAsset(asset) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("lobby_crypto_chip_${asset.symbol.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${asset.badgeSymbol} ${asset.symbol}",
                            color = if (isSelected) chipAccent else FireKernAsh,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FireKernEmberDark)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "RATES >",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
