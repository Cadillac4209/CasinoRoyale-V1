package com.example.ui.videopoker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.CardBackTheme
import com.example.model.PlayerProfile
import com.example.ui.components.CasinoChipView
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.ChipSelectorBar
import com.example.ui.components.PlayingCardView
import com.example.ui.components.WinCelebrationOverlay
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoFeltGreen
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.CasinoSurfaceCard
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow

@Composable
fun VideoPokerScreen(
    viewModel: VideoPokerViewModel,
    profile: PlayerProfile,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cards.collectAsState()
    val heldIndices by viewModel.heldIndices.collectAsState()
    val coinsBet by viewModel.coinsBet.collectAsState()
    val chipDenom by viewModel.chipDenom.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val handRank by viewModel.handRank.collectAsState()
    val winAmount by viewModel.winAmount.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val doubleUpGame by viewModel.doubleUpGame.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(profile.selectedFelt.darkColor, FireKernObsidian, FireKernVoid)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            CasinoTopBar(
                profile = profile,
                title = profile.selectedPokerVariant.displayName.uppercase(),
                isSoundEnabled = isSoundEnabled,
                onToggleSound = onToggleSound,
                onBackClick = onBackClick,
                onEmergencyRefill = onEmergencyRefill
            )

            // Paytable Matrix
            PaytableMatrix(
                currentCoins = coinsBet,
                winningHand = handRank,
                variant = profile.selectedPokerVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Casino Marquee Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FireKernCard)
                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage,
                    color = GoldPrimary,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cards Area or Double Up Screen
            if (gameState == VideoPokerState.DOUBLE_UP_ACTIVE && doubleUpGame != null) {
                DoubleUpView(
                    game = doubleUpGame!!,
                    onSelectCard = { viewModel.selectDoubleUpCard(it) },
                    cardBackTheme = profile.selectedCardBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            } else {
                // 5 Poker Cards Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 5) {
                        val card = cards.getOrNull(i)
                        val isHeld = heldIndices.contains(i)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlayingCardView(
                                card = card,
                                width = 64.dp,
                                isHeld = isHeld,
                                cardBackTheme = profile.selectedCardBack,
                                onClick = if (gameState == VideoPokerState.DRAW_READY) {
                                    { viewModel.toggleHold(i) }
                                } else null
                            )

                            // Hold button underneath
                            if (gameState == VideoPokerState.DRAW_READY) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isHeld) GoldPrimary else FireKernCardElevated)
                                        .border(1.dp, GoldPrimary, RoundedCornerShape(4.dp))
                                        .clickable { viewModel.toggleHold(i) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("hold_button_$i"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isHeld) "HELD" else "HOLD",
                                        color = if (isHeld) Color.Black else GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chip Selector Bar
            if (gameState == VideoPokerState.BETTING || gameState == VideoPokerState.ROUND_OVER) {
                ChipSelectorBar(
                    selectedDenom = chipDenom,
                    onSelectDenom = { viewModel.setChipDenom(it) },
                    modifier = Modifier.fillMaxWidth(),
                    playerLevel = profile.level
                )
            }

            // Bet and Win summary strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BET: $${viewModel.totalBet} (${coinsBet}x)",
                    color = GoldLight,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "WIN: $${winAmount}",
                    color = if (winAmount > 0) GoldPrimary else FireKernAsh.copy(alpha = 0.6f),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (gameState) {
                    VideoPokerState.DOUBLE_UP_READY -> {
                        Button(
                            onClick = { viewModel.startDoubleUp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CasinoRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("double_up_button")
                        ) {
                            Text("DOUBLE UP (2X)", fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = { viewModel.collectWin() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("collect_button")
                        ) {
                            Text("COLLECT", fontWeight = FontWeight.Black)
                        }
                    }
                    VideoPokerState.DRAW_READY -> {
                        Button(
                            onClick = { viewModel.draw() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("draw_button")
                        ) {
                            Text(
                                text = "DRAW CARDS",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.betOne() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireKernCardElevated,
                                contentColor = GoldLight
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("bet_one_button")
                        ) {
                            Text("BET ONE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.betMax() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireKernCardBorder,
                                contentColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("bet_max_button")
                        ) {
                            Text("BET MAX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.deal() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("deal_button")
                        ) {
                            Text("DEAL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaytableMatrix(
    currentCoins: Int,
    winningHand: PokerHandRank?,
    variant: com.example.model.PokerVariant = com.example.model.PokerVariant.JACKS_OR_BETTER,
    modifier: Modifier = Modifier
) {
    val hands = if (variant == com.example.model.PokerVariant.DEUCES_WILD) {
        listOf(
            PokerHandRank.NATURAL_ROYAL_FLUSH,
            PokerHandRank.FOUR_DEUCES,
            PokerHandRank.WILD_ROYAL_FLUSH,
            PokerHandRank.FIVE_OF_A_KIND,
            PokerHandRank.STRAIGHT_FLUSH,
            PokerHandRank.FOUR_OF_A_KIND,
            PokerHandRank.FULL_HOUSE,
            PokerHandRank.FLUSH,
            PokerHandRank.STRAIGHT,
            PokerHandRank.THREE_OF_A_KIND
        )
    } else {
        listOf(
            PokerHandRank.ROYAL_FLUSH,
            PokerHandRank.STRAIGHT_FLUSH,
            PokerHandRank.FOUR_OF_A_KIND,
            PokerHandRank.FULL_HOUSE,
            PokerHandRank.FLUSH,
            PokerHandRank.STRAIGHT,
            PokerHandRank.THREE_OF_A_KIND,
            PokerHandRank.TWO_PAIR,
            PokerHandRank.JACKS_OR_BETTER
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FireKernVoid)
            .border(1.dp, FireKernCardBorder, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        // Coin Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HAND",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(100.dp)
            )
            for (coin in 1..5) {
                Text(
                    text = "${coin}x",
                    color = if (coin == currentCoins) GoldPrimary else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        // Hands rows
        hands.forEach { hand ->
            val isWinning = hand == winningHand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isWinning) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent
                    )
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hand.displayName,
                    color = if (isWinning) NeonYellow else Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = if (isWinning) FontWeight.Black else FontWeight.Normal,
                    modifier = Modifier.width(100.dp)
                )

                for (coin in 1..5) {
                    val payoutMultiplier = if ((hand == PokerHandRank.ROYAL_FLUSH || hand == PokerHandRank.NATURAL_ROYAL_FLUSH) && coin == 5) 800 else hand.multiplier * coin
                    Text(
                        text = "$payoutMultiplier",
                        color = when {
                            isWinning && coin == currentCoins -> GoldPrimary
                            coin == currentCoins -> GoldLight
                            else -> Color(0xFF64748B)
                        },
                        fontSize = 10.sp,
                        fontWeight = if (coin == currentCoins) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoubleUpView(
    game: DoubleUpGame,
    onSelectCard: (Int) -> Unit,
    cardBackTheme: CardBackTheme = CardBackTheme.ROYAL_RED,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FireKernObsidian)
            .border(1.5.dp, GoldPrimary, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DOUBLE OR NOTHING",
            color = GoldPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dealer Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DEALER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                PlayingCardView(card = game.dealerCard, width = 64.dp, cardBackTheme = cardBackTheme)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("SELECT A HIGHER CARD:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // 4 Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            game.options.forEachIndexed { index, card ->
                PlayingCardView(
                    card = card,
                    width = 60.dp,
                    cardBackTheme = cardBackTheme,
                    onClick = if (game.selectedIndex == null) {
                        { onSelectCard(index) }
                    } else null
                )
            }
        }

        if (game.resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = game.resultText,
                color = if (game.isWin == true) GoldPrimary else if (game.isWin == false) CasinoRed else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}
