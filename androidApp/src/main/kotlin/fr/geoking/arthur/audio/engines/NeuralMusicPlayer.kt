package fr.geoking.arthur.audio.engines

import android.content.Context
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
    object Stopped : MusicPlaybackState()
    data class Playing(val pack: NeuralAudioPack, val prompt: NeuralPromptDef, val isFallbackProcedural: Boolean) : MusicPlaybackState()
}

/**
 * Controller orchestrating local neural AI music playback, crossfading,
 * and seamless fallback to procedural music when model weights are unavailable or downloading.
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

    private val engines: Map<NeuralAudioPack, NeuralMusicEngine> = mapOf(
        NeuralAudioPack.ATMOSPHERES_NATURE to soundGenEngine,
        NeuralAudioPack.MUSEUMS_CLASSICAL to musicGenSmallEngine,
        NeuralAudioPack.GENART_DIGITAL to stableAudio3Engine,
    )

    private var activePlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow<MusicPlaybackState>(MusicPlaybackState.Stopped)
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    fun getEngineForPack(pack: NeuralAudioPack): NeuralMusicEngine = engines.getValue(pack)

    /**
     * Plays a 2-minute track variation for the given pack.
     * Uses the neural engine if ready, or falls back seamlessly to procedural ambient music.
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

            if (engine.isModelReady()) {
                val trackFile = engine.generateTrack(selectedPrompt, randomSeed, durationSeconds = 120)
                if (trackFile != null && trackFile.exists()) {
                    playAudioFileWithCrossfade(trackFile, pack, selectedPrompt, isProcedural = false, crossfadeMs = crossfadeMs)
                } else {
                    playProceduralFallback(pack, selectedPrompt)
                }
            } else {
                val sampleFile = engine.generateTrack(selectedPrompt, randomSeed, durationSeconds = 120)
                if (sampleFile != null && sampleFile.exists()) {
                    playAudioFileWithCrossfade(sampleFile, pack, selectedPrompt, isProcedural = false, crossfadeMs = crossfadeMs)
                } else {
                    playProceduralFallback(pack, selectedPrompt)
                }
            }
        }
    }

    private suspend fun playAudioFileWithCrossfade(
        audioFile: File,
        pack: NeuralAudioPack,
        prompt: NeuralPromptDef,
        isProcedural: Boolean,
        crossfadeMs: Long,
    ) = withContext(Dispatchers.Main) {
        try {
            proceduralFallbackEngine.stop(abandonFocus = false)

            val newMp = MediaPlayer().apply {
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

            _playbackState.value = MusicPlaybackState.Playing(pack, prompt, isProcedural)
        } catch (e: Exception) {
            playProceduralFallback(pack, prompt)
        }
    }

    private fun playProceduralFallback(pack: NeuralAudioPack, prompt: NeuralPromptDef) {
        activePlayer?.stop()
        activePlayer?.release()
        activePlayer = null

        proceduralFallbackEngine.start()
        _playbackState.value = MusicPlaybackState.Playing(pack, prompt, isFallbackProcedural = true)
    }

    fun stop() {
        scope.launch(Dispatchers.Main) {
            activePlayer?.stop()
            activePlayer?.release()
            activePlayer = null
            proceduralFallbackEngine.stop()
            _playbackState.value = MusicPlaybackState.Stopped
        }
    }
}
