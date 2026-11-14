package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import kotlin.random.Random

/**
 * Unified casino sound manager using Android SoundPool with preloaded classic casino WAV assets
 * in `res/raw/` and tactile haptic feedback, backed by [CasinoSoundEngine] user audio preferences
 * and multi-tone streak synthesis.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    val soundEngine: CasinoSoundEngine = CasinoSoundEngine(appContext)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val loadedSampleIds = mutableSetOf<Int>()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSampleIds.add(sampleId)
                }
            }
        }

    private val chipClickId: Int = loadRawSafe(R.raw.sfx_chip_click)
    private val cardShuffleId: Int = loadRawSafe(R.raw.sfx_card_shuffle)
    private val cardDealId: Int = loadRawSafe(R.raw.sfx_card_deal)
    private val reelSpinId: Int = loadRawSafe(R.raw.sfx_reel_spin)
    private val reelStopId: Int = loadRawSafe(R.raw.sfx_reel_stop)
    private val rouletteSpinId: Int = loadRawSafe(R.raw.sfx_roulette_spin)
    private val winChimeId: Int = loadRawSafe(R.raw.sfx_win_chime)
    private val bigWinId: Int = loadRawSafe(R.raw.sfx_big_win)

    private fun loadRawSafe(resId: Int): Int {
        return try {
            soundPool.load(appContext, resId, 1)
        } catch (_: Exception) {
            0
        }
    }

    var isSoundEnabled: Boolean
        get() = soundEngine.isSoundEnabled
        set(value) {
            soundEngine.isSoundEnabled = value
        }

    var isMusicEnabled: Boolean
        get() = soundEngine.isMusicEnabled
        set(value) {
            soundEngine.isMusicEnabled = value
        }

    var isHapticsEnabled: Boolean
        get() = soundEngine.isHapticsEnabled
        set(value) {
            soundEngine.isHapticsEnabled = value
        }

    val masterVolume: Float get() = soundEngine.masterVolume
    val sfxVolume: Float get() = soundEngine.sfxVolume
    val musicVolume: Float get() = soundEngine.musicVolume
    val activePreset: SoundProfilePreset get() = soundEngine.activePreset

    fun updateSettings(
        masterVol: Float = masterVolume,
        sfxVol: Float = sfxVolume,
        musicVol: Float = musicVolume,
        soundEnabled: Boolean = isSoundEnabled,
        musicEnabled: Boolean = isMusicEnabled,
        hapticsEnabled: Boolean = isHapticsEnabled,
        preset: SoundProfilePreset = activePreset
    ) {
        soundEngine.updateSettings(
            masterVol = masterVol,
            sfxVol = sfxVol,
            musicVol = musicVol,
            soundEnabled = soundEnabled,
            musicEnabled = musicEnabled,
            hapticsEnabled = hapticsEnabled,
            preset = preset
        )
    }

    fun onAppForeground() {
        soundEngine.onAppForeground()
    }

    fun onAppBackground() {
        soundEngine.onAppBackground()
    }

    private fun computeEffectiveVolume(): Float {
        if (!isSoundEnabled || activePreset == SoundProfilePreset.MUTED) return 0f
        val presetMultiplier = when (activePreset) {
            SoundProfilePreset.VEGAS_CASINO -> 1.0f
            SoundProfilePreset.CRISP_ARCADE -> 1.0f
            SoundProfilePreset.SUBTLE_LOUNGE -> 0.65f
            SoundProfilePreset.MUTED -> 0f
        }
        return (masterVolume * sfxVolume * presetMultiplier).coerceIn(0f, 1f)
    }

    private fun playAsset(sampleId: Int, rate: Float = 1.0f, gain: Float = 1.0f): Boolean {
        val vol = computeEffectiveVolume()
        if (vol <= 0.01f || sampleId == 0 || !loadedSampleIds.contains(sampleId)) return false
        val finalVol = (vol * gain).coerceIn(0f, 1f)
        return try {
            soundPool.play(sampleId, finalVol, finalVol, 1, 0, rate.coerceIn(0.5f, 2.0f)) != 0
        } catch (_: Exception) {
            false
        }
    }

    // --- Requested Core Sound Actions ---

    fun playChipClick(amount: Long = 0L) {
        if (isHapticsEnabled) triggerHaptic(14L, 85)
        val pitch = when {
            amount >= 1000L -> 0.88f
            amount >= 100L -> 0.96f
            else -> 1.04f
        } + (Random.nextFloat() * 0.08f - 0.04f)
        if (!playAsset(chipClickId, rate = pitch, gain = 0.92f)) {
            soundEngine.playChipPlace(amount)
        }
    }

    fun playChipCollect() {
        if (!playAsset(winChimeId, rate = 1.18f, gain = 0.72f)) {
            soundEngine.playChipCollect()
        } else if (isHapticsEnabled) {
            triggerHapticPattern(longArrayOf(0, 15, 25, 20, 20, 30))
        }
    }

    fun playChipClear() {
        if (isHapticsEnabled) triggerHaptic(12L, 60)
        if (!playAsset(chipClickId, rate = 0.75f, gain = 0.7f)) {
            soundEngine.playChipClear()
        }
    }

    fun playShuffleCards() {
        if (isHapticsEnabled) {
            triggerHapticPattern(longArrayOf(0, 20, 15, 20, 15, 20, 15, 25, 20, 40))
        }
        if (!playAsset(cardShuffleId, rate = 1.0f, gain = 0.95f)) {
            soundEngine.playCardShuffle()
        }
    }

    fun playCardShuffle() = playShuffleCards()

    fun playCardDeal() {
        if (isHapticsEnabled) triggerHaptic(18L, 70)
        if (!playAsset(cardDealId, rate = 0.96f + Random.nextFloat() * 0.08f, gain = 0.88f)) {
            soundEngine.playCardDeal()
        }
    }

    fun playCardFlip() {
        if (isHapticsEnabled) triggerHaptic(22L, 105)
        if (!playAsset(cardDealId, rate = 1.25f, gain = 0.92f)) {
            soundEngine.playCardFlip()
        }
    }

    fun playSpinReels() {
        if (isHapticsEnabled) triggerHaptic(25L, 140)
        if (!playAsset(reelSpinId, rate = 1.0f, gain = 0.9f)) {
            soundEngine.playSlotSpinStart()
        }
    }

    fun playSlotSpinStart() = playSpinReels()

    fun playReelClick() {
        if (!playAsset(reelSpinId, rate = 1.35f + Random.nextFloat() * 0.12f, gain = 0.35f)) {
            soundEngine.playSlotReelClick()
        }
    }

    fun playReelStop(reelIndex: Int = 0) {
        val hapticForce = (35 + reelIndex * 15).coerceAtMost(255)
        if (isHapticsEnabled) triggerHaptic(28L, hapticForce)
        val rate = (0.88f + reelIndex * 0.07f).coerceAtMost(1.3f)
        if (!playAsset(reelStopId, rate = rate, gain = 0.95f)) {
            soundEngine.playSlotReelStop(reelIndex)
        }
    }

    fun playRouletteSpin() {
        if (isHapticsEnabled) triggerHaptic(20L, 75)
        if (!playAsset(rouletteSpinId, rate = 1.0f, gain = 0.9f)) {
            soundEngine.playRouletteSpin()
        }
    }

    fun playBallBounce() {
        if (isHapticsEnabled) triggerHaptic(14L, 95)
        if (!playAsset(chipClickId, rate = 1.42f, gain = 0.65f)) {
            soundEngine.playBallBounce()
        }
    }

    fun playWin() {
        if (!playAsset(winChimeId, rate = 1.0f, gain = 0.9f)) {
            soundEngine.playWin()
        } else if (isHapticsEnabled) {
            triggerHapticPattern(longArrayOf(0, 45, 35, 60))
        }
    }

    fun playWinStreak(streak: Int, winAmount: Long = 0L) {
        if (streak >= 3) {
            if (!playAsset(bigWinId, rate = 1.0f + ((streak - 3).coerceAtMost(3) * 0.05f), gain = 0.98f)) {
                soundEngine.playWinStreak(streak, winAmount)
            } else if (isHapticsEnabled) {
                triggerHapticPattern(longArrayOf(0, 50, 40, 80, 40, 120))
            }
        } else {
            if (!playAsset(winChimeId, rate = if (streak == 2) 1.1f else 1.0f, gain = 0.92f)) {
                soundEngine.playWinStreak(streak, winAmount)
            } else if (isHapticsEnabled) {
                triggerHapticPattern(longArrayOf(0, 40, 30, 70))
            }
        }
    }

    fun playBigWin() {
        if (!playAsset(bigWinId, rate = 1.0f, gain = 1.0f)) {
            soundEngine.playBigWin()
        } else if (isHapticsEnabled) {
            triggerHapticPattern(longArrayOf(0, 60, 40, 80, 40, 120, 50, 180))
        }
    }

    fun playJackpot() = playBigWin()

    fun playLoss() {
        soundEngine.playLoss()
    }

    fun playButtonClick() {
        if (isHapticsEnabled) triggerHaptic(10L, 65)
        if (!playAsset(chipClickId, rate = 1.25f, gain = 0.45f)) {
            soundEngine.playButtonClick()
        }
    }

    fun playCheckInClaim(isJackpot: Boolean) {
        if (isJackpot) {
            playBigWin()
        } else {
            playWin()
        }
    }

    fun release() {
        try {
            soundEngine.release()
            soundPool.release()
        } catch (_: Exception) {}
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticPattern(timings: LongArray) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(timings, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(timings, -1)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
