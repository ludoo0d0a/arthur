package fr.geoking.arthur.ui.screens.neural

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.engines.NeuralMusicPlayer
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.shared.neural.NeuralTake
import fr.geoking.arthur.shared.neural.NeuralTakeLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.util.UUID

@Composable
fun NeuralGeneratingScreen(
    packId: String,
    promptId: String,
    durationSec: Int,
    takeLibrary: NeuralTakeLibrary,
    onCompleted: (takeId: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    player: NeuralMusicPlayer? = remember {
        runCatching { GlobalContext.get().get<NeuralMusicPlayer>() }.getOrNull()
    },
) {
    val pack = remember(packId) { neuralAudioPackFromId(packId) }
    val prompt = remember(promptId) { NeuralPromptCatalog.promptById(promptId) }
    var progress by remember { mutableFloatStateOf(0.05f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cancelled by remember { mutableStateOf(false) }

    BackHandler {
        cancelled = true
        onCancel()
    }

    LaunchedEffect(packId, promptId, durationSec) {
        if (pack == null || prompt == null || player == null) {
            errorMessage = "unavailable"
            return@LaunchedEffect
        }
        val ticker: Job = launch {
            var p = 0.05f
            while (p < 0.92f && isActive) {
                delay(400)
                p = (p + 0.04f).coerceAtMost(0.92f)
                progress = p
            }
        }
        try {
            val file = withContext(Dispatchers.IO) {
                player.getEngineForPack(pack).generateTrack(
                    prompt = prompt,
                    durationSeconds = durationSec.coerceIn(30, 120),
                )
            }
            ticker.cancel()
            if (cancelled || !isActive) return@LaunchedEffect
            if (file == null || !file.exists()) {
                errorMessage = "failed"
                progress = 0f
                return@LaunchedEffect
            }
            progress = 1f
            val take = NeuralTake(
                id = UUID.randomUUID().toString(),
                packId = packId,
                promptId = promptId,
                durationSec = durationSec,
                createdAtEpochMs = System.currentTimeMillis(),
                localPath = file.absolutePath,
            )
            takeLibrary.save(take)
            onCompleted(take.id)
        } catch (e: CancellationException) {
            ticker.cancel()
            throw e
        } catch (_: Exception) {
            ticker.cancel()
            if (!cancelled) errorMessage = "failed"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("neural_generating_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (errorMessage == null) {
            CircularProgressIndicator(modifier = Modifier.testTag("neural_generating_spinner"))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = prompt?.title ?: promptId,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("neural_generating_title"),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("neural_generating_progress"),
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = {
                    cancelled = true
                    onCancel()
                },
                modifier = Modifier.testTag("neural_generating_cancel"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        } else {
            Text(
                text = stringResource(R.string.neural_generating_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("neural_generating_error"),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.testTag("neural_generating_back"),
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
    }
}
