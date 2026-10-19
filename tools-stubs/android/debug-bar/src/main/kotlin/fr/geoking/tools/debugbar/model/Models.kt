package fr.geoking.tools.debugbar.model

data class CacheStatRow(
    val label: String = "",
    val sizeBytes: Long = 0L,
    val itemCount: Int = 0,
)

data class CacheStats(
    val totalSizeBytes: Long = 0L,
    val totalItemCount: Int = 0,
    val byType: List<CacheStatRow> = emptyList(),
    val byHost: List<CacheStatRow> = emptyList(),
)

data class HostDataConsumption(
    val host: String = "",
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val requestCount: Int = 0,
)

data class NetworkLog(
    val id: String = "",
    val url: String = "",
    val host: String = "",
    val method: String = "GET",
    val requestHeaders: Map<String, List<String>>? = null,
    val requestBody: String? = null,
    val responseHeaders: Map<String, List<String>>? = null,
    val responseBody: String? = null,
    val statusCode: Int? = null,
    val durationMs: Long = 0L,
    val timestamp: Long = 0L,
    val requestSizeBytes: Long = 0L,
    val responseSizeBytes: Long = 0L,
)
