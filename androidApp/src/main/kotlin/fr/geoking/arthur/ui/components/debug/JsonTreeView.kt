package fr.geoking.arthur.ui.components.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Caps how many children of a single object/array are rendered, to bound memory/CPU on huge payloads. */
private const val MAX_CONTAINER_ITEMS = 30

/** Hard cap on how many nodes the search pass will visit, to bound cost on pathological/huge JSON. */
private const val MAX_SEARCH_SCAN_NODES = 5_000

/**
 * Renders [bodyText] as a searchable, expandable JSON tree when it parses as JSON, otherwise falls
 * back to plain monospace text. This is the single reusable "body viewer" used for request/response
 * bodies, query parameter values, and the fullscreen body dialog.
 */
@Composable
fun JsonOrTextViewer(
    bodyText: String,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    useLazyColumn: Boolean = false,
    showSearch: Boolean = false,
) {
    val jsonElement = remember(bodyText) {
        try {
            Json.parseToJsonElement(bodyText)
        } catch (_: Exception) {
            null
        }
    }

    if (jsonElement == null) {
        Text(
            text = bodyText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = modifier,
        )
        return
    }

    var searchQuery by remember(bodyText) { mutableStateOf("") }

    Column(modifier) {
        if (showSearch) {
            JsonSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
            )
        }
        JsonTree(
            jsonElement = jsonElement,
            searchQuery = searchQuery.trim(),
            initialExpanded = initialExpanded,
            useLazyColumn = useLazyColumn,
        )
    }
}

