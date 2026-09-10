package fr.geoking.arthur.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
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
import fr.geoking.arthur.source.SafeBitmapDecoder
import fr.geoking.arthur.source.StillImageDownloader
import fr.geoking.arthur.source.rememberArtworkImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders Artwork for Control Plane hero and phone Ambient.
 * Generative kinds use live Canvas engines; stills prefer local cache then remote URL.
 * Failures show the category icon on a gradient with a warning — never another engine.
 */
@Composable
fun ArtworkRenderer(
    artwork: Artwork,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: GenartQuality = GenartQuality.Medium,
) {
    key(artwork.id) {
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
                    } else if (!artwork.localPath.isNullOrBlank()) {
                        RemoteStillImage(
                            artworkId = artwork.id,
                            localPath = artwork.localPath,
                            remoteUrl = null,
                            kind = artwork.kind,
                        )
                    } else {
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
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
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
                ArtworkKind.FractalPreset -> {
                    val type = fractalTypeForArtworkId(artwork.id)
                    if (type != null) {
                        FractalEffectCanvas(
                            isActive = isActive,
                            quality = quality.toFractalQuality(),
                            forceType = type,
                        )
                    } else {
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
                ArtworkKind.Video -> {
                    val url = artwork.remoteUrl?.takeIf { it.isNotBlank() }
                        ?: artwork.localPath?.takeIf { it.isNotBlank() }
                    if (url != null) {
                        AmbientVideoPlayer(
                            url = url,
                            isActive = isActive,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
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
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
            }
        }
    }
}

/** Small still preview for detail chrome (falls back to kind placeholder). */
@Composable
fun StillArtworkThumbnail(
    artwork: Artwork,
    modifier: Modifier = Modifier,
) {
    if (!artwork.localPath.isNullOrBlank() || !artwork.remoteUrl.isNullOrBlank()) {
        RemoteStillImage(
            artworkId = artwork.id,
            localPath = artwork.localPath,
            remoteUrl = artwork.remoteUrl,
            kind = artwork.kind,
            modifier = modifier,
        )
    } else {
        StillArtworkPlaceholder(
            kind = artwork.kind,
            showWarning = true,
            modifier = modifier,
        )
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
    val imageCache = rememberArtworkImageCache()
    var bitmapState by remember(artworkId, localPath, remoteUrl) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    var hasFailed by remember(artworkId, localPath, remoteUrl) { mutableStateOf(false) }
    var failureReason by remember(artworkId, localPath, remoteUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(artworkId, localPath, remoteUrl) {
        hasFailed = false
        failureReason = null
        bitmapState = null
        val result = withContext(Dispatchers.IO) {
            val fromDisk = sequenceOf(
                localPath,
                imageCache.localPathOrNull(artworkId),
            ).filterNotNull().firstNotNullOfOrNull { path ->
                SafeBitmapDecoder.decodeFile(path)
            }
            if (fromDisk != null) return@withContext Result.success(fromDisk)
            val url = remoteUrl?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("No image URL provided"))
            runCatching {
                val downloadedFile = imageCache.downloadAndCache(artworkId, url)
                SafeBitmapDecoder.decodeFile(downloadedFile.absolutePath)
                    ?: error("Failed to decode downloaded image file")
            }
        }
        result
            .onSuccess { bmp -> bitmapState = bmp }
            .onFailure { error ->
                hasFailed = true
                failureReason = error.message ?: error.toString()
            }
    }

    val bmp = bitmapState
    when {
        bmp != null && !hasFailed -> {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("artwork_remote_image_container"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(25.dp)
                            } else {
                                Modifier
                            },
                        )
                        .testTag("artwork_remote_image_bg"),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                )
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("artwork_remote_image"),
                )
            }
        }
        hasFailed -> {
            StillArtworkPlaceholder(
                kind = kind,
                showWarning = true,
                errorDetail = failureReason,
                modifier = modifier,
            )
        }
        else -> {
            Box(modifier = modifier.fillMaxSize()) {
                StillArtworkPlaceholder(kind = kind, showWarning = false)
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .testTag("artwork_image_loading"),
                    color = Color.White.copy(alpha = 0.85f),
                    trackColor = Color.White.copy(alpha = 0.25f),
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}

/** Gradient field with the artwork-kind glyph — used when stills / engines are unavailable. */
@Composable
fun StillArtworkPlaceholder(
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
    showWarning: Boolean = false,
    errorDetail: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val visual = kind.visual()
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("artwork_placeholder")
            .background(
                Brush.radialGradient(
                    colors = listOf(visual.container, scheme.background),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                painter = painterResource(visual.iconRes),
                contentDescription = null,
                tint = visual.onContainer.copy(alpha = 0.55f),
                modifier = Modifier.size(88.dp),
            )
            if (showWarning) {
                Text(
                    text = stringResource(R.string.artwork_unavailable),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("artwork_unavailable_message"),
                )
                Text(
                    text = stringResource(R.string.artwork_unavailable_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!errorDetail.isNullOrBlank()) {
                    Text(
                        text = errorDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("artwork_unavailable_reason"),
                    )
                }
            }
        }
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
