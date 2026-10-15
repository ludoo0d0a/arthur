package fr.geoking.arthur.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.rememberArtworkImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compact list item for full-screen Genart list with preview icon thumbnail.
 */
@Composable
fun GenartListItem(
    artwork: Artwork,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selectOnFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    imageCache: ArtworkImageCache = rememberArtworkImageCache(),
) {
    val scheme = MaterialTheme.colorScheme
    val visual = artwork.kind.visual()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(focused, artwork.id) {
        if (selectOnFocus && focused) currentOnClick()
    }

    var previewBitmap by remember(artwork.id) {
        mutableStateOf(
            imageCache.localPathOrNull(artwork.id)?.let { path ->
                BitmapFactory.decodeFile(path)
            },
        )
    }

    LaunchedEffect(artwork.id) {
        if (previewBitmap == null) {
            withContext(Dispatchers.IO) {
                val file = imageCache.imageFile(artwork.id)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        previewBitmap = bmp
                    }
                }
            }
        }
    }

    val containerTarget = when {
        focused -> scheme.primary
        selected -> scheme.secondary
        else -> scheme.surfaceContainerHigh
    }
    val titleTarget = when {
        focused -> scheme.onPrimary
        selected -> scheme.onSecondary
        else -> scheme.onSurface
    }
    val metaTarget = when {
        focused -> scheme.onPrimary.copy(alpha = 0.82f)
        selected -> scheme.onSecondary.copy(alpha = 0.85f)
        else -> scheme.onSurfaceVariant
    }
    val borderColor: Color? = when {
        focused -> scheme.secondary
        selected -> scheme.primary.copy(alpha = 0.9f)
        else -> null
    }

    val container by animateColorAsState(containerTarget, label = "genart_item_container")
    val titleColor by animateColorAsState(titleTarget, label = "genart_item_title")
    val metaColor by animateColorAsState(metaTarget, label = "genart_item_meta")

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag("genart_item_${artwork.id}")
            .semantics {
                this.selected = selected
                role = Role.Button
            },
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                focused -> 6.dp
                selected -> 3.dp
                else -> 0.dp
            },
        ),
        border = borderColor?.let { BorderStroke(if (focused) 2.dp else 1.dp, it) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        when {
                            focused -> scheme.onPrimary.copy(alpha = 0.18f)
                            selected -> scheme.onSecondary.copy(alpha = 0.18f)
                            else -> visual.container
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val currentBmp = previewBitmap
                if (currentBmp != null) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(visual.iconRes),
                        contentDescription = null,
                        tint = when {
                            focused -> scheme.onPrimary
                            selected -> scheme.onSecondary
                            else -> visual.onContainer
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displaySubtitle = subtitle ?: artwork.genartCategorySubtitle() ?: buildString {
                    append(stringResource(visual.labelRes))
                    if (artwork.attribution.isNotBlank()) {
                        append(" · ")
                        append(artwork.attribution)
                    }
                }
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected || focused) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected_artwork),
                    tint = titleColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
