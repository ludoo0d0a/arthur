package fr.geoking.arthur.ui.components.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.geoking.arthur.shared.debug.DebugQueryItem

@Composable
fun DomainFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
fun NetworkLogRow(item: DebugQueryItem, onClick: () -> Unit) {
    val statusColor = when (item.statusCode) {
        in 200..299 -> Color(0xFF4ADE80)
        in 300..399 -> Color(0xFF38BDF8)
        in 400..499 -> Color(0xFFFACC15)
        in 500..599 -> Color(0xFFF87171)
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(
                text = item.statusCode?.toString() ?: (if (item.errorMessage != null) "ERR" else "?"),
                fontSize = 10.sp,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
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

internal fun shortenUrl(url: String): String = url.substringAfter("://").substringAfter("/")
