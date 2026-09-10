package fr.geoking.arthur.ui.components.debug

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.debug.DebugQueryItem
import fr.geoking.arthur.source.HttpCacheController
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Floating bug-report button that expands into a big overlay window showing all HTTP traffic:
 * counts, per-request cache hit/miss + timing, domain filter chips, and a details drill-down
 * (headers, payload, response, JSON tree). The top bar's fullscreen icon takes the overlay full
 * page; the back arrow steps out of full page first, then closes the overlay (also wired to the
 * system back button via [BackHandler]).
 */
@Composable
fun DebugBarButton(
    debugLogger: DebugLogger,
    cacheController: HttpCacheController,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFullPage by remember { mutableStateOf(false) }
    val goBack = { if (isFullPage) isFullPage = false else isExpanded = false }

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
        BackHandler(onBack = goBack)
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
                        onBack = goBack,
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
    onBack: () -> Unit,
) {
    val stats by debugLogger.stats.collectAsState()
    val cacheDisabled by cacheController.disabled.collectAsState()
    var selectedHost by remember { mutableStateOf<String?>(null) }
    var cacheFilter by remember { mutableStateOf<Boolean?>(null) }
    var selectedLog by remember { mutableStateOf<DebugQueryItem?>(null) }

    val hosts = remember(stats.recentQueries) {
        stats.recentQueries.map { it.host }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredQueries = remember(stats.recentQueries, selectedHost, cacheFilter) {
        stats.recentQueries
            .filter { selectedHost == null || it.host == selectedHost }
            .filter { cacheFilter == null || it.isCached == cacheFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
                    label = if (isFullPage) "⤡ Window" else "⤢ Full page",
                    active = isFullPage,
                    onClick = onToggleFullPage,
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CacheFilterChip(
                    label = "All",
                    color = Color(0xFF334155),
                    selected = cacheFilter == null,
                    onClick = { cacheFilter = null },
                )
            }
            item {
                CacheFilterChip(
                    label = "HIT",
                    color = Color(0xFF166534),
                    selected = cacheFilter == true,
                    onClick = { cacheFilter = if (cacheFilter == true) null else true },
                )
            }
            item {
                CacheFilterChip(
                    label = "MISS",
                    color = Color(0xFF1E40AF),
                    selected = cacheFilter == false,
                    onClick = { cacheFilter = if (cacheFilter == false) null else false },
                )
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

@Composable
private fun DomainFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = Color.White.copy(alpha = 0.6f),
            selectedContainerColor = Color.White.copy(alpha = 0.2f),
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.White.copy(alpha = 0.2f),
            selectedBorderColor = Color.White.copy(alpha = 0.5f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
            enabled = true,
            selected = selected,
        ),
    )
}

@Composable
private fun CacheFilterChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = color,
            selectedContainerColor = color.copy(alpha = 0.25f),
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = color.copy(alpha = 0.5f),
            selectedBorderColor = color,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
            enabled = true,
            selected = selected,
        ),
    )
}

