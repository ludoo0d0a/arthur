package fr.geoking.arthur.ui.screens.neural

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.engines.ModelDownloadState
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import org.koin.core.context.GlobalContext

private val DurationChoices = listOf(30, 60, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralCreateScreen(
    packId: String,
    onGenerate: (promptId: String, durationSec: Int, modelReady: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    downloader: NeuralModelDownloader? = remember {
        runCatching { GlobalContext.get().get<NeuralModelDownloader>() }.getOrNull()
    },
) {
    val pack = remember(packId) { neuralAudioPackFromId(packId) }
    val prompts = remember(pack) {
        pack?.let { NeuralPromptCatalog.promptsForPack(it) }.orEmpty()
    }
    var selectedPromptId by remember(prompts) {
        mutableStateOf(prompts.firstOrNull()?.id.orEmpty())
    }
    var durationSec by remember { mutableIntStateOf(120) }

    val downloadStates by (
        downloader?.downloadStates
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap()) }
        ).collectAsState()
    val engineType = pack?.engineType
    val modelState = engineType?.let { downloadStates[it] } ?: ModelDownloadState.Idle
    val modelReady = modelState is ModelDownloadState.Ready

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("neural_create_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.neural_create_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("neural_create_back"),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = when (modelState) {
                    is ModelDownloadState.Ready -> stringResource(R.string.neural_model_ready)
                    is ModelDownloadState.Downloading ->
                        stringResource(R.string.neural_model_downloading, modelState.progressPercent)
                    else -> stringResource(R.string.neural_model_not_downloaded)
                },
                style = MaterialTheme.typography.labelLarge,
                color = when (modelState) {
                    is ModelDownloadState.Ready -> MaterialTheme.colorScheme.primary
                    is ModelDownloadState.Downloading -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("neural_create_model_status"),
            )

            Text(
                text = stringResource(R.string.neural_create_prompt_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState())
                    .testTag("neural_create_prompts"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                prompts.forEach { prompt ->
                    FilterChip(
                        selected = selectedPromptId == prompt.id,
                        onClick = { selectedPromptId = prompt.id },
                        label = { Text(prompt.title) },
                        modifier = Modifier.testTag("neural_create_prompt_${prompt.id}"),
                    )
                }
            }

            val selectedPrompt = prompts.firstOrNull { it.id == selectedPromptId }
            if (selectedPrompt != null) {
                Text(
                    text = selectedPrompt.promptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .testTag("neural_create_prompt_text"),
                )
            }

            Text(
                text = stringResource(R.string.neural_create_duration_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("neural_create_durations"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DurationChoices.forEach { sec ->
                    FilterChip(
                        selected = durationSec == sec,
                        onClick = { durationSec = sec },
                        label = { Text(stringResource(R.string.neural_duration_seconds, sec)) },
                        modifier = Modifier.testTag("neural_create_duration_$sec"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    if (selectedPromptId.isNotEmpty()) {
                        onGenerate(selectedPromptId, durationSec, modelReady)
                    }
                },
                enabled = selectedPromptId.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("neural_create_generate"),
            ) {
                Text(stringResource(R.string.neural_create_generate))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
