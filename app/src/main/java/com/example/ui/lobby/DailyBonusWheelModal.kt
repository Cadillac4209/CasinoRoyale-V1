package com.example.ui.lobby

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.model.DailyBonusWheelCatalog
import com.example.model.DailyLoginRecord
import com.example.model.DailyWheelSpinResult
import com.example.model.PlayerProfile
import com.example.model.WheelPrizeType
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCardElevated
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernEmberDark
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.FireKernObsidian
import com.example.ui.theme.FireKernVoid
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyBonusWheelModal(
    profile: PlayerProfile,
    dailyLoginRecord: DailyLoginRecord? = null,
    onSpinAndClaim: (Int) -> DailyWheelSpinResult?,
    onDismiss: () -> Unit,
    soundManager: SoundManager
) {
    val slices = DailyBonusWheelCatalog.slices
    val scope = rememberCoroutineScope()
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val wheelStatus = remember(profile.lastDailyBonusTime, nowMs) {
        DailyBonusWheelCatalog.evaluateStatus(
            lastSpinTimestampMs = profile.lastDailyBonusTime,
            currentTimeMs = nowMs
        )
    }

    var isSpinning by remember { mutableStateOf(false) }
    var currentRotation by remember { mutableFloatStateOf(0f) }
    var winningSliceIndex by remember { mutableIntStateOf(-1) }
    var spinResult by remember { mutableStateOf<DailyWheelSpinResult?>(null) }

    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = tween(
            durationMillis = 3600,
            easing = CubicBezierEasing(0.10f, 0.86f, 0.22f, 1.0f)
        ),
        label = "daily_bonus_wheel_spin"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wheel_marquee")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wheel_cta_pulse"
    )
    val marqueePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wheel_bulbs"
    )

    Dialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    2.dp,
                    Brush.verticalGradient(listOf(GoldPrimary, FireKernCrimson, FireKernCardBorder)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("daily_bonus_wheel_dialog"),
            color = FireKernObsidian
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldPrimary, FireKernEmber)))
                                .border(1.5.dp, GoldLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = FireKernVoid,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DAILY BONUS WHEEL",
                                color = GoldPrimary,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "1 Free Spin Every 24 Hours • Chips or Royale Points",
                                color = FireKernAsh,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isSpinning) {
                                soundManager.playButtonClick()
                                onDismiss()
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("wheel_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Daily Bonus Wheel",
                            tint = if (isSpinning) FireKernCardBorder else FireKernAsh
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 24-Hour Status & Cooldown Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wheel_status_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (wheelStatus.canSpin) FireKernEmberDark else FireKernCard
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (wheelStatus.canSpin) GoldPrimary else FireKernCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (wheelStatus.canSpin) Icons.Default.AutoAwesome else Icons.Default.LockClock,
                                contentDescription = null,
                                tint = if (wheelStatus.canSpin) GoldPrimary else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (wheelStatus.canSpin) {
                                        "24-HOUR SPIN READY"
                                    } else {
                                        "NEXT FREE SPIN IN"
                                    },
                                    color = if (wheelStatus.canSpin) GoldLight else FireKernAsh,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = if (wheelStatus.canSpin) {
                                        val voidBoost = profile.voidWallet.activePowerTier.bonusMultiplier
                                        if (voidBoost > 1f) {
                                            "Spin now! Active \$VOID Power adds ${((voidBoost - 1f) * 100).toInt()}% Chip Bonus"
                                        } else {
                                            "Win up to $25,000 Chips or 500 Royale Points"
                                        }
                                    } else {
                                        wheelStatus.formattedCountdown
                                    },
                                    color = if (wheelStatus.canSpin) NeonCyan else GoldPrimary,
                                    fontFamily = if (wheelStatus.canSpin) DisplayFontFamily else MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (wheelStatus.canSpin) 11.sp else 16.sp
                                )
                            }
                        }

                        if (!wheelStatus.canSpin && wheelStatus.lastSpinTimestampMs > 0L) {
                            val lastFormatted = remember(wheelStatus.lastSpinTimestampMs) {
                                DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)
                                    .format(Date(wheelStatus.lastSpinTimestampMs))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LAST SPIN",
                                    color = FireKernAsh,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lastFormatted,
                                    color = Color.White,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Wheel Canvas with Top Pointer & Slice Labels
                Box(
                    modifier = Modifier
                        .size(264.dp)
                        .testTag("daily_bonus_wheel_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(248.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadius = size.width / 2f
                        val wheelRadius = outerRadius - 12.dp.toPx()
                        val wheelTopLeft = Offset(center.x - wheelRadius, center.y - wheelRadius)
                        val wheelSize = Size(wheelRadius * 2f, wheelRadius * 2f)
                        val sweep = 360f / slices.size

                        // Outer metallic gold bezel
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldLight, GoldDark, FireKernVoid),
                                center = center,
                                radius = outerRadius
                            ),
                            radius = outerRadius,
                            center = center
                        )

                        // 16 twinkling rim bulbs
                        val bulbCount = 16
                        val bulbRingRadius = outerRadius - 6.dp.toPx()
                        for (b in 0 until bulbCount) {
                            val angleRad = Math.toRadians((b * (360.0 / bulbCount)) - 90.0)
                            val bx = center.x + bulbRingRadius * cos(angleRad).toFloat()
                            val by = center.y + bulbRingRadius * sin(angleRad).toFloat()
                            val isEven = b % 2 == 0
                            val litAlpha = if (isEven) 0.55f + 0.45f * marqueePhase else 1.0f - 0.45f * marqueePhase
                            drawCircle(
                                color = if (isEven) GoldLight.copy(alpha = litAlpha) else FireKernFlame.copy(alpha = litAlpha),
                                radius = 3.2.dp.toPx(),
                                center = Offset(bx, by)
                            )
                        }

                        // Rotating wheel slices & radial text labels
                        rotate(degrees = animatedRotation, pivot = center) {
                            for (i in slices.indices) {
                                val slice = slices[i]
                                val startAngle = -90f + i * sweep

                                drawArc(
                                    color = Color(slice.colorHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = wheelTopLeft,
                                    size = wheelSize
                                )

                                // Highlight border around slice divider
                                val dividerRad = Math.toRadians(startAngle.toDouble())
                                drawLine(
                                    color = GoldLight.copy(alpha = 0.85f),
                                    start = center,
                                    end = Offset(
                                        center.x + wheelRadius * cos(dividerRad).toFloat(),
                                        center.y + wheelRadius * sin(dividerRad).toFloat()
                                    ),
                                    strokeWidth = 1.8.dp.toPx()
                                )

                                // Draw rotated text label inside each wedge
                                val midAngleDeg = startAngle + sweep / 2f
                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas
                                    nativeCanvas.save()
                                    nativeCanvas.translate(center.x, center.y)
                                    nativeCanvas.rotate(midAngleDeg)

                                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color(slice.accentTextColorHex).toArgb()
                                        textSize = 12.5.sp.toPx()
                                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                        textAlign = Paint.Align.CENTER
                                    }
                                    val textDistance = wheelRadius * 0.65f
                                    nativeCanvas.drawText(slice.wheelLabel, textDistance, 4.dp.toPx(), textPaint)
                                    nativeCanvas.restore()
                                }
                            }

                            // Inner wheel rim ring
                            drawCircle(
                                color = GoldPrimary,
                                radius = wheelRadius,
                                center = center,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Center Hub Medallion
                        drawCircle(
                            color = FireKernVoid,
                            radius = 28.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = GoldPrimary,
                            radius = 25.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawIntoCanvas { canvas ->
                            val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = GoldLight.toArgb()
                                textSize = 13.sp.toPx()
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.nativeCanvas.drawText("ROYALE", center.x, center.y + 4.5.dp.toPx(), hubPaint)
                        }
                    }

                    // Top 12 o'clock Downward Pointer
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 28.dp, height = 24.dp)
                    ) {
                        val pointerPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path = pointerPath, color = GoldPrimary)
                        drawPath(
                            path = pointerPath,
                            color = FireKernVoid,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Celebration Win Card
                AnimatedVisibility(
                    visible = spinResult != null,
                    enter = fadeIn() + scaleIn()
                ) {
                    spinResult?.let { result ->
                        val isRoyalePoints = result.slice.prizeType == WheelPrizeType.ROYALE_POINTS
                        val accentColor = if (isRoyalePoints) NeonCyan else GoldPrimary
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("wheel_win_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = FireKernCardElevated),
                            border = BorderStroke(1.5.dp, accentColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRoyalePoints) "ROYALE POINTS WON!" else "VIRTUAL CHIPS WON!",
                                        color = accentColor,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.7.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (isRoyalePoints) {
                                        "+${numberFormatter.format(result.royalePointsAwarded)} RP"
                                    } else {
                                        "+$${numberFormatter.format(result.chipsAwarded)} CHIPS"
                                    },
                                    color = Color.White,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (isRoyalePoints) {
                                        "Balance: ${numberFormatter.format(result.newRoyalePoints)} RP • ${profile.loyaltyTier.title}"
                                    } else {
                                        val boostText = if (result.chipsAwarded > result.slice.amount) {
                                            " (includes \$VOID Power boost)"
                                        } else ""
                                        "New Bankroll: $${numberFormatter.format(result.newBankroll)}$boostText"
                                    },
                                    color = FireKernAsh,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Prize Legend (Chips vs Royale Points)
                Text(
                    text = "WHEEL PRIZES • CHIPS & ROYALE POINTS",
                    color = FireKernAsh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEachIndexed { idx, slice ->
                        val isWinner = idx == winningSliceIndex && spinResult != null
                        val isRp = slice.prizeType == WheelPrizeType.ROYALE_POINTS
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isWinner) Color(slice.colorHex) else FireKernCard
                                )
                                .border(
                                    width = if (isWinner) 1.5.dp else 1.dp,
                                    color = if (isWinner) GoldLight else if (isRp) NeonCyan.copy(alpha = 0.45f) else GoldDark.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = slice.wheelLabel,
                                color = if (isWinner) Color(slice.accentTextColorHex) else if (isRp) NeonCyan else GoldLight,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary CTA & Dismiss Controls
                if (spinResult != null) {
                    Button(
                        onClick = {
                            soundManager.playChipCollect()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = FireKernVoid
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("claim_bonus_button")
                    ) {
                        Text("COLLECT & CONTINUE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                } else if (wheelStatus.canSpin) {
                    Button(
                        onClick = {
                            if (!isSpinning && wheelStatus.canSpin) {
                                scope.launch {
                                    isSpinning = true
                                    soundManager.playSpinReels()
                                    val chosenIndex = DailyBonusWheelCatalog.pickWeightedSliceIndex()
                                    winningSliceIndex = chosenIndex
                                    currentRotation = DailyBonusWheelCatalog.computeTargetRotationDegrees(
                                        currentRotationDeg = currentRotation,
                                        targetSliceIndex = chosenIndex,
                                        sliceCount = slices.size,
                                        extraFullSpins = 6
                                    )
                                    delay(3700L)
                                    val result = onSpinAndClaim(chosenIndex)
                                    spinResult = result
                                    if (result != null) {
                                        soundManager.playBigWin()
                                    }
                                    isSpinning = false
                                }
                            }
                        },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = FireKernVoid
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(if (!isSpinning) pulseScale else 1f)
                            .testTag("spin_wheel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSpinning) "SPINNING..." else "SPIN DAILY WHEEL",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = FireKernCardElevated,
                                disabledContentColor = FireKernAsh
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("spin_wheel_cooldown_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UNLOCKS IN ${wheelStatus.formattedCountdown}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                soundManager.playButtonClick()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FireKernCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("wheel_dismiss_button")
                        ) {
                            Text("CLOSE", color = GoldLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Cloud wheel sync timestamp note if available
                dailyLoginRecord?.lastWheelSpinAt?.let { ts ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val syncedAt = remember(ts) {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US)
                            .format(ts.toDate())
                    }
                    Text(
                        text = "Cloud Cooldown Synced • $syncedAt",
                        color = FireKernAsh.copy(alpha = 0.75f),
                        fontFamily = MonoFontFamily,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
