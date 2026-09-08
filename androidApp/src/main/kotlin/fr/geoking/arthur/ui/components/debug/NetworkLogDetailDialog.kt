package fr.geoking.arthur.ui.components.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import fr.geoking.arthur.shared.debug.DebugQueryItem
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogDetailDialog(log: DebugQueryItem, onDismiss: () -> Unit) {
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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    JsonOrTextViewer(
                        bodyText = body,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        initialExpanded = true,
                        useLazyColumn = true,
                        showSearch = true,
                    )
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BodyContent(body: String, onFullscreen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            JsonOrTextViewer(
                bodyText = body,
                modifier = Modifier.padding(8.dp),
            )
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

internal fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
