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
import fr.geoking.arthur.audio.voices.SoftPianoVoice
import fr.geoking.arthur.audio.voices.SoftPulseVoice
import fr.geoking.arthur.audio.voices.WaveNoiseVoice
import fr.geoking.arthur.audio.voices.WindTextureVoice
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
import kotlin.math.pow
import kotlin.random.Random

/**
 * Multi-track procedural ambient engine: Markov melody/harmony + style voices.
 * Preset is unique per artwork; performance walks randomly inside that preset.
 */
class ProceduralMusicEngine(
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

    @Volatile private var masterVolume: Float = audioSettings.volume.value
    @Volatile private var audioMode: AmbientAudioMode = audioSettings.mode.value
    @Volatile private var isEnabled: Boolean = audioSettings.enabled.value

    @Volatile private var preset: MusicPreset? = null
    @Volatile private var pendingPreset: MusicPreset? = null
    @Volatile private var crossfadeSamples = 0
    @Volatile private var crossfadeTotal = 0
    @Volatile private var triggerTransitionFlag = false

    private val sessionSalt = System.nanoTime()

    private var sequencer: MarkovSequencer? = null
    private var form: ArrangementForm? = null

    // Voices (reused across presets)
    private val padA = SinePadVoice(6f)
    private val padB = SinePadVoice(12f)
    private val piano = SoftPianoVoice()
    private val guitar = PluckGuitarVoice()
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
    ).coerceAtLeast(2048)

    init {
        settingsJob = scope.launch {
            combine(audioSettings.enabled, audioSettings.volume, audioSettings.mode) { enabled, vol, mode ->
                Triple(enabled, vol, mode)
            }.collect { (enabled, vol, mode) ->
                isEnabled = enabled
                masterVolume = vol
                audioMode = mode
                if (!enabled && isRunning.get()) stop()
            }
        }
    }

    /** Apply artwork-derived preset with crossfade; safe when stopped. */
    fun setArtwork(artwork: Artwork) {
        val next = MusicPresetResolver.resolve(artwork)
        if (!isRunning.get()) {
            preset = next
            sequencer = MarkovSequencer(next, sessionSalt)
            form = ArrangementForm(next.formSeed)
            return
        }
        pendingPreset = next
        crossfadeTotal = (2.0f * sampleRate).toInt()
        crossfadeSamples = crossfadeTotal
    }

    /** Fire a style-appropriate transition cue (slide advance). */
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
            // Neutral zen bed until first artwork arrives.
            val fallback = MusicPresetResolver.resolve(
                Artwork(id = "ambient.default", title = "Ambient", sourceId = "ambient", kind = fr.geoking.arthur.shared.domain.ArtworkKind.Genart),
            )
            preset = fallback
            sequencer = MarkovSequencer(fallback, sessionSalt)
            form = ArrangementForm(fallback.formSeed)
        }
        initAudioTrack()
        audioTrack?.play()
        synthesisJob = scope.launch(Dispatchers.Default) { renderAudioLoop() }
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

    private suspend fun renderAudioLoop() {
        val chunk = 1024
        val pcm = ShortArray(chunk)
        var sampleIndex = 0L
        var pulseClock = 0
        var outgoingSequencer: MarkovSequencer? = null
        var outgoingPreset: MusicPreset? = null

        while (scope.isActive && isRunning.get()) {
            val track = audioTrack ?: break
            val playPad = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.PAD_ONLY
            val playEvents = audioMode == AmbientAudioMode.PAD_AND_CHIME || audioMode == AmbientAudioMode.CHIME_ONLY

            // Commit pending preset at start of crossfade.
            pendingPreset?.let { next ->
                if (crossfadeSamples == crossfadeTotal) {
                    outgoingPreset = preset
                    outgoingSequencer = sequencer
                    preset = next
                    sequencer = MarkovSequencer(next, sessionSalt xor sampleIndex)
                    form = ArrangementForm(next.formSeed)
                    applyBedVoices(next, sequencer!!)
                    pendingPreset = null
                }
            }

            val activePreset = preset ?: continue
            val seq = sequencer ?: continue
            val arrangement = form ?: continue
            applyBedVoices(activePreset, seq)

            for (i in 0 until chunk) {
                sampleIndex++
                arrangement.tick(sampleRate)
                seq.advanceHarmonyClock(sampleRate)

                if (playEvents && seq.tickMelody(sampleRate)) {
                    fireMelodyNote(activePreset, seq)
                    when (seq.nextOrnament()) {
                        OrnamentKind.Grace -> seq.currentMelodyHz?.let { chimes.noteOn(it * 1.5f, 0.35f) }
                        OrnamentKind.Roll -> seq.currentMelodyHz?.let { kalimba.noteOn(it, 0.4f) }
                        OrnamentKind.DoubleStrike -> seq.currentMelodyHz?.let {
                            piano.noteOn(it, 0.35f)
                            chimes.noteOn(it * 2f, 0.25f)
                        }
                        OrnamentKind.None -> Unit
                    }
                }

                // Soft pulse grid
                if (playEvents && activePreset.trackMix.pulse > 0.05f) {
                    val samplesPerPulse = (60.0 / activePreset.tempoBpm * sampleRate).toInt().coerceAtLeast(1)
                    pulseClock++
                    if (pulseClock >= samplesPerPulse) {
                        pulseClock = 0
                        if (Random(sampleIndex).nextFloat() < activePreset.density * 0.35f) {
                            pulse.noteOn(90f, 0.25f * activePreset.trackMix.pulse)
                        }
                    }
                }

                if (triggerTransitionFlag) {
                    triggerTransitionFlag = false
                    if (playEvents) fireTransition(activePreset, seq)
                }

                var bed = 0f
                var harmony = 0f
                var melody = 0f
                var texture = 0f
                var pulseS = 0f
                var trans = 0f

                if (playPad) {
                    bed = padA.render(sampleIndex, sampleRate) * activePreset.trackMix.bed
                    when (activePreset.style) {
                        MusicStyle.OceanWaves ->
                            bed += waves.render(sampleIndex, sampleRate) * activePreset.trackMix.bed * 0.8f
                        MusicStyle.TibetanBowl, MusicStyle.CosmicDrone ->
                            bed += bowl.render(sampleIndex, sampleRate) * activePreset.trackMix.bed * 0.6f
                        MusicStyle.WindChimes ->
                            bed += wind.render(sampleIndex, sampleRate) * 0.3f
                        else -> Unit
                    }
                    // Harmony as second detuned pad
                    padB.setChord(seq.harmonyPartialsHz())
                    harmony = padB.render(sampleIndex, sampleRate) * activePreset.trackMix.harmony
                }

                if (playEvents) {
                    melody = (
                        piano.render(sampleIndex, sampleRate) +
                            guitar.render(sampleIndex, sampleRate) +
                            kalimba.render(sampleIndex, sampleRate)
                        ) * activePreset.trackMix.melody * arrangement.melodyMul
                    texture = (
                        wind.render(sampleIndex, sampleRate) * 0.5f +
                            waves.render(sampleIndex, sampleRate) * 0.25f
                        ) * activePreset.trackMix.texture * arrangement.textureMul
                    pulseS = pulse.render(sampleIndex, sampleRate) * activePreset.trackMix.pulse
                    trans = (
                        chimes.render(sampleIndex, sampleRate) +
                            bowl.render(sampleIndex, sampleRate) * 0.35f
                        ) * activePreset.trackMix.transition
                }

                var mixed = bed + harmony + melody + texture + pulseS + trans

                // Crossfade from outgoing bed if active
                if (crossfadeSamples > 0 && outgoingPreset != null && playPad) {
                    val t = 1f - crossfadeSamples.toFloat() / crossfadeTotal.toFloat()
                    val outBed = padA.render(sampleIndex, sampleRate) * (1f - t) * 0.3f
                    mixed = mixed * t + outBed * (1f - t)
                    crossfadeSamples--
                    if (crossfadeSamples == 0) {
                        outgoingPreset = null
                        outgoingSequencer = null
                    }
                }

                mixed *= masterVolume
                pcm[i] = (mixed.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            track.write(pcm, 0, chunk)
        }
    }

    private fun applyBedVoices(p: MusicPreset, seq: MarkovSequencer) {
        val partials = seq.harmonyPartialsHz()
        padA.setChord(partials.ifEmpty { listOf(p.rootHz) })
        when (p.style) {
            MusicStyle.TibetanBowl, MusicStyle.CosmicDrone -> bowl.setSustain(p.rootHz, true)
            else -> bowl.setSustain(p.rootHz, false)
        }
    }

    private fun fireMelodyNote(p: MusicPreset, seq: MarkovSequencer) {
        val hz = seq.currentMelodyHz ?: return
        when (p.style) {
            MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.NightLounge ->
                piano.noteOn(hz, 0.45f)
            MusicStyle.SoftGuitar -> guitar.noteOn(hz, 0.5f)
            MusicStyle.AfricanPulse -> kalimba.noteOn(hz, 0.55f)
            MusicStyle.WindChimes -> chimes.noteOn(hz, 0.5f)
            MusicStyle.TibetanBowl -> bowl.noteOn(hz, 0.4f)
            MusicStyle.OceanWaves -> piano.noteOn(hz * 0.5f, 0.25f)
            MusicStyle.CosmicDrone -> chimes.noteOn(hz, 0.3f)
            MusicStyle.Zen -> {
                piano.noteOn(hz, 0.35f)
                if (Random(hz.toBits().toLong()).nextFloat() < 0.3f) {
                    chimes.noteOn(hz, 0.25f)
                }
            }
        }
    }

    private fun fireTransition(p: MusicPreset, seq: MarkovSequencer) {
        val cue = seq.transitionCue()
        val root = p.rootHz
        when (cue) {
            TextureCueKind.BowlStrike -> bowl.noteOn(root, 0.7f)
            TextureCueKind.ChimeCluster -> {
                val scale = p.scaleSemitones
                val freqs = (0 until 4).map { i ->
                    val semi = scale[Math.floorMod(i + p.melodyBankIndex, scale.size)]
                    root * 2.0.pow(semi / 12.0).toFloat()
                }
                chimes.noteCluster(freqs, 0.65f)
            }
            TextureCueKind.ArpeggioCascade -> {
                piano.noteOn(root, 0.4f)
                guitar.noteOn(root * 1.5f, 0.35f)
                kalimba.noteOn(root * 2f, 0.3f)
            }
            TextureCueKind.ThumbPianoRoll -> {
                kalimba.noteOn(root, 0.5f)
                kalimba.noteOn(root * 1.25f, 0.4f)
            }
            TextureCueKind.WaveSwell, TextureCueKind.SoftRain, TextureCueKind.WindGust -> {
                // Texture voices are continuous; add a soft chime accent.
                chimes.noteOn(root * 2f, 0.3f)
            }
            TextureCueKind.SilenceBreath -> Unit
        }
    }

    private fun requestFocus(): Boolean {
        if (audioManager == null) return false
        val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                -> stop()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    audioTrack?.setVolume(0.2f * masterVolume)
                AudioManager.AUDIOFOCUS_GAIN ->
                    audioTrack?.setVolume(1.0f * masterVolume)
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
