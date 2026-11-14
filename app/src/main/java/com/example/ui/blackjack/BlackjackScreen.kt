package com.example.ui.blackjack

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.ui.components.CasinoChipView
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.ChipSelectorBar
import com.example.ui.components.PlayingCardView
import com.example.ui.components.WinCelebrationOverlay
import com.example.ui.theme.CasinoFeltDark
import com.example.ui.theme.CasinoFeltGreen
import com.example.ui.theme.CasinoFeltMedium
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.CasinoSurfaceBorder
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow

@Composable
fun BlackjackScreen(
    viewModel: BlackjackViewModel,
    profile: PlayerProfile,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage by viewModel.stage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val currentBet by viewModel.currentBet.collectAsState()
    val dealerHand by viewModel.dealerHand.collectAsState()
    val playerHands by viewModel.playerHands.collectAsState()
    val activeHandIndex by viewModel.activeHandIndex.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val lastPayout by viewModel.lastPayout.collectAsState()

    val isBettingPhase = stage == BlackjackStage.BETTING || stage == BlackjackStage.ROUND_OVER
    val activeHand = playerHands.getOrNull(activeHandIndex) ?: playerHands.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(profile.selectedFelt.primaryColor, profile.selectedFelt.mediumColor, profile.selectedFelt.darkColor),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CasinoTopBar(
                profile = profile,
                title = profile.selectedBlackjackVariant.displayName.uppercase(),
                isSoundEnabled = isSoundEnabled,
                onToggleSound = onToggleSound,
                onBackClick = onBackClick,
                onEmergencyRefill = onEmergencyRefill
            )

            // Felt Table Header Rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (profile.selectedBlackjackVariant == com.example.model.BlackjackVariant.HIGH_ROLLER_VIP) {
                        "VIP HIGH ROLLER • 2:1 SUITED BLACKJACK • TABLE LIMIT $${profile.maxBetAllowed}"
                    } else {
                        "BLACKJACK PAYS 3 TO 2 • DEALER MUST STAND ON 17"
                    },
                    color = GoldLight.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }

            // DEALER AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dealer score tag
                val dealerScore = if (dealerHand.isNotEmpty()) {
                    val visibleCards = dealerHand.filter { it.isFaceUp }
                    if (visibleCards.size == dealerHand.size) {
                        val score = viewModel.calculateScore(dealerHand)
                        if (score > 21) "DEALER BUST ($score)" else "DEALER: $score"
                    } else {
                        "DEALER: ${viewModel.calculateScore(visibleCards)}"
                    }
                } else "DEALER"

                Text(
                    text = dealerScore,
                    color = GoldLight,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Dealer Cards Row (overlapping cards)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dealerHand.forEachIndexed { index, card ->
                        Box(
                            modifier = Modifier
                                .offset(x = if (index > 0) (-18 * index).dp else 0.dp)
                        ) {
                            PlayingCardView(card = card, width = 64.dp, cardBackTheme = profile.selectedCardBack)
                        }
                    }
                }
            }

            // STATUS BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FireKernCard.copy(alpha = 0.92f))
                    .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage,
                    color = when {
                        lastPayout > 0 -> GoldPrimary
                        stage == BlackjackStage.ROUND_OVER -> GoldLight
                        else -> NeonYellow
                    },
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // PLAYER HANDS AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    playerHands.forEachIndexed { idx, hand ->
                        val isActive = idx == activeHandIndex && stage == BlackjackStage.PLAYER_TURN
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .border(
                                    width = if (isActive) 1.5.dp else 0.dp,
                                    color = if (isActive) GoldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(6.dp)
                        ) {
                            Text(
                                text = if (playerHands.size > 1) "HAND ${idx + 1}: ${hand.displayScore}" else "PLAYER: ${hand.displayScore}",
                                color = if (isActive) GoldPrimary else GoldLight,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                hand.cards.forEachIndexed { cardIdx, card ->
                                    Box(
                                        modifier = Modifier.offset(
                                            x = if (cardIdx > 0) (-16 * cardIdx).dp else 0.dp
                                        )
                                    ) {
                                        PlayingCardView(card = card, width = 64.dp, cardBackTheme = profile.selectedCardBack)
                                    }
                                }
                            }
                            if (hand.bet > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("BET: $${hand.bet}", color = FireKernAsh, fontFamily = MonoFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // INSURANCE DECISION ROW
            AnimatedVisibility(visible = stage == BlackjackStage.INSURANCE_OFFER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.takeInsurance(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("insurance_yes")
                    ) {
                        Text("TAKE INSURANCE ($${currentBet / 2})", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.takeInsurance(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("insurance_no")
                    ) {
                        Text("DECLINE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // BETTING FELT CIRCLE (When in betting phase)
            if (isBettingPhase) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(CasinoFeltDark.copy(alpha = 0.6f))
                        .border(2.dp, GoldPrimary, CircleShape)
                        .clickable { viewModel.addChipBet() }
                        .testTag("betting_spot"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentBet > 0) {
                            CasinoChipView(denom = selectedChip, size = 44.dp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$$currentBet",
                                color = GoldPrimary,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "TAP TO\nBET",
                                color = GoldLight.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ChipSelectorBar(
                    selectedDenom = selectedChip,
                    onSelectDenom = { viewModel.selectChip(it) },
                    modifier = Modifier.fillMaxWidth(),
                    playerLevel = profile.level
                )

                // Bet controls: CLEAR, REBET, DEAL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearBet() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("clear_bet_button")
                    ) {
                        Text("CLEAR", color = GoldLight, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.rebet() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("rebet_button")
                    ) {
                        Text("REBET", color = GoldLight, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.deal() },
                        enabled = currentBet > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("bj_deal_button")
                    ) {
                        Text("DEAL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            } else if (stage == BlackjackStage.PLAYER_TURN && activeHand != null) {
                // ACTION BUTTONS: HIT, STAND, DOUBLE, SPLIT, SURRENDER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.hit() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("bj_hit_button")
                        ) {
                            Text("HIT", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Button(
                            onClick = { viewModel.stand() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CasinoRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("bj_stand_button")
                        ) {
                            Text("STAND", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    // Secondary actions: DOUBLE, SPLIT, SURRENDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeHand.canDouble) {
                            Button(
                                onClick = { viewModel.doubleDown() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("bj_double_button")
                            ) {
                                Text("DOUBLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (activeHand.canSplit && playerHands.size == 1) {
                            Button(
                                onClick = { viewModel.split() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9333EA),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("bj_split_button")
                            ) {
                                Text("SPLIT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (activeHand.cards.size == 2 && playerHands.size == 1) {
                            Button(
                                onClick = { viewModel.surrender() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF475569),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("bj_surrender_button")
                            ) {
                                Text("SURRENDER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
