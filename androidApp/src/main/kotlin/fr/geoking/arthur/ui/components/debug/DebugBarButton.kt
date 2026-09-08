package fr.geoking.arthur.ui.components.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.debug.DebugQueryItem
import fr.geoking.arthur.source.HttpCacheController

/**
 * Floating bug-report button that expands into a big overlay window showing all HTTP traffic:
 * counts, per-request cache hit/miss + timing, domain filter chips, and a details drill-down
 * (headers, payload, response, searchable JSON tree). Toggle "Full page" to take over the screen.
 */
@Composable
fun DebugBarButton(
    debugLogger: DebugLogger,
    cacheController: HttpCacheController,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFullPage by remember { mutableStateOf(false) }

    if (!isExpanded) {
        FloatingActionButton(
            onClick = { isExpanded = true },
            containerColor = Color(0xFF334155).copy(alpha = 0.85f),
            contentColor = Color.White,
            modifier = modifier.size(48.dp).testTag("debug_bar_button"),
        ) {
            Text("🐛", fontSize = 20.sp)
        }
    }

    if (isExpanded) {
        Popup(
            onDismissRequest = { isExpanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (!isFullPage) isExpanded = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val cardModifier = if (isFullPage) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
                }
                Surface(
                    modifier = cardModifier
                        .testTag("debug_overlay")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume: don't dismiss when tapping inside */ },
                        ),
                    color = Color(0xEE0F172A),
                    shape = if (isFullPage) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                ) {
                    DebugOverlayContent(
                        debugLogger = debugLogger,
                        cacheController = cacheController,
                        isFullPage = isFullPage,
                        onToggleFullPage = { isFullPage = !isFullPage },
                        onClose = { isExpanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugOverlayContent(
    debugLogger: DebugLogger,
    cacheController: HttpCacheController,
    isFullPage: Boolean,
    onToggleFullPage: () -> Unit,
    onClose: () -> Unit,
) {
    val stats by debugLogger.stats.collectAsState()
    val cacheDisabled by cacheController.disabled.collectAsState()
    var selectedHost by remember { mutableStateOf<String?>(null) }
    var selectedLog by remember { mutableStateOf<DebugQueryItem?>(null) }

    val hosts = remember(stats.recentQueries) {
        stats.recentQueries.map { it.host }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredQueries = remember(stats.recentQueries, selectedHost) {
        val host = selectedHost
        if (host == null) stats.recentQueries else stats.recentQueries.filter { it.host == host }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "🐛 Network (${stats.recentQueries.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "Hit ${stats.cacheHits} · Miss ${stats.cacheMisses} · Active ${stats.activeQueries}" +
                        if (stats.lastLoadDurationMs > 0) " · Load ${stats.lastLoadDurationMs}ms" else "",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeaderChip(
                    label = if (cacheDisabled) "Cache: OFF" else "Cache: ON",
                    active = cacheDisabled,
                    onClick = { cacheController.setDisabled(!cacheDisabled) },
                )
                IconButton(onClick = { cacheController.clear() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Clear HTTP cache", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { debugLogger.clear() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear logs", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                HeaderChip(
                    label = if (isFullPage) "Window" else "Full page",
                    active = isFullPage,
                    onClick = onToggleFullPage,
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (hosts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DomainFilterChip(label = "All", selected = selectedHost == null, onClick = { selectedHost = null })
                }
                items(hosts) { host ->
                    DomainFilterChip(
                        label = host,
                        selected = selectedHost == host,
                        onClick = { selectedHost = if (selectedHost == host) null else host },
                    )
                }
            }
        }

        if (filteredQueries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No requests logged yet.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filteredQueries, key = { it.id }) { item ->
                    NetworkLogRow(item, onClick = { selectedLog = item })
                }
            }
        }
    }

    selectedLog?.let { log ->
        NetworkLogDetailDialog(log = log, onDismiss = { selectedLog = null })
    }
}

@Composable
private fun HeaderChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) Color(0xFF991B1B) else Color(0xFF334155),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}
