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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.ui.components.ArtworkRenderer

@Composable
fun AmbientScreenContent(
    title: String,
    modifier: Modifier = Modifier,
    artwork: Artwork? = null,
    isActive: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ambient_screen"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            artwork != null && artwork.isGenerative -> {
                ArtworkRenderer(
                    artwork = artwork,
                    isActive = isActive,
                    quality = GenartQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                // Always animate Ambient — stills / missing selection fall back to fractal cycle.
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
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("ambient_title"),
            )
        }
    }
}
