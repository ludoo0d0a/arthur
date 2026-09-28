package fr.geoking.arthur.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import fr.geoking.arthur.auto.AmbientStillRenderer
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
import fr.geoking.arthur.source.DeveloperSettings
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.RemoteStillCacheOnlyMiss
import fr.geoking.arthur.source.RemoteStillNetworkGate
import fr.geoking.arthur.source.StillImageDownloader
import fr.geoking.arthur.source.rememberArtworkImageCache
import fr.geoking.arthur.shared.error.ErrorLogger
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders Artwork for Control Plane hero and phone Ambient.
 * Generative kinds use live Canvas engines; stills prefer local cache then remote URL.
 * Failures show the category icon on a gradient with a warning — never another engine.
 *
 * [onDisplayReady] fires once the still is on screen (or generative/video has started),
 * including after a failed load so Ambient rotation does not wait forever.
 * [onStillFailed] fires when a remote/local still cannot be shown (Ambient may skip).
 */
@Composable
fun ArtworkRenderer(
    artwork: Artwork,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: GenartQuality = GenartQuality.High,
    onDisplayReady: (() -> Unit)? = null,
    onStillFailed: (() -> Unit)? = null,
) {
    key(artwork.id) {
        Box(modifier = modifier.fillMaxSize()) {
            when (artwork.kind) {
                ArtworkKind.Genart -> {
                    val engine = GenartCatalog.engineForId(artwork.id)
                    if (engine != null) {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        GenartEffectCanvas(
                            engine = engine,
                            isActive = isActive,
                            quality = quality,
                        )
                    } else if (!artwork.localPath.isNullOrBlank() || !artwork.remoteUrl.isNullOrBlank()) {
                        RemoteStillImage(
                            artworkId = artwork.id,
                            localPath = artwork.localPath,
                            remoteUrl = artwork.remoteUrl,
                            kind = artwork.kind,
                            onDisplayReady = onDisplayReady,
                            onStillFailed = onStillFailed,
                        )
                    } else {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
                ArtworkKind.CustomFractal -> {
                    val params = CustomFractalParams.fromArtworkId(artwork.id)
                    if (params != null) {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        CustomFractalEffectCanvas(
                            params = params,
                            isActive = isActive,
                            quality = quality.toFractalQuality(),
                        )
                    } else {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
                ArtworkKind.FractalPreset -> {
                    val type = fractalTypeForArtworkId(artwork.id)
                    if (type != null) {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        FractalEffectCanvas(
                            isActive = isActive,
                            quality = quality.toFractalQuality(),
                            forceType = type,
                        )
                    } else {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        StillArtworkPlaceholder(kind = artwork.kind, showWarning = true)
                    }
                }
                ArtworkKind.Video -> {
                    val url = artwork.remoteUrl?.takeIf { it.isNotBlank() }
                        ?: artwork.localPath?.takeIf { it.isNotBlank() }
                    if (url != null) {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
                        AmbientVideoPlayer(
                            url = url,
                            isActive = isActive,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
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
                            onDisplayReady = onDisplayReady,
                            onStillFailed = onStillFailed,
                        )
                    } else {
                        LaunchedEffect(artwork.id) { onDisplayReady?.invoke() }
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
    onDisplayReady: (() -> Unit)? = null,
    onStillFailed: (() -> Unit)? = null,
) {
    val imageCache = rememberArtworkImageCache()
    val developerSettings = remember {
        runCatching { GlobalContext.get().get<DeveloperSettings>() }.getOrNull()
    }
    val errorLogger = remember {
        runCatching { GlobalContext.get().get<ErrorLogger>() }.getOrNull()
    }
    val networkGate = remember {
        runCatching { GlobalContext.get().get<RemoteStillNetworkGate>() }.getOrNull()
    }
    val invalidStore = remember {
        runCatching { GlobalContext.get().get<InvalidArtworkStore>() }.getOrNull()
    }
    val isVerbose = developerSettings?.verbose?.collectAsState()?.value ?: false
    var retryCount by remember(artworkId, localPath, remoteUrl) { mutableIntStateOf(0) }
    var bitmapState by remember(artworkId, localPath, remoteUrl) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    var hasFailed by remember(artworkId, localPath, remoteUrl) { mutableStateOf(false) }
    var failureReason by remember(artworkId, localPath, remoteUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(artworkId, localPath, remoteUrl, retryCount) {
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
            // Corrupt oversized cache left a path that wouldn't decode via localPathOrNull;
            // if raw bytes remain but aren't an image, purge before network attempt.
            if (imageCache.hasImage(artworkId) && !imageCache.hasDecodableImage(artworkId)) {
                imageCache.purgeInvalid(artworkId)
            }
            val url = remoteUrl?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("No image URL provided"))
            val allowNetwork = networkGate?.canDownloadRemoteStill() ?: true
            runCatching {
                val downloadedFile = imageCache.downloadAndCache(
                    artworkId = artworkId,
                    remoteUrl = url,
                    errorLogger = errorLogger,
                    allowNetwork = allowNetwork,
                )
                SafeBitmapDecoder.decodeFile(downloadedFile.absolutePath)
                    ?: run {
                        imageCache.purgeInvalid(artworkId)
                        error("Failed to decode downloaded image file")
                    }
            }.onFailure { error ->
                if (error is RemoteStillCacheOnlyMiss) return@onFailure
                val httpCode = Regex("""HTTP (\d{3})""")
                    .find(error.message.orEmpty())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                if (StillImageDownloader.isNonRetryable(httpCode, error)) {
                    invalidStore?.markInvalid(artworkId)
                }
            }
        }
        result
            .onSuccess { bmp -> bitmapState = bmp }
            .onFailure { error ->
                hasFailed = true
                failureReason = when (error) {
                    is RemoteStillCacheOnlyMiss -> "Cached image unavailable"
                    else -> error.message ?: error.toString()
                }
            }
    }

    LaunchedEffect(bitmapState, hasFailed) {
        if (bitmapState != null || hasFailed) {
            onDisplayReady?.invoke()
            if (hasFailed) onStillFailed?.invoke()
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
                val (topColor, bottomColor) = remember(bmp) {
                    AmbientStillRenderer.extractGradientColors(bmp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(topColor), Color(bottomColor)),
                            ),
                        )
                        .testTag("artwork_remote_image_bg"),
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
            val finalReason = remember(hasFailed, failureReason, isVerbose) {
                if (isVerbose) {
                    val detailedError = errorLogger?.getLastErrorForArtwork(artworkId)
                    if (detailedError != null) {
                        "HTTP ${detailedError.statusCode ?: "Unknown"}: ${detailedError.url ?: "No URL"}"
                    } else {
                        failureReason
                    }
                } else {
                    null
                }
            }
            StillArtworkPlaceholder(
                kind = kind,
                showWarning = true,
                errorDetail = finalReason,
                onRetry = { retryCount++ },
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
    onRetry: (() -> Unit)? = null,
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
                if (onRetry != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("artwork_retry_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.retry),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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
        "nova" -> FractalType.Nova
        "newton" -> FractalType.Newton
        "mandelbrotglow" -> FractalType.MandelbrotGlow
        "juliatouch" -> FractalType.JuliaTouch
        else -> null
    }

internal fun GenartQuality.toFractalQuality(): FractalQuality = when (this) {
    GenartQuality.Low -> FractalQuality.Low
    GenartQuality.Medium -> FractalQuality.Medium
    GenartQuality.High -> FractalQuality.High
}
