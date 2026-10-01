package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCamera
import fr.geoking.arthur.shared.pairing.PairingCodec
import fr.geoking.arthur.shared.source.SpeedCameraRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking

/** TV-side LAN pairing host (Ktor CIO). */
class LanPairingServer(
    private val port: Int = DEFAULT_PORT,
    private val speedCameraRepository: SpeedCameraRepository? = null,
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val manifest = AtomicReference<String?>(null)
    private val engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
        routing {
            get("/health") { call.respondText("ok") }
            get("/manifest") {
                val body = manifest.get()
                if (body == null) call.respond(HttpStatusCode.NoContent)
                else call.respondText(body, ContentType.Application.Json)
            }
            post("/manifest") {
                val raw = call.receiveText()
                PairingCodec.decodeManifest(raw)
                manifest.set(raw)
                call.respond(HttpStatusCode.Accepted, "accepted")
            }
            get("/api/radars") {
                val repository = speedCameraRepository
                if (repository == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "SpeedCameraRepository unavailable")
                    return@get
                }
                val country = call.request.queryParameters["country"]
                val bboxParam = call.request.queryParameters["bbox"]

                val cameras: List<SpeedCamera> = when {
                    !country.isNullOrBlank() -> {
                        repository.getFixedRadarsByCountry(country)
                    }
                    !bboxParam.isNullOrBlank() -> {
                        val parts = bboxParam.split(",").mapNotNull { it.toDoubleOrNull() }
                        if (parts.size == 4) {
                            val bbox = BoundingBox(parts[0], parts[1], parts[2], parts[3])
                            repository.getFixedRadarsByBoundingBox(bbox)
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }

                val responseJson = json.encodeToString(cameras)
                call.respondText(responseJson, ContentType.Application.Json)
            }
        }
    }

    fun start() {
        engine.start(wait = false)
    }

    fun stop() {
        engine.stop(500L, 1000L)
    }

    fun latestManifest(): String? = manifest.get()

    companion object {
        const val DEFAULT_PORT = 8742
    }
}

class LanPairingClient(
    private val host: String,
    private val port: Int = LanPairingServer.DEFAULT_PORT,
) {
    private val http = HttpClient(OkHttp)

    fun health(): Boolean = runBlocking {
        try {
            http.get("http://$host:$port/health").status == HttpStatusCode.OK
        } catch (_: Exception) {
            false
        }
    }

    fun pushManifest(json: String): Boolean = runBlocking {
        try {
            val status = http.post("http://$host:$port/manifest") {
                contentType(ContentType.Application.Json)
                setBody(json)
            }.status
            status == HttpStatusCode.Accepted
        } catch (_: Exception) {
            false
        }
    }

    fun close() {
        http.close()
    }
}
