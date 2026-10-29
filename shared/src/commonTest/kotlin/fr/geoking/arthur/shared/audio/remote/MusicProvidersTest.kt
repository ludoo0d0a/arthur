package fr.geoking.arthur.shared.audio.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class MusicProvidersTest {

    @Test
    fun factoryCreatesAllProvidersAndResolvesById() {
        val factory = MusicProviderFactory(
            jamendoClientId = "test-jamendo",
            freesoundApiKey = "test-freesound",
            pixabayApiKey = "test-pixabay",
        )
        val all = factory.createAll()
        assertEquals(5, all.size)

        val expectedIds = listOf("jamendo", "freesound", "archive_org", "pixabay_music", "incompetech")
        assertEquals(expectedIds, all.map { it.id })

        expectedIds.forEach { id ->
            val provider = factory.provider(id)
            assertNotNull(provider, "Provider for $id should not be null")
            assertEquals(id, provider.id)
        }
    }

    @Test
    fun jamendoSourceParsesTracks() = runBlocking {
        val json = """
            {
                "results": [
                    {
                        "id": "12345",
                        "name": "Deep Zen Meditation",
                        "artist_name": "Calm Waves",
                        "audio": "https://prod-1.storage.jamendo.com/12345.mp3",
                        "duration": 240,
                        "license_ccurl": "http://creativecommons.org/licenses/by/4.0/",
                        "image": "https://usercontent.jamendo.com/12345.jpg"
                    }
                ]
            }
        """.trimIndent()

        val source = JamendoMusicSource(
            httpGet = { json },
            clientId = "test-client",
        )
        val tracks = source.fetchZenTracks().getOrNull()
        assertNotNull(tracks)
        assertEquals(1, tracks.size)

        val track = tracks[0]
        assertEquals("jamendo-12345", track.id)
        assertEquals("Deep Zen Meditation", track.title)
        assertEquals("Calm Waves", track.artist)
        assertEquals("https://prod-1.storage.jamendo.com/12345.mp3", track.streamUrl)
        assertEquals(240, track.durationSeconds)
        assertEquals("jamendo", track.sourceId)
    }

    @Test
    fun freesoundSourceParsesSounds() = runBlocking {
        val json = """
            {
                "results": [
                    {
                        "id": 8888,
                        "name": "Tibetan Singing Bowl",
                        "username": "zenmaster",
                        "duration": 45.5,
                        "license": "http://creativecommons.org/publicdomain/zero/1.0/",
                        "previews": {
                            "preview-hq-mp3": "https://freesound.org/data/previews/88/8888.mp3"
                        }
                    }
                ]
            }
        """.trimIndent()

        val source = FreesoundMusicSource(
            httpGet = { json },
            apiKey = "test-key",
        )
        val tracks = source.fetchZenTracks().getOrNull()
        assertNotNull(tracks)
        assertEquals(1, tracks.size)

        val track = tracks[0]
        assertEquals("freesound-8888", track.id)
        assertEquals("Tibetan Singing Bowl", track.title)
        assertEquals("zenmaster", track.artist)
        assertEquals("https://freesound.org/data/previews/88/8888.mp3", track.streamUrl)
        assertEquals(45, track.durationSeconds)
        assertEquals("freesound", track.sourceId)
    }

    @Test
    fun archiveOrgSourceParsesDocs() = runBlocking {
        val searchJson = """
            {
                "response": {
                    "docs": [
                        {
                            "identifier": "zen_ambient_session",
                            "title": "Zen Ambient Session",
                            "creator": "Netlabel Ambient",
                            "licenseurl": "http://creativecommons.org/licenses/by/3.0/"
                        }
                    ]
                }
            }
        """.trimIndent()
        val metaJson = """
            {
                "files": [
                    {"name": "zen_ambient_session_meta.xml"},
                    {"name": "session_part_a.mp3"}
                ]
            }
        """.trimIndent()

        val source = ArchiveOrgMusicSource(
            httpGet = { url ->
                if ("metadata" in url) metaJson else searchJson
            },
        )
        val tracks = source.fetchZenTracks().getOrNull()
        assertNotNull(tracks)
        assertEquals(1, tracks.size)

        val track = tracks[0]
        assertEquals("archive_org-zen_ambient_session", track.id)
        assertEquals("Zen Ambient Session", track.title)
        assertEquals("Netlabel Ambient", track.artist)
        assertEquals(
            "https://archive.org/download/zen_ambient_session/session_part_a.mp3",
            track.streamUrl,
        )
        assertEquals("archive_org", track.sourceId)
    }

    @Test
    fun pixabayMusicSourceParsesHits() = runBlocking {
        val json = """
            {
                "hits": [
                    {
                        "id": 999,
                        "title": "Gentle Yoga Breeze",
                        "user": "PeacefulMind",
                        "audio": "https://cdn.pixabay.com/audio/999.mp3",
                        "duration": 180
                    }
                ]
            }
        """.trimIndent()

        val source = PixabayMusicSource(
            httpGet = { json },
            apiKey = "test-key",
        )
        val tracks = source.fetchZenTracks().getOrNull()
        assertNotNull(tracks)
        assertEquals(1, tracks.size)

        val track = tracks[0]
        assertEquals("pixabay_music-999", track.id)
        assertEquals("Gentle Yoga Breeze", track.title)
        assertEquals("PeacefulMind", track.artist)
        assertEquals("https://cdn.pixabay.com/audio/999.mp3", track.streamUrl)
        assertEquals(180, track.durationSeconds)
        assertEquals("pixabay_music", track.sourceId)
    }

    @Test
    fun incompetechSourceReturnsCuratedTracks() = runBlocking {
        val source = IncompetechMusicSource()
        val tracks = source.fetchZenTracks().getOrNull()
        assertNotNull(tracks)
        assertTrue(tracks.isNotEmpty())

        val first = tracks[0]
        assertEquals("incompetech-meditation-impromptu-01", first.id)
        assertEquals("Meditation Impromptu 01", first.title)
        assertEquals("Kevin MacLeod", first.artist)
        assertEquals("incompetech", first.sourceId)
        assertTrue(first.streamUrl.endsWith(".mp3"))
    }

    @Test
    fun jamendoFailsWhenClientIdMissing() = runBlocking {
        val source = JamendoMusicSource(httpGet = { "{}" }, clientId = "")
        val result = source.fetchZenTracks()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("JAMENDO_CLIENT_ID") == true)
    }

    @Test
    fun factoryReportsMissingApiKeys() {
        val factory = MusicProviderFactory()
        val missing = factory.missingApiKeys()
        assertTrue(missing.contains("JAMENDO_CLIENT_ID"))
        assertTrue(missing.contains("FREESOUND_API_KEY"))
        assertTrue(missing.contains("PIXABAY_API_KEY"))
        assertEquals(3, missing.size)
    }
}
