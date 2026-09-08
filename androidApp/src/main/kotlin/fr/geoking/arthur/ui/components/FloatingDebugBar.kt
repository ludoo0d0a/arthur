package fr.geoking.arthur.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.debug.DebugQueryItem

@Composable
fun FloatingDebugBar(
    debugLogger: DebugLogger,
    modifier: Modifier = Modifier,
) {
    val stats by debugLogger.stats.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF3B82F6).copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
            )
            .testTag("floating_debug_bar"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xEE0F172A),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🐛 DEBUG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        fontFamily = FontFamily.Monospace,
                    )

                    Text(
                        text = "Queries: ${stats.totalQueries} (Hit: ${stats.cacheHits} / Miss: ${stats.cacheMisses})",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (stats.lastLoadDurationMs > 0) {
                        Text(
                            text = "Load: ${stats.lastLoadDurationMs}ms",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBF24),
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    if (stats.activeQueries > 0) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "Active: ${stats.activeQueries}",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { debugLogger.clear() },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear stats",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle debug details",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Recent Queries (${stats.recentQueries.size}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    if (stats.recentQueries.isEmpty()) {
                        Text(
                            text = "No queries executed yet.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(stats.recentQueries, key = { it.id }) { item ->
                                QueryRow(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryRow(item: DebugQueryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            Surface(
                color = Color(0xFF334155),
                shape = RoundedCornerShape(3.dp),
            ) {
                Text(
                    text = item.sourceId,
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            Surface(
                color = if (item.isCached) Color(0xFF166534) else Color(0xFF1E40AF),
                shape = RoundedCornerShape(3.dp),
            ) {
                Text(
                    text = if (item.isCached) "CACHE" else "NET (${item.statusCode ?: "?"})",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            Text(
                text = shortenUrl(item.url),
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.width(6.dp))

        Text(
            text = "${item.durationMs}ms",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.durationMs > 300) Color(0xFFF87171) else Color(0xFF4ADE80),
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun shortenUrl(url: String): String {
    return url.substringAfter("://").substringAfter("/")
}
