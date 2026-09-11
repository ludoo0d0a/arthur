package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class DeviantArtSourceTest {
    @Test
    fun loadsArtworkFromDeviantArtFixtures() = runBlocking {
        val fixtures = mapOf(
            DeviantArtSource.tokenUrl("client-id", "client-secret") to """
                { "access_token": "token-abc" }
            """.trimIndent(),
            DeviantArtSource.browseUrl("nature", "token-abc", RemoteSample.SEARCH_POOL, 0) to """
                {
                  "results": [
                    {
                      "deviationid": "abc123",
                      "title": "Misty Forest",
                      "url": "https://www.deviantart.com/artist/art/misty-forest-abc123",
                      "author": { "username": "artist" },
                      "content": { "src": "https://images-wixmp.deviantart.com/abc123/full.jpg" },
                      "is_mature": false
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = DeviantArtSource(
            clientId = "client-id",
            clientSecret = "client-secret",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("deviantart-abc123", art[0].id)
        assertEquals("Misty Forest", art[0].title)
        assertEquals("artist / DeviantArt", art[0].attribution)
        assertEquals(
            "https://images-wixmp.deviantart.com/abc123/full.jpg",
            art[0].remoteUrl,
        )
        assertEquals(
            "https://www.deviantart.com/artist/art/misty-forest-abc123",
            art[0].externalUrl,
        )
        assertEquals(DeviantArtSource.ID, art[0].sourceId)
        assertEquals(fr.geoking.arthur.shared.domain.ArtworkKind.Photo, art[0].kind)
    }

    @Test
    fun fallbackToThumbsWhenContentAndPreviewAreMissing() = runBlocking {
        val fixtures = mapOf(
            DeviantArtSource.tokenUrl("client-id", "client-secret") to """
                { "access_token": "token-abc" }
            """.trimIndent(),
            DeviantArtSource.browseUrl("nature", "token-abc", RemoteSample.SEARCH_POOL, 0) to """
                {
                  "results": [
                    {
                      "deviationid": "thumb123",
                      "title": "Thumb Only",
                      "thumbs": [
                        { "src": "https://images-wixmp.deviantart.com/thumb1.jpg" }
                      ],
                      "is_mature": false
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = DeviantArtSource(
            clientId = "client-id",
            clientSecret = "client-secret",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("https://images-wixmp.deviantart.com/thumb1.jpg", art[0].remoteUrl)
    }

    @Test
    fun blankCredentialsLogAuthenticationError() = runBlocking {
        val errorLogger = fr.geoking.arthur.shared.error.ErrorLogger { 0L }
        val source = DeviantArtSource(
            clientId = "",
            clientSecret = "secret",
            httpGet = { error("should not call") },
            errorLogger = errorLogger,
        )
        source.load()
        val logs = errorLogger.errors.value
        assertEquals(1, logs.size)
        assertEquals(fr.geoking.arthur.shared.error.ErrorCategory.Authentication, logs[0].category)
        assertEquals(DeviantArtSource.ID, logs[0].sourceId)
    }

    @Test
    fun blankCredentialsUseOfflineFallback() = runBlocking {
        var called = false
        val cached = listOf(
            fr.geoking.arthur.shared.domain.Artwork(
                id = "deviantart-1",
                title = "Cached",
                sourceId = DeviantArtSource.ID,
                kind = fr.geoking.arthur.shared.domain.ArtworkKind.Painting,
                localPath = "/tmp/cached.jpg",
            ),
        )
        val source = DeviantArtSource(
            clientId = " ",
            clientSecret = "client-secret",
            offlineFallback = { cached },
            httpGet = {
                called = true
                error("should not call")
            },
        )
        assertEquals(cached, source.load())
        assertEquals(false, called)
    }

    @Test
    fun skipsMatureContent() = runBlocking {
        val fixtures = mapOf(
            DeviantArtSource.tokenUrl("client-id", "client-secret") to """
                { "access_token": "token-abc" }
            """.trimIndent(),
            DeviantArtSource.browseUrl("nature", "token-abc", RemoteSample.SEARCH_POOL, 0) to """
                {
                  "results": [
                    {
                      "deviationid": "mature1",
                      "title": "Mature piece",
                      "content": { "src": "https://images-wixmp.deviantart.com/mature1/full.jpg" },
                      "is_mature": true
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = DeviantArtSource(
            clientId = "client-id",
            clientSecret = "client-secret",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        assertEquals(emptyList(), source.load())
    }
}
