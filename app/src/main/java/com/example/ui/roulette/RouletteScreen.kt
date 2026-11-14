package com.example.ui.roulette

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.ui.components.CasinoTopBar
import com.example.ui.components.ChipSelectorBar
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
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RouletteScreen(
    viewModel: RouletteViewModel,
    profile: PlayerProfile,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit,
    onEmergencyRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bets by viewModel.bets.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val wheelRotation by viewModel.wheelRotation.collectAsState()
    val ballRotation by viewModel.ballRotation.collectAsState()
    val winningNumber by viewModel.winningNumber.collectAsState()
    val history by viewModel.history.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val lastPayout by viewModel.lastPayout.collectAsState()

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CasinoTopBar(
                profile = profile,
                title = profile.selectedRouletteVariant.displayName.uppercase(),
                isSoundEnabled = isSoundEnabled,
                onToggleSound = onToggleSound,
                onBackClick = onBackClick,
                onEmergencyRefill = onEmergencyRefill
            )

            // History Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORY: ",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    history.forEach { num ->
                        val col = RouletteData.getColor(num)
                        val badgeColor = when (col) {
                            RouletteColor.RED -> CasinoRed
                            RouletteColor.BLACK -> Color(0xFF1E293B)
                            RouletteColor.GREEN -> Color(0xFF16A34A)
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .border(1.dp, GoldLight.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$num",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Status Marquee Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FireKernCard)
                    .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusMessage,
                    color = if (lastPayout > 0) GoldPrimary else NeonYellow,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // WHEEL CANVAS DISPLAY
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                RouletteWheelCanvas(
                    wheelRotation = wheelRotation,
                    ballRotation = ballRotation,
                    isSpinning = isSpinning
                )

                // Winning number center plaque
                winningNumber?.let { winNum ->
                    val winCol = RouletteData.getColor(winNum)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                when (winCol) {
                                    RouletteColor.RED -> CasinoRed
                                    RouletteColor.BLACK -> Color(0xFF1E293B)
                                    RouletteColor.GREEN -> Color(0xFF16A34A)
                                }
                            )
                            .border(2.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$winNum",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // BETTING BOARD
            RouletteBettingBoard(
                bets = bets,
                isSpinning = isSpinning,
                onPlaceBet = { key, type -> viewModel.placeBet(key, type) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chip Selector Bar
            ChipSelectorBar(
                selectedDenom = selectedChip,
                onSelectDenom = { viewModel.selectChip(it) },
                modifier = Modifier.fillMaxWidth(),
                playerLevel = profile.level
            )

            // Betting Summary & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TOTAL BET: $${viewModel.totalBet}",
                    color = GoldLight,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "WIN: $${lastPayout}",
                    color = if (lastPayout > 0) GoldPrimary else FireKernAsh.copy(alpha = 0.6f),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            // Action Buttons: CLEAR, 2X, REBET, SPIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearBets() },
                    enabled = !isSpinning && bets.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rl_clear_button")
                ) {
                    Text("CLEAR", color = GoldLight, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.doubleBets() },
                    enabled = !isSpinning && bets.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rl_double_button")
                ) {
                    Text("2X BET", color = GoldLight, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.rebet() },
                    enabled = !isSpinning,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rl_rebet_button")
                ) {
                    Text("REBET", color = GoldLight, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.spin() },
                    enabled = !isSpinning && viewModel.totalBet > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("rl_spin_button")
                ) {
                    Text(
                        text = if (isSpinning) "SPINNING" else "SPIN",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RouletteWheelCanvas(
    wheelRotation: Float,
    ballRotation: Float,
    isSpinning: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f
        val innerRadius = outerRadius * 0.72f
        val centerHubRadius = outerRadius * 0.35f

        // Outer wooden rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5A1A05), Color(0xFF2C0B02)),
                center = center,
                radius = outerRadius
            ),
            radius = outerRadius,
            center = center
        )

        // Outer brass ring
        drawCircle(
            color = GoldDark,
            radius = outerRadius - 4.dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Ball track ring
        drawCircle(
            color = Color(0xFF1E293B),
            radius = outerRadius - 12.dp.toPx(),
            center = center
        )

        // Rotate wheel
        rotate(degrees = wheelRotation, pivot = center) {
            val sweep = 360f / 37f

            for (i in 0 until 37) {
                val num = RouletteData.WHEEL_NUMBERS[i]
                val color = when (RouletteData.getColor(num)) {
                    RouletteColor.RED -> CasinoRed
                    RouletteColor.BLACK -> Color(0xFF0F172A)
                    RouletteColor.GREEN -> Color(0xFF15803D)
                }

                drawArc(
                    color = color,
                    startAngle = i * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2)
                )

                // Divider frets
                val angleRad = Math.toRadians((i * sweep).toDouble())
                val x1 = center.x + (centerHubRadius) * cos(angleRad).toFloat()
                val y1 = center.y + (centerHubRadius) * sin(angleRad).toFloat()
                val x2 = center.x + (innerRadius) * cos(angleRad).toFloat()
                val y2 = center.y + (innerRadius) * sin(angleRad).toFloat()
                drawLine(
                    color = GoldLight.copy(alpha = 0.8f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Inner gold ring
            drawCircle(
                color = GoldPrimary,
                radius = centerHubRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Center brass turret
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldLight, GoldDark, Color(0xFF78350F)),
                    center = center,
                    radius = centerHubRadius * 0.7f
                ),
                radius = centerHubRadius * 0.7f,
                center = center
            )
        }

        // Draw Ivory Roulette Ball
        val ballTrackRadius = if (isSpinning) outerRadius - 16.dp.toPx() else innerRadius * 0.85f
        val ballAngleRad = Math.toRadians(ballRotation.toDouble())
        val ballX = center.x + ballTrackRadius * cos(ballAngleRad).toFloat()
        val ballY = center.y + ballTrackRadius * sin(ballAngleRad).toFloat()

        // Ball shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 5.dp.toPx(),
            center = Offset(ballX + 1.dp.toPx(), ballY + 1.dp.toPx())
        )
        // White ivory ball
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                center = Offset(ballX - 1.dp.toPx(), ballY - 1.dp.toPx()),
                radius = 5.dp.toPx()
            ),
            radius = 4.5.dp.toPx(),
            center = Offset(ballX, ballY)
        )
    }
}

@Composable
private fun RouletteBettingBoard(
    bets: Map<String, ActiveRouletteBet>,
    isSpinning: Boolean,
    onPlaceBet: (String, RouletteBetType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FireKernEmberDark)
            .border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zero Cell (Row at top)
        BoardCell(
            label = "0",
            betAmount = bets["0"]?.amount,
            backgroundColor = Color(0xFF16A34A),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            onClick = { if (!isSpinning) onPlaceBet("0", RouletteBetType.Straight(0)) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 12 Rows of 3 Numbers: (1,2,3), (4,5,6), ... (34,35,36)
        for (row in 0 until 12) {
            val n1 = row * 3 + 1
            val n2 = row * 3 + 2
            val n3 = row * 3 + 3

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(n1, n2, n3).forEach { num ->
                    val isRed = RouletteData.RED_NUMBERS.contains(num)
                    val bg = if (isRed) CasinoRed else Color(0xFF1E293B)
                    BoardCell(
                        label = "$num",
                        betAmount = bets["$num"]?.amount,
                        backgroundColor = bg,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isSpinning) onPlaceBet("$num", RouletteBetType.Straight(num)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // 2 to 1 Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BoardCell(
                label = "2 to 1",
                betAmount = bets["col1"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("col1", RouletteBetType.Column1) }
            )
            BoardCell(
                label = "2 to 1",
                betAmount = bets["col2"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("col2", RouletteBetType.Column2) }
            )
            BoardCell(
                label = "2 to 1",
                betAmount = bets["col3"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("col3", RouletteBetType.Column3) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dozens: 1st 12, 2nd 12, 3rd 12
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BoardCell(
                label = "1st 12",
                betAmount = bets["doz1"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("doz1", RouletteBetType.Dozen1) }
            )
            BoardCell(
                label = "2nd 12",
                betAmount = bets["doz2"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("doz2", RouletteBetType.Dozen2) }
            )
            BoardCell(
                label = "3rd 12",
                betAmount = bets["doz3"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("doz3", RouletteBetType.Dozen3) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Outside Bets: 1-18, EVEN, RED, BLACK, ODD, 19-36
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BoardCell(
                label = "1-18",
                betAmount = bets["low"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("low", RouletteBetType.Low) }
            )
            BoardCell(
                label = "EVEN",
                betAmount = bets["even"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("even", RouletteBetType.Even) }
            )
            BoardCell(
                label = "RED",
                betAmount = bets["red"]?.amount,
                backgroundColor = CasinoRed,
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("red", RouletteBetType.Red) }
            )
            BoardCell(
                label = "BLACK",
                betAmount = bets["black"]?.amount,
                backgroundColor = Color(0xFF1E293B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("black", RouletteBetType.Black) }
            )
            BoardCell(
                label = "ODD",
                betAmount = bets["odd"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("odd", RouletteBetType.Odd) }
            )
            BoardCell(
                label = "19-36",
                betAmount = bets["high"]?.amount,
                backgroundColor = Color(0xFF1E3A2B),
                modifier = Modifier.weight(1f),
                onClick = { if (!isSpinning) onPlaceBet("high", RouletteBetType.High) }
            )
        }
    }
}

@Composable
private fun BoardCell(
    label: String,
    betAmount: Long?,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(1.dp, GoldLight.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .testTag("cell_$label"),
        contentAlignment = Alignment.Center
    ) {
        if (betAmount != null && betAmount > 0) {
            // Chip on top
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary)
                    .border(1.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$betAmount",
                    color = Color.Black,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp
                )
            }
        } else {
            Text(
                text = label,
                color = Color.White,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
