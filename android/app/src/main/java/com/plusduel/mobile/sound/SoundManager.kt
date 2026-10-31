package com.plusduel.mobile.sound

import android.content.Context
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Warm acoustic procedural sound engine (Marimba, Kalimba, Celesta & Ambient Pad)
 * with native Android vibration feedback.
 */
class SoundManager(private val context: Context) {

    enum class Timbre {
        WOOD_TAP,
        SOFT_CHIME,
        MUTED_WARM,
        WARM_PAD
    }

    private data class Note(
        val timbre: Timbre,
        val freqHz: Double,
        val startOffsetSec: Double,
        val durationSec: Double,
        val volume: Double = 0.10
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            updateMusicLoopState()
        }

    @Volatile
    var inMenuScreen: Boolean = true
        set(value) {
            field = value
            updateMusicLoopState()
        }

    @Volatile
    var appInForeground: Boolean = true
        set(value) {
            field = value
            updateMusicLoopState()
        }

    @Volatile
    var hapticsEnabled: Boolean = true

    private var musicJob: Job? = null
    private var keyPitchStep = 0
    private val pentatonicKeyFreqs = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 659.25, 587.33)

    private val ambientMelodyPhrase: List<Note> = buildList {
        val progressions = listOf(
            174.61 to listOf(440.00, 523.25, 659.25, 783.99, 659.25, 523.25),
            164.81 to listOf(392.00, 493.88, 587.33, 783.99, 587.33, 493.88),
            146.83 to listOf(349.23, 440.00, 523.25, 659.25, 523.25, 440.00),
            130.81 to listOf(329.63, 392.00, 493.88, 587.33, 493.88, 392.00)
        )
        val stepSec = 0.52
        var t = 0.05
        for ((bassFreq, arp) in progressions) {
            val barDur = stepSec * arp.size
            add(Note(Timbre.WARM_PAD, bassFreq, t, barDur * 0.98, 0.022))
            add(Note(Timbre.WARM_PAD, bassFreq * 1.5, t, barDur * 0.98, 0.014))
            for (freq in arp) {
                add(Note(Timbre.SOFT_CHIME, freq, t, stepSec * 2.1, 0.024))
                t += stepSec
            }
        }
    }

    private fun updateMusicLoopState() {
        val shouldTurnOn = musicEnabled && appInForeground
        if (shouldTurnOn) {
            if (musicJob?.isActive != true) {
                musicJob = scope.launch {
                    val sampleRate = 32000
                    val buffer = renderNotesToPcm(ambientMelodyPhrase, sampleRate, smoothLoopEdges = true)
                    val byteCount = buffer.size * 2
                    var currentGain = 0f

                    while (isActive && musicEnabled && appInForeground) {
                        var track: AudioTrack? = null
                        try {
                            track = AudioTrack.Builder()
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
                                .setBufferSizeInBytes(
                                    byteCount.coerceAtLeast(
                                        AudioTrack.getMinBufferSize(
                                            sampleRate,
                                            AudioFormat.CHANNEL_OUT_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT
                                        )
                                    )
                                )
                                .setTransferMode(AudioTrack.MODE_STATIC)
                                .build()

                            track.write(buffer, 0, buffer.size)
                            track.setVolume(currentGain)
                            track.play()

                            val totalMs = (buffer.size.toLong() * 1000L) / sampleRate
                            var waited = 0L
                            val stepMs = 40L
                            while (waited < totalMs && isActive && musicEnabled && appInForeground) {
                                val targetGain = if (inMenuScreen) 1.0f else 0.0f
                                if (currentGain != targetGain) {
                                    val delta = if (targetGain > currentGain) 0.06f else -0.07f
                                    currentGain = (currentGain + delta).coerceIn(0.0f, 1.0f)
                                    runCatching { track.setVolume(currentGain) }
                                }
                                delay(stepMs)
                                waited += stepMs
                            }
                        } catch (_: Throwable) {
                            delay(1000L)
                        } finally {
                            runCatching {
                                track?.stop()
                                track?.release()
                            }
                        }
                    }
                }
            }
        } else {
            musicJob?.cancel()
            musicJob = null
        }
    }

    fun playClick() {
        vibrate(8)
        playSequence(listOf(Note(Timbre.WOOD_TAP, 523.25, 0.0, 0.075, 0.09)))
    }

    fun playKey() {
        vibrate(7)
        val freq = pentatonicKeyFreqs[keyPitchStep++ % pentatonicKeyFreqs.size]
        playSequence(listOf(Note(Timbre.WOOD_TAP, freq, 0.0, 0.095, 0.10)))
    }

    fun playDelete() {
        vibrate(8)
        playSequence(
            listOf(
                Note(Timbre.MUTED_WARM, 392.00, 0.0, 0.065, 0.085),
                Note(Timbre.MUTED_WARM, 329.63, 0.035, 0.080, 0.085)
            )
        )
    }

    fun playSubmit() {
        vibrate(12)
        playSequence(
            listOf(
                Note(Timbre.SOFT_CHIME, 523.25, 0.0, 0.16, 0.10),
                Note(Timbre.SOFT_CHIME, 659.25, 0.055, 0.20, 0.11)
            )
        )
    }

    fun playCoinEarned() {
        vibrate(12)
        playSequence(
            listOf(
                Note(Timbre.SOFT_CHIME, 587.33, 0.0, 0.24, 0.09),
                Note(Timbre.SOFT_CHIME, 880.00, 0.06, 0.26, 0.10),
                Note(Timbre.SOFT_CHIME, 1174.66, 0.12, 0.34, 0.11)
            )
        )
    }

    fun playMistake() {
        vibrateError()
        playSequence(
            listOf(
                Note(Timbre.MUTED_WARM, 349.23, 0.0, 0.14, 0.095),
                Note(Timbre.MUTED_WARM, 293.66, 0.10, 0.20, 0.095)
            )
        )
    }

    fun playRoundWin() {
        vibrateSuccess()
        playSequence(
            listOf(
                Note(Timbre.SOFT_CHIME, 523.25, 0.00, 0.26, 0.10),
                Note(Timbre.SOFT_CHIME, 659.25, 0.06, 0.28, 0.10),
                Note(Timbre.SOFT_CHIME, 783.99, 0.12, 0.36, 0.11)
            )
        )
    }

    fun playRoundLose() {
        vibrateError()
        playSequence(
            listOf(
                Note(Timbre.MUTED_WARM, 392.00, 0.00, 0.18, 0.095),
                Note(Timbre.MUTED_WARM, 329.63, 0.11, 0.24, 0.095)
            )
        )
    }

    fun playMatchWin() {
        vibrateSuccess()
        playSequence(
            listOf(
                Note(Timbre.SOFT_CHIME, 523.25, 0.00, 0.42, 0.10),
                Note(Timbre.SOFT_CHIME, 659.25, 0.07, 0.38, 0.10),
                Note(Timbre.SOFT_CHIME, 783.99, 0.14, 0.36, 0.10),
                Note(Timbre.SOFT_CHIME, 987.77, 0.21, 0.36, 0.10),
                Note(Timbre.SOFT_CHIME, 1046.50, 0.28, 0.50, 0.12)
            )
        )
    }

    fun playMatchDraw() {
        vibrate(14)
        playSequence(
            listOf(
                Note(Timbre.SOFT_CHIME, 440.00, 0.00, 0.25, 0.095),
                Note(Timbre.SOFT_CHIME, 523.25, 0.10, 0.30, 0.095)
            )
        )
    }

    fun playMatchLose() {
        vibrateError()
        playSequence(
            listOf(
                Note(Timbre.MUTED_WARM, 392.00, 0.00, 0.18, 0.095),
                Note(Timbre.MUTED_WARM, 349.23, 0.11, 0.18, 0.095),
                Note(Timbre.MUTED_WARM, 293.66, 0.22, 0.28, 0.10)
            )
        )
    }

    private fun renderNotesToPcm(
        notes: List<Note>,
        sampleRate: Int,
        smoothLoopEdges: Boolean = false
    ): ShortArray {
        val totalDuration = notes.maxOf { it.startOffsetSec + it.durationSec } + 0.04
        val totalSamples = (totalDuration * sampleRate).toInt().coerceAtLeast(512)
        val floatBuf = DoubleArray(totalSamples)

        for (note in notes) {
            val startSample = (note.startOffsetSec * sampleRate).toInt()
            val noteSamples = (note.durationSec * sampleRate).toInt().coerceAtLeast(1)
            val attackSec = when (note.timbre) {
                Timbre.WARM_PAD -> 0.12
                Timbre.SOFT_CHIME -> 0.008
                Timbre.WOOD_TAP -> 0.004
                Timbre.MUTED_WARM -> 0.007
            }
            val releaseSec = (note.durationSec * 0.25).coerceAtLeast(0.012)

            for (i in 0 until noteSamples) {
                val idx = startSample + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val p = (t / note.durationSec).coerceIn(0.0, 1.0)

                val attackEnv = if (t < attackSec) {
                    0.5 * (1.0 - cos(PI * (t / attackSec)))
                } else {
                    1.0
                }
                val timeRemaining = note.durationSec - t
                val releaseEnv = if (timeRemaining < releaseSec) {
                    0.5 * (1.0 - cos(PI * (timeRemaining / releaseSec).coerceIn(0.0, 1.0)))
                } else {
                    1.0
                }

                val decayRate = when (note.timbre) {
                    Timbre.WOOD_TAP -> 5.2
                    Timbre.SOFT_CHIME -> 3.4
                    Timbre.MUTED_WARM -> 4.0
                    Timbre.WARM_PAD -> 1.1
                }
                val envelope = attackEnv * releaseEnv * exp(-decayRate * p)

                val w = 2.0 * PI * note.freqHz * t
                val rawWave = when (note.timbre) {
                    Timbre.WOOD_TAP -> sin(w) + 0.16 * exp(-16.0 * p) * sin(3.0 * w)
                    Timbre.SOFT_CHIME -> sin(w) + 0.20 * exp(-5.0 * p) * sin(2.0 * w) + 0.05 * exp(-9.0 * p) * sin(3.0 * w)
                    Timbre.MUTED_WARM -> 0.85 * sin(w) + 0.15 * sin(0.5 * w)
                    Timbre.WARM_PAD -> 0.60 * sin(w) + 0.25 * sin(w * 1.002) + 0.15 * sin(2.0 * w)
                }

                floatBuf[idx] += rawWave * envelope * note.volume
            }
        }

        val out = ShortArray(totalSamples)
        val edgeSamples = if (smoothLoopEdges) (0.08 * sampleRate).toInt().coerceAtMost(totalSamples / 4) else 0

        for (i in 0 until totalSamples) {
            var v = floatBuf[i]
            if (edgeSamples > 0) {
                if (i < edgeSamples) {
                    v *= 0.5 * (1.0 - cos(PI * i.toDouble() / edgeSamples))
                } else if (i > totalSamples - edgeSamples) {
                    val rem = (totalSamples - i).toDouble() / edgeSamples
                    v *= 0.5 * (1.0 - cos(PI * rem.coerceIn(0.0, 1.0)))
                }
            }
            val clamped = v.coerceIn(-0.95, 0.95)
            out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playSequence(notes: List<Note>) {
        if (!soundEnabled || notes.isEmpty()) return
        scope.launch {
            runCatching {
                val sampleRate = 32000
                val buffer = renderNotesToPcm(notes, sampleRate)
                val totalDuration = buffer.size.toDouble() / sampleRate
                val byteCount = buffer.size * 2
                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(
                        byteCount.coerceAtLeast(
                            AudioTrack.getMinBufferSize(
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                        )
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((totalDuration * 1000).toLong() + 45L)
                track.release()
            }
        }
    }

    private fun getVibrator(): Vibrator? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    fun vibrate(durationMs: Long = 10L) {
        if (!hapticsEnabled) return
        runCatching {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun vibrateError() {
        if (!hapticsEnabled) return
        runCatching {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 28, 40, 38), -1))
            }
        }
    }

    private fun vibrateSuccess() {
        if (!hapticsEnabled) return
        runCatching {
            val v = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 18, 35, 30), -1))
            }
        }
    }
}
