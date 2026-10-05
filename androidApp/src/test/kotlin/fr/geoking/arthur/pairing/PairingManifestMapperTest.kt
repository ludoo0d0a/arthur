package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.pairing.PairingCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingManifestMapperTest {
    @Test
    fun toArtworks_preservesRotationOrder() {
        val artworks = listOf(
            Artwork(
                id = "b",
                title = "B",
                sourceId = "bundled",
                kind = ArtworkKind.Painting,
                remoteUrl = "https://example.com/b.jpg",
            ),
            Artwork(
                id = "a",
                title = "A",
                sourceId = "bundled",
                kind = ArtworkKind.Photo,
                remoteUrl = "https://example.com/a.jpg",
            ),
        )
        val decoded = PairingCodec.decodeManifest(PairingCodec.encodeManifest(artworks))
        val mapped = PairingManifestMapper.toArtworks(decoded)
        assertEquals(listOf("b", "a"), mapped.map { it.id })
        assertEquals("https://example.com/b.jpg", mapped.first().remoteUrl)
    }
}

class LanPairingServerInfoTest {
    @Test
    fun info_and_manifestFlow_updateOnPush() {
        val server = LanPairingServer(port = 18743, deviceName = "Test TV")
        server.start()
        try {
            val client = LanPairingClient(host = "127.0.0.1", port = 18743)
            assertTrue(client.health())
            val info = client.info()
            assertTrue(info!!.contains("arthur-pairing"))
            assertTrue(info.contains("Test TV"))
            val json = PairingCodec.encodeManifest(
                listOf(
                    Artwork(
                        id = "flow",
                        title = "Flow",
                        sourceId = "bundled",
                        kind = ArtworkKind.Photo,
                        remoteUrl = "https://example.com/f.jpg",
                    ),
                ),
            )
            assertTrue(client.pushManifest(json))
            assertEquals("flow", PairingCodec.decodeManifest(server.latestManifestJson.value!!).rotationIds.single())
            client.close()
        } finally {
            server.stop()
        }
    }
}
