package fr.geoking.arthur.source

import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.UnsplashSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Live e2e against Pexels / Unsplash: load catalog, download one sample image each,
 * write under `androidApp/build/e2e-samples/stock/` for manual inspection.
 *
 * Skips when API keys are blank. Opt-in so default CI does not hit rate limits:
 * `./gradlew :androidApp:testDebugUnitTest -Pe2eStockPhotos=true --tests '*StockPhotoSourceE2eTest'`
 */
class StockPhotoSourceE2eTest {

    private lateinit var client: HttpClient
    private lateinit var samplesDir: File

    @Before
    fun setUp() {
        assumeTrue(
            "Pass -Pe2eStockPhotos=true (or E2E_STOCK_PHOTOS=1) to run live stock-photo e2e",
            e2eEnabled(),
        )
        client = HttpClient(OkHttp)
        samplesDir = File("build/e2e-samples/stock").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
    }

    @Test
    fun pexels_loadsCatalogAndDownloadsSampleImage() = runBlocking {
        val apiKey = BuildConfig.PEXELS_API_KEY
        assumeTrue("PEXELS_API_KEY missing in local.properties / CI secrets", apiKey.isNotBlank())

        val source = PexelsSource(
            apiKey = apiKey,
            limit = 2,
            httpGet = { url ->
                client.get(url) {
                    header(HttpHeaders.Authorization, apiKey)
                }.bodyAsText()
            },
        )
        val art = source.load()
        assertTrue("Pexels catalog empty", art.isNotEmpty())
        val sample = art.first()
        assertTrue("Pexels remoteUrl missing for ${sample.id}", !sample.remoteUrl.isNullOrBlank())

        val bytes = client.get(sample.remoteUrl!!).bodyAsBytes()
        assertImageBytes(bytes, label = "Pexels ${sample.id}")
        val out = writeSample("${sample.id}.jpg", bytes)
        println("Pexels sample → ${out.absolutePath} (${bytes.size} bytes)")
    }

    @Test
    fun unsplash_loadsCatalogAndDownloadsSampleImage() = runBlocking {
        val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
        assumeTrue(
            "UNSPLASH_ACCESS_KEY missing in local.properties / CI secrets",
            accessKey.isNotBlank(),
        )

        val source = UnsplashSource(
            accessKey = accessKey,
            limit = 2,
            httpGet = { url ->
                client.get(url) {
                    header(HttpHeaders.Authorization, "Client-ID $accessKey")
                    header("Accept-Version", "v1")
                }.bodyAsText()
            },
        )
        val art = source.load()
        assertTrue("Unsplash catalog empty", art.isNotEmpty())
        val sample = art.first()
        assertTrue("Unsplash remoteUrl missing for ${sample.id}", !sample.remoteUrl.isNullOrBlank())

        val bytes = client.get(sample.remoteUrl!!).bodyAsBytes()
        assertImageBytes(bytes, label = "Unsplash ${sample.id}")
        val out = writeSample("${sample.id}.jpg", bytes)
        println("Unsplash sample → ${out.absolutePath} (${bytes.size} bytes)")
    }

    private fun writeSample(name: String, bytes: ByteArray): File {
        val file = File(samplesDir, name)
        file.writeBytes(bytes)
        return file
    }

    private fun assertImageBytes(bytes: ByteArray, label: String) {
        assertTrue("$label: empty download", bytes.size >= MIN_IMAGE_BYTES)
        val isJpeg = bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        val isPng = bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isWebp = bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte()
        assertTrue("$label: not a JPEG/PNG/WebP (size=${bytes.size})", isJpeg || isPng || isWebp)
    }

    companion object {
        private const val MIN_IMAGE_BYTES = 8_000

        private fun e2eEnabled(): Boolean {
            val prop = System.getProperty("e2e.stockPhotos").orEmpty()
            if (prop.equals("true", ignoreCase = true) || prop == "1") return true
            val env = System.getenv("E2E_STOCK_PHOTOS").orEmpty()
            return env.equals("true", ignoreCase = true) || env == "1"
        }
    }
}
