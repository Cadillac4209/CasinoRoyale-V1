package com.example.ui.lobby

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.ui.theme.CasinoRed
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FireKernAsh
import com.example.ui.theme.FireKernCard
import com.example.ui.theme.FireKernCardBorder
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmber
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DailyBonusWheelModal(
    onClaimReward: (Long) -> Unit,
    onDismiss: () -> Unit,
    soundManager: SoundManager
) {
    val prizes = listOf(1000L, 2500L, 5000L, 10000L, 25000L, 1500L, 3000L, 50000L)
    val sliceColors = listOf(
        Color(0xFF2563EB), Color(0xFFDC2626), Color(0xFF16A34A), Color(0xFF9333EA),
        GoldPrimary, Color(0xFF0284C7), Color(0xFFE11D48), Color(0xFFF59E0B)
    )

    var isSpinning by remember { mutableStateOf(false) }
    var currentRotation by remember { mutableFloatStateOf(0f) }
    var wonPrize by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = tween(durationMillis = 3500),
        label = "bonus_wheel_spin"
    )

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FIRE KERN BONUS WHEEL",
                    color = GoldPrimary,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SPIN FOR MOLTEN CASINO CHIPS",
                    color = FireKernAsh,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wheel Canvas
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f
                        val sweep = 360f / prizes.size

                        rotate(degrees = animatedRotation, pivot = center) {
                            for (i in prizes.indices) {
                                drawArc(
                                    color = sliceColors[i % sliceColors.size],
                                    startAngle = i * sweep,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = Offset.Zero,
                                    size = size
                                )

                                val angleRad = Math.toRadians((i * sweep).toDouble())
                                val lineX = center.x + radius * cos(angleRad).toFloat()
                                val lineY = center.y + radius * sin(angleRad).toFloat()
                                drawLine(
                                    color = GoldLight,
                                    start = center,
                                    end = Offset(lineX, lineY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // Rim
                            drawCircle(
                                color = GoldPrimary,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }

                        // Center Hub
                        drawCircle(
                            color = FireKernVoid,
                            radius = 24.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = GoldPrimary,
                            radius = 20.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Top indicator arrow pointer
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(FireKernEmber)
                            .border(1.5.dp, GoldLight, CircleShape)
                    )
                }

                if (wonPrize > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "YOU WON +$$wonPrize CHIPS!",
                        color = GoldPrimary,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            if (wonPrize > 0) {
                Button(
                    onClick = {
                        onClaimReward(wonPrize)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("claim_bonus_button")
                ) {
                    Text("CLAIM CHIPS", fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = {
                        if (!isSpinning) {
                            scope.launch {
                                isSpinning = true
                                soundManager.playSpinReels()
                                val prizeIdx = Random.nextInt(prizes.size)
                                val sweep = 360f / prizes.size
                                // Target the top pointer
                                val spins = 5 * 360f
                                val targetAngle = spins + (prizes.size - prizeIdx) * sweep - (sweep / 2)
                                currentRotation += targetAngle

                                delay(3600)
                                wonPrize = prizes[prizeIdx]
                                soundManager.playBigWin()
                                isSpinning = false
                            }
                        }
                    },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("spin_wheel_button")
                ) {
                    Text(if (isSpinning) "SPINNING..." else "SPIN WHEEL!", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            if (!isSpinning) {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = FireKernAsh)
                }
            }
        },
        containerColor = FireKernObsidian
    )
}
