package fr.geoking.arthur.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import fr.geoking.arthur.audio.banks.TextureCueKind
import fr.geoking.arthur.audio.markov.ArrangementForm
import fr.geoking.arthur.audio.markov.MarkovSequencer
import fr.geoking.arthur.audio.markov.OrnamentKind
import fr.geoking.arthur.audio.voices.BowlVoice
import fr.geoking.arthur.audio.voices.ChimeClusterVoice
import fr.geoking.arthur.audio.voices.KalimbaPluckVoice
import fr.geoking.arthur.audio.voices.PluckGuitarVoice
import fr.geoking.arthur.audio.voices.SinePadVoice
import fr.geoking.arthur.audio.voices.SoftBassVoice
import fr.geoking.arthur.audio.voices.SoftPianoPool
import fr.geoking.arthur.audio.voices.SoftPulseVoice
import fr.geoking.arthur.audio.voices.WaveNoiseVoice
import fr.geoking.arthur.audio.voices.WindTextureVoice
import fr.geoking.arthur.audio.voices.softLimit
import fr.geoking.arthur.error.ErrorTrap
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.source.AmbientAudioCharacter
import fr.geoking.arthur.source.AmbientAudioSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlin.random.Random

/**
 * Multi-track procedural ambient engine: Markov melody/harmony/bass + style voices.
 * Preset is unique per artwork; performance walks randomly inside that preset.
 *
 * Melody/Balanced: up to 3 stems (bass, harmony pad, melody).
 * Atmosphere: drone bed + tonal texture + sparse lead.
 */
