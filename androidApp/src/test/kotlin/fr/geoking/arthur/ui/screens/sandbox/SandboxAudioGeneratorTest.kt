package fr.geoking.arthur.ui.screens.sandbox

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.shared.audio.NeuralEngineType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SandboxAudioGeneratorTest {

    private lateinit var context: Context
    private lateinit var downloader: NeuralModelDownloader
    private lateinit var generator: SandboxAudioGenerator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        downloader = NeuralModelDownloader(context)
        generator = SandboxAudioGenerator(context, downloader)
    }

    @Test
    fun customLlm_generatesAudibleWavWithoutDownload() = runBlocking {
        val model = SandboxModelList.first { it.id == "custom_llm" }
        val result = generator.generate(
            model = model,
            format = MusicOutputFormat.PROCEDURAL_DSP,
            promptText = "Cyberpunk Synthwave 120 BPM",
            durationSeconds = 5,
        )
        assertTrue(result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.length() > 44)
        assertTrue(
            AudioCacheManager.wavPeakAbsolute(file) >= AudioCacheManager.MIN_AUDIBLE_PEAK,
        )
    }

    @Test
    fun musicGen_withoutModel_fails() = runBlocking {
        val model = SandboxModelList.first { it.engineType == NeuralEngineType.MUSICGEN_SMALL }
        val result = generator.generate(
            model = model,
            format = MusicOutputFormat.FULL_TRACK,
            promptText = "Soft piano",
            durationSeconds = 5,
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun musicGen_withFakeWeights_generatesAudibleWav() = runBlocking {
        val file = downloader.getModelFile(NeuralEngineType.MUSICGEN_SMALL)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(2048) { i -> (i * 17).toByte() })
        val model = SandboxModelList.first { it.engineType == NeuralEngineType.MUSICGEN_SMALL }
        val result = generator.generate(
            model = model,
            format = MusicOutputFormat.FULL_TRACK,
            promptText = "Lush Baroque String Quartet Adagio",
            durationSeconds = 5,
        )
        assertTrue(result.isSuccess)
        assertTrue(
            AudioCacheManager.wavPeakAbsolute(result.getOrThrow()) >=
                AudioCacheManager.MIN_AUDIBLE_PEAK,
        )
    }
}
