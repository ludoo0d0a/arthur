package fr.geoking.arthur.ui.components.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.debug.DebugQueryItem
import fr.geoking.arthur.source.HttpCacheController
import fr.geoking.tools.debugbar.DebugLogOverlay
import fr.geoking.tools.debugbar.model.HostDataConsumption
import fr.geoking.tools.debugbar.model.NetworkLog

/**
 * Arthur wiring for [DebugLogOverlay]: maps [DebugLogger] traffic + [HttpCacheController]
 * into the shared `fr.geoking.tools:debug-bar` UI.
 */
@Composable
fun ArthurDebugLogOverlay(
    debugLogger: DebugLogger,
    cacheController: HttpCacheController,
    modifier: Modifier = Modifier,
) {
    val stats by debugLogger.stats.collectAsState()
    val cacheDisabled by cacheController.disabled.collectAsState()

    val mappedLogs = remember(stats.recentQueries) {
        stats.recentQueries.map { it.toDebugBarLog() }
    }
    val hostConsumption = remember(mappedLogs) {
        mappedLogs.groupBy { it.host }
            .filterKeys { it.isNotBlank() }
            .mapValues { (host, logs) ->
                HostDataConsumption(
                    host = host,
                    bytesSent = logs.sumOf { it.requestSizeBytes },
                    bytesReceived = logs.sumOf { it.responseSizeBytes },
                    requestCount = logs.size,
                )
            }
    }
    val totalBytesSent = remember(hostConsumption) {
        hostConsumption.values.sumOf { it.bytesSent }
    }
    val totalBytesReceived = remember(hostConsumption) {
        hostConsumption.values.sumOf { it.bytesReceived }
    }

    DebugLogOverlay(
        logs = mappedLogs,
        providerTraces = emptyList(),
        hostConsumption = hostConsumption,
        totalBytesSent = totalBytesSent,
        totalBytesReceived = totalBytesReceived,
        disableCache = cacheDisabled,
        onDisableCacheChange = cacheController::setDisabled,
        onClearCaches = cacheController::clear,
        onClearLogs = debugLogger::clear,
        onResetDataConsumption = { /* derived from logs; clear logs to reset */ },
        modifier = modifier,
    )
}

private fun DebugQueryItem.toDebugBarLog(): NetworkLog =
    NetworkLog(
        id = id.toString(),
        url = url,
        host = host,
        method = method,
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        responseHeaders = responseHeaders.ifEmpty { null },
        responseBody = responseBody,
        statusCode = statusCode,
        durationMs = durationMs,
        timestamp = timestamp,
        requestSizeBytes = requestSizeBytes,
        responseSizeBytes = responseSizeBytes,
    )
