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
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.ui.components.ArtworkRenderer
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun AmbientScreenContent(
    title: String,
    modifier: Modifier = Modifier,
    artwork: Artwork? = null,
    rotationPool: List<Artwork> = emptyList(),
    isActive: Boolean = true,
) {
    val picturePool = remember(rotationPool, artwork) {
        val stills = rotationPool.filter { hasDisplayableStill(it) }
        when {
            stills.isNotEmpty() -> stills
            artwork != null && hasDisplayableStill(artwork) -> listOf(artwork)
            else -> emptyList()
        }
    }
    var current by remember(artwork?.id, picturePool.map { it.id }) {
        mutableStateOf(
            when {
                artwork != null && picturePool.any { it.id == artwork.id } -> artwork
                picturePool.isNotEmpty() -> picturePool.random(Random.Default)
                else -> artwork
            },
        )
    }

    LaunchedEffect(picturePool.map { it.id }, isActive) {
        if (!isActive || picturePool.size < 2) return@LaunchedEffect
        while (isActive) {
            delay(AmbientAlbumArt.ROTATION_INTERVAL_MS)
            val next = picturePool.filter { it.id != current?.id }.randomOrNull(Random.Default)
                ?: picturePool.random(Random.Default)
            current = next
        }
    }

    val shown = current ?: artwork
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
                FractalEffectCanvas(
                    isActive = isActive,
                    quality = FractalQuality.High,
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

private fun hasDisplayableStill(artwork: Artwork): Boolean =
    !artwork.remoteUrl.isNullOrBlank() || !artwork.localPath.isNullOrBlank()
