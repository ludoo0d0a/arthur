package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.ArtworkKind

internal data class ArtworkKindVisual(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val container: Color,
    val onContainer: Color,
)

@Composable
internal fun ArtworkKind.visual(): ArtworkKindVisual {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        ArtworkKind.Painting -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_painting,
            labelRes = R.string.kind_painting,
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
        )
        ArtworkKind.Sculpture -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_sculpture,
            labelRes = R.string.kind_sculpture,
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
        )
        ArtworkKind.Photo -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_photo,
            labelRes = R.string.kind_photo,
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
        )
        ArtworkKind.FractalPreset, ArtworkKind.CustomFractal -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_fractal,
            labelRes = if (this == ArtworkKind.CustomFractal) {
                R.string.kind_custom_fractal
            } else {
                R.string.kind_fractal
            },
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
        )
        ArtworkKind.Genart -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_genart,
            labelRes = R.string.kind_genart,
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
        )
        ArtworkKind.PersonalPhoto -> ArtworkKindVisual(
            iconRes = R.drawable.ic_kind_personal,
            labelRes = R.string.kind_personal,
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
        )
    }
}