@Composable
private fun JsonSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        placeholder = { Text("Search in JSON…", fontSize = 12.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

private data class JsonNode(
    val path: String,
    val key: String?,
    val value: JsonElement,
    val depth: Int,
)

@Composable
private fun JsonTree(
    jsonElement: JsonElement,
    searchQuery: String,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    useLazyColumn: Boolean = false,
) {
    val expandedPaths = remember(jsonElement) { mutableStateMapOf<String, Boolean>() }
    val searchActive = searchQuery.isNotEmpty()

    // Ancestor paths of every node whose own key/value matches the query; these force-expand
    // regardless of manual toggle state so a hit is always reachable without a global unroll.
    val ancestorPaths = remember(jsonElement, searchQuery) {
        if (!searchActive) {
            emptySet()
        } else {
            val matches = mutableSetOf<String>()
            var visited = 0
            fun matchesQuery(key: String?, element: JsonElement): Boolean {
                if (key != null && key.contains(searchQuery, ignoreCase = true)) return true
                return element is JsonPrimitive && element.content.contains(searchQuery, ignoreCase = true)
            }
            fun scan(path: String, key: String?, element: JsonElement, ancestors: List<String>): Boolean {
                visited++
                if (visited > MAX_SEARCH_SCAN_NODES) return false
                val selfMatch = matchesQuery(key, element)
                var descendantMatch = false
                when (element) {
                    is JsonObject -> element.entries.forEach { (k, v) ->
                        if (scan("$path/$k", k, v, ancestors + path)) descendantMatch = true
                    }
                    is JsonArray -> element.forEachIndexed { i, v ->
                        if (scan("$path/$i", i.toString(), v, ancestors + path)) descendantMatch = true
                    }
                    else -> {}
                }
                if (selfMatch || descendantMatch) {
                    matches.addAll(ancestors)
                    matches.add(path)
                }
                return selfMatch || descendantMatch
            }
            scan("", null, jsonElement, emptyList())
            matches
        }
    }

    val nodes = remember(jsonElement, expandedPaths.toMap(), searchQuery) {
        val list = mutableListOf<JsonNode>()
        fun isExpanded(path: String): Boolean = when {
            searchActive -> path.isEmpty() || path in ancestorPaths
            else -> expandedPaths.getOrPut(path) { initialExpanded }
        }
        fun collect(path: String, key: String?, value: JsonElement, depth: Int) {
            list.add(JsonNode(path, key, value, depth))
            if (!isExpanded(path)) return
            when (value) {
                is JsonObject -> {
                    val entries = value.entries.toList().take(MAX_CONTAINER_ITEMS)
                    entries.forEach { (k, v) -> collect("$path/$k", k, v, depth + 1) }
                    val omitted = value.size - entries.size
                    if (omitted > 0) {
                        list.add(JsonNode("$path/__truncated", null, JsonPrimitive("… ($omitted more truncated)"), depth + 1))
                    }
                }
                is JsonArray -> {
                    val elements = value.take(MAX_CONTAINER_ITEMS)
                    elements.forEachIndexed { i, v -> collect("$path/$i", i.toString(), v, depth + 1) }
                    val omitted = value.size - elements.size
                    if (omitted > 0) {
                        list.add(JsonNode("$path/__truncated", null, JsonPrimitive("… ($omitted more truncated)"), depth + 1))
                    }
                }
                else -> {}
            }
        }
        collect("", null, jsonElement, 0)
        list
    }

    if (useLazyColumn) {
        LazyColumn(modifier = modifier) {
            items(nodes, key = { it.path }) { node ->
                JsonNodeRow(
                    node = node,
                    isExpanded = if (searchActive) node.path in ancestorPaths else expandedPaths.getOrPut(node.path) { initialExpanded },
                    searchQuery = searchQuery,
                    onToggle = { expandedPaths[node.path] = !expandedPaths.getOrDefault(node.path, initialExpanded) },
                )
            }
        }
    } else {
        Column(modifier = modifier.verticalScroll(rememberScrollState())) {
            nodes.forEach { node ->
                JsonNodeRow(
                    node = node,
                    isExpanded = if (searchActive) node.path in ancestorPaths else expandedPaths.getOrPut(node.path) { initialExpanded },
                    searchQuery = searchQuery,
                    onToggle = { expandedPaths[node.path] = !expandedPaths.getOrDefault(node.path, initialExpanded) },
                )
            }
        }
    }
}

@Composable
private fun JsonNodeRow(
    node: JsonNode,
    isExpanded: Boolean,
    searchQuery: String,
    onToggle: () -> Unit,
) {
    val indent = (node.depth * 12).dp
    val value = node.value

    when (value) {
        is JsonObject, is JsonArray -> {
            val label = when (value) {
                is JsonObject -> if (value.isEmpty()) "{ }" else "{ … }"
                else -> if ((value as JsonArray).isEmpty()) "[ ]" else "[ … ]"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(start = indent, top = 2.dp, bottom = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(16.dp).padding(end = 0.dp),
                )
                HighlightedKey(node.key, searchQuery)
                Text(text = label, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        is JsonPrimitive -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indent, top = 2.dp, bottom = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                HighlightedKey(node.key, searchQuery)
                HighlightedValue(
                    text = if (value.isString) "\"${value.content}\"" else value.content,
                    color = when {
                        value.isString -> Color(0xFF2DD4BF)
                        value.content == "true" || value.content == "false" -> Color(0xFFF472B6)
                        value.content == "null" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color(0xFFFB923C)
                    },
                    searchQuery = searchQuery,
                )
            }
        }
    }
}

@Composable
private fun HighlightedKey(key: String?, searchQuery: String) {
    if (key == null) return
    HighlightedValue(text = "\"$key\": ", color = Color(0xFF94A3B8), searchQuery = searchQuery, bold = true)
}

@Composable
private fun HighlightedValue(text: String, color: Color, searchQuery: String, bold: Boolean = false) {
    val annotated = remember(text, searchQuery, color) {
        if (searchQuery.isEmpty()) {
            buildAnnotatedString { append(text) }
        } else {
            buildAnnotatedString {
                var start = 0
                while (true) {
                    val idx = text.indexOf(searchQuery, start, ignoreCase = true)
                    if (idx == -1) {
                        append(text.substring(start))
                        break
                    }
                    append(text.substring(start, idx))
                    withStyle(SpanStyle(background = Color(0xFFFACC15), color = Color.Black)) {
                        append(text.substring(idx, idx + searchQuery.length))
                    }
                    start = idx + searchQuery.length
                }
            }
        }
    }
    Text(
        text = annotated,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
    )
}
