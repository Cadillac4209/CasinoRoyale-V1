package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.FireKernCrimson
import com.example.ui.theme.FireKernEmber
import com.example.ui.theme.FireKernFlame
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class LevelUpParticleKind {
    EMBER_SPARK,
    GOLD_COIN,
    DIAMOND_STAR,
    CONFETTI_RIBBON
}

data class LevelUpParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val kind: LevelUpParticleKind,
    val color: Color,
    val secondaryColor: Color,
    var rotationDeg: Float,
    val rotationSpeedDeg: Float,
    var phase: Float,
    val flutterSpeed: Float,
    val gravity: Float,
    val drag: Float,
    var ageSec: Float = 0f,
    val maxLifeSec: Float
) {
    val normalizedLife: Float
        get() = if (maxLifeSec <= 0f) 1f else (ageSec / maxLifeSec).coerceIn(0f, 1f)

    val isAlive: Boolean
        get() = ageSec < maxLifeSec

    val alpha: Float
        get() {
            val t = normalizedLife
            return when {
                t < 0.08f -> (t / 0.08f).coerceIn(0f, 1f)
                t > 0.65f -> (1f - (t - 0.65f) / 0.35f).coerceIn(0f, 1f)
                else -> 1f
            }
        }
}

data class LevelUpShockwave(
    val originX: Float,
    val originY: Float,
    val maxRadius: Float,
    val color: Color,
    val strokeWidth: Float,
    var ageSec: Float = 0f,
    val durationSec: Float = 0.85f
) {
    val progress: Float
        get() = if (durationSec <= 0f) 1f else (ageSec / durationSec).coerceIn(0f, 1f)

    val isAlive: Boolean
        get() = ageSec < durationSec
}

object LevelUpParticleEngine {
    private val fireKernColors = listOf(
        GoldPrimary,
        GoldLight,
        FireKernFlame,
        FireKernEmber,
        NeonYellow,
        FireKernCrimson,
        NeonCyan
    )

    fun createRadialBurst(
        originX: Float,
        originY: Float,
        count: Int = 96,
        speedScale: Float = 1f,
        random: Random = Random.Default
    ): List<LevelUpParticle> {
        return List(count) { index ->
            val angle = (index.toFloat() / count.toFloat()) * (2f * PI.toFloat()) +
                random.nextFloat() * 0.35f - 0.175f
            val speed = (260f + random.nextFloat() * 760f) * speedScale
            val kindRoll = random.nextInt(100)
            val kind = when {
                kindRoll < 36 -> LevelUpParticleKind.EMBER_SPARK
                kindRoll < 62 -> LevelUpParticleKind.GOLD_COIN
                kindRoll < 82 -> LevelUpParticleKind.DIAMOND_STAR
                else -> LevelUpParticleKind.CONFETTI_RIBBON
            }
            val primaryColor = if (kind == LevelUpParticleKind.GOLD_COIN) {
                if (random.nextBoolean()) GoldPrimary else GoldLight
            } else {
                fireKernColors[random.nextInt(fireKernColors.size)]
            }
            val secondaryColor = when (kind) {
                LevelUpParticleKind.GOLD_COIN -> GoldDark
                LevelUpParticleKind.EMBER_SPARK -> GoldLight
                else -> FireKernFlame
            }
            val size = when (kind) {
                LevelUpParticleKind.EMBER_SPARK -> 6f + random.nextFloat() * 9f
                LevelUpParticleKind.GOLD_COIN -> 12f + random.nextFloat() * 10f
                LevelUpParticleKind.DIAMOND_STAR -> 10f + random.nextFloat() * 12f
                LevelUpParticleKind.CONFETTI_RIBBON -> 12f + random.nextFloat() * 12f
            }
            LevelUpParticle(
                x = originX + (random.nextFloat() - 0.5f) * 24f,
                y = originY + (random.nextFloat() - 0.5f) * 24f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - (160f * speedScale),
                size = size,
                kind = kind,
                color = primaryColor,
                secondaryColor = secondaryColor,
                rotationDeg = random.nextFloat() * 360f,
                rotationSpeedDeg = (random.nextFloat() - 0.5f) * 520f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                flutterSpeed = 5f + random.nextFloat() * 6f,
                gravity = when (kind) {
                    LevelUpParticleKind.EMBER_SPARK -> 260f
                    LevelUpParticleKind.CONFETTI_RIBBON -> 420f
                    else -> 580f
                },
                drag = when (kind) {
                    LevelUpParticleKind.CONFETTI_RIBBON -> 0.965f
                    else -> 0.982f
                },
                maxLifeSec = 1.65f + random.nextFloat() * 1.15f
            )
        }
    }

