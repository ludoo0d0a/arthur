package fr.geoking.arthur.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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

/**
 * Catalog row with TV/D-pad-friendly contrast:
 * focused = primary fill (where the remote is), selected = cyan fill when focus moves away.
 */
@Composable
internal fun ArtworkCard(
    artwork: Artwork,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selectOnFocus: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val visual = artwork.kind.visual()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(focused, artwork.id) {
        if (selectOnFocus && focused) currentOnClick()
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

    val container by animateColorAsState(containerTarget, label = "card_container")
    val titleColor by animateColorAsState(titleTarget, label = "card_title")
    val metaColor by animateColorAsState(metaTarget, label = "card_meta")

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag("artwork_${artwork.id}")
            .semantics {
                this.selected = selected
                role = Role.Button
            },
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                focused -> 8.dp
                selected -> 4.dp
                else -> 0.dp
            },
        ),
        border = borderColor?.let { BorderStroke(if (focused) 3.dp else 2.dp, it) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        when {
                            focused -> scheme.onPrimary.copy(alpha = 0.18f)
                            selected -> scheme.onSecondary.copy(alpha = 0.18f)
                            else -> visual.container
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(visual.iconRes),
                    contentDescription = null,
                    tint = when {
                        focused -> scheme.onPrimary
                        selected -> scheme.onSecondary
                        else -> visual.onContainer
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.titleMedium.copy(
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
                    style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
