package fr.geoking.arthur.source

import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MuseumSearchKind
import fr.geoking.arthur.shared.source.SmithsonianSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Live e2e against museum OpenAPI contracts: authenticate, search by category,
 * paginate, download one sample image each.
 *
 * Writes under `androidApp/build/e2e-samples/museum/` for manual inspection.
 *
 * Opt-in (default CI skips live calls):
 * `./gradlew :androidApp:testDebugUnitTest -Pe2eMuseumSources=true --tests '*MuseumSourceE2eTest'`
 */
class MuseumSourceE2eTest {

    private lateinit var client: HttpClient
    private lateinit var samplesDir: File

    @Before
    fun setUp() {
        assumeTrue(
            "Pass -Pe2eMuseumSources=true (or E2E_MUSEUM_SOURCES=1) to run live museum e2e",
            e2eEnabled(),
        )
        client = HttpClient(OkHttp)
        samplesDir = File("build/e2e-samples/museum").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
    }

    @Test
    fun europeana_auth_category_pagination_and_image() = runBlocking {
        val apiKey = BuildConfig.EUROPEANA_API_KEY
        assumeTrue("EUROPEANA_API_KEY missing", apiKey.isNotBlank())

        suspend fun httpGet(url: String): String =
            client.get(url) { header("X-Api-Key", apiKey) }.bodyAsText()

        val paintings = EuropeanaSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Painting },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Europeana painting catalog empty", paintings.isNotEmpty())
        assertEquals(ArtworkKind.Painting, paintings.first().kind)

