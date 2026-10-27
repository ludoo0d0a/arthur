package fr.geoking.arthur.audio.engines

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import fr.geoking.arthur.audio.ProceduralMusicEngine
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class MusicPlaybackState {
    data object Stopped : MusicPlaybackState()
    data object Failed : MusicPlaybackState()
    data class Playing(
        val pack: NeuralAudioPack,
        val prompt: NeuralPromptDef,
    ) : MusicPlaybackState()
}

/**
 * Controller orchestrating local neural AI music playback.
 * Generation failures surface as [MusicPlaybackState.Failed] — no procedural/sample substitute.
 */
class NeuralMusicPlayer(
    private val context: Context,
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
    private val proceduralFallbackEngine: ProceduralMusicEngine,
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val soundGenEngine = SoundGenEngine(downloader, cacheManager)
    val musicGenSmallEngine = MusicGenSmallEngine(downloader, cacheManager)
    val stableAudio3Engine = StableAudio3Engine(downloader, cacheManager)
    val execuTorchMidiEngine = ExecuTorchMidiEngine(downloader, cacheManager)

    private val engines: Map<NeuralAudioPack, NeuralMusicEngine> = mapOf(
        NeuralAudioPack.ATMOSPHERES_NATURE to soundGenEngine,
        NeuralAudioPack.MUSEUMS_CLASSICAL to musicGenSmallEngine,
        NeuralAudioPack.GENART_DIGITAL to stableAudio3Engine,
        NeuralAudioPack.EXECUTORCH_MIDI to execuTorchMidiEngine,
    )

    private var activePlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow<MusicPlaybackState>(MusicPlaybackState.Stopped)
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    fun getEngineForPack(pack: NeuralAudioPack): NeuralMusicEngine = engines.getValue(pack)

    /**
     * Plays a generated track for the given pack.
     * On missing model / failed generation, sets [MusicPlaybackState.Failed] (no substitute audio).
     */
    fun playPackTrack(
        pack: NeuralAudioPack,
        promptIndex: Int = 0,
        randomSeed: Long = System.currentTimeMillis(),
        crossfadeMs: Long = 1000L,
    ) {
        scope.launch {
            val prompts = NeuralPromptCatalog.promptsForPack(pack)
            val selectedPrompt = prompts.getOrElse(promptIndex % prompts.size) { prompts.first() }
            val engine = getEngineForPack(pack)

            val trackFile = engine.generateTrack(selectedPrompt, randomSeed, durationSeconds = 120)
            if (trackFile != null && trackFile.exists()) {
                playAudioFileWithCrossfade(trackFile, pack, selectedPrompt, crossfadeMs = crossfadeMs)
            } else {
                stopActivePlayer()
                _playbackState.value = MusicPlaybackState.Failed
            }
        }
    }

    private suspend fun playAudioFileWithCrossfade(
        audioFile: File,
        pack: NeuralAudioPack,
        prompt: NeuralPromptDef,
        crossfadeMs: Long,
    ) = withContext(Dispatchers.Main) {
        try {
            proceduralFallbackEngine.stop(abandonFocus = false)

            val newMp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(audioFile.absolutePath)
                isLooping = true
                setVolume(0f, 0f)
                prepare()
                start()
            }

            val oldMp = activePlayer
            activePlayer = newMp

            val steps = 20
            val stepDelay = (crossfadeMs / steps).coerceAtLeast(10L)

            for (i in 1..steps) {
                val fadeInVol = i.toFloat() / steps
                val fadeOutVol = 1.0f - fadeInVol

                newMp.setVolume(fadeInVol, fadeInVol)
                oldMp?.setVolume(fadeOutVol, fadeOutVol)
                delay(stepDelay)
            }

            oldMp?.stop()
            oldMp?.release()

            _playbackState.value = MusicPlaybackState.Playing(pack, prompt)
        } catch (_: Exception) {
            stopActivePlayer()
            _playbackState.value = MusicPlaybackState.Failed
        }
    }

    private fun stopActivePlayer() {
        activePlayer?.stop()
        activePlayer?.release()
        activePlayer = null
    }

    fun stop() {
        scope.launch(Dispatchers.Main) {
            stopActivePlayer()
            proceduralFallbackEngine.stop()
            _playbackState.value = MusicPlaybackState.Stopped
        }
    }
}
