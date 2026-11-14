package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChipBlack
import com.example.ui.theme.ChipBlue
import com.example.ui.theme.ChipGold
import com.example.ui.theme.ChipGreen
import com.example.ui.theme.ChipPurple
import com.example.ui.theme.ChipRed
import com.example.ui.theme.ChipWhite
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan

enum class ChipDenom(
    val value: Long,
    val label: String,
    val primaryColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val requiredLevel: Int = 1
) {
    TEN(10L, "10", ChipWhite, ChipBlue, Color(0xFF1E293B), 1),
    TWENTY_FIVE(25L, "25", ChipGreen, ChipWhite, Color.White, 1),
    FIFTY(50L, "50", ChipBlue, ChipWhite, Color.White, 1),
    HUNDRED(100L, "100", ChipBlack, ChipGold, GoldLight, 1),
    FIVE_HUNDRED(500L, "500", ChipPurple, ChipWhite, Color.White, 1),
    THOUSAND(1000L, "1K", ChipGold, ChipBlack, Color(0xFF451A03), 1),
    TWO_THOUSAND_FIVE_HUNDRED(2500L, "2.5K", Color(0xFF7C3AED), GoldLight, Color.White, 4),
    FIVE_THOUSAND(5000L, "5K", Color(0xFF0F172A), GoldPrimary, GoldPrimary, 7),
    TWENTY_FIVE_THOUSAND(25000L, "25K", Color(0xFF0284C7), NeonCyan, Color.White, 15)
}

@Composable
fun CasinoChipView(
    denom: ChipDenom,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val offsetY by animateDpAsState(targetValue = if (isSelected) (-8).dp else 0.dp, label = "chip_lift")

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .size(size)
            .shadow(if (isSelected) 10.dp else 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        denom.primaryColor,
                        denom.primaryColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) GoldPrimary else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .drawBehind {
                val radius = size.toPx() / 2f
                val center = Offset(radius, radius)
                val notchCount = 6
                for (i in 0 until notchCount) {
                    val angle = Math.toRadians((i * (360.0 / notchCount)))
                    val stripeX = center.x + (radius - 4.dp.toPx()) * Math.cos(angle).toFloat()
                    val stripeY = center.y + (radius - 4.dp.toPx()) * Math.sin(angle).toFloat()
                    drawCircle(
                        color = denom.accentColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(stripeX, stripeY)
                    )
                }
                drawCircle(
                    color = denom.accentColor.copy(alpha = 0.7f),
                    radius = radius * 0.65f,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable { onClick() }
                        .testTag("chip_${denom.label}")
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$${denom.label}",
            color = denom.textColor,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = if (size > 44.dp) 12.sp else 9.sp,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun ChipSelectorBar(
    selectedDenom: ChipDenom,
    onSelectDenom: (ChipDenom) -> Unit,
    modifier: Modifier = Modifier,
    playerLevel: Int = 1
) {
    val availableChips = ChipDenom.values().filter { playerLevel >= it.requiredLevel }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableChips.forEach { denom ->
            CasinoChipView(
                denom = denom,
                size = 48.dp,
                isSelected = denom == selectedDenom,
                onClick = { onSelectDenom(denom) }
            )
        }
    }
}
