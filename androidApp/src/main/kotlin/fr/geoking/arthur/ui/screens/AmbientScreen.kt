package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.ui.components.ArtworkRenderer
import fr.geoking.arthur.ui.components.StillArtworkPlaceholder
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Fullscreen ambient surface.
 *
 * - Control Plane Start: pass the selected [artwork] and an empty [rotationPool] to pin it.
 * - Dream / screensaver: pass a multi-item [rotationPool] to rotate every
 *   [AmbientAlbumArt.ROTATION_INTERVAL_MS].
 *
 * Missing or unloadable assets render the category placeholder + warning — never a
 * silent swap to an unrelated genart engine (e.g. pond ripples).
 */
@Composable
fun AmbientScreenContent(
    title: String,
    modifier: Modifier = Modifier,
    artwork: Artwork? = null,
    rotationPool: List<Artwork> = emptyList(),
    isActive: Boolean = true,
) {
    val rotatePool = remember(rotationPool) {
        rotationPool.filter { it.isAmbientDisplayable() }
    }
    val shouldRotate = rotatePool.size >= 2

    var current by remember(artwork?.id, rotatePool.map { it.id }, shouldRotate) {
        mutableStateOf(
            when {
                shouldRotate && artwork != null && rotatePool.any { it.id == artwork.id } -> artwork
                shouldRotate -> rotatePool.first()
                else -> artwork
            },
        )
    }

    LaunchedEffect(rotatePool.map { it.id }, isActive, shouldRotate) {
        if (!isActive || !shouldRotate) return@LaunchedEffect
        while (isActive) {
            delay(AmbientAlbumArt.ROTATION_INTERVAL_MS)
            val next = rotatePool.filter { it.id != current?.id }.randomOrNull(Random.Default)
                ?: rotatePool.random(Random.Default)
            current = next
        }
    }

    val shown = if (shouldRotate) current else artwork
    val shownTitle = shown?.title ?: title

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ambient_screen"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            shown != null -> {
                ArtworkRenderer(
                    artwork = shown,
                    isActive = isActive,
                    quality = GenartQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                StillArtworkPlaceholder(
                    kind = ArtworkKind.Genart,
                    showWarning = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.ambient_title),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = shownTitle,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("ambient_title"),
            )
        }
    }
}

private fun Artwork.isAmbientDisplayable(): Boolean =
    isGenerative || !remoteUrl.isNullOrBlank() || !localPath.isNullOrBlank()
