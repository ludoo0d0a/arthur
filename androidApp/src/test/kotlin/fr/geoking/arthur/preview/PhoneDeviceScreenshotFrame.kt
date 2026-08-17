package fr.geoking.arthur.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Scora-aligned phone viewport (411×891 dp @ xxhdpi). */
internal val PhoneScreenContentWidth = 411.dp
internal val PhoneScreenContentHeight = 891.dp
internal val PhoneBezel = 14.dp
internal val PhoneScreenCornerRadius = 48.dp
internal val PhoneFrameExportWidth = PhoneScreenContentWidth + PhoneBezel * 2
internal val PhoneFrameExportHeight = PhoneScreenContentHeight + PhoneBezel * 2
internal const val PhoneFramedQualifiers = "w439dp-h919dp-xxhdpi"

private val ChassisColor = Color(0xFF1A1D22)
private val PunchHoleColor = Color(0xFF0A0A0A)
private val EdgeStroke = Color(0x2EFFFFFF)

@Composable
internal fun PhoneDeviceScreenshotFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(PhoneFrameExportWidth, PhoneFrameExportHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bezel = PhoneBezel.toPx()
            val screenW = PhoneScreenContentWidth.toPx()
            val screenH = PhoneScreenContentHeight.toPx()
            val radius = PhoneScreenCornerRadius.toPx()
            drawRoundRect(
                color = ChassisColor,
                cornerRadius = CornerRadius(radius + bezel / 2, radius + bezel / 2),
            )
            drawRoundRect(
                color = EdgeStroke,
                cornerRadius = CornerRadius(radius + bezel / 2, radius + bezel / 2),
                style = Stroke(width = 2f),
            )
            val holeR = 8.dp.toPx()
            drawCircle(
                color = PunchHoleColor,
                radius = holeR,
                center = Offset(size.width / 2, bezel + 18.dp.toPx()),
            )
            // Screen area is composed below; canvas only draws chassis.
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(bezel, bezel),
                size = Size(screenW, screenH),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
        Box(
            modifier = Modifier
                .size(PhoneScreenContentWidth, PhoneScreenContentHeight)
                .clip(RoundedCornerShape(PhoneScreenCornerRadius)),
        ) {
            content()
        }
    }
}
