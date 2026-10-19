package fr.geoking.tools.debugbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.geoking.tools.debugbar.model.CacheStats
import fr.geoking.tools.debugbar.model.HostDataConsumption
import fr.geoking.tools.debugbar.model.NetworkLog

@Composable
fun DebugLogOverlay(
    logs: List<NetworkLog>,
    providerTraces: List<Any>,
    hostConsumption: Map<String, HostDataConsumption>,
    totalBytesSent: Long,
    totalBytesReceived: Long,
    disableCache: Boolean,
    onDisableCacheChange: (Boolean) -> Unit,
    onClearCaches: () -> Unit,
    onClearLogs: () -> Unit,
    onResetDataConsumption: () -> Unit,
    cacheStats: CacheStats,
    onRefreshCacheStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
}