    fun createCornerFountain(
        originX: Float,
        originY: Float,
        aimRight: Boolean,
        count: Int = 28,
        random: Random = Random.Default
    ): List<LevelUpParticle> {
        val baseAngleDeg = if (aimRight) -62f else -118f
        return List(count) {
            val spreadDeg = (random.nextFloat() - 0.5f) * 38f
            val angleRad = ((baseAngleDeg + spreadDeg) * PI / 180.0).toFloat()
            val speed = 680f + random.nextFloat() * 580f
            val kind = if (random.nextInt(10) < 4) {
                LevelUpParticleKind.GOLD_COIN
            } else if (random.nextBoolean()) {
                LevelUpParticleKind.EMBER_SPARK
            } else {
                LevelUpParticleKind.DIAMOND_STAR
            }
            val color = if (kind == LevelUpParticleKind.GOLD_COIN) {
                GoldPrimary
            } else {
                fireKernColors[random.nextInt(fireKernColors.size)]
            }
            LevelUpParticle(
                x = originX,
                y = originY,
                vx = cos(angleRad) * speed,
                vy = sin(angleRad) * speed,
                size = 9f + random.nextFloat() * 10f,
                kind = kind,
                color = color,
                secondaryColor = GoldLight,
                rotationDeg = random.nextFloat() * 360f,
                rotationSpeedDeg = (random.nextFloat() - 0.5f) * 480f,
                phase = random.nextFloat() * 6.28f,
                flutterSpeed = 6f + random.nextFloat() * 5f,
                gravity = 540f,
                drag = 0.985f,
                maxLifeSec = 1.5f + random.nextFloat() * 0.9f
            )
        }
    }

    fun stepParticles(
        particles: MutableList<LevelUpParticle>,
        shockwaves: MutableList<LevelUpShockwave>,
        dtSec: Float
    ) {
        val dt = dtSec.coerceIn(0f, 0.05f)
        if (dt <= 0f) return

        val dragFactor = Math.pow(0.985, (dt * 60f).toDouble()).toFloat()

        val pIter = particles.listIterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.ageSec += dt
            if (!p.isAlive) {
                pIter.remove()
                continue
            }
            val perParticleDrag = if (p.drag != 0.985f) {
                Math.pow(p.drag.toDouble(), (dt * 60f).toDouble()).toFloat()
            } else {
                dragFactor
            }
            p.vx *= perParticleDrag
            p.vy = (p.vy * perParticleDrag) + (p.gravity * dt)
            p.phase += p.flutterSpeed * dt

            val flutterOffset = if (p.kind == LevelUpParticleKind.CONFETTI_RIBBON) {
                sin(p.phase) * 55f * dt
            } else {
                0f
            }
            p.x += (p.vx * dt) + flutterOffset
            p.y += p.vy * dt
            p.rotationDeg += p.rotationSpeedDeg * dt
        }

        val sIter = shockwaves.listIterator()
        while (sIter.hasNext()) {
            val s = sIter.next()
            s.ageSec += dt
            if (!s.isAlive) {
                sIter.remove()
            }
        }
    }
}

