package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCamera
import fr.geoking.arthur.shared.domain.SpeedCameraSource
import fr.geoking.arthur.shared.pairing.PairingCodec
import fr.geoking.arthur.shared.source.SpeedCameraRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
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

    @Test
    fun servesRadarsApiOverLocalhost() = runBlocking {
        val fakeSource = object : SpeedCameraSource {
            override val id: String = "test"
            override val displayName: String = "Test Source"
            override suspend fun getSpeedCamerasByCountry(countryCode: String): List<SpeedCamera> {
                return if (countryCode == "BE") {
                    listOf(
                        SpeedCamera(
                            id = "be-1",
                            latitude = 50.85,
                            longitude = 4.35,
                            countryCode = "BE",
                            maxSpeed = 120,
                        )
                    )
                } else emptyList()
            }

            override suspend fun getSpeedCamerasByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera> {
                return listOf(
                    SpeedCamera(
                        id = "bbox-1",
                        latitude = 50.0,
                        longitude = 4.0,
                        countryCode = "BE",
                        maxSpeed = 90,
                    )
                )
            }
        }

        val repository = SpeedCameraRepository(primarySource = fakeSource)
        val server = LanPairingServer(port = 18743, speedCameraRepository = repository)
        server.start()
        val client = HttpClient(OkHttp)
        try {
            val responseCountry = client.get("http://127.0.0.1:18743/api/radars?country=BE").bodyAsText()
            assertTrue(responseCountry.contains("be-1"))
            assertTrue(responseCountry.contains("120"))

            val responseBbox = client.get("http://127.0.0.1:18743/api/radars?bbox=49.0,3.0,51.0,5.0").bodyAsText()
            assertTrue(responseBbox.contains("bbox-1"))
            assertTrue(responseBbox.contains("90"))
        } finally {
            client.close()
            server.stop()
        }
    }
}
