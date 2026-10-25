package fr.geoking.arthur.ui.screens.neural

import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.audio.NeuralPromptCatalog
import fr.geoking.arthur.shared.neural.NeuralTakeLibrary
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.applySoundPack
import java.io.File

@Composable
fun NeuralResultScreen(
    packId: String,
    takeId: String,
    takeLibrary: NeuralTakeLibrary,
    onVariant: (promptId: String, durationSec: Int) -> Unit,
    onNewTrack: () -> Unit,
    onSwapTake: (otherTakeId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    audioSettings: AmbientAudioSettings? = null,
) {
    val pack = remember(packId) { neuralAudioPackFromId(packId) }
    val take = remember(takeId, takeLibrary) { takeLibrary.get(takeId) }
    val prompt = remember(take) { take?.let { NeuralPromptCatalog.promptById(it.promptId) } }
    val siblingTakes = remember(packId, takeId, takeLibrary) {
        takeLibrary.listForPack(packId).filter { it.id != takeId }
    }
    val otherTake = siblingTakes.firstOrNull()

    var isPlaying by remember(takeId) { mutableStateOf(true) }
    val mediaPlayer = remember(takeId) {
        val path = take?.localPath ?: return@remember null
        val file = File(path)
        if (!file.exists()) return@remember null
        runCatching {
            MediaPlayer().apply {
                setDataSource(path)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    DisposableEffect(isPlaying, mediaPlayer) {
        if (mediaPlayer != null) {
            if (isPlaying && !mediaPlayer.isPlaying) {
                runCatching { mediaPlayer.start() }
            } else if (!isPlaying && mediaPlayer.isPlaying) {
                runCatching { mediaPlayer.pause() }
            }
        }
        onDispose { }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("neural_result_screen"),
    ) {
        if (pack != null) {
            Image(
                painter = painterResource(pack.coverRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("neural_result_back"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = prompt?.title ?: take?.promptId.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.testTag("neural_result_title"),
            )
            Text(
                text = stringResource(
                    R.string.neural_take_meta,
                    take?.durationSec ?: 0,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )

            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                    .testTag("neural_result_play_pause"),
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                    ),
                    contentDescription = if (isPlaying) {
                        stringResource(R.string.car_pause)
                    } else {
                        stringResource(R.string.car_play)
                    },
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (otherTake != null) {
                    TextButton(
                        onClick = { onSwapTake(otherTake.id) },
                        modifier = Modifier.testTag("neural_result_ab"),
                    ) {
                        Text(
                            text = stringResource(R.string.neural_result_other_take),
                            color = Color.White,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        val t = take ?: return@TextButton
                        onVariant(t.promptId, t.durationSec)
                    },
                    modifier = Modifier.testTag("neural_result_variant"),
                ) {
                    Text(
                        text = stringResource(R.string.neural_result_variant),
                        color = Color.White,
                    )
                }
            }

            TextButton(
                onClick = {
                    val topicSuffix = when (pack) {
                        fr.geoking.arthur.shared.audio.NeuralAudioPack.ATMOSPHERES_NATURE ->
                            "atmospheres_nature"
                        fr.geoking.arthur.shared.audio.NeuralAudioPack.MUSEUMS_CLASSICAL ->
                            "museums_classical"
                        fr.geoking.arthur.shared.audio.NeuralAudioPack.GENART_DIGITAL ->
                            "genart_digital"
                        fr.geoking.arthur.shared.audio.NeuralAudioPack.EXECUTORCH_MIDI ->
                            "executorch"
                        null -> return@TextButton
                    }
                    audioSettings?.let {
                        PackSelection(PackFamily.Neural, topicSuffix).applySoundPack(it)
                    }
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("neural_result_use_ambient"),
            ) {
                Text(
                    text = stringResource(R.string.sound_player_use_in_ambient),
                    color = Color.White,
                )
            }

            Button(
                onClick = onNewTrack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
                    .testTag("neural_result_new"),
            ) {
                Text(stringResource(R.string.neural_hub_new_track))
            }
        }
    }
}
