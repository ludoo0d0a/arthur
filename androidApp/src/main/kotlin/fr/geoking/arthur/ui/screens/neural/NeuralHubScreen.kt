package fr.geoking.arthur.ui.screens.neural

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.shared.neural.NeuralTake
import fr.geoking.arthur.shared.neural.NeuralTakeLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralHubScreen(
    packId: String,
    takeLibrary: NeuralTakeLibrary,
    onOpenCreate: () -> Unit,
    onOpenTake: (takeId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    refreshKey: Int = 0,
) {
    val pack = remember(packId) { neuralAudioPackFromId(packId) }
    val takes = remember(packId, takeLibrary, refreshKey) { takeLibrary.listForPack(packId) }

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("neural_hub_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pack?.let { stringResource(it.titleRes()) }
                            ?: stringResource(R.string.pack_neural),
                        modifier = Modifier.testTag("neural_hub_title"),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("neural_hub_back"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            if (takes.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.neural_hub_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("neural_hub_empty"),
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("neural_hub_list"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(takes, key = { it.id }) { take ->
                        NeuralTakeRow(
                            take = take,
                            onClick = { onOpenTake(take.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("neural_hub_new"),
            ) {
                Text(stringResource(R.string.neural_hub_new_track))
            }
        }
    }
}

@Composable
private fun NeuralTakeRow(
    take: NeuralTake,
    onClick: () -> Unit,
) {
    val promptTitle = NeuralPromptCatalog.promptById(take.promptId)?.title
        ?: take.promptId
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("neural_hub_take_${take.id}"),
    ) {
        Text(
            text = promptTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.neural_take_meta, take.durationSec),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
