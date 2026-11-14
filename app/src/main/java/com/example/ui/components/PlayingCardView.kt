package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.CardBackTheme
import com.example.model.Rank
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily

@Composable
fun PlayingCardView(
    card: Card?,
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    isHeld: Boolean = false,
    cardBackTheme: CardBackTheme = CardBackTheme.ROYAL_RED,
    onClick: (() -> Unit)? = null
) {
    val cardHeight = width * 1.42f
    val isFaceUp = card?.isFaceUp ?: false

    val rotationY by animateFloatAsState(
        targetValue = if (isFaceUp) 0f else 180f,
        animationSpec = tween(350),
        label = "card_flip"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(cardHeight)
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            }
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable { onClick() }
                        .testTag("card_${card?.id ?: "empty"}")
                } else Modifier
            )
    ) {
        if (rotationY < 90f && card != null && card.isFaceUp) {
            // Front Face
            CardFace(card = card, width = width, isHeld = isHeld)
        } else {
            // Card Back
            CardBack(width = width, theme = cardBackTheme)
        }

        // Held Banner overlay (Video Poker)
        if (isHeld) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(GoldPrimary, GoldDark)
                        )
                    )
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Held",
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "HELD",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFace(card: Card, width: Dp, isHeld: Boolean) {
    val suitColor = if (card.isRed) CasinoRed else FireKernVoid
    val rankText = card.rank.symbol
    val suitSymbol = card.suit.symbol

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBF4))
            .border(
                width = if (isHeld) 2.dp else 1.dp,
                color = if (isHeld) GoldPrimary else Color(0xFFE6D5B8),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Top Left Corner Rank & Suit
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = rankText,
                color = suitColor,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (width > 60.dp) 14.sp else 11.sp,
                lineHeight = 14.sp
            )
            Text(
                text = suitSymbol,
                color = suitColor,
                fontSize = if (width > 60.dp) 12.sp else 10.sp,
                lineHeight = 12.sp
            )
        }

        // Center Symbol / Face Emblem
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                card.rank.isFaceCard -> {
                    // Face card crown / portrait badge
                    Box(
                        modifier = Modifier
                            .size(if (width > 60.dp) 32.dp else 24.dp)
                            .background(
                                color = if (card.isRed) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, suitColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rankText,
                            color = suitColor,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = if (width > 60.dp) 18.sp else 14.sp
                        )
                    }
                }
                card.rank == Rank.ACE -> {
                    // Grand Ace suit symbol
                    Text(
                        text = suitSymbol,
                        color = suitColor,
                        fontSize = if (width > 60.dp) 36.sp else 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {
                    // Numerical suit badge
                    Text(
                        text = suitSymbol,
                        color = suitColor,
                        fontSize = if (width > 60.dp) 26.sp else 20.sp
                    )
                }
            }
        }

        // Bottom Right Corner Rank & Suit (Rotated 180)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(180f)
        ) {
            Text(
                text = rankText,
                color = suitColor,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (width > 60.dp) 14.sp else 11.sp,
                lineHeight = 14.sp
            )
            Text(
                text = suitSymbol,
                color = suitColor,
                fontSize = if (width > 60.dp) 12.sp else 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun CardBack(width: Dp, theme: CardBackTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationY = 180f }
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.color1, theme.color2)
                )
            )
            .border(2.dp, theme.accentColor, RoundedCornerShape(8.dp))
            .padding(4.dp)
            .drawBehind {
                // Diamond lattice pattern
                val step = 10.dp.toPx()
                for (x in 0..(size.width.toInt()) step step.toInt()) {
                    drawLine(
                        color = theme.accentColor.copy(alpha = 0.35f),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat() + size.height, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = theme.accentColor.copy(alpha = 0.35f),
                        start = Offset(x.toFloat(), size.height),
                        end = Offset(x.toFloat() + size.height, 0f),
                        strokeWidth = 1f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Royal Casino Seal in center
        Box(
            modifier = Modifier
                .size(if (width > 60.dp) 26.dp else 20.dp)
                .background(theme.accentColor, RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = theme.emblem,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = if (width > 60.dp) 14.sp else 10.sp
            )
        }
    }
}
