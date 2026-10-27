package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.audio.engines.ModelDownloadState
import fr.geoking.arthur.audio.engines.MusicGenSmallEngine
import fr.geoking.arthur.audio.engines.MusicPlaybackState
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.audio.engines.NeuralMusicPlayer
import fr.geoking.arthur.audio.engines.SoundGenEngine
import fr.geoking.arthur.audio.engines.StableAudio3Engine
import fr.geoking.arthur.audio.engines.llm.LocalNeuralInference
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.source.AmbientAudioSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeuralMusicEngineTest {

    private lateinit var context: Context
    private lateinit var downloader: NeuralModelDownloader
    private lateinit var cacheManager: AudioCacheManager
    private lateinit var audioSettings: AmbientAudioSettings
    private lateinit var proceduralEngine: ProceduralMusicEngine
    private lateinit var player: NeuralMusicPlayer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        downloader = NeuralModelDownloader(context)
        cacheManager = AudioCacheManager(context)
        audioSettings = AmbientAudioSettings(context)
        proceduralEngine = ProceduralMusicEngine(context, audioSettings)
        player = NeuralMusicPlayer(context, downloader, cacheManager, proceduralEngine)
    }

    private fun writeFakeModel(engine: NeuralEngineType, size: Int = 2048) {
        val file = downloader.getModelFile(engine)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(size) { i -> (i * 37 + 11).toByte() })
    }

    @Test
    fun promptCatalog_containsTenPromptsPerPackAndValidDurations() {
        val soundGenPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.ATMOSPHERES_NATURE)
        val musicGenPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.MUSEUMS_CLASSICAL)
        val stableAudioPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.GENART_DIGITAL)
        val executorchPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.EXECUTORCH_MIDI)

        assertEquals(10, soundGenPrompts.size)
        assertEquals(10, musicGenPrompts.size)
        assertEquals(10, stableAudioPrompts.size)
        assertEquals(4, executorchPrompts.size)
        assertEquals(34, NeuralPromptCatalog.allPrompts.size)

        for (prompt in NeuralPromptCatalog.allPrompts) {
            assertTrue(
                "Duration exceeds 120s limit: ${prompt.targetDurationSeconds}",
                prompt.targetDurationSeconds <= 120,
            )
            assertTrue(
                "Prompt should not contain vocals: ${prompt.promptText}",
                !prompt.promptText.lowercase().contains("vocals") ||
                    prompt.promptText.lowercase().contains("no vocals"),
            )
        }
    }

    @Test
    fun modelDownloader_resolvesModelFilePathsCorrectly() {
        val soundGenFile = downloader.getModelFile(NeuralEngineType.SOUNDGEN)
        val musicGenFile = downloader.getModelFile(NeuralEngineType.MUSICGEN_SMALL)
        val stableAudioFile = downloader.getModelFile(NeuralEngineType.STABLE_AUDIO_3_SMALL)

        assertEquals("soundgen_small.bin", soundGenFile.name)
        assertEquals("musicgen_small.bin", musicGenFile.name)
        assertEquals("stable_audio_3_small.bin", stableAudioFile.name)
    }

    @Test
    fun modelDownloader_providesDefaultUrlsForEngines() {
        val sgUrl = downloader.getDefaultDownloadUrl(NeuralEngineType.SOUNDGEN)
        val mgUrl = downloader.getDefaultDownloadUrl(NeuralEngineType.MUSICGEN_SMALL)
        val saUrl = downloader.getDefaultDownloadUrl(NeuralEngineType.STABLE_AUDIO_3_SMALL)
        val etUrl = downloader.getDefaultDownloadUrl(NeuralEngineType.EXECUTORCH_MIDI)

        assertTrue(sgUrl.contains("audiogen") || sgUrl.contains("soundgen"))
        assertTrue(mgUrl.contains("musicgen"))
        assertTrue(saUrl.contains("stable-audio"))
        assertTrue(etUrl.contains("FluidR3") || etUrl.contains("urish/cinto"))
        assertFalse(etUrl.contains("geoking/arthur-executorch-midi"))
    }

    @Test
    fun modelDownloader_managesDeletionAndSizes() {
        val engine = NeuralEngineType.SOUNDGEN
        val file = downloader.getModelFile(engine)
        file.writeBytes(ByteArray(100))

        assertTrue(downloader.isModelAvailable(engine))
        assertEquals(100L, downloader.getModelSizeBytes(engine))

        val deleted = downloader.deleteModel(engine)
        assertTrue(deleted)
        assertEquals(false, downloader.isModelAvailable(engine))
        assertEquals(0L, downloader.getModelSizeBytes(engine))
        assertEquals(ModelDownloadState.Idle, downloader.downloadStates.value[engine])
    }

    @Test
    fun soundGenEngine_returnsNullWhenModelMissing() = runBlocking {
        val engine = SoundGenEngine(downloader, cacheManager)
        val prompt = NeuralPromptCatalog.soundGenPrompts.first()
        assertFalse(engine.isModelReady())
        assertNull(engine.generateTrack(prompt, randomSeed = 1L, durationSeconds = 5))
    }

    @Test
    fun soundGenEngine_llmGenerateTrack_isAudibleWhenModelPresent() = runBlocking {
        writeFakeModel(NeuralEngineType.SOUNDGEN)
        val engine = SoundGenEngine(downloader, cacheManager)
        val prompt = NeuralPromptCatalog.soundGenPrompts.first()
        assertTrue(engine.isModelReady())
        val file = engine.generateTrack(prompt, randomSeed = 99L, durationSeconds = 5)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(
            "SoundGen LLM output must be audible",
            AudioCacheManager.wavPeakAbsolute(file) >= AudioCacheManager.MIN_AUDIBLE_PEAK,
        )
    }

    @Test
    fun musicGenAndStableAudio_llmGenerateDistinctTracks() = runBlocking {
        writeFakeModel(NeuralEngineType.MUSICGEN_SMALL)
        writeFakeModel(NeuralEngineType.STABLE_AUDIO_3_SMALL)
        val music = MusicGenSmallEngine(downloader, cacheManager)
            .generateTrack(NeuralPromptCatalog.musicGenPrompts.first(), 7L, 5)
        val stable = StableAudio3Engine(downloader, cacheManager)
            .generateTrack(NeuralPromptCatalog.stableAudioPrompts.first(), 7L, 5)
        assertNotNull(music)
        assertNotNull(stable)
        assertTrue(AudioCacheManager.wavPeakAbsolute(music!!) >= AudioCacheManager.MIN_AUDIBLE_PEAK)
        assertTrue(AudioCacheManager.wavPeakAbsolute(stable!!) >= AudioCacheManager.MIN_AUDIBLE_PEAK)
        // Different engines must not emit identical PCM (prompt-conditioned decode differs).
        assertFalse(music.readBytes().contentEquals(stable.readBytes()))
    }

    @Test
    fun localNeuralInference_rejectsTinyModelFile() = runBlocking {
        val tiny = downloader.getModelFile(NeuralEngineType.SOUNDGEN)
        tiny.parentFile?.mkdirs()
        tiny.writeBytes(ByteArray(32))
        val inference = LocalNeuralInference(cacheManager)
        val out = inference.generate(
            NeuralEngineType.SOUNDGEN,
            tiny,
            NeuralPromptCatalog.soundGenPrompts.first(),
            seed = 1L,
            durationSeconds = 5,
        )
        assertNull(out)
    }

    @Test
    fun neuralMusicPlayer_mapsPacksToCorrespondingEngines() {
        val sgEngine = player.getEngineForPack(NeuralAudioPack.ATMOSPHERES_NATURE)
        val mgEngine = player.getEngineForPack(NeuralAudioPack.MUSEUMS_CLASSICAL)
        val saEngine = player.getEngineForPack(NeuralAudioPack.GENART_DIGITAL)

        assertEquals(NeuralEngineType.SOUNDGEN, sgEngine.engineType)
        assertEquals(NeuralEngineType.MUSICGEN_SMALL, mgEngine.engineType)
        assertEquals(NeuralEngineType.STABLE_AUDIO_3_SMALL, saEngine.engineType)
    }

    @Test
    fun neuralMusicPlayer_failsWhenGenerationUnavailable() = runBlocking {
        val pack = NeuralAudioPack.ATMOSPHERES_NATURE
        player.playPackTrack(pack, promptIndex = 0, randomSeed = 9999L)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        delay(50)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(MusicPlaybackState.Failed, player.playbackState.value)
    }
}