        val sculptures = EuropeanaSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Sculpture },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Europeana sculpture catalog empty", sculptures.isNotEmpty())
        assertEquals(ArtworkKind.Sculpture, sculptures.first().kind)

        val page1Ids = parseEuropeanaIds(
            httpGet(EuropeanaSource.searchUrl(PAGE_SIZE, start = 1, apiKey = apiKey)),
        )
        val page2Ids = parseEuropeanaIds(
            httpGet(EuropeanaSource.searchUrl(PAGE_SIZE, start = 1 + PAGE_SIZE, apiKey = apiKey)),
        )
        assertTrue("Europeana page 1 empty", page1Ids.isNotEmpty())
        assertTrue("Europeana page 2 empty", page2Ids.isNotEmpty())
        assertNotEquals(
            "Europeana pagination should advance results",
            page1Ids.first(),
            page2Ids.first(),
        )

        downloadSample(paintings.first(), "europeana")
    }

    @Test
    fun harvard_auth_category_pagination_and_image() = runBlocking {
        val apiKey = BuildConfig.HARVARD_API_KEY
        assumeTrue("HARVARD_API_KEY missing", apiKey.isNotBlank())

        suspend fun httpGet(url: String): String = client.get(url).bodyAsText()

        val paintings = HarvardSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Painting },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Harvard painting catalog empty", paintings.isNotEmpty())
        assertEquals(ArtworkKind.Painting, paintings.first().kind)

        val sculptures = HarvardSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Sculpture },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Harvard sculpture catalog empty", sculptures.isNotEmpty())
        assertEquals(ArtworkKind.Sculpture, sculptures.first().kind)

        val page1Ids = parseHarvardIds(
            httpGet(HarvardSource.searchUrl(apiKey, PAGE_SIZE, page = 1)),
        )
        val page2Ids = parseHarvardIds(
            httpGet(HarvardSource.searchUrl(apiKey, PAGE_SIZE, page = 2)),
        )
        assertTrue("Harvard page 1 empty", page1Ids.isNotEmpty())
        assertTrue("Harvard page 2 empty", page2Ids.isNotEmpty())
        assertNotEquals(
            "Harvard pagination should advance results",
            page1Ids.first(),
            page2Ids.first(),
        )

        downloadSample(paintings.first(), "harvard")
    }

    @Test
    fun smithsonian_auth_category_pagination_and_image() = runBlocking {
        val apiKey = BuildConfig.SMITHSONIAN_API_KEY
        assumeTrue("SMITHSONIAN_API_KEY missing", apiKey.isNotBlank())

        suspend fun httpGet(url: String): String = client.get(url).bodyAsText()

        val paintings = SmithsonianSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Painting },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Smithsonian painting catalog empty", paintings.isNotEmpty())
        assertEquals(ArtworkKind.Painting, paintings.first().kind)

        val sculptures = SmithsonianSource(
            apiKey = apiKey,
            kind = { MuseumSearchKind.Sculpture },
            limit = PAGE_SIZE,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Smithsonian sculpture catalog empty", sculptures.isNotEmpty())
        assertEquals(ArtworkKind.Sculpture, sculptures.first().kind)

        val page1Ids = parseSmithsonianIds(
            httpGet(SmithsonianSource.searchUrl(apiKey, PAGE_SIZE, start = 0)),
        )
        val page2Ids = parseSmithsonianIds(
            httpGet(SmithsonianSource.searchUrl(apiKey, PAGE_SIZE, start = PAGE_SIZE)),
        )
        assertTrue("Smithsonian page 1 empty", page1Ids.isNotEmpty())
        assertTrue("Smithsonian page 2 empty", page2Ids.isNotEmpty())
        assertNotEquals(
            "Smithsonian pagination should advance results",
            page1Ids.first(),
            page2Ids.first(),
        )

        downloadSample(paintings.first(), "smithsonian")
    }

    @Test
    fun louvre_category_and_image() = runBlocking {
        suspend fun httpGet(url: String): String = client.get(url).bodyAsText()

        val paintings = LouvreSource(
            kind = { MuseumSearchKind.Painting },
            limit = 3,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Louvre painting catalog empty", paintings.isNotEmpty())
        assertEquals(ArtworkKind.Painting, paintings.first().kind)

        val sculptures = LouvreSource(
            kind = { MuseumSearchKind.Sculpture },
            limit = 3,
            httpGet = ::httpGet,
        ).load()
        assertTrue("Louvre sculpture catalog empty", sculptures.isNotEmpty())
        assertEquals(ArtworkKind.Sculpture, sculptures.first().kind)

        downloadSample(paintings.first(), "louvre")
    }

    private suspend fun downloadSample(sample: Artwork, prefix: String) {
        assertTrue("$prefix remoteUrl missing for ${sample.id}", !sample.remoteUrl.isNullOrBlank())
        val bytes = client.get(sample.remoteUrl!!).bodyAsBytes()
        assertImageBytes(bytes, label = "$prefix ${sample.id}")
        val safeName = sample.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(samplesDir, "$safeName.jpg").also { it.writeBytes(bytes) }
        println("$prefix sample → ${out.absolutePath} (${bytes.size} bytes) «${sample.title}»")
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
        private const val PAGE_SIZE = 5
        private const val MIN_IMAGE_BYTES = 4_000

        private fun e2eEnabled(): Boolean {
            val prop = System.getProperty("e2e.museumSources").orEmpty()
            if (prop.equals("true", ignoreCase = true) || prop == "1") return true
            val env = System.getenv("E2E_MUSEUM_SOURCES").orEmpty()
            return env.equals("true", ignoreCase = true) || env == "1"
        }

        /** Lightweight id extraction without depending on internal serializers. */
        private fun parseEuropeanaIds(json: String): List<String> =
            Regex(""""id"\s*:\s*"([^"]+)"""").findAll(json).map { it.groupValues[1] }.toList()

        private fun parseHarvardIds(json: String): List<String> =
            Regex(""""id"\s*:\s*(\d+)""").findAll(json).map { it.groupValues[1] }.toList()

        private fun parseSmithsonianIds(json: String): List<String> =
            Regex(""""url"\s*:\s*"(edanmdm:[^"]+)"""").findAll(json)
                .map { it.groupValues[1] }
                .toList()
                .ifEmpty {
                    Regex(""""id"\s*:\s*"([^"]+)"""").findAll(json)
                        .map { it.groupValues[1] }
                        .toList()
                }
    }
}
