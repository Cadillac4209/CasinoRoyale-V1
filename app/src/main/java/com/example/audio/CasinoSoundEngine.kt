package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundProfilePreset(val displayName: String, val description: String) {
    VEGAS_CASINO("Vegas Floor", "Rich, punchy tactile casino audio with full haptic feedback"),
    CRISP_ARCADE("Crisp Arcade", "Sharp, high-definition chimes and energetic responses"),
    SUBTLE_LOUNGE("Subtle Lounge", "Gentle, understated tones suited for relaxed play"),
    MUTED("Muted", "Silent audio with optional haptics")
}

class CasinoSoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private val audioScope = CoroutineScope(Dispatchers.Default)

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("casino_sound_preferences", Context.MODE_PRIVATE)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var masterVolume: Float = prefs.getFloat("master_volume", 0.85f)
        private set

    var sfxVolume: Float = prefs.getFloat("sfx_volume", 0.90f)
        private set

    var musicVolume: Float = prefs.getFloat("music_volume", 0.45f)
        private set

    var isSoundEnabled: Boolean = prefs.getBoolean("sound_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }

    var isMusicEnabled: Boolean = prefs.getBoolean("music_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("music_enabled", value).apply()
            syncMusicPlayback()
        }

    var isHapticsEnabled: Boolean = prefs.getBoolean("haptics_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("haptics_enabled", value).apply()
        }

    var activePreset: SoundProfilePreset =
        try {
            SoundProfilePreset.valueOf(
                prefs.getString("sound_preset", SoundProfilePreset.VEGAS_CASINO.name)
                    ?: SoundProfilePreset.VEGAS_CASINO.name
            )
        } catch (_: Exception) {
            SoundProfilePreset.VEGAS_CASINO
        }
        private set

    private var isForeground: Boolean = false
    private var musicLoopJob: Job? = null
    private var activeMusicTrack: AudioTrack? = null

    private val effectiveVolume: Float
        get() {
            if (!isSoundEnabled || activePreset == SoundProfilePreset.MUTED) return 0.0f
            val presetMultiplier = when (activePreset) {
                SoundProfilePreset.VEGAS_CASINO -> 1.0f
                SoundProfilePreset.CRISP_ARCADE -> 1.1f
                SoundProfilePreset.SUBTLE_LOUNGE -> 0.65f
                SoundProfilePreset.MUTED -> 0.0f
            }
            return (masterVolume * sfxVolume * presetMultiplier).coerceIn(0.0f, 1.0f)
        }

    private val effectiveMusicVolume: Float
        get() {
            if (!isMusicEnabled || activePreset == SoundProfilePreset.MUTED) return 0.0f
            val presetMultiplier = when (activePreset) {
                SoundProfilePreset.VEGAS_CASINO -> 1.0f
                SoundProfilePreset.CRISP_ARCADE -> 0.85f
                SoundProfilePreset.SUBTLE_LOUNGE -> 0.70f
                SoundProfilePreset.MUTED -> 0.0f
            }
            return (masterVolume * musicVolume * presetMultiplier).coerceIn(0.0f, 1.0f)
        }

    fun updateSettings(
        masterVol: Float = masterVolume,
        sfxVol: Float = sfxVolume,
        musicVol: Float = musicVolume,
        soundEnabled: Boolean = isSoundEnabled,
        musicEnabled: Boolean = isMusicEnabled,
        hapticsEnabled: Boolean = isHapticsEnabled,
        preset: SoundProfilePreset = activePreset
    ) {
        masterVolume = masterVol.coerceIn(0.0f, 1.0f)
        sfxVolume = sfxVol.coerceIn(0.0f, 1.0f)
        musicVolume = musicVol.coerceIn(0.0f, 1.0f)
        isSoundEnabled = soundEnabled
        isHapticsEnabled = hapticsEnabled
        activePreset = preset
        // Assign backing field through setter only after volumes/preset are updated
        isMusicEnabled = musicEnabled

        prefs.edit()
            .putFloat("master_volume", masterVolume)
            .putFloat("sfx_volume", sfxVolume)
            .putFloat("music_volume", musicVolume)
            .putBoolean("sound_enabled", isSoundEnabled)
            .putBoolean("music_enabled", isMusicEnabled)
            .putBoolean("haptics_enabled", isHapticsEnabled)
            .putString("sound_preset", activePreset.name)
            .apply()

        syncMusicPlayback()
    }

    fun onAppForeground() {
        isForeground = true
        syncMusicPlayback()
    }

    fun onAppBackground() {
        isForeground = false
        stopMusicLoop()
    }

    fun release() {
        isForeground = false
        stopMusicLoop()
    }

    private fun syncMusicPlayback() {
        val shouldPlay = isForeground && effectiveMusicVolume > 0.01f
        if (shouldPlay) {
            if (musicLoopJob?.isActive != true) {
                startMusicLoop()
            } else {
                try {
                    val gain = (effectiveMusicVolume * 0.25f).coerceIn(0f, 0.35f)
                    activeMusicTrack?.setVolume(gain)
                } catch (_: Exception) {}
            }
        } else {
            stopMusicLoop()
        }
    }

    private fun stopMusicLoop() {
        musicLoopJob?.cancel()
        musicLoopJob = null
        try {
            activeMusicTrack?.pause()
            activeMusicTrack?.flush()
            activeMusicTrack?.release()
        } catch (_: Exception) {}
        activeMusicTrack = null
    }

    private fun startMusicLoop() {
        stopMusicLoop()
        musicLoopJob = audioScope.launch {
            // Warm Vegas lounge 4-chord ambient progression rendered as seamless PCM loop
            val sampleRate = 22050
            val chordDurationSec = 2.0f
            val chords = listOf(
                floatArrayOf(130.81f, 196.00f, 246.94f, 293.66f, 329.63f), // Cmaj9
                floatArrayOf(110.00f, 164.81f, 220.00f, 246.94f, 261.63f), // Am9
                floatArrayOf(87.31f, 174.61f, 220.00f, 261.63f, 329.63f),  // Fmaj7
                floatArrayOf(98.00f, 146.83f, 196.00f, 246.94f, 293.66f)   // G6
            )
            val samplesPerChord = (sampleRate * chordDurationSec).toInt()
            val totalSamples = samplesPerChord * chords.size
            val loopBuffer = ShortArray(totalSamples)

            for ((cIdx, freqs) in chords.withIndex()) {
                val offset = cIdx * samplesPerChord
                for (i in 0 until samplesPerChord) {
                    val t = i.toFloat() / samplesPerChord
                    // Smooth cross-faded pad envelope so there are zero clicks between chords
                    val env = sin(PI * t.toDouble()).toFloat().coerceIn(0f, 1f)
                    var sum = 0.0
                    for ((fIdx, f) in freqs.withIndex()) {
                        val weight = if (fIdx == 0) 0.32 else 0.17
                        sum += sin(2.0 * PI * f * (i.toDouble() / sampleRate)) * weight
                    }
                    val sample = (sum * env * 0.45 * Short.MAX_VALUE).toInt()
                    loopBuffer[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeMusicTrack = track
                val initialGain = (effectiveMusicVolume * 0.25f).coerceIn(0f, 0.35f)
                track.setVolume(initialGain)
                track.play()

                val chunkSize = 2048
                var cursor = 0
                while (isActive && isForeground && effectiveMusicVolume > 0.01f) {
                    val remaining = loopBuffer.size - cursor
                    val count = minOf(chunkSize, remaining)
                    track.write(loopBuffer, cursor, count)
                    cursor = (cursor + count) % loopBuffer.size
                }
            } catch (_: Exception) {
                // Safe no-op in headless test environments
            } finally {
                stopMusicLoop()
            }
        }
    }

    // ==========================================
    // 1. CHIP MOVEMENT AUDIO FEEDBACK
    // ==========================================

    fun playChipClick() {
        playChipPlace(0L)
    }

    fun playChipPlace(amount: Long = 0L) {
        if (isHapticsEnabled) triggerHaptic(14, 85)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Subtle random pitch variation for organic tactile chip stacking
            val basePitch = when {
                amount >= 1000L -> 1400f
                amount >= 100L -> 1650f
                else -> 1900f
            } + Random.nextInt(-60, 60)

            playDualToneDecay(
                freq1 = basePitch,
                freq2 = basePitch * 1.55f,
                durationMs = 28,
                volume = 0.55f * vol
            )
        }
    }

    fun playChipCollect() {
        if (isHapticsEnabled) triggerHapticPattern(longArrayOf(0, 15, 25, 20, 20, 30))
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Rapid multi-chip scoop and rake
            val chipFrequencies = listOf(1750f, 2100f, 1600f, 2250f)
            for (freq in chipFrequencies) {
                playToneSweep(startFreq = freq, endFreq = freq * 0.7f, durationMs = 22, volume = 0.45f * vol)
                delay(28)
            }
        }
    }

    fun playChipClear() {
        if (isHapticsEnabled) triggerHaptic(12, 60)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playToneSweep(startFreq = 900f, endFreq = 400f, durationMs = 35, volume = 0.35f * vol)
        }
    }

    // ==========================================
    // 2. CARD SHUFFLING & HANDLING AUDIO FEEDBACK
    // ==========================================

    fun playCardShuffle() {
        if (isHapticsEnabled) triggerHapticPattern(longArrayOf(0, 20, 15, 20, 15, 20, 15, 25, 20, 40))
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Realistic riffle shuffle: multi-flutter cards sliding and squaring off
            for (i in 0 until 9) {
                val freq = 450f + (i * 45f) + Random.nextInt(-20, 20)
                playToneSweep(
                    startFreq = freq,
                    endFreq = freq * 1.4f,
                    durationMs = 18,
                    volume = 0.38f * vol
                )
                delay(22)
            }
            // Final squaring tap
            playToneSweep(startFreq = 800f, endFreq = 300f, durationMs = 30, volume = 0.45f * vol)
        }
    }

    fun playCardDeal() {
        if (isHapticsEnabled) triggerHaptic(18, 70)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playToneSweep(startFreq = 420f, endFreq = 950f, durationMs = 38, volume = 0.42f * vol)
        }
    }

    fun playCardFlip() {
        if (isHapticsEnabled) triggerHaptic(22, 105)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playToneSweep(startFreq = 720f, endFreq = 1350f, durationMs = 30, volume = 0.48f * vol)
        }
    }

    // ==========================================
    // 3. SLOT SPINNING AUDIO FEEDBACK
    // ==========================================

    fun playSlotSpinStart() {
        if (isHapticsEnabled) triggerHaptic(25, 140)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Mechanical crank release + motor windup
            playToneSweep(startFreq = 180f, endFreq = 480f, durationMs = 60, volume = 0.5f * vol)
        }
    }

    fun playSlotReelClick() {
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playTone(frequency = 320f + Random.nextInt(-15, 15), durationMs = 16, volume = 0.22f * vol)
        }
    }

    fun playSlotReelStop(reelIndex: Int = 0) {
        val hapticForce = 35 + (reelIndex * 15)
        if (isHapticsEnabled) triggerHaptic(28, hapticForce.coerceAtMost(255))
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Ascending pitch per reel stop for dramatic slot cadence
            val baseFreq = when (reelIndex) {
                0 -> 260f
                1 -> 320f
                2 -> 390f
                3 -> 470f
                else -> 580f
            }
            playToneSweep(startFreq = baseFreq * 1.5f, endFreq = baseFreq, durationMs = 55, volume = 0.58f * vol)
        }
    }

    fun playSlotAnticipation() {
        if (isHapticsEnabled) triggerHapticPattern(longArrayOf(0, 40, 30, 40, 30, 60))
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            val notes = listOf(440f, 554.37f, 659.25f, 880f)
            for (freq in notes) {
                playTone(frequency = freq, durationMs = 50, volume = 0.45f * vol)
                delay(30)
            }
        }
    }

    fun playReelSpin() {
        playSlotReelClick()
    }

    fun playReelStop() {
        playSlotReelStop(0)
    }

    // ==========================================
    // 4. WINNING STREAKS AUDIO FEEDBACK
    // ==========================================

    fun playWinStreak(streak: Int, winAmount: Long = 0L) {
        val vol = effectiveVolume
        if (vol <= 0.01f) {
            if (isHapticsEnabled) triggerHapticPattern(longArrayOf(0, 50, 40, 70))
            return
        }

        audioScope.launch {
            when {
                // Tier 4: Epic Streak (4+ consecutive wins) - ON FIRE CELEBRATION!
                streak >= 4 -> {
                    if (isHapticsEnabled) {
                        triggerHapticPattern(longArrayOf(0, 60, 40, 80, 40, 100, 50, 160, 60, 240))
                    }
                    val notes = listOf(
                        523.25f, 659.25f, 783.99f, 1046.50f,
                        1174.66f, 1318.51f, 1567.98f, 2093.00f
                    )
                    for (freq in notes) {
                        playTone(freq, 75, 0.75f * vol)
                        delay(25)
                    }
                }
                // Tier 3: Hot Streak (3 consecutive wins) - Fanfare!
                streak == 3 -> {
                    if (isHapticsEnabled) {
                        triggerHapticPattern(longArrayOf(0, 50, 40, 60, 40, 120))
                    }
                    val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f)
                    for (freq in notes) {
                        playTone(freq, 70, 0.65f * vol)
                        delay(20)
                    }
                }
                // Tier 2: Double Win Streak - Upbeat Ascending Arpeggio
                streak == 2 -> {
                    if (isHapticsEnabled) {
                        triggerHapticPattern(longArrayOf(0, 40, 30, 70, 30, 90))
                    }
                    val notes = listOf(523.25f, 659.25f, 783.99f, 987.77f, 1046.50f)
                    for (freq in notes) {
                        playTone(freq, 65, 0.58f * vol)
                        delay(15)
                    }
                }
                // Tier 1: Single Win
                else -> {
                    if (isHapticsEnabled) {
                        triggerHapticPattern(longArrayOf(0, 45, 35, 60))
                    }
                    val notes = listOf(523.25f, 659.25f, 783.99f)
                    for (freq in notes) {
                        playTone(freq, 65, 0.50f * vol)
                        delay(15)
                    }
                }
            }
        }
    }

    fun playWin() {
        playWinStreak(1)
    }

    fun playBigWin() {
        playWinStreak(4)
    }

    fun playJackpot() {
        playWinStreak(5)
    }

    fun playLoss() {
        if (isHapticsEnabled) triggerHaptic(40, 90)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            val notes = listOf(440f, 415.30f, 392f, 349.23f)
            for (freq in notes) {
                playTone(freq, 75, 0.38f * vol)
                delay(20)
            }
        }
    }

    // ==========================================
    // 5. ROULETTE AUDIO FEEDBACK
    // ==========================================

    fun playRouletteSpin() {
        if (isHapticsEnabled) triggerHaptic(20, 75)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            // Whir of spinning wooden rotor
            playToneSweep(startFreq = 400f, endFreq = 220f, durationMs = 80, volume = 0.4f * vol)
        }
    }

    fun playBallBounce() {
        if (isHapticsEnabled) triggerHaptic(14, 95)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playToneSweep(startFreq = 1500f, endFreq = 900f, durationMs = 18, volume = 0.42f * vol)
        }
    }

    // ==========================================
    // 6. GENERAL CONTROLS & TEST AUDIO
    // ==========================================

    fun playButtonClick() {
        if (isHapticsEnabled) triggerHaptic(10, 65)
        val vol = effectiveVolume
        if (vol <= 0.01f) return

        audioScope.launch {
            playTone(frequency = 880f, durationMs = 18, volume = 0.28f * vol)
        }
    }

    fun playCheckInClaim(isJackpot: Boolean) {
        val vol = effectiveVolume
        if (isJackpot) {
            triggerHapticPattern(longArrayOf(0, 40, 60, 50, 60, 100))
        } else {
            triggerHaptic(25, 110)
        }
        if (vol <= 0.01f) return

        audioScope.launch {
            if (isJackpot) {
                // Day 7 Grand Jackpot Fanfare
                val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f, 1567.98f, 2093.00f)
                for ((idx, freq) in notes.withIndex()) {
                    playTone(freq, 110, (0.42f + idx * 0.05f).coerceAtMost(0.85f) * vol)
                    delay(55)
                }
                delay(60)
                playChipCollect()
            } else {
                // Standard Daily Check-In Ascending Chime
                val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
                for (freq in notes) {
                    playTone(freq, 75, 0.45f * vol)
                    delay(40)
                }
                delay(20)
                playChipCollect()
            }
        }
    }

    // Haptics Helper Methods
    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(
                            VibrationEffect.createOneShot(
                                durationMs,
                                amplitude.coerceIn(1, 255)
                            )
                        )
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

    // PCM Audio Synthesis Helpers
    private fun playTone(frequency: Float, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)

            val attack = (numSamples * 0.1f).toInt().coerceAtLeast(1)
            val decay = (numSamples * 0.25f).toInt().coerceAtLeast(1)

            for (i in 0 until numSamples) {
                val envelope = when {
                    i < attack -> i.toFloat() / attack
                    i > numSamples - decay -> (numSamples - i).toFloat() / decay
                    else -> 1.0f
                }
                val angle = 2.0 * PI * i * frequency / sampleRate
                val sample = (sin(angle) * envelope * volume * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            writeAndPlayAudio(buffer, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private fun playToneSweep(startFreq: Float, endFreq: Float, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * PI * currentFreq / sampleRate

                val envelope = 1.0f - progress // Linear natural decay
                val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            writeAndPlayAudio(buffer, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private fun playDualToneDecay(freq1: Float, freq2: Float, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val envelope = (1.0f - progress) * (1.0f - progress) // exponential-like decay
                val angle1 = 2.0 * PI * i * freq1 / sampleRate
                val angle2 = 2.0 * PI * i * freq2 / sampleRate
                val combined = (sin(angle1) * 0.6 + sin(angle2) * 0.4)
                val sample = (combined * envelope * volume * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            writeAndPlayAudio(buffer, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private fun writeAndPlayAudio(buffer: ShortArray, sampleRate: Int, durationMs: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Thread.sleep(durationMs.toLong() + 15)
        audioTrack.release()
    }
}
