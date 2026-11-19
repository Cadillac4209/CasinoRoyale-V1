package com.example.ui.baccarat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BaccaratBetSpot
import com.example.model.BaccaratOutcome
import com.example.model.BaccaratRules
import com.example.model.Card
import com.example.model.PlayerProfile
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.ChipSelectorBar
import com.example.ui.components.PlayingCardView
import com.example.ui.components.WinCelebrationOverlay
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BaccaratScreen(
    viewModel: BaccaratViewModel,
    profile: PlayerProfile,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()
    val bets by viewModel.bets.collectAsStateWithLifecycle()
    val isDealing by viewModel.isDealing.collectAsStateWithLifecycle()
    val playerCards by viewModel.playerCards.collectAsStateWithLifecycle()
    val bankerCards by viewModel.bankerCards.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val lastPayout by viewModel.lastPayout.collectAsStateWithLifecycle()
    val lastOutcome by viewModel.lastOutcome.collectAsStateWithLifecycle()

    val totalBet = bets.values.sum()
    val playerScore = BaccaratRules.handScore(playerCards)
    val bankerScore = BaccaratRules.handScore(bankerCards)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        profile.selectedFelt.primaryColor,
                        profile.selectedFelt.mediumColor,
                        profile.selectedFelt.darkColor
                    ),
                    radius = 1200f
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
            CasinoTopBar(
                profile = profile,
                title = "ROYAL BACCARAT",
                isSoundEnabled = isSoundEnabled,
                onToggleSound = onToggleSound,
                onBackClick = onBackClick,
                onEmergencyRefill = onEmergencyRefill
            )

            // Salon Table Rules Strip
            Text(
                text = "PUNTO BANCO • TIE PAYS 8:1 • PAIR SIDE BET PAYS 11:1",
                color = GoldLight.copy(alpha = 0.78f),
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Macau Bead Road Outcome Strip
            BeadRoadHistoryStrip(history = history)

            Spacer(modifier = Modifier.height(10.dp))

            // Side-by-Side Hands Felt Arena: PLAYER vs BANKER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HandZoneCard(
                    title = "PLAYER",
                    score = if (playerCards.isEmpty()) null else playerScore,
                    cards = playerCards,
                    cardBackTheme = profile.selectedCardBack,
                    isWinner = lastOutcome == BaccaratOutcome.PLAYER_WIN,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                HandZoneCard(
                    title = "BANKER",
                    score = if (bankerCards.isEmpty()) null else bankerScore,
                    cards = bankerCards,
                    cardBackTheme = profile.selectedCardBack,
                    isWinner = lastOutcome == BaccaratOutcome.BANKER_WIN,
                    accentColor = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FireKernVoid.copy(alpha = 0.75f))
                    .border(1.dp, FireKernCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage,
                    color = GoldPrimary,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("baccarat_status_text")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive 4-Spot Wagering Felt
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BaccaratBetSpotTile(
                        spot = BaccaratBetSpot.PLAYER,
                        wager = bets[BaccaratBetSpot.PLAYER] ?: 0L,
                        accentColor = NeonCyan,
                        enabled = !isDealing,
                        onClick = { viewModel.addBetToSpot(BaccaratBetSpot.PLAYER) },
                        modifier = Modifier.weight(1f)
                    )
                    BaccaratBetSpotTile(
                        spot = BaccaratBetSpot.BANKER,
                        wager = bets[BaccaratBetSpot.BANKER] ?: 0L,
                        accentColor = Color(0xFFF43F5E),
                        enabled = !isDealing,
                        onClick = { viewModel.addBetToSpot(BaccaratBetSpot.BANKER) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BaccaratBetSpotTile(
                        spot = BaccaratBetSpot.TIE,
                        wager = bets[BaccaratBetSpot.TIE] ?: 0L,
                        accentColor = Color(0xFF14F195),
                        enabled = !isDealing,
                        onClick = { viewModel.addBetToSpot(BaccaratBetSpot.TIE) },
                        modifier = Modifier.weight(1f)
                    )
                    BaccaratBetSpotTile(
                        spot = BaccaratBetSpot.DRAGON_PAIR,
                        wager = bets[BaccaratBetSpot.DRAGON_PAIR] ?: 0L,
                        accentColor = GoldPrimary,
                        enabled = !isDealing,
                        onClick = { viewModel.addBetToSpot(BaccaratBetSpot.DRAGON_PAIR) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chip Denominations Selector
            ChipSelectorBar(
                selectedDenom = selectedChip,
                onSelectDenom = { viewModel.selectChip(it) },
                playerLevel = profile.level
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Table Actions: CLEAR | REBET | DEAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearBets() },
                    enabled = !isDealing && totalBet > 0L,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("baccarat_clear_button")
                ) {
                    Text("CLEAR", color = FireKernAsh, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.rebetPrevious() },
                    enabled = !isDealing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("baccarat_rebet_button")
                ) {
                    Text("REBET", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.dealRound() },
                    enabled = !isDealing && totalBet > 0L,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("baccarat_deal_button")
                ) {
                    Text(
                        text = if (isDealing) "DEALING..." else "DEAL ($$totalBet)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedVisibility(visible = lastPayout > 0L && !isDealing) {
            WinCelebrationOverlay(
                winAmount = lastPayout,
                title = lastOutcome?.displayTitle ?: "BACCARAT WIN!",
                onDismiss = { viewModel.dismissWinCelebration() }
            )
        }
    }
}

@Composable
private fun BeadRoadHistoryStrip(history: List<BaccaratOutcome>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FireKernVoid.copy(alpha = 0.65f))
            .border(1.dp, FireKernCardBorder, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "ROAD",
            color = FireKernAsh,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )

        if (history.isEmpty()) {
            Text(
                text = "Deal first hand to start Bead Road",
                color = FireKernAsh.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        } else {
            history.forEach { outcome ->
                val bgColor = when (outcome) {
                    BaccaratOutcome.PLAYER_WIN -> Color(0xFF2563EB)
                    BaccaratOutcome.BANKER_WIN -> CasinoRed
                    BaccaratOutcome.TIE -> Color(0xFF15803D)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = outcome.shortCode,
                        color = Color.White,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HandZoneCard(
    title: String,
    score: Int?,
    cards: List<Card>,
    cardBackTheme: com.example.model.CardBackTheme,
    isWinner: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FireKernCard.copy(alpha = 0.86f))
            .border(
                width = if (isWinner) 2.dp else 1.dp,
                color = if (isWinner) GoldPrimary else accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = accentColor,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isWinner) GoldPrimary else FireKernCardElevated)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = score?.toString() ?: "-",
                    color = if (isWinner) Color.Black else Color.White,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, FireKernCardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WAITING",
                    color = FireKernAsh.copy(alpha = 0.45f),
                    fontFamily = MonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-14).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEach { card ->
                    PlayingCardView(
                        card = card,
                        width = 56.dp,
                        cardBackTheme = cardBackTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun BaccaratBetSpotTile(
    spot: BaccaratBetSpot,
    wager: Long,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasBet = wager > 0L
    val numberFmt = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (hasBet) accentColor.copy(alpha = 0.22f)
                else FireKernCardElevated.copy(alpha = 0.85f)
            )
            .border(
                width = if (hasBet) 2.dp else 1.dp,
                color = if (hasBet) accentColor else accentColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("baccarat_spot_${spot.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = spot.title,
                color = Color.White,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(
                text = spot.payoutLabel,
                color = accentColor,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasBet) "$${numberFmt.format(wager)}" else "TAP TO BET",
            color = if (hasBet) GoldPrimary else FireKernAsh.copy(alpha = 0.65f),
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}
