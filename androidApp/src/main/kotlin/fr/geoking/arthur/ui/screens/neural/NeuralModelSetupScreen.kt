package fr.geoking.arthur.ui.screens.neural

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.engines.ModelDownloadState
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralModelSetupScreen(
    packId: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    downloader: NeuralModelDownloader? = remember {
        runCatching { GlobalContext.get().get<NeuralModelDownloader>() }.getOrNull()
    },
) {
    val pack = remember(packId) { neuralAudioPackFromId(packId) }
    val engineType = pack?.engineType
    val scope = rememberCoroutineScope()
    val downloadStates by (
        downloader?.downloadStates
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap()) }
        ).collectAsState()
    val modelState = engineType?.let { downloadStates[it] } ?: ModelDownloadState.Idle
    val ready = modelState is ModelDownloadState.Ready

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("neural_model_setup_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.neural_model_setup_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("neural_model_setup_back"),
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
            Text(
                text = pack?.displayName ?: packId,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("neural_model_setup_engine"),
            )
            Text(
                text = stringResource(R.string.neural_model_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = modelState) {
                is ModelDownloadState.Downloading -> {
                    Text(
                        text = stringResource(
                            R.string.neural_model_downloading_progress,
                            state.progressPercent,
                            formatBytes(state.bytesDownloaded),
                            formatBytes(state.totalBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("neural_model_setup_progress_label"),
                    )
                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("neural_model_setup_progress"),
                    )
                }
                is ModelDownloadState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("neural_model_setup_error"),
                    )
                    TextButton(
                        onClick = {
                            engineType?.let { type ->
                                downloader?.let { d -> scope.launch { d.downloadModel(type) } }
                            }
                        },
                        modifier = Modifier.testTag("neural_model_setup_retry"),
                    ) {
                        Text(stringResource(R.string.neural_model_setup_retry))
                    }
                }
                is ModelDownloadState.Ready -> {
                    Text(
                        text = stringResource(R.string.neural_model_ready),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("neural_model_setup_ready"),
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.neural_model_not_downloaded),
                        modifier = Modifier.testTag("neural_model_setup_idle"),
                    )
                    val sizeHint = engineType?.let { downloader?.getModelSizeBytes(it) }
                    if (sizeHint != null && sizeHint > 0) {
                        Text(
                            text = stringResource(
                                R.string.neural_model_setup_size,
                                formatBytes(sizeHint),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Button(
                        onClick = {
                            engineType?.let { type ->
                                downloader?.let { d -> scope.launch { d.downloadModel(type) } }
                            }
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .testTag("neural_model_setup_download"),
                    ) {
                        Text(stringResource(R.string.neural_model_download))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onContinue,
                enabled = ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("neural_model_setup_continue"),
            ) {
                Text(stringResource(R.string.neural_model_setup_continue))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