@Composable
fun LevelUpParticleOverlay(
    burstKey: Int,
    level: Int,
    modifier: Modifier = Modifier,
    onBurstFinished: () -> Unit = {}
) {
    if (burstKey <= 0) return

    val particles = remember { mutableStateListOf<LevelUpParticle>() }
    val shockwaves = remember { mutableStateListOf<LevelUpShockwave>() }
    var frameTick by remember { mutableLongStateOf(0L) }
    var canvasSize by remember { androidx.compose.runtime.mutableStateOf(Size.Zero) }

    LaunchedEffect(burstKey) {
        // Wait briefly for Canvas layout measurement if not yet measured
        while (canvasSize.width <= 0f || canvasSize.height <= 0f) {
            withFrameNanos { }
        }

        val w = canvasSize.width
        val h = canvasSize.height
        val centerX = w * 0.5f
        val centerY = h * 0.44f

        particles.clear()
        shockwaves.clear()

        // Primary center radial burst + twin expanding shockwaves
        val burstCount = (92 + (level * 3).coerceAtMost(36))
        particles.addAll(
            LevelUpParticleEngine.createRadialBurst(
                originX = centerX,
                originY = centerY,
                count = burstCount,
                speedScale = (minOf(w, h) / 1000f).coerceIn(0.85f, 1.35f)
            )
        )
        shockwaves.add(
            LevelUpShockwave(
                originX = centerX,
                originY = centerY,
                maxRadius = minOf(w, h) * 0.62f,
                color = GoldPrimary,
                strokeWidth = 10f,
                durationSec = 0.78f
            )
        )
        shockwaves.add(
            LevelUpShockwave(
                originX = centerX,
                originY = centerY,
                maxRadius = minOf(w, h) * 0.48f,
                color = FireKernEmber,
                strokeWidth = 6f,
                durationSec = 0.60f
            )
        )

        var lastNanos = 0L
        var elapsedSec = 0f
        var triggeredLeftFountain = false
        var triggeredRightFountain = false
        var triggeredFinalSpark = false

        while (particles.isNotEmpty() || shockwaves.isNotEmpty() || !triggeredFinalSpark) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos
                elapsedSec += dt

                if (!triggeredLeftFountain && elapsedSec >= 0.28f) {
                    triggeredLeftFountain = true
                    particles.addAll(
                        LevelUpParticleEngine.createCornerFountain(
                            originX = w * 0.14f,
                            originY = h * 0.85f,
                            aimRight = true,
                            count = 30
                        )
                    )
                }
                if (!triggeredRightFountain && elapsedSec >= 0.48f) {
                    triggeredRightFountain = true
                    particles.addAll(
                        LevelUpParticleEngine.createCornerFountain(
                            originX = w * 0.86f,
                            originY = h * 0.85f,
                            aimRight = false,
                            count = 30
                        )
                    )
                }
                if (!triggeredFinalSpark && elapsedSec >= 0.85f) {
                    triggeredFinalSpark = true
                    particles.addAll(
                        LevelUpParticleEngine.createRadialBurst(
                            originX = centerX,
                            originY = centerY,
                            count = 38,
                            speedScale = 0.75f
                        )
                    )
                    shockwaves.add(
                        LevelUpShockwave(
                            originX = centerX,
                            originY = centerY,
                            maxRadius = minOf(w, h) * 0.55f,
                            color = GoldLight,
                            strokeWidth = 5f,
                            durationSec = 0.65f
                        )
                    )
                }

                LevelUpParticleEngine.stepParticles(particles, shockwaves, dt)
                frameTick++
            }
        }

        onBurstFinished()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("level_up_particle_overlay")
    ) {
        if (canvasSize != size) {
            canvasSize = size
        }
        // Read frameTick inside DrawScope so Canvas redraws smoothly every animation frame
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        for (wave in shockwaves) {
            val t = wave.progress
            val easeOut = 1f - (1f - t) * (1f - t)
            val radius = (wave.maxRadius * easeOut).coerceAtLeast(1f)
            val alpha = (1f - t).coerceIn(0f, 1f) * 0.75f
            drawCircle(
                color = wave.color.copy(alpha = alpha),
                radius = radius,
                center = Offset(wave.originX, wave.originY),
                style = Stroke(width = (wave.strokeWidth * (1f - t * 0.6f)).coerceAtLeast(1f))
            )
        }

        for (particle in particles) {
            drawLevelUpParticle(particle)
        }
    }
}

private fun DrawScope.drawLevelUpParticle(particle: LevelUpParticle) {
    val alpha = particle.alpha
    if (alpha <= 0.01f) return
    val center = Offset(particle.x, particle.y)

    when (particle.kind) {
        LevelUpParticleKind.EMBER_SPARK -> {
            val glowRadius = particle.size * 2.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.secondaryColor.copy(alpha = alpha * 0.85f),
                        particle.color.copy(alpha = alpha * 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size * 0.65f,
                center = center
            )
        }
        LevelUpParticleKind.GOLD_COIN -> {
            val spinWidthFactor = (0.28f + 0.72f * kotlin.math.abs(cos(particle.phase)))
            val rx = particle.size * spinWidthFactor
            val ry = particle.size
            rotate(degrees = particle.rotationDeg * 0.25f, pivot = center) {
                drawOval(
                    color = particle.secondaryColor.copy(alpha = alpha),
                    topLeft = Offset(center.x - rx, center.y - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
                drawOval(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(center.x - rx * 0.8f, center.y - ry * 0.8f),
                    size = Size(rx * 1.6f, ry * 1.6f)
                )
                drawOval(
                    color = Color.White.copy(alpha = alpha * 0.45f),
                    topLeft = Offset(center.x - rx * 0.45f, center.y - ry * 0.45f),
                    size = Size(rx * 0.65f, ry * 0.55f)
                )
            }
        }
        LevelUpParticleKind.DIAMOND_STAR -> {
            val r = particle.size
            val inner = r * 0.34f
            val path = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + inner, center.y - inner)
                lineTo(center.x + r, center.y)
                lineTo(center.x + inner, center.y + inner)
                lineTo(center.x, center.y + r)
                lineTo(center.x - inner, center.y + inner)
                lineTo(center.x - r, center.y)
                lineTo(center.x - inner, center.y - inner)
                close()
            }
            rotate(degrees = particle.rotationDeg, pivot = center) {
                drawPath(path = path, color = particle.color.copy(alpha = alpha))
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.65f),
                    radius = r * 0.22f,
                    center = center
                )
            }
        }
        LevelUpParticleKind.CONFETTI_RIBBON -> {
            val w = particle.size * 1.15f
            val h = particle.size * 0.48f
            rotate(degrees = particle.rotationDeg, pivot = center) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                    size = Size(w, h)
                )
            }
        }
    }
}
