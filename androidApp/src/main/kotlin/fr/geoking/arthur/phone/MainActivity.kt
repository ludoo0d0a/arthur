package fr.geoking.arthur.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
        setContent {
            MaterialTheme {
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
) {
    var catalog by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var selected by remember { mutableStateOf<Artwork?>(null) }

    LaunchedEffect(Unit) {
        catalog = contentEngine.catalog(
            PreparedRotation(
                sourceIds = listOf(BundledPackSource.ID),
                artworkIds = emptyList(),
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("control_plane"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Arthur Control Plane",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("control_plane_title"),
        )
        Text(
            text = "Configure sources and preview Artwork for Auto / TV canvases.",
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("artwork_list"),
        ) {
            items(catalog, key = { it.id }) { art ->
                Text(
                    text = "${art.title} · ${art.kind}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = art }
                        .padding(vertical = 12.dp)
                        .testTag("artwork_${art.id}"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        selected?.let {
            Text(
                text = "Preview: ${it.title}",
                modifier = Modifier.testTag("preview_title"),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Button(
            onClick = onStartAmbient,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("start_ambient"),
        ) {
            Text("Start ambient")
        }
        Spacer(Modifier.height(8.dp))
    }
}
