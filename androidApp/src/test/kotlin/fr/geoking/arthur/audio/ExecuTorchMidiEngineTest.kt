package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.audio.engines.ExecuTorchMidiBridge
import fr.geoking.arthur.audio.engines.ExecuTorchMidiEngine
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.audio.engines.NeuralMusicPlayer
import fr.geoking.arthur.shared.audio.ExecuTorchMidiPresets
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

@RunWith(RobolectricTestRunner::class)
class ExecuTorchMidiEngineTest {

    private lateinit var context: Context
    private lateinit var downloader: NeuralModelDownloader
    private lateinit var cacheManager: AudioCacheManager
    private lateinit var engine: ExecuTorchMidiEngine
    private lateinit var audioSettings: AmbientAudioSettings
    private lateinit var proceduralEngine: ProceduralMusicEngine
    private lateinit var player: NeuralMusicPlayer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        downloader = NeuralModelDownloader(context)
        cacheManager = AudioCacheManager(context)
        engine = ExecuTorchMidiEngine(downloader, cacheManager)
        audioSettings = AmbientAudioSettings(context)
        proceduralEngine = ProceduralMusicEngine(context, audioSettings)
        player = NeuralMusicPlayer(context, downloader, cacheManager, proceduralEngine)
    }

    @Test
    fun engine_hasCorrectTypeAndPack() {
        assertEquals(NeuralEngineType.EXECUTORCH_MIDI, engine.engineType)
        assertEquals(NeuralAudioPack.EXECUTORCH_MIDI, engine.pack)
    }

    @Test
    fun presets_areDefinedCorrectly() {
        val presets = ExecuTorchMidiPresets.allPresets
        assertTrue("Presets should not be empty", presets.isNotEmpty())
        assertEquals(4, presets.size)

        val piano = ExecuTorchMidiPresets.AMBIENT_PIANO
        assertEquals("ambient_piano", piano.id)
        assertEquals("ambient_piano.sf2", piano.soundFontFileName)
        assertTrue(piano.expectedSizeBytes > 0)
    }

    @Test
    fun bridge_handlesNoteEventsAndSynthesis() {
        val bridge = ExecuTorchMidiBridge()
        val initialized = bridge.init(null, null)
        assertTrue(initialized)

        bridge.sendMidiNoteOn(channel = 0, key = 60, velocity = 80)
        bridge.sendMidiNoteOff(channel = 0, key = 60)

        val buffer = ShortArray(256)
        val renderedFrames = bridge.renderPcmShorts(buffer, 128)
        assertEquals(128, renderedFrames)

        val tokens = bridge.generateMidiTokens(floatArrayOf(1.0f), 16)
        assertNotNull(tokens)
        assertEquals(16, tokens?.size)

        bridge.release()
    }

    @Test
    fun generateTrack_createsValidAudioFile() = runBlocking {
        val prompt = NeuralPromptCatalog.executorchPrompts.first()
        val seed = 42L
        val trackFile = engine.generateTrack(prompt, seed, durationSeconds = 5)

        assertNotNull(trackFile)
        assertTrue("Generated track file should exist", trackFile?.exists() == true)
        assertTrue("WAV track file should have size > 44 bytes header", (trackFile?.length() ?: 0) > 44)
    }

    @Test
    fun player_routesExecuTorchMidiPackCorrectly() {
        val packEngine = player.getEngineForPack(NeuralAudioPack.EXECUTORCH_MIDI)
        assertEquals(NeuralEngineType.EXECUTORCH_MIDI, packEngine.engineType)
    }
}
