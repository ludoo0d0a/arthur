package fr.geoking.arthur.pairing

import android.net.Uri

/**
 * Canvas pairing deep links — custom scheme only.
 * Example: `arthur://pair?host=192.168.1.20&port=8742`
 */
object PairingDeepLink {
    const val SCHEME = "arthur"
    const val HOST_PAIR = "pair"
    const val PARAM_HOST = "host"
    const val PARAM_PORT = "port"

    data class Target(
        val host: String,
        val port: Int = LanPairingServer.DEFAULT_PORT,
    )

    fun build(host: String, port: Int = LanPairingServer.DEFAULT_PORT): String {
        val trimmed = host.trim()
        require(trimmed.isNotEmpty()) { "host required" }
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_PAIR)
            .appendQueryParameter(PARAM_HOST, trimmed)
            .appendQueryParameter(PARAM_PORT, port.toString())
            .build()
            .toString()
    }

    fun parse(uri: Uri): Target? {
        if (!wantsPairing(uri)) return null
        val host = uri.getQueryParameter(PARAM_HOST)?.trim().orEmpty()
        if (host.isEmpty()) return null
        val port = uri.getQueryParameter(PARAM_PORT)?.toIntOrNull()
            ?: LanPairingServer.DEFAULT_PORT
        if (port !in 1..65535) return null
        return Target(host = host, port = port)
    }

    fun parse(raw: String): Target? {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return null
        return parse(uri)
    }

    fun wantsPairing(uri: Uri): Boolean {
        if (uri.scheme.equals(SCHEME, ignoreCase = true) &&
            (uri.host.equals(HOST_PAIR, ignoreCase = true) ||
                uri.host.equals("pairing", ignoreCase = true))
        ) {
            return true
        }
        val path = uri.path.orEmpty()
        return path.contains("/pair") || path.contains("/pairing")
    }
}
