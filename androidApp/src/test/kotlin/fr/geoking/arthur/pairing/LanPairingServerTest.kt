package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.pairing.PairingCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanPairingServerTest {
    @Test
    fun pushManifest_overLocalhost() {
        val server = LanPairingServer(port = 18742)
        server.start()
        try {
            val client = LanPairingClient(host = "127.0.0.1", port = 18742)
            assertTrue(client.health())
            val json = PairingCodec.encodeManifest(
                listOf(
                    Artwork(
                        id = "x",
                        title = "X",
                        sourceId = "bundled",
                        kind = ArtworkKind.Photo,
                        remoteUrl = "https://example.com/x.jpg",
                    ),
                ),
            )
            assertTrue(client.pushManifest(json))
            assertEquals("x", PairingCodec.decodeManifest(server.latestManifest()!!).rotationIds.single())
            client.close()
        } finally {
            server.stop()
        }
    }
}
