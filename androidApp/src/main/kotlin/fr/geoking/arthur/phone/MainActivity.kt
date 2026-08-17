package fr.geoking.arthur.phone

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.tv.AmbientActivity
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            ArthurTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlPlaneScreen(
                        contentEngine = contentEngine,
                        onStartAmbient = {
                            startActivity(AmbientActivity.intent(this))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    initialSelected: Artwork? = null,
    showFractalPreview: Boolean = true,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var selected by remember { mutableStateOf(initialSelected) }

    LaunchedEffect(contentEngine, initialCatalog) {
        if (initialCatalog == null) {
            catalog = contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            )
        }
    }

    ControlPlaneContent(
        catalog = catalog,
        selected = selected,
        onSelect = { selected = it },
        onStartAmbient = onStartAmbient,
        modifier = modifier,
        showFractalPreview = showFractalPreview,
    )
}

@Composable
fun ControlPlaneContent(
    catalog: List<Artwork>,
    selected: Artwork?,
    onSelect: (Artwork) -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    showFractalPreview: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("control_plane"),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            StartAmbientBar(
                selectedTitle = selected?.title,
                onStartAmbient = onStartAmbient,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            ControlPlaneHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            GalleryHero(
                livePreview = showFractalPreview,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(168.dp)
                    .testTag("fractal_preview"),
            )
            Text(
                text = stringResource(R.string.rotation_section),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artwork_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(catalog, key = { it.id }) { art ->
                    ArtworkCard(
                        artwork = art,
                        selected = art.id == selected?.id,
                        onClick = { onSelect(art) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlPlaneHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mark),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.control_plane_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("control_plane_title"),
            )
            Text(
                text = stringResource(R.string.control_plane_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GalleryHero(
    livePreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
    ) {
        Box {
            if (livePreview) {
                FractalEffectCanvas(isActive = true, quality = FractalQuality.Low)
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_hero_gallery),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
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
                    text = stringResource(
                        if (livePreview) R.string.fractal_hero_label else R.string.gallery_hero_label,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ArtworkCard(
    artwork: Artwork,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = artwork.kind.visual()
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "card_container",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("artwork_${artwork.id}")
            .semantics {
                this.selected = selected
                role = Role.Button
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(visual.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(visual.iconRes),
                    contentDescription = null,
                    tint = visual.onContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(stringResource(visual.labelRes))
                        if (artwork.attribution.isNotBlank()) {
                            append(" · ")
                            append(artwork.attribution)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected_artwork),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun StartAmbientBar(
    selectedTitle: String?,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(visible = selectedTitle != null) {
                Text(
                    text = stringResource(R.string.preview_label, selectedTitle.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("preview_title"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onStartAmbient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_ambient"),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_ambient),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.start_ambient),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun AmbientScreenContent(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .testTag("ambient_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mark),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.ambient_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("ambient_title"),
            )
        }
    }
}

@Preview(showBackground = true, name = "Control plane")
@Composable
private fun ControlPlanePreview() {
    val catalog = BundledPackSource.defaultPack()
    ArthurTheme {
        ControlPlaneContent(
            catalog = catalog,
            selected = catalog.firstOrNull(),
            onSelect = {},
            onStartAmbient = {},
            showFractalPreview = false,
        )
    }
}