class ProceduralMusicEngine(
    context: Context,
    private val audioSettings: AmbientAudioSettings,
    private val errorLogger: ErrorLogger? = null,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val audioExceptionHandler = errorLogger?.let {
        ErrorTrap.coroutineHandler(it, sourceId = "ambient_audio")
    } ?: CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + audioExceptionHandler)

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var settingsJob: Job? = null

    private val isRunning = AtomicBoolean(false)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    /** Notified on focus loss/gain so callers can pause/resume ambient play state. */
    var onAudioFocusChanged: ((AudioFocusEvent) -> Unit)? = null

    @Volatile private var masterVolume: Float = audioSettings.volume.value
    @Volatile private var character: AmbientAudioCharacter = audioSettings.character.value
    @Volatile private var complexity: Float = audioSettings.complexity.value
    @Volatile private var stylePreference: MusicStyle? = audioSettings.stylePreference.value
    @Volatile private var isEnabled: Boolean = audioSettings.enabled.value

    @Volatile private var preset: MusicPreset? = null
    @Volatile private var pendingPreset: MusicPreset? = null
    @Volatile private var crossfadeSamples = 0
    @Volatile private var crossfadeTotal = 0
    @Volatile private var triggerTransitionFlag = false
    @Volatile private var currentArtwork: Artwork? = null

    // Master duck envelope for slide transitions (samples remaining in each phase).
    @Volatile private var duckSamples = 0
    @Volatile private var duckTotal = 0
    @Volatile private var duckPhase = DuckPhase.Idle
    private var pendingTransitionCue = false

    private val sessionSalt = System.nanoTime()
    private val pulseRng = Random(sessionSalt)

    private var sequencer: MarkovSequencer? = null
    private var form: ArrangementForm? = null
    private var cachedPartials: List<Float> = emptyList()
    private var lastChordKey: Int = Int.MIN_VALUE

    // Voices (reused across presets). Melody/Balanced use a single pad; Atmosphere may use padB.
    private val padA = SinePadVoice(2.5f, glideSeconds = 0.06f)
    private val padB = SinePadVoice(3f, glideSeconds = 0.06f)
    private val piano = SoftPianoPool(3)
    private val guitar = PluckGuitarVoice()
    private val bass = SoftBassVoice()
    private val bowl = BowlVoice()
    private val waves = WaveNoiseVoice(11L)
    private val wind = WindTextureVoice(22L)
    private val pulse = SoftPulseVoice()
    private val chimes = ChimeClusterVoice()
    private val kalimba = KalimbaPluckVoice()

    private val cpuLoad = CpuLoadTracker()

    /** Device-native rate when available (often 48000); avoids system resampling. */
    private var sampleRate: Int = resolveSampleRate()
    private var useFloatPcm: Boolean = true
    private var bufferSizeBytes: Int = 0

    /** Latest adaptive quality (for diagnostics / tests). */
    internal val synthQuality: SynthQuality get() = cpuLoad.quality
    internal val cpuLoadEma: Float get() = cpuLoad.loadEma

    private enum class DuckPhase { Idle, Down, Hold, Up }

    private fun resolveSampleRate(): Int {
        val fromProp = audioManager
            ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()
        if (fromProp != null && fromProp in 22_050..96_000) return fromProp
        val native = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        return if (native in 22_050..96_000) native else 48_000
    }

    init {
        settingsJob = scope.launch {
            combine(
                audioSettings.enabled,
                audioSettings.volume,
                audioSettings.character,
                audioSettings.complexity,
                audioSettings.stylePreference,
            ) { enabled, vol, char, comp, stylePref ->
                SettingsSnapshot(enabled, vol, char, comp, stylePref)
            }.collect { snap ->
                val characterChanged = character != snap.character
                val prefsChanged = characterChanged ||
                    complexity != snap.complexity ||
                    stylePreference != snap.stylePreference
                isEnabled = snap.enabled
                masterVolume = snap.volume
                character = snap.character
                complexity = snap.complexity
                stylePreference = snap.stylePreference
                if (!snap.enabled && isRunning.get()) stop()
                if (prefsChanged && isEnabled) {
                    currentArtwork?.let { setArtwork(it) }
                }
            }
        }
    }

    private data class SettingsSnapshot(
        val enabled: Boolean,
        val volume: Float,
        val character: AmbientAudioCharacter,
        val complexity: Float,
        val stylePreference: MusicStyle?,
    )

    private fun userPrefs() = MusicUserPrefs(
        character = character,
        stylePreference = stylePreference,
        complexity = complexity,
    )

    /** Apply artwork-derived preset with crossfade; safe when stopped. */
    fun setArtwork(artwork: Artwork) {
        currentArtwork = artwork
        val next = MusicPresetResolver.resolve(artwork, userPrefs())
        if (!isRunning.get()) {
            preset = next
            sequencer = MarkovSequencer(next, sessionSalt, character)
            form = ArrangementForm(next.formSeed)
            return
        }
        pendingPreset = next
        crossfadeTotal = (2.0f * sampleRate).toInt()
        crossfadeSamples = crossfadeTotal
    }

    /** Fire a style-appropriate transition cue with master duck (slide advance). */
    fun triggerTransition() {
        if (!isEnabled) return
        triggerTransitionFlag = true
        form?.forceBridge(sampleRate, 2.5f)
        sequencer?.resetPhrase()
    }

    /** Back-compat alias used by older call sites. */
    fun triggerChime() = triggerTransition()

    @Synchronized
    fun start() {
        if (!isEnabled) return
        if (isRunning.getAndSet(true)) return
        if (!requestFocus()) {
            isRunning.set(false)
            return
        }
        if (preset == null) {
            val fallbackArt = Artwork(
                id = "ambient.default",
                title = "Ambient",
                sourceId = "ambient",
                kind = fr.geoking.arthur.shared.domain.ArtworkKind.Genart,
            )
            currentArtwork = fallbackArt
            val fallback = MusicPresetResolver.resolve(fallbackArt, userPrefs())
            preset = fallback
            sequencer = MarkovSequencer(fallback, sessionSalt, character)
            form = ArrangementForm(fallback.formSeed)
        }
        initAudioTrack()
        audioTrack?.play()
        synthesisJob = scope.launch(Dispatchers.Default) { renderAudioLoop() }
    }

    @Synchronized
    fun stop(abandonFocus: Boolean = true) {
        if (!isRunning.getAndSet(false)) {
            if (abandonFocus) abandonFocus()
            return
        }
        synthesisJob?.cancel()
        synthesisJob = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
        if (abandonFocus) abandonFocus()
    }

    fun destroy() {
        stop()
        settingsJob?.cancel()
        scope.cancel()
    }

    private fun initAudioTrack() {
        sampleRate = resolveSampleRate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        // Prefer float PCM; fall back to 16-bit if the device rejects it.
        useFloatPcm = true
        var encoding = AudioFormat.ENCODING_PCM_FLOAT
        var minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            encoding,
        )
        if (minBuf <= 0) {
            useFloatPcm = false
            encoding = AudioFormat.ENCODING_PCM_16BIT
            minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                encoding,
            )
        }
        bufferSizeBytes = minBuf.coerceAtLeast(if (useFloatPcm) 8192 else 4096)

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(encoding)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val builder = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        audioTrack = builder.build()
    }

    private fun beginDuck() {
        duckTotal = (0.32f * sampleRate).toInt().coerceAtLeast(1)
        duckSamples = duckTotal
        duckPhase = DuckPhase.Down
        pendingTransitionCue = true
    }

    private fun duckGain(): Float {
        if (duckPhase == DuckPhase.Idle) return 1f
        val t = 1f - duckSamples.toFloat() / duckTotal.toFloat()
        return when (duckPhase) {
            DuckPhase.Down -> 1f - 0.75f * t
            DuckPhase.Hold -> 0.25f
            DuckPhase.Up -> 0.25f + 0.75f * t
            DuckPhase.Idle -> 1f
        }
    }

    private fun advanceDuck() {
        if (duckPhase == DuckPhase.Idle) return
        if (duckSamples > 0) {
            duckSamples--
            return
        }
        when (duckPhase) {
            DuckPhase.Down -> {
                duckPhase = DuckPhase.Hold
                duckTotal = (0.12f * sampleRate).toInt().coerceAtLeast(1)
                duckSamples = duckTotal
            }
            DuckPhase.Hold -> {
                duckPhase = DuckPhase.Up
                duckTotal = (0.40f * sampleRate).toInt().coerceAtLeast(1)
                duckSamples = duckTotal
            }
            DuckPhase.Up -> duckPhase = DuckPhase.Idle
            DuckPhase.Idle -> Unit
        }
    }

    private fun releaseLeadVoices() {
        // Soft-release event voices; pads glide to the new chord instead of cutting.
        piano.noteOff()
        guitar.noteOff()
        kalimba.noteOff()
        chimes.noteOff()
        bass.noteOff()
    }

    private suspend fun renderAudioLoop() = coroutineScope {
        val chunk = 2048
        // Ping-pong buffers: render into one while the previous is written.
        val floatBufs = arrayOf(FloatArray(chunk), FloatArray(chunk))
        val shortBufs = arrayOf(ShortArray(chunk), ShortArray(chunk))
        var sampleIndex = 0L
        var pulseClock = 0
        var crossfading = false
        var bufIdx = 0
        var prefilled = false
        cpuLoad.reset()

        while (scope.isActive && isRunning.get()) {
            val track = audioTrack ?: break
            val char = character
            val atmosphere = char == AmbientAudioCharacter.Atmosphere
            val sr = sampleRate
            val quality = cpuLoad.quality

            pendingPreset?.let { next ->
                if (crossfadeSamples == crossfadeTotal) {
                    releaseLeadVoices()
                    crossfading = true
                    preset = next
                    sequencer = MarkovSequencer(next, sessionSalt xor sampleIndex, char)
                    form = ArrangementForm(next.formSeed)
                    lastChordKey = Int.MIN_VALUE
                    applyBedVoices(next, sequencer!!, atmosphere, quality)
                    pendingPreset = null
                }
            }

            val activePreset = preset ?: continue
            val seq = sequencer ?: continue
            val arrangement = form ?: continue

            val glide = if (atmosphere) 0.22f else 0.06f
            padA.setGlideSeconds(glide)
            padB.setGlideSeconds(glide)

            val trackMix = activePreset.trackMix
            val melodyGain = trackMix.melody
            val bassGain = trackMix.bass
            val bedGain = trackMix.bed
            val harmonyGain = trackMix.harmony
            // Melody/Balanced: one pad stem (bed+harmony merged). Atmosphere Full may use padB.
            val useDualPad = atmosphere && quality == SynthQuality.Full
            val padGain = combinedPadGain(bedGain, harmonyGain, useDualPad)
            val textureGain = when (quality) {
                SynthQuality.Minimal -> 0f
                else -> trackMix.texture
            }
            val pulseGain = when (quality) {
                SynthQuality.Minimal -> 0f
                SynthQuality.Reduced -> trackMix.pulse * 0.5f
                SynthQuality.Full -> trackMix.pulse
            }
            val transitionGain = when (quality) {
                SynthQuality.Minimal -> trackMix.transition * 0.35f
                else -> trackMix.transition
            }
            val allowOrnaments = quality == SynthQuality.Full && !atmosphere
            val root = activePreset.rootHz
            waves.setRootHz(root)
            wind.setRootHz(root)
            val vol = masterVolume
            val samplesPerPulse = if (pulseGain > 0.05f) {
                (60.0 / activePreset.tempoBpm * sr).toInt().coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
            val pianoAudible = melodyGain > 0.02f
            val bassAudibleStem = bassGain > 0.02f
            val pulseAudibleStem = pulseGain > 0.05f
            val transAudibleStem = transitionGain > 0.02f
            val bowlBed = atmosphere &&
                activePreset.style in setOf(MusicStyle.TibetanBowl, MusicStyle.CosmicDrone) &&
                bedGain > 0.02f &&
                quality != SynthQuality.Minimal
            // Exactly one texture voice for Atmosphere (never wind+waves together).
            val textureKind = atmosphereTextureKind(activePreset.style, atmosphere, textureGain)

            // Prefill buf0 once, then ping-pong: write(bufIdx) || render(next).
            val renderIdx = if (!prefilled) 0 else 1 - bufIdx
            val writeJob = if (prefilled) {
                async(Dispatchers.IO) {
                    writePcm(track, floatBufs[bufIdx], shortBufs[bufIdx], chunk)
                }
            } else {
                null
            }
            val t0 = System.nanoTime()

            // Coarse clock: skip sequencer work in idle stretches (up to 64 samples).
            var i = 0
            while (i < chunk) {
                val melWait = seq.samplesUntilMelody()
                val bassWait = seq.samplesUntilBass()
                val harmWait = seq.samplesUntilHarmony()
                val eventDue = melWait == 0 || bassWait == 0 || harmWait == 0 ||
                    triggerTransitionFlag ||
                    (pulseAudibleStem && pulseClock + 1 >= samplesPerPulse)

                val run = if (eventDue) {
                    1
                } else {
                    minOf(
                        64,
                        chunk - i,
                        melWait,
                        bassWait,
                        harmWait,
                        samplesPerPulse - pulseClock,
                        if (crossfading) crossfadeSamples.coerceAtLeast(1) else Int.MAX_VALUE,
                        if (duckPhase != DuckPhase.Idle) duckSamples.coerceAtLeast(1) else Int.MAX_VALUE,
                    ).coerceAtLeast(1)
                }

                if (run > 1) {
                    seq.skipSamples(run)
                    arrangement.skipSamples(run, sr)
                }

                var k = 0
                while (k < run) {
                    sampleIndex++
                    if (run == 1) {
                        arrangement.tick(sr)
                        seq.advanceHarmonyClock(sr)
                        if (seq.harmonyChanged) {
                            applyBedVoices(activePreset, seq, atmosphere, quality)
                        }
                        if (seq.tickMelody(sr)) {
                            fireMelodyNote(activePreset, seq, atmosphere, quality)
                            if (allowOrnaments) {
                                when (seq.nextOrnament()) {
                                    OrnamentKind.Grace ->
                                        seq.currentMelodyHz?.let {
                                            // Soft fifth above, still under the melody ceiling.
                                            val grace = (it * 1.5f)
                                                .coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                                            chimes.noteOn(grace, 0.22f)
                                        }
                                    OrnamentKind.Roll ->
                                        seq.currentMelodyHz?.let { kalimba.noteOn(it, 0.35f) }
                                    OrnamentKind.DoubleStrike -> seq.currentMelodyHz?.let {
                                        piano.noteOn(it, 0.30f)
                                        // Same register echo — avoid octave-up chime glare.
                                        chimes.noteOn(it, 0.16f)
                                    }
                                    OrnamentKind.None -> Unit
                                }
                            }
                        }
                        if (bassAudibleStem && seq.tickBass(sr)) {
                            seq.currentBassFrequencyHz?.let { hz ->
                                val vel = if (atmosphere) 0.35f else 0.55f
                                bass.noteOn(hz, vel * bassGain.coerceAtMost(1f))
                            }
                        }
                        if (pulseAudibleStem) {
                            pulseClock++
                            if (pulseClock >= samplesPerPulse) {
                                pulseClock = 0
                                if (pulseRng.nextFloat() < activePreset.density * 0.35f) {
                                    pulse.noteOn(90f, 0.22f * pulseGain)
                                }
                            }
                        }
                        if (triggerTransitionFlag) {
                            triggerTransitionFlag = false
                            beginDuck()
                        }
                        if (pendingTransitionCue && duckPhase == DuckPhase.Hold && duckSamples == duckTotal) {
                            pendingTransitionCue = false
                            fireTransition(activePreset, seq)
                        }
                    } else if (pulseAudibleStem) {
                        pulseClock++
                    }

                    var mixed = 0f

                    // Single pad for Melody/Balanced; dual only for Atmosphere Full.
                    if (padGain > 0.02f) {
                        mixed += padA.render(sampleIndex, sr) * padGain
                    } else {
                        padA.render(sampleIndex, sr)
                    }
                    if (useDualPad && harmonyGain > 0.02f) {
                        mixed += padB.render(sampleIndex, sr) * harmonyGain
                    } else {
                        // Keep padB warm cheaply only when dual-pad may return.
                        if (atmosphere) padB.render(sampleIndex, sr)
                    }

                    if (bassAudibleStem && bass.isAudible()) {
                        mixed += bass.render(sampleIndex, sr) * bassGain
                    }

                    if (pianoAudible &&
                        (piano.isAudible() || guitar.isAudible() || kalimba.isAudible())
                    ) {
                        mixed += (
                            piano.render(sampleIndex, sr) +
                                guitar.render(sampleIndex, sr) +
                                kalimba.render(sampleIndex, sr)
                            ) * melodyGain * arrangement.melodyMul
                    }

                    if (textureKind != TextureKind.None) {
                        val texMul = textureGain * arrangement.textureMul
                        mixed += when (textureKind) {
                            TextureKind.Waves -> waves.render(sampleIndex, sr) * texMul
                            TextureKind.Wind -> wind.render(sampleIndex, sr) * texMul
                            TextureKind.None -> 0f
                        }
                    }

                    if (bowlBed && bowl.isAudible()) {
                        mixed += bowl.render(sampleIndex, sr) * bedGain * 0.45f
                    }

                    if (pulseAudibleStem && pulse.isAudible()) {
                        mixed += pulse.render(sampleIndex, sr) * pulseGain
                    }

                    if (transAudibleStem) {
                        if (chimes.isAudible() || bowl.isAudible()) {
                            mixed += (
                                chimes.render(sampleIndex, sr) +
                                    bowl.render(sampleIndex, sr) * 0.25f
                                ) * transitionGain
                        }
                    } else if (chimes.isAudible()) {
                        chimes.render(sampleIndex, sr)
                    }

                    if (crossfading && crossfadeSamples > 0) {
                        val t = 1f - crossfadeSamples.toFloat() / crossfadeTotal.toFloat()
                        val fadeIn =
                            (0.5f - 0.5f * kotlin.math.cos(Math.PI.toFloat() * t)).coerceIn(0f, 1f)
                        mixed *= fadeIn
                        crossfadeSamples--
                        if (crossfadeSamples == 0) crossfading = false
                    }

                    advanceDuck()
                    mixed = softLimit(mixed * duckGain() * vol)

                    if (useFloatPcm) {
                        floatBufs[renderIdx][i] = mixed
                    } else {
                        shortBufs[renderIdx][i] = (mixed * 32767f).toInt().toShort()
                    }
                    i++
                    k++
                }
            }

            cpuLoad.observe(System.nanoTime() - t0, chunk, sr)

            if (!prefilled) {
                // First fill only — next loop iteration writes it while rendering the other buffer.
                prefilled = true
                bufIdx = 0
                continue
            }

            val written = try {
                writeJob!!.await()
            } catch (e: IllegalStateException) {
                errorLogger?.log(
                    sourceId = "ambient_audio",
                    category = ErrorCategory.Unknown,
                    message = e.message ?: "AudioTrack write failed",
                    details = e.stackTraceToString().take(500),
                    throwable = e,
                )
                isRunning.set(false)
                break
            }
            if (written < 0) {
                errorLogger?.log(
                    sourceId = "ambient_audio",
                    category = ErrorCategory.Unknown,
                    message = "AudioTrack write returned $written",
                )
                isRunning.set(false)
                break
            }
            bufIdx = renderIdx
        }
    }

    private fun writePcm(track: AudioTrack, floats: FloatArray, shorts: ShortArray, chunk: Int): Int =
        if (useFloatPcm) {
            track.write(floats, 0, chunk, AudioTrack.WRITE_BLOCKING)
        } else {
            track.write(shorts, 0, chunk)
        }

    private fun applyBedVoices(
        p: MusicPreset,
        seq: MarkovSequencer,
        atmosphere: Boolean,
        quality: SynthQuality = SynthQuality.Full,
    ) {
        val partials = seq.harmonyPartialsHz()
        val key = partials.fold(0) { acc, f -> acc * 31 + f.toBits() }
        if (key != lastChordKey || cachedPartials.isEmpty()) {
            lastChordKey = key
            cachedPartials = partials
            padA.setChord(partials.ifEmpty { listOf(p.rootHz) })
            if (atmosphere && quality == SynthQuality.Full) {
                padB.setChord(partials.ifEmpty { listOf(p.rootHz) })
            }
        }
        val sustainBowl = atmosphere &&
            quality != SynthQuality.Minimal &&
            p.style in setOf(MusicStyle.TibetanBowl, MusicStyle.CosmicDrone)
        if (sustainBowl) {
            bowl.setSustainPartials(partials.ifEmpty { listOf(p.rootHz) }, true)
        } else {
            bowl.setSustain(p.rootHz, false)
        }
    }

    private fun fireMelodyNote(
        p: MusicPreset,
        seq: MarkovSequencer,
        atmosphere: Boolean,
        quality: SynthQuality = SynthQuality.Full,
    ) {
        val hz = seq.currentMelodyHz ?: return
        val vel = if (atmosphere) 0.32f else 0.55f
        when (p.style) {
            MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.NightLounge ->
                piano.noteOn(hz, vel)
            MusicStyle.SoftGuitar -> guitar.noteOn(hz, vel)
            MusicStyle.AfricanPulse -> kalimba.noteOn(hz, vel + 0.05f)
            MusicStyle.WindChimes -> chimes.noteOn(hz, vel)
            MusicStyle.TibetanBowl -> bowl.noteOn(hz, vel * 0.85f)
            MusicStyle.OceanWaves -> piano.noteOn(hz, vel * 0.75f)
            MusicStyle.CosmicDrone -> {
                piano.noteOn(hz, vel * 0.7f)
                if (!atmosphere && quality == SynthQuality.Full) {
                    chimes.noteOn(hz, vel * 0.5f)
                }
            }
            MusicStyle.Zen -> {
                piano.noteOn(hz, vel)
                if (!atmosphere &&
                    quality == SynthQuality.Full &&
                    Random(hz.toBits().toLong()).nextFloat() < 0.15f
                ) {
                    chimes.noteOn(hz, 0.18f)
                }
            }
        }
    }

    internal enum class TextureKind { None, Waves, Wind }

    companion object {
        /** Merge bed+harmony into one pad gain for Melody/Balanced (or Reduced Atmosphere). */
        internal fun combinedPadGain(bed: Float, harmony: Float, dualPad: Boolean): Float =
            if (dualPad) {
                bed.coerceIn(0f, 1f)
            } else {
                (bed + harmony * 0.85f).coerceIn(0f, 0.75f)
            }

        internal fun atmosphereTextureKind(
            style: MusicStyle,
            atmosphere: Boolean,
            textureGain: Float,
        ): TextureKind {
            if (!atmosphere || textureGain <= 0.02f) return TextureKind.None
            return when (style) {
                MusicStyle.OceanWaves -> TextureKind.Waves
                MusicStyle.WindChimes, MusicStyle.TibetanBowl, MusicStyle.CosmicDrone ->
                    TextureKind.Wind
                else -> TextureKind.Wind // single texture, never both
            }
        }
    }

    private fun fireTransition(p: MusicPreset, seq: MarkovSequencer) {
        val cue = seq.transitionCue()
        val root = p.rootHz
        when (cue) {
            TextureCueKind.BowlStrike -> bowl.noteOn(root, 0.45f)
            TextureCueKind.ChimeCluster -> {
                val scale = p.scaleSemitones
                val freqs = (0 until 3).map { i ->
                    val semi = scale[Math.floorMod(i + p.melodyBankIndex, scale.size)]
                    (root * 2.0.pow((semi + 12) / 12.0).toFloat())
                        .coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                }
                chimes.noteCluster(freqs, 0.40f)
            }
            TextureCueKind.ArpeggioCascade -> {
                val a = (root * 2f).coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                val b = (root * 2.5f).coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                piano.noteOn(a, 0.35f)
                piano.noteOn(b, 0.28f)
            }
            TextureCueKind.ThumbPianoRoll -> {
                val a = (root * 2f).coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                val b = (root * 2.5f).coerceAtMost(MarkovSequencer.MELODY_MAX_HZ)
                kalimba.noteOn(a, 0.4f)
                kalimba.noteOn(b, 0.32f)
            }
            TextureCueKind.WaveSwell, TextureCueKind.SoftRain, TextureCueKind.WindGust -> {
                chimes.noteOn(
                    (root * 2f).coerceAtMost(MarkovSequencer.MELODY_MAX_HZ),
                    0.22f,
                )
            }
            TextureCueKind.SilenceBreath -> Unit
        }
    }

    private fun requestFocus(): Boolean {
        if (audioManager == null) return false
        val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    hasAudioFocus = false
                    stop(abandonFocus = false)
                    onAudioFocusChanged?.invoke(AudioFocusEvent.Lost(transient = false))
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    hasAudioFocus = false
                    stop(abandonFocus = false)
                    onAudioFocusChanged?.invoke(AudioFocusEvent.Lost(transient = true))
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    audioTrack?.setVolume(0.2f * masterVolume)
                AudioManager.AUDIOFOCUS_GAIN -> {
                    hasAudioFocus = true
                    audioTrack?.setVolume(1.0f * masterVolume)
                    onAudioFocusChanged?.invoke(AudioFocusEvent.Gained)
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
            hasAudioFocus = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
            hasAudioFocus = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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
}
