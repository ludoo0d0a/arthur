package fr.geoking.arthur.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import fr.geoking.arthur.shared.domain.Artwork
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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Jazz Bar / Piano Ambient procedural audio engine using Android [AudioTrack].
 *
 * Generates continuous warm piano chords (Rhodes/Electric piano style with warm overtones),
 * ambient jazz harmonies (Major 7th, Minor 9th, Dorian, Lydian), and gentle random melodic accents
 * based on the active artwork identity without whistle/buzz/rumble frequencies.
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

    @Volatile
    private var currentArtwork: Artwork? = null

    // Musical profile for active artwork
    data class MusicalProfile(
        val scaleNotes: FloatArray, // Frequencies in Hz
        val chordFrequencies: List<FloatArray>, // 3-4 chord voicings (frequencies in Hz)
        val bpm: Double, // Slow tempo (30 - 55 BPM)
        val detuneFactor: Double, // Gentle chorus effect
    )

    @Volatile
    private var activeProfile: MusicalProfile = defaultProfile()

    // Transition chime trigger state
    @Volatile
    private var chimeIndex = 0

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

    /**
     * Updates the current artwork, deterministically deriving a unique jazz/piano musical identity.
     */
    fun setArtwork(artwork: Artwork?) {
        currentArtwork = artwork
        activeProfile = generateProfile(artwork)
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

        val scale = activeProfile.scaleNotes
        if (scale.isNotEmpty()) {
            chimeIndex = (chimeIndex + 1) % scale.size
            activeChimeFreq = scale[chimeIndex]
            chimeSampleIndex = 0L
        }
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

    // Active key voice structure for piano synthesis
    private class ActivePianoNote(
        val freq: Double,
        val startTime: Double,
        val amplitude: Double,
        var currentSampleIdx: Long = 0L,
    )

    private suspend fun renderAudioLoop() {
        val chunkSamples = 1024
        val pcmBuffer = ShortArray(chunkSamples)
        var totalSampleCount = 0L

        val activeNotes = mutableListOf<ActivePianoNote>()
        var lastChordChangeTime = -10.0
        var currentChordIndex = 0
        var lastMelodyTime = -10.0

        val rng = Random(System.currentTimeMillis())

        // Smooth low-pass state for anti-sibilance / warm cutoff filter
        var lpPrev = 0.0

        while (scope.isActive && isRunning.get()) {
            val track = audioTrack ?: break
            val playPad = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.PAD_ONLY
            val playChime = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.CHIME_ONLY

            val profile = activeProfile
            val chordDurationSec = 60.0 / profile.bpm * 4.0 // 1 measure of 4 beats per chord

            for (i in 0 until chunkSamples) {
                totalSampleCount++
                val t = totalSampleCount.toDouble() / sampleRate

                // 1. Chord & Melody Generator logic (scheduled at sample boundaries)
                if (playPad) {
                    // Check chord progression change
                    if (t - lastChordChangeTime >= chordDurationSec) {
                        lastChordChangeTime = t
                        val chordList = profile.chordFrequencies
                        if (chordList.isNotEmpty()) {
                            currentChordIndex = (currentChordIndex + 1) % chordList.size
                            val chord = chordList[currentChordIndex]

                            // Trigger soft piano keys for the chord
                            for (freq in chord) {
                                activeNotes.add(
                                    ActivePianoNote(
                                        freq = freq.toDouble(),
                                        startTime = t,
                                        amplitude = 0.22,
                                    ),
                                )
                            }
                        }
                    }

                    // Check random gentle jazz piano melody note (every 1.5 to 3.5 seconds)
                    val melodyInterval = 1.5 + rng.nextDouble() * 2.0
                    if (t - lastMelodyTime >= melodyInterval) {
                        lastMelodyTime = t
                        if (rng.nextDouble() < 0.7 && profile.scaleNotes.isNotEmpty()) {
                            val noteFreq = profile.scaleNotes[rng.nextInt(profile.scaleNotes.size)]
                            activeNotes.add(
                                ActivePianoNote(
                                    freq = noteFreq.toDouble(),
                                    startTime = t,
                                    amplitude = 0.18 + rng.nextDouble() * 0.08,
                                ),
                            )
                        }
                    }
                }

                // 2. Synthesize active piano voices (Pad / Chords + Melody)
                var padSample = 0.0
                if (playPad && activeNotes.isNotEmpty()) {
                    val noteIterator = activeNotes.iterator()
                    while (noteIterator.hasNext()) {
                        val note = noteIterator.next()
                        val noteT = note.currentSampleIdx.toDouble() / sampleRate
                        note.currentSampleIdx++

                        // Electric Piano / Rhodes acoustic curve:
                        // Envelope: 12ms soft attack, exponential decay (decay constant = 0.8 => ~3.5s length)
                        val attack = (noteT / 0.012).coerceAtMost(1.0)
                        val decay = exp(-0.75 * noteT)
                        val env = attack * decay

                        if (env < 0.001 || noteT > 5.0) {
                            noteIterator.remove()
                            continue
                        }

                        // Warm Piano Harmonics:
                        // Fundamental + soft 2nd harmonic + gentle 3rd harmonic (Rhodes metallic bell tines)
                        val f = note.freq
                        val detune = profile.detuneFactor
                        val h1 = sin(2.0 * PI * f * noteT)
                        val h1Chorus = sin(2.0 * PI * (f * (1.0 + detune)) * noteT) * 0.5
                        val h2 = sin(2.0 * PI * (f * 2.0) * noteT) * 0.25 * exp(-1.5 * noteT) // Second harmonic decays faster
                        val h3 = sin(2.0 * PI * (f * 3.002) * noteT) * 0.08 * exp(-2.5 * noteT) // Bell overtone

                        val rawVoice = (h1 + h1Chorus + h2 + h3) * note.amplitude * env
                        padSample += rawVoice
                    }
                }

                // 3. Synthesize Chime / Transition sound
                var chimeSample = 0.0
                if (playChime && activeChimeFreq > 0f) {
                    val chimeSampleIdx = chimeSampleIndex++
                    val chimeT = chimeSampleIdx.toDouble() / sampleRate
                    if (chimeT < 3.5) {
                        val freq = activeChimeFreq.toDouble()
                        // Bell envelope: 8ms attack, smooth exponential decay
                        val attack = (chimeT / 0.008).coerceAtMost(1.0)
                        val decay = exp(-1.2 * chimeT)
                        val envelope = attack * decay

                        // Warm Fender-Rhodes chime tone
                        val h1 = sin(2.0 * PI * freq * chimeT)
                        val h2 = sin(2.0 * PI * freq * 2.001 * chimeT) * 0.3 * exp(-2.0 * chimeT)
                        val h3 = sin(2.0 * PI * freq * 3.005 * chimeT) * 0.1 * exp(-3.0 * chimeT)

                        chimeSample = (h1 + h2 + h3) * 0.35 * envelope
                    } else {
                        activeChimeFreq = 0f
                    }
                }

                // 4. Mixing and Band-pass / Low-pass Filter
                val mixed = (padSample + chimeSample) * masterVolume

                // One-pole IIR Low-pass Filter (~2200 Hz cutoff) to eliminate whistle/sibilance
                // y[n] = y[n-1] + alpha * (x[n] - y[n-1])
                val alpha = 0.28
                val filtered = lpPrev + alpha * (mixed - lpPrev)
                lpPrev = filtered

                val clamped = filtered.coerceIn(-1.0, 1.0)
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

    companion object {
        private fun midiToFreq(note: Double): Float {
            return (440.0 * 2.0.pow((note - 69.0) / 12.0)).toFloat()
        }

        /** Default jazz profile (C Major 7 / A Minor 9 Bar Ambiance) */
        private fun defaultProfile(): MusicalProfile {
            // Notes in C Dorian / Major 7 (C, D, E, F, G, A, B)
            val scaleMidi = doubleArrayOf(60.0, 62.0, 64.0, 65.0, 67.0, 69.0, 71.0, 72.0, 74.0, 76.0)
            val scaleFreqs = FloatArray(scaleMidi.size) { midiToFreq(scaleMidi[it]) }

            // Chords: Cmaj7 (C3, G3, B3, E4), Am9 (A2, G3, C4, E4), Fmaj7 (F2, F3, C4, E4), Dm7 (D3, F3, C4, E4)
            val chords = listOf(
                floatArrayOf(midiToFreq(48.0), midiToFreq(55.0), midiToFreq(59.0), midiToFreq(64.0)),
                floatArrayOf(midiToFreq(45.0), midiToFreq(55.0), midiToFreq(60.0), midiToFreq(64.0)),
                floatArrayOf(midiToFreq(41.0), midiToFreq(53.0), midiToFreq(60.0), midiToFreq(64.0)),
                floatArrayOf(midiToFreq(50.0), midiToFreq(53.0), midiToFreq(60.0), midiToFreq(64.0)),
            )

            return MusicalProfile(
                scaleNotes = scaleFreqs,
                chordFrequencies = chords,
                bpm = 38.0,
                detuneFactor = 0.0015,
            )
        }

        /** Deterministically generate a unique jazz/bar ambient profile based on artwork identity */
        fun generateProfile(artwork: Artwork?): MusicalProfile {
            if (artwork == null) return defaultProfile()

            val seed = (artwork.id.hashCode() xor artwork.title.hashCode()).toLong()
            val rng = Random(seed)

            // Roots for Jazz Bar ambiance (C, D, Eb, F, G, Ab, A, Bb) in octave 2/3
            val rootMidis = intArrayOf(48, 50, 51, 53, 55, 56, 57, 58)
            val root = rootMidis[rng.nextInt(rootMidis.size)]

            // Jazz Scales: 0 = Dorian, 1 = Major 7th, 2 = Lydian, 3 = Minor 9th
            val scaleType = rng.nextInt(4)
            val scaleIntervals = when (scaleType) {
                0 -> intArrayOf(0, 2, 3, 5, 7, 9, 10) // Dorian
                1 -> intArrayOf(0, 2, 4, 7, 9, 11) // Major 7th
                2 -> intArrayOf(0, 2, 4, 6, 7, 9, 11) // Lydian
                else -> intArrayOf(0, 2, 3, 5, 7, 8, 10) // Minor 9th / Aeolian
            }

            // Build melodic scale notes (spanning octaves 4 and 5: 60..84)
            val melodyScaleList = mutableListOf<Float>()
            for (octave in 1..2) {
                for (interval in scaleIntervals) {
                    val noteMidi = (root + 12) + (octave - 1) * 12 + interval
                    if (noteMidi in 60..86) {
                        melodyScaleList.add(midiToFreq(noteMidi.toDouble()))
                    }
                }
            }
            val scaleFreqs = melodyScaleList.toFloatArray()

            // Build Jazz Chord Progression (4 chords: i - iv - ii - V / I - vi - ii - V)
            val chordProgressions = when (scaleType) {
                0 -> listOf(
                    intArrayOf(0, 3, 7, 10), // i7
                    intArrayOf(5, 8, 12, 15), // iv7
                    intArrayOf(2, 5, 8, 12), // ii7b5
                    intArrayOf(7, 10, 14, 17), // v7
                )
                1 -> listOf(
                    intArrayOf(0, 4, 7, 11), // Imaj7
                    intArrayOf(9, 12, 16, 19), // vi7
                    intArrayOf(2, 5, 9, 12), // ii7
                    intArrayOf(7, 11, 14, 17), // V7
                )
                2 -> listOf(
                    intArrayOf(0, 4, 7, 11), // Imaj7#11
                    intArrayOf(2, 6, 9, 12), // II7
                    intArrayOf(7, 11, 14, 17), // Vmaj7
                    intArrayOf(0, 4, 7, 11),
                )
                else -> listOf(
                    intArrayOf(0, 3, 7, 10), // i7
                    intArrayOf(8, 12, 15, 19), // VImaj7
                    intArrayOf(5, 8, 12, 15), // iv7
                    intArrayOf(7, 10, 14, 17), // v7
                )
            }

            val chordFrequencies = chordProgressions.map { prog ->
                floatArrayOf(
                    midiToFreq((root + prog[0]).toDouble()),
                    midiToFreq((root + prog[1]).toDouble()),
                    midiToFreq((root + prog[2]).toDouble()),
                    midiToFreq((root + prog[3]).toDouble()),
                )
            }

            val bpm = 32.0 + rng.nextDouble() * 20.0 // 32..52 BPM slow relaxation
            val detune = 0.001 + rng.nextDouble() * 0.0015 // Chorus warmth

            return MusicalProfile(
                scaleNotes = scaleFreqs,
                chordFrequencies = chordFrequencies,
                bpm = bpm,
                detuneFactor = detune,
            )
        }
    }
}
