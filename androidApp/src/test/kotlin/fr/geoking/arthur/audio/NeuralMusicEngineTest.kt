package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.audio.engines.ModelDownloadState
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.audio.engines.NeuralMusicPlayer
import fr.geoking.arthur.audio.engines.SoundGenEngine
import fr.geoking.arthur.audio.engines.MusicGenSmallEngine
import fr.geoking.arthur.audio.engines.StableAudio3Engine
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.source.AmbientAudioSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

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

    @Test
    fun promptCatalog_containsTenPromptsPerPackAndValidDurations() {
        val soundGenPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.ATMOSPHERES_NATURE)
        val musicGenPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.MUSEUMS_CLASSICAL)
        val stableAudioPrompts = NeuralPromptCatalog.promptsForPack(NeuralAudioPack.GENART_DIGITAL)

        assertEquals(10, soundGenPrompts.size)
        assertEquals(10, musicGenPrompts.size)
        assertEquals(10, stableAudioPrompts.size)
        assertEquals(30, NeuralPromptCatalog.allPrompts.size)

        for (prompt in NeuralPromptCatalog.allPrompts) {
            assertTrue("Duration exceeds 120s limit: ${prompt.targetDurationSeconds}", prompt.targetDurationSeconds <= 120)
            assertTrue("Prompt should not contain vocals: ${prompt.promptText}", !prompt.promptText.lowercase().contains("vocals") || prompt.promptText.lowercase().contains("no vocals"))
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

        assertTrue(sgUrl.contains("soundgen"))
        assertTrue(mgUrl.contains("musicgen"))
        assertTrue(saUrl.contains("stable-audio"))
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
    fun audioCacheManager_generatesFallbackSampleWavFile() = runBlocking {
        val prompt = NeuralPromptCatalog.soundGenPrompts.first()
        val seed = 12345L
        val wavFile = cacheManager.generateFallbackSampleWav(prompt, seed, durationSeconds = 10)

        assertTrue(wavFile.exists())
        assertTrue("WAV file should not be empty", wavFile.length() > 44) // Header size is 44 bytes
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
    fun neuralMusicPlayer_fallsBackToProceduralEngineWhenModelNotReady() = runBlocking {
        val pack = NeuralAudioPack.ATMOSPHERES_NATURE
        player.playPackTrack(pack, promptIndex = 0, randomSeed = 9999L)

        assertNotNull(player.playbackState.value)
    }
}
