package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class WikimediaStreetArtSourceTest {
    @Test
    fun loadsOpenLicensedStreetArtFromFixtures() = runBlocking {
        val fixtures = mapOf(
            WikimediaStreetArtSource.searchUrl() to """
                {
                  "query": {
                    "pages": [
                      {
                        "pageid": 100,
                        "title": "File:Mural in Berlin.jpg",
                        "imageinfo": [
                          {
                            "mime": "image/jpeg",
                            "thumburl": "https://example.com/thumb.jpg",
                            "url": "https://example.com/orig.jpg",
                            "extmetadata": {
                              "LicenseShortName": { "value": "CC BY 4.0" },
                              "Artist": {
                                "value": "<a href='https://example.com'>Ada Artist</a> from Berlin"
                              }
                            }
                          }
                        ]
                      },
                      {
                        "pageid": 101,
                        "title": "File:All Rights.jpg",
                        "imageinfo": [
                          {
                            "mime": "image/jpeg",
                            "thumburl": "https://example.com/arr.jpg",
                            "extmetadata": {
                              "LicenseShortName": { "value": "All rights reserved" },
                              "Artist": { "value": "Someone" }
                            }
                          }
                        ]
                      },
                      {
                        "pageid": 102,
                        "title": "File:NC Photo.jpg",
                        "imageinfo": [
                          {
                            "mime": "image/jpeg",
                            "thumburl": "https://example.com/nc.jpg",
                            "extmetadata": {
                              "LicenseShortName": { "value": "CC BY-NC 2.0" },
                              "Artist": { "value": "Someone" }
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )
        val source = WikimediaStreetArtSource(
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("wikimedia-streetart-100", art[0].id)
        assertEquals("Mural in Berlin", art[0].title)
        assertEquals("Ada Artist from Berlin / Wikimedia Commons", art[0].attribution)
        assertEquals("https://example.com/thumb.jpg", art[0].remoteUrl)
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(WikimediaStreetArtSource.ID, art[0].sourceId)
    }

    @Test
    fun skipsMissingImageAndDisallowedMime() = runBlocking {
        val fixtures = mapOf(
            WikimediaStreetArtSource.searchUrl() to """
                {
                  "query": {
                    "pages": [
                      {
                        "pageid": 1,
                        "title": "File:No Image.svg",
                        "imageinfo": [
                          {
                            "mime": "image/svg+xml",
                            "url": "https://example.com/a.svg",
                            "extmetadata": {
                              "LicenseShortName": { "value": "CC0" }
                            }
                          }
                        ]
                      },
                      {
                        "pageid": 2,
                        "title": "File:Public Domain Wall.png",
                        "imageinfo": [
                          {
                            "mime": "image/png",
                            "url": "https://example.com/pd.png",
                            "extmetadata": {
                              "LicenseShortName": { "value": "Public domain" },
                              "Artist": { "value": "Unknown" }
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )
        val source = WikimediaStreetArtSource(
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("wikimedia-streetart-2", art[0].id)
        assertEquals("https://example.com/pd.png", art[0].remoteUrl)
    }

    @Test
    fun searchUrlUsesStreetArtCategoryAndThumbWidth() {
        assertEquals(
            "https://commons.wikimedia.org/w/api.php" +
                "?action=query" +
                "&generator=categorymembers" +
                "&gcmtitle=Category:Street_art" +
                "&gcmtype=file" +
                "&gcmlimit=60" +
                "&prop=imageinfo" +
                "&iiprop=url|extmetadata|mime" +
                "&iiurlwidth=1600" +
                "&format=json" +
                "&formatversion=2",
            WikimediaStreetArtSource.searchUrl(),
        )
    }

    @Test
    fun openLicenseHelpers() {
        assertTrue(WikimediaStreetArtSource.isOpenLicense("CC BY 2.0"))
        assertTrue(WikimediaStreetArtSource.isOpenLicense("CC BY-SA 4.0"))
        assertTrue(WikimediaStreetArtSource.isOpenLicense("Public domain"))
        assertFalse(WikimediaStreetArtSource.isOpenLicense("CC BY-NC 2.0"))
        assertFalse(WikimediaStreetArtSource.isOpenLicense("CC BY-ND 3.0"))
        assertFalse(WikimediaStreetArtSource.isOpenLicense(null))
        assertEquals("Bolt On", WikimediaStreetArtSource.cleanTitle("File:Bolt On.jpg"))
        assertNull(WikimediaStreetArtSource.stripHtml("  "))
        assertEquals(
            "Ada from Berlin",
            WikimediaStreetArtSource.stripHtml("<b>Ada</b> from Berlin"),
        )
    }
}
