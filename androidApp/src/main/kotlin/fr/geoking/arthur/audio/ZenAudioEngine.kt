package fr.geoking.arthur.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import fr.geoking.arthur.source.AmbientAudioMode
import fr.geoking.arthur.source.AmbientAudioSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural Zen audio engine using Android [AudioTrack].
 * Generates continuous soft ambient pads and soothing pentatonic transition chimes
 * without external audio sample files.
 */
class ZenAudioEngine(
    context: Context,
    private val audioSettings: AmbientAudioSettings,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var settingsJob: Job? = null

    private val isRunning = AtomicBoolean(false)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    @Volatile
    private var masterVolume: Float = audioSettings.volume.value

    @Volatile
    private var audioMode: AmbientAudioMode = audioSettings.mode.value

    @Volatile
    private var isEnabled: Boolean = audioSettings.enabled.value

    // Pentatonic scale notes in Hz (C5, D5, E5, G5, A5, C6)
    private val chimeNotes = floatArrayOf(
        523.25f, // C5
        587.33f, // D5
        659.25f, // E5
        783.99f, // G5
        880.00f, // A5
        1046.50f, // C6
    )

    private var chimeIndex = 0

    // Active chime state
    @Volatile
    private var activeChimeFreq = 0f

    @Volatile
    private var chimeSampleIndex = 0L

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(2048)

    init {
        settingsJob = scope.launch {
            combine(audioSettings.enabled, audioSettings.volume, audioSettings.mode) { enabled, vol, mode ->
                Triple(enabled, vol, mode)
            }.collect { (enabled, vol, mode) ->
                isEnabled = enabled
                masterVolume = vol
                audioMode = mode
                if (!enabled && isRunning.get()) {
                    stop()
                }
            }
        }
    }

    @Synchronized
    fun start() {
        if (!isEnabled) return
        if (isRunning.getAndSet(true)) return

        if (!requestFocus()) {
            isRunning.set(false)
            return
        }

        initAudioTrack()
        audioTrack?.play()

        synthesisJob = scope.launch(Dispatchers.Default) {
            renderAudioLoop()
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning.getAndSet(false)) return

        synthesisJob?.cancel()
        synthesisJob = null

        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null

        abandonFocus()
    }

    fun triggerChime() {
        if (!isEnabled || !isRunning.get()) return
        if (audioMode == AmbientAudioMode.PAD_ONLY) return

        chimeIndex = (chimeIndex + 1) % chimeNotes.size
        activeChimeFreq = chimeNotes[chimeIndex]
        chimeSampleIndex = 0L
    }

    private fun initAudioTrack() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private suspend fun renderAudioLoop() {
        val chunkSamples = 1024
        val pcmBuffer = ShortArray(chunkSamples)
        var totalSampleCount = 0L

        // Pad oscillators phase accumulators
        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0

        // Soft pad fundamental base frequencies (A3, E4, A4)
        val f1 = 220.0
        val f2 = 329.63
        val f3 = 440.0

        while (scope.isActive && isRunning.get()) {
            val track = audioTrack ?: break
            val playPad = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.PAD_ONLY
            val playChime = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.CHIME_ONLY

            for (i in 0 until chunkSamples) {
                totalSampleCount++
                val t = totalSampleCount.toDouble() / sampleRate

                var padSample = 0.0
                if (playPad) {
                    // LFO breathing (0.15 Hz slow volume wave)
                    val lfo = 0.6 + 0.4 * sin(2.0 * PI * 0.15 * t)

                    // Phase increments
                    phase1 += 2.0 * PI * f1 / sampleRate
                    phase2 += 2.0 * PI * f2 / sampleRate
                    phase3 += 2.0 * PI * f3 / sampleRate

                    if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                    if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                    if (phase3 > 2.0 * PI) phase3 -= 2.0 * PI

                    // Soft warm synthesis: sine harmonics
                    val wave1 = sin(phase1)
                    val wave2 = sin(phase2) * 0.5
                    val wave3 = sin(phase3) * 0.25

                    padSample = (wave1 + wave2 + wave3) * 0.25 * lfo
                }

                var chimeSample = 0.0
                if (playChime && activeChimeFreq > 0f) {
                    val chimeSampleIdx = chimeSampleIndex++
                    val chimeT = chimeSampleIdx.toDouble() / sampleRate
                    if (chimeT < 3.5) {
                        val freq = activeChimeFreq.toDouble()
                        // Envelope: 10ms attack, exponential decay
                        val attack = (chimeT / 0.010).coerceAtMost(1.0)
                        val decay = exp(-1.8 * chimeT)
                        val envelope = attack * decay

                        // Bell harmonics (fundamental + metallic overtones)
                        val h1 = sin(2.0 * PI * freq * chimeT)
                        val h2 = sin(2.0 * PI * freq * 2.76 * chimeT) * 0.4
                        val h3 = sin(2.0 * PI * freq * 5.40 * chimeT) * 0.15

                        chimeSample = (h1 + h2 + h3) * 0.5 * envelope
                    } else {
                        activeChimeFreq = 0f
                    }
                }

                val mixed = (padSample + chimeSample) * masterVolume
                val clamped = mixed.coerceIn(-1.0, 1.0)
                pcmBuffer[i] = (clamped * 32767.0).toInt().toShort()
            }

            track.write(pcmBuffer, 0, chunkSamples)
        }
    }

    private fun requestFocus(): Boolean {
        if (audioManager == null) return false
        val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                -> stop()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    audioTrack?.setVolume(0.2f * masterVolume)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    audioTrack?.setVolume(1.0f * masterVolume)
                }
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setOnAudioFocusChangeListener(listener)
                .build()
            audioFocusRequest = req
            val res = audioManager.requestAudioFocus(req)
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            hasAudioFocus
        }
    }

    private fun abandonFocus() {
        if (!hasAudioFocus || audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    fun destroy() {
        stop()
        settingsJob?.cancel()
        scope.cancel()
    }
}
