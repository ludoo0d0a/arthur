package fr.geoking.arthur.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.ui.components.StillArtworkThumbnail
import fr.geoking.arthur.ui.components.authorForDisplay
import fr.geoking.arthur.ui.components.sourceLabel
import fr.geoking.arthur.ui.components.visual

/**
 * Full-screen Artwork detail: thumbnail, title, source, author, description,
 * and other metadata including an external collection / stock link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkDetailScreen(
    artwork: Artwork,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val sourceLabel = artwork.sourceLabel()
    val author = artwork.authorForDisplay(sourceLabel)
    val kindLabel = stringResource(artwork.kind.visual().labelRes)
    val externalUrl = artwork.externalUrl?.takeIf { it.isNotBlank() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("artwork_detail_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_artwork_detail)) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("artwork_detail_back"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                    titleContentColor = scheme.onSurface,
                    navigationIconContentColor = scheme.onSurface,
                ),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                StillArtworkThumbnail(
                    artwork = artwork,
                    modifier = Modifier
                        .size(112.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("artwork_detail_thumbnail"),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onBackground,
                        modifier = Modifier.testTag("artwork_detail_title"),
                    )
                    if (sourceLabel.isNotEmpty()) {
                        DetailMetaLine(
                            label = stringResource(R.string.detail_source),
                            value = sourceLabel,
                            testTag = "artwork_detail_source",
                        )
                    }
                    if (author.isNotEmpty()) {
                        DetailMetaLine(
                            label = stringResource(R.string.detail_author),
                            value = author,
                            testTag = "artwork_detail_author",
                        )
                    }
                }
            }

            HorizontalDivider()

            DetailMetaLine(
                label = stringResource(R.string.detail_kind),
                value = kindLabel,
                testTag = "artwork_detail_kind",
            )
            if (artwork.date.isNotBlank()) {
                DetailMetaLine(
                    label = stringResource(R.string.detail_date),
                    value = artwork.date,
                    testTag = "artwork_detail_date",
                )
            }
            if (artwork.medium.isNotBlank()) {
                DetailMetaLine(
                    label = stringResource(R.string.detail_medium),
                    value = artwork.medium,
                    testTag = "artwork_detail_medium",
                )
            }
            if (artwork.license.isNotBlank()) {
                DetailMetaLine(
                    label = stringResource(R.string.detail_license),
                    value = artwork.license,
                    testTag = "artwork_detail_license",
                )
            }
            if (artwork.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = artwork.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onBackground,
                        modifier = Modifier.testTag("artwork_detail_description"),
                    )
                }
            }
            if (externalUrl != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)),
                            )
                        }
                        .padding(vertical = 8.dp)
                        .testTag("artwork_detail_link"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.detail_open_source_page),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                        Text(
                            text = externalUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
private fun DetailMetaLine(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(testTag),
        )
    }
}
