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
import kotlinx.coroutines.cancel
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

    // Voices (reused across presets)
    private val padA = SinePadVoice(2.5f, glideSeconds = 0.06f)
    private val padB = SinePadVoice(3f, glideSeconds = 0.06f)
    private val piano = SoftPianoPool(4)
    private val guitar = PluckGuitarVoice()
    private val bass = SoftBassVoice()
    private val bowl = BowlVoice()
    private val waves = WaveNoiseVoice(11L)
    private val wind = WindTextureVoice(22L)
    private val pulse = SoftPulseVoice()
    private val chimes = ChimeClusterVoice()
    private val kalimba = KalimbaPluckVoice()

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(4096)

    private enum class DuckPhase { Idle, Down, Hold, Up }

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

    private suspend fun renderAudioLoop() {
        val chunk = 2048
        val pcm = ShortArray(chunk)
        var sampleIndex = 0L
        var pulseClock = 0
        var crossfading = false

        while (scope.isActive && isRunning.get()) {
            val track = audioTrack ?: break
            val char = character
            val atmosphere = char == AmbientAudioCharacter.Atmosphere
            val playEvents = true

            pendingPreset?.let { next ->
                if (crossfadeSamples == crossfadeTotal) {
                    // Soft swap: release old voices (they decay), fade new mix in over crossfade window.
                    releaseLeadVoices()
                    crossfading = true
                    preset = next
                    sequencer = MarkovSequencer(next, sessionSalt xor sampleIndex, char)
                    form = ArrangementForm(next.formSeed)
                    lastChordKey = Int.MIN_VALUE
                    applyBedVoices(next, sequencer!!, atmosphere)
                    pendingPreset = null
                }
            }

            val activePreset = preset ?: continue
            val seq = sequencer ?: continue
            val arrangement = form ?: continue

            val glide = if (atmosphere) 0.22f else 0.06f
            padA.setGlideSeconds(glide)
            padB.setGlideSeconds(glide)

            val mix = activePreset.trackMix
            val melodyGain = mix.melody
            val bassGain = mix.bass
            val bedGain = mix.bed
            val harmonyGain = mix.harmony
            val textureGain = mix.texture
            val pulseGain = mix.pulse
            val transitionGain = mix.transition

            // Mute pad contribution for Melody character via near-zero bed/harmony already in mix.
            val padAudible = bedGain > 0.02f || harmonyGain > 0.02f || atmosphere

            for (i in 0 until chunk) {
                sampleIndex++
                arrangement.tick(sampleRate)
                seq.advanceHarmonyClock(sampleRate)

                if (seq.harmonyChanged) {
                    applyBedVoices(activePreset, seq, atmosphere)
                }

                if (playEvents && seq.tickMelody(sampleRate)) {
                    fireMelodyNote(activePreset, seq, atmosphere)
                    when (seq.nextOrnament()) {
                        OrnamentKind.Grace -> seq.currentMelodyHz?.let { chimes.noteOn(it * 1.5f, 0.28f) }
                        OrnamentKind.Roll -> seq.currentMelodyHz?.let { kalimba.noteOn(it, 0.35f) }
                        OrnamentKind.DoubleStrike -> seq.currentMelodyHz?.let {
                            piano.noteOn(it, 0.30f)
                            chimes.noteOn(it * 2f, 0.20f)
                        }
                        OrnamentKind.None -> Unit
                    }
                }

                if (bassGain > 0.02f && seq.tickBass(sampleRate)) {
                    seq.currentBassFrequencyHz?.let { hz ->
                        val vel = if (atmosphere) 0.35f else 0.55f
                        bass.noteOn(hz, vel * bassGain.coerceAtMost(1f))
                    }
                }

                if (playEvents && pulseGain > 0.05f) {
                    val samplesPerPulse = (60.0 / activePreset.tempoBpm * sampleRate).toInt().coerceAtLeast(1)
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

                // Fire soft transition cue at bottom of duck.
                if (pendingTransitionCue && duckPhase == DuckPhase.Hold && duckSamples == duckTotal) {
                    pendingTransitionCue = false
                    if (playEvents) fireTransition(activePreset, seq)
                }

                var bed = 0f
                var harmony = 0f
                var melody = 0f
                var bassS = 0f
                var texture = 0f
                var pulseS = 0f
                var trans = 0f

                val root = activePreset.rootHz
                waves.setRootHz(root)
                wind.setRootHz(root)

                if (padAudible) {
                    if (bedGain > 0.02f) {
                        bed = padA.render(sampleIndex, sampleRate) * bedGain
                    } else {
                        padA.render(sampleIndex, sampleRate)
                    }
                    if (harmonyGain > 0.02f) {
                        padB.setChord(seq.harmonyPartialsHz())
                        harmony = padB.render(sampleIndex, sampleRate) * harmonyGain
                    } else {
                        padB.setChord(seq.harmonyPartialsHz())
                        padB.render(sampleIndex, sampleRate)
                    }
                } else {
                    padA.render(sampleIndex, sampleRate)
                    padB.setChord(seq.harmonyPartialsHz())
                    padB.render(sampleIndex, sampleRate)
                }

                if (bassGain > 0.02f) {
                    bassS = bass.render(sampleIndex, sampleRate) * bassGain
                }

                if (playEvents) {
                    if (melodyGain > 0.02f) {
                        melody = (
                            piano.render(sampleIndex, sampleRate) +
                                guitar.render(sampleIndex, sampleRate) +
                                kalimba.render(sampleIndex, sampleRate)
                            ) * melodyGain * arrangement.melodyMul
                    }

                    // Single texture layer — never double-count waves in bed + texture.
                    texture = if (textureGain > 0.02f) {
                        when (activePreset.style) {
                            MusicStyle.OceanWaves ->
                                waves.render(sampleIndex, sampleRate) *
                                    textureGain * arrangement.textureMul
                            MusicStyle.WindChimes ->
                                wind.render(sampleIndex, sampleRate) *
                                    textureGain * arrangement.textureMul
                            MusicStyle.TibetanBowl, MusicStyle.CosmicDrone ->
                                if (atmosphere) {
                                    wind.render(sampleIndex, sampleRate) *
                                        textureGain * arrangement.textureMul * 0.45f
                                } else {
                                    0f
                                }
                            else -> if (atmosphere) {
                                (
                                    wind.render(sampleIndex, sampleRate) * 0.35f +
                                        waves.render(sampleIndex, sampleRate) * 0.12f
                                    ) * textureGain * arrangement.textureMul
                            } else {
                                0f
                            }
                        }
                    } else {
                        0f
                    }

                    // Bowl sustain is part of bed for Atmosphere bowl/drone styles (not texture).
                    if (atmosphere &&
                        activePreset.style in setOf(MusicStyle.TibetanBowl, MusicStyle.CosmicDrone) &&
                        bedGain > 0.02f
                    ) {
                        bed += bowl.render(sampleIndex, sampleRate) * bedGain * 0.45f
                    }

                    if (pulseGain > 0.02f) {
                        pulseS = pulse.render(sampleIndex, sampleRate) * pulseGain
                    }
                    if (transitionGain > 0.02f) {
                        trans = (
                            chimes.render(sampleIndex, sampleRate) +
                                bowl.render(sampleIndex, sampleRate) * 0.25f
                            ) * transitionGain
                    } else {
                        // Still advance chime/bowl state lightly if transition muted.
                        chimes.render(sampleIndex, sampleRate)
                    }
                }

                var mixed = bed + harmony + melody + bassS + texture + pulseS + trans

                // Artwork crossfade: releasing old voices + cosine fade-in of new mix.
                if (crossfading && crossfadeSamples > 0) {
                    val t = 1f - crossfadeSamples.toFloat() / crossfadeTotal.toFloat()
                    val fadeIn = (0.5f - 0.5f * kotlin.math.cos(Math.PI.toFloat() * t)).coerceIn(0f, 1f)
                    mixed *= fadeIn
                    crossfadeSamples--
                    if (crossfadeSamples == 0) {
                        crossfading = false
                    }
                }

                advanceDuck()
                mixed *= duckGain()

                mixed = softLimit(mixed * masterVolume)
                pcm[i] = (mixed.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            val written = try {
                track.write(pcm, 0, chunk)
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
        }
    }

    private fun applyBedVoices(p: MusicPreset, seq: MarkovSequencer, atmosphere: Boolean) {
        val partials = seq.harmonyPartialsHz()
        val key = partials.fold(0) { acc, f -> acc * 31 + f.toBits() }
        if (key == lastChordKey && cachedPartials.isNotEmpty()) {
            // Still refresh bowl sustain path below if needed.
        } else {
            lastChordKey = key
            cachedPartials = partials
            padA.setChord(partials.ifEmpty { listOf(p.rootHz) })
            padB.setChord(partials.ifEmpty { listOf(p.rootHz) })
        }
        val sustainBowl = atmosphere &&
            p.style in setOf(MusicStyle.TibetanBowl, MusicStyle.CosmicDrone)
        if (sustainBowl) {
            bowl.setSustainPartials(partials.ifEmpty { listOf(p.rootHz) }, true)
        } else {
            bowl.setSustain(p.rootHz, false)
        }
    }

    private fun fireMelodyNote(p: MusicPreset, seq: MarkovSequencer, atmosphere: Boolean) {
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
                if (!atmosphere) chimes.noteOn(hz, vel * 0.5f)
            }
            MusicStyle.Zen -> {
                piano.noteOn(hz, vel)
                if (!atmosphere && Random(hz.toBits().toLong()).nextFloat() < 0.15f) {
                    chimes.noteOn(hz, 0.18f)
                }
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
                    root * 2.0.pow((semi + 12) / 12.0).toFloat()
                }
                chimes.noteCluster(freqs, 0.40f)
            }
            TextureCueKind.ArpeggioCascade -> {
                piano.noteOn(root * 2f, 0.35f)
                piano.noteOn(root * 2.5f, 0.28f)
            }
            TextureCueKind.ThumbPianoRoll -> {
                kalimba.noteOn(root * 2f, 0.4f)
                kalimba.noteOn(root * 2.5f, 0.32f)
            }
            TextureCueKind.WaveSwell, TextureCueKind.SoftRain, TextureCueKind.WindGust -> {
                chimes.noteOn(root * 2f, 0.22f)
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