@Composable
private fun NetworkLogRow(item: DebugQueryItem, onClick: () -> Unit) {
    val time = remember(item.timestamp) { formatTime(item.timestamp) }
    val statusColor = when (item.statusCode) {
        in 200..299 -> Color(0xFF4ADE80)
        in 300..399 -> Color(0xFF38BDF8)
        in 400..499 -> Color(0xFFFACC15)
        in 500..599 -> Color(0xFFF87171)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF334155), shape = RoundedCornerShape(3.dp)) {
                Text(
                    text = item.sourceId,
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (item.isCached) Color(0xFF166534) else Color(0xFF1E40AF),
                shape = RoundedCornerShape(3.dp),
            ) {
                Text(
                    text = if (item.isCached) "HIT" else "MISS",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.statusCode?.toString() ?: (if (item.errorMessage != null) "ERR" else "?"),
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${item.durationMs}ms",
                color = if (item.durationMs > 300) Color(0xFFF87171) else Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = time,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            text = item.url,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkLogDetailDialog(log: DebugQueryItem, onDismiss: () -> Unit) {
    var fullscreenBody by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Request details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        DetailSection("General")
                        DetailItem("URL", log.url)
                        DetailItem("Host", log.host)
                        DetailItem("Source", log.sourceId)
                        DetailItem("Status", log.statusCode?.toString() ?: "N/A")
                        DetailItem("Cache", if (log.isCached) "HIT" else "MISS")
                        DetailItem("Duration", "${log.durationMs}ms")
                        DetailItem("Time", Date(log.timestamp).toString())
                        log.errorMessage?.let { DetailItem("Error", it) }

                        val queryParams = remember(log.url) {
                            log.url.toHttpUrlOrNull()?.let { url ->
                                url.queryParameterNames.associateWith { name -> url.queryParameterValues(name) }
                            }.orEmpty()
                        }
                        if (queryParams.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailSection("Query parameters")
                            queryParams.forEach { (k, v) ->
                                DetailItem(k, v.filterNotNull().joinToString(", "))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DetailSection("Request headers")
                        if (log.requestHeaders.isEmpty()) {
                            Text("(none)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        log.requestHeaders.forEach { (k, v) -> DetailItem(k, v.joinToString(", ")) }

                        val reqBody = log.requestBody
                        if (!reqBody.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailSection("Request body${if (log.requestBodyTruncated) " (truncated)" else ""}")
                            BodyContent(reqBody, onFullscreen = { fullscreenBody = reqBody })
                        }

                        if (log.responseHeaders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailSection("Response headers")
                            log.responseHeaders.forEach { (k, v) -> DetailItem(k, v.joinToString(", ")) }
                        }

                        val respBody = log.responseBody
                        if (!respBody.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailSection("Response body${if (log.responseBodyTruncated) " (truncated)" else ""}")
                            BodyContent(respBody, onFullscreen = { fullscreenBody = respBody })
                        }
                    }
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxSize(0.85f),
    )

    fullscreenBody?.let { body ->
        FullscreenBodyDialog(body = body, onDismiss = { fullscreenBody = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenBodyDialog(body: String, onDismiss: () -> Unit) {
    val jsonElement = remember(body) {
        try {
            Json.parseToJsonElement(body)
        } catch (_: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Body viewer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            SelectionContainer {
                Surface(
                    color = Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (jsonElement != null) {
                        JsonTree(
                            jsonElement = jsonElement,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            initialExpanded = true,
                            useLazyColumn = true,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            item {
                                Text(text = body, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BodyContent(body: String, onFullscreen: () -> Unit) {
    val jsonElement = remember(body) {
        try {
            Json.parseToJsonElement(body)
        } catch (_: Exception) {
            null
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            color = Color.Black.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (jsonElement != null) {
                JsonTree(jsonElement = jsonElement, modifier = Modifier.padding(8.dp))
            } else {
                Text(text = body, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
            }
        }
        Text(
            text = "⤢",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp).clickable(onClick = onFullscreen),
        )
    }
}

@Composable
private fun DetailSection(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(text = value, fontSize = 12.sp)
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

private data class JsonNode(
    val path: String,
    val key: String?,
    val value: JsonElement,
    val depth: Int,
)

private const val MAX_JSON_CONTAINER_ITEMS = 20

@Composable
private fun JsonTree(
    jsonElement: JsonElement,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    useLazyColumn: Boolean = false,
) {
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }

    val nodes = remember(jsonElement, expandedPaths.toMap()) {
        val list = mutableListOf<JsonNode>()
        fun collectNodes(path: String, key: String?, value: JsonElement, depth: Int) {
            list.add(JsonNode(path, key, value, depth))
            val isExpanded = expandedPaths.getOrPut(path) { initialExpanded }
            if (isExpanded) {
                when (value) {
                    is JsonObject -> {
                        val entries = value.entries.toList()
                        entries.take(MAX_JSON_CONTAINER_ITEMS).forEach { (k, v) ->
                            collectNodes("$path/$k", k, v, depth + 1)
                        }
                        val truncated = entries.size - MAX_JSON_CONTAINER_ITEMS
                        if (truncated > 0) {
                            list.add(
                                JsonNode(
                                    path = "$path/__truncated",
                                    key = null,
                                    value = JsonPrimitive("… ($truncated items truncated)"),
                                    depth = depth + 1,
                                ),
                            )
                        }
                    }
                    is JsonArray -> {
                        value.take(MAX_JSON_CONTAINER_ITEMS).forEachIndexed { i, v ->
                            collectNodes("$path/$i", i.toString(), v, depth + 1)
                        }
                        val truncated = value.size - MAX_JSON_CONTAINER_ITEMS
                        if (truncated > 0) {
                            list.add(
                                JsonNode(
                                    path = "$path/__truncated",
                                    key = null,
                                    value = JsonPrimitive("… ($truncated items truncated)"),
                                    depth = depth + 1,
                                ),
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
        collectNodes("", null, jsonElement, 0)
        list
    }

    if (useLazyColumn) {
        LazyColumn(modifier = modifier) {
            items(nodes, key = { it.path }) { node ->
                JsonNodeRow(
                    node = node,
                    isExpanded = expandedPaths.getOrPut(node.path) { initialExpanded },
                    onToggle = { expandedPaths[node.path] = !expandedPaths.getOrDefault(node.path, initialExpanded) },
                )
            }
        }
    } else {
        Column(modifier = modifier) {
            nodes.forEach { node ->
                JsonNodeRow(
                    node = node,
                    isExpanded = expandedPaths.getOrPut(node.path) { initialExpanded },
                    onToggle = { expandedPaths[node.path] = !expandedPaths.getOrDefault(node.path, initialExpanded) },
                )
            }
        }
    }
}

@Composable
private fun JsonNodeRow(node: JsonNode, isExpanded: Boolean, onToggle: () -> Unit) {
    val indent = (node.depth * 12).dp
    val value = node.value

    when (value) {
        is JsonObject, is JsonArray -> {
            val label = when (value) {
                is JsonObject -> if (value.isEmpty()) "{ }" else "{ … }"
                else -> if ((value as JsonArray).isEmpty()) "[ ]" else "[ … ]"
            }
            ExpandableNode(indent = indent, key = node.key, label = label, isExpanded = isExpanded, onToggle = onToggle)
        }
        is JsonPrimitive -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = indent, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                if (node.key != null) {
                    Text(
                        text = "\"${node.key}\": ",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = if (value.isString) "\"${value.content}\"" else value.content,
                    color = when {
                        value.isString -> Color(0xFF2DD4BF)
                        value.content == "true" || value.content == "false" -> Color(0xFFF472B6)
                        value.content == "null" -> Color(0xFF94A3B8)
                        else -> Color(0xFFFB923C)
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun ExpandableNode(indent: androidx.compose.ui.unit.Dp, key: String?, label: String, isExpanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = indent, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
        if (key != null) {
            Text(
                text = "\"$key\": ",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
