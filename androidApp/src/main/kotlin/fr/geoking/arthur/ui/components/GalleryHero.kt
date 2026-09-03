package fr.geoking.arthur.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind

@Composable
internal fun GalleryHero(
    livePreview: Boolean,
    modifier: Modifier = Modifier,
    artwork: Artwork? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
    ) {
        Box {
            when {
                artwork != null -> {
                    ArtworkRenderer(
                        artwork = artwork,
                        isActive = true,
                        quality = GenartQuality.Low,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                livePreview -> {
                    FractalEffectCanvas(isActive = true, quality = FractalQuality.Low)
                }
                else -> {
                    Image(
                        painter = painterResource(R.drawable.ic_hero_gallery),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, scheme.scrim.copy(alpha = 0.5f)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = scheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(heroLabelRes(livePreview, artwork?.kind)),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

private fun heroLabelRes(livePreview: Boolean, kind: ArtworkKind?): Int = when (kind) {
    ArtworkKind.Genart -> R.string.genart_hero_label
    ArtworkKind.FractalPreset, ArtworkKind.CustomFractal -> R.string.fractal_hero_label
    ArtworkKind.Painting -> R.string.kind_painting
    ArtworkKind.Sculpture -> R.string.kind_sculpture
    ArtworkKind.Photo -> R.string.kind_photo
    ArtworkKind.PersonalPhoto -> R.string.kind_personal
    null -> if (livePreview) R.string.fractal_hero_label else R.string.gallery_hero_label
}
