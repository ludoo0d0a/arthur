package fr.geoking.arthur.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.core.context.GlobalContext

/** Koin singleton when available; falls back for previews / tests without Koin. */
@Composable
fun rememberArtworkImageCache(): ArtworkImageCache {
    val context = LocalContext.current
    return remember(context) {
        runCatching { GlobalContext.get().get<ArtworkImageCache>() }
            .getOrElse { ArtworkImageCache(context) }
    }
}
