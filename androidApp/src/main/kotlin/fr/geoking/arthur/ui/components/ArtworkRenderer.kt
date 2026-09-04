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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
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

/**
 * Renders Artwork for Control Plane hero and phone Ambient.
 * Generative kinds use live Canvas engines; stills use a tonal placeholder.
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
                if (!artwork.remoteUrl.isNullOrBlank()) {
                    RemoteStillImage(
                        url = artwork.remoteUrl!!,
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
    url: String,
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
) {
    var bitmapState by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var hasFailed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.onSuccess { bmp ->
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
            modifier = modifier.fillMaxSize(),
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
        else -> null
    }

internal fun GenartQuality.toFractalQuality(): FractalQuality = when (this) {
    GenartQuality.Low -> FractalQuality.Low
    GenartQuality.Medium -> FractalQuality.Medium
    GenartQuality.High -> FractalQuality.High
}
