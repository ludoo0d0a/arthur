package fr.geoking.arthur.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.fractal.CustomFractalEffectCanvas
import fr.geoking.arthur.fractal.CustomFractalParams
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.fractal.FractalType
import fr.geoking.arthur.genart.GenartCatalog
import fr.geoking.arthur.genart.GenartEffectCanvas
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.source.ArtworkImageCache
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders Artwork for Control Plane hero and phone Ambient.
 * Generative kinds use live Canvas engines; stills prefer local cache then remote URL.
 */
@Composable
fun ArtworkRenderer(
    artwork: Artwork,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: GenartQuality = GenartQuality.Medium,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (artwork.kind) {
            ArtworkKind.Genart -> {
                if (!artwork.localPath.isNullOrBlank()) {
                    RemoteStillImage(
                        artworkId = artwork.id,
                        localPath = artwork.localPath,
                        remoteUrl = null,
                        kind = artwork.kind,
                    )
                } else {
                    val engine = GenartCatalog.engineForId(artwork.id)
                    if (engine != null) {
                        GenartEffectCanvas(
                            engine = engine,
                            isActive = isActive,
                            quality = quality,
                        )
                    } else {
                        StillArtworkPlaceholder(kind = artwork.kind)
                    }
                }
            }
            ArtworkKind.CustomFractal -> {
                val params = CustomFractalParams.fromArtworkId(artwork.id)
                if (params != null) {
                    CustomFractalEffectCanvas(
                        params = params,
                        isActive = isActive,
                        quality = quality.toFractalQuality(),
                    )
                } else {
                    StillArtworkPlaceholder(kind = artwork.kind)
                }
            }
            ArtworkKind.FractalPreset -> {
                FractalEffectCanvas(
                    isActive = isActive,
                    quality = quality.toFractalQuality(),
                    forceType = fractalTypeForArtworkId(artwork.id),
                )
            }
            else -> {
                if (!artwork.localPath.isNullOrBlank() || !artwork.remoteUrl.isNullOrBlank()) {
                    RemoteStillImage(
                        artworkId = artwork.id,
                        localPath = artwork.localPath,
                        remoteUrl = artwork.remoteUrl,
                        kind = artwork.kind,
                    )
                } else {
                    StillArtworkPlaceholder(kind = artwork.kind)
                }
            }
        }
    }
}

@Composable
private fun RemoteStillImage(
    artworkId: String,
    localPath: String?,
    remoteUrl: String?,
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageCache = remember(context) { ArtworkImageCache(context) }
    var bitmapState by remember(artworkId, localPath, remoteUrl) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    var hasFailed by remember(artworkId, localPath, remoteUrl) { mutableStateOf(false) }

    LaunchedEffect(artworkId, localPath, remoteUrl) {
        withContext(Dispatchers.IO) {
            val fromDisk = sequenceOf(
                localPath,
                imageCache.localPathOrNull(artworkId),
            ).filterNotNull().firstNotNullOfOrNull { path ->
                runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
            }
            if (fromDisk != null) {
                bitmapState = fromDisk
                return@withContext
            }
            val url = remoteUrl?.takeIf { it.isNotBlank() }
            if (url == null) {
                hasFailed = true
                return@withContext
            }
            runCatching {
                URL(url).openStream().use { stream -> stream.readBytes() }
            }.onSuccess { bytes ->
                imageCache.putImage(artworkId, bytes)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    bitmapState = bmp
                } else {
                    hasFailed = true
                }
            }.onFailure {
                hasFailed = true
            }
        }
    }

    val bmp = bitmapState
    if (bmp != null && !hasFailed) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize().testTag("artwork_remote_image"),
        )
    } else {
        StillArtworkPlaceholder(kind = kind, modifier = modifier)
    }
}

/** Gradient field with the artwork-kind glyph — used for bundled / museum stills. */
@Composable
private fun StillArtworkPlaceholder(
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val visual = kind.visual()
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("artwork_placeholder")
            .background(
                Brush.radialGradient(
                    colors = listOf(scheme.surfaceVariant, scheme.background),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(visual.iconRes),
            contentDescription = null,
            tint = visual.onContainer.copy(alpha = 0.42f),
            modifier = Modifier.size(88.dp),
        )
    }
}

internal fun fractalTypeForArtworkId(artworkId: String): FractalType? =
    when (FractalSource.typeKey(artworkId)?.lowercase()) {
        "mandelbrot" -> FractalType.Mandelbrot
        "julia" -> FractalType.Julia
        "burningship" -> FractalType.BurningShip
        "tricorn" -> FractalType.Tricorn
        "multibrot" -> FractalType.Multibrot
        "celtic" -> FractalType.Celtic
        "buffalo" -> FractalType.Buffalo
        "phoenix" -> FractalType.Phoenix
        else -> null
    }

internal fun GenartQuality.toFractalQuality(): FractalQuality = when (this) {
    GenartQuality.Low -> FractalQuality.Low
    GenartQuality.Medium -> FractalQuality.Medium
    GenartQuality.High -> FractalQuality.High
}
