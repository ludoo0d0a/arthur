package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.pairing.PairingCodec
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/** TV-side LAN pairing host (Ktor CIO). */
class LanPairingServer(
    private val port: Int = DEFAULT_PORT,
    private val deviceName: String = "Arthur TV",
) {
    private val manifest = AtomicReference<String?>(null)
    private val _latestManifestJson = MutableStateFlow<String?>(null)
    val latestManifestJson: StateFlow<String?> = _latestManifestJson.asStateFlow()

    private val engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
        routing {
            get("/health") { call.respondText("ok") }
            get("/info") {
                call.respondText(
                    """{"service":"arthur-pairing","port":$port,"deviceName":${jsonString(deviceName)}}""",
                    ContentType.Application.Json,
                )
            }
            get("/manifest") {
                val body = manifest.get()
                if (body == null) call.respond(HttpStatusCode.NoContent)
                else call.respondText(body, ContentType.Application.Json)
            }
            post("/manifest") {
                val raw = call.receiveText()
                PairingCodec.decodeManifest(raw)
                manifest.set(raw)
                _latestManifestJson.value = raw
                call.respond(HttpStatusCode.Accepted, "accepted")
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

    fun clearManifest() {
        manifest.set(null)
        _latestManifestJson.value = null
    }

    companion object {
        const val DEFAULT_PORT = 8742

        private fun jsonString(value: String): String =
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
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

    fun info(): String? = runBlocking {
        try {
            http.get("http://$host:$port/info").bodyAsText()
        } catch (_: Exception) {
            null
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
