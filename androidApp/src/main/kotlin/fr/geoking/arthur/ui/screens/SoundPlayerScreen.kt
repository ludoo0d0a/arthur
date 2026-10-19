package fr.geoking.arthur.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.ZenAudioEngine
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.AmbientAudioCharacter
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.PackCovers
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.applySoundPack
import fr.geoking.arthur.ui.components.audioPackTopicOrNull
import fr.geoking.arthur.ui.components.labelRes
import fr.geoking.arthur.ui.components.primaryStyle
import fr.geoking.arthur.ui.components.stylesInPack
import org.koin.core.context.GlobalContext

/**
 * Dedicated listen surface for a Sound pack: full-bleed cover + procedural playback.
 * Does not start Ambient slideshow or change the artwork pool.
 * Volume is left to the system (phone/TV hardware); no in-app slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPlayerScreen(
    selection: PackSelection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    audioSettings: AmbientAudioSettings? = remember {
        runCatching { GlobalContext.get().get<AmbientAudioSettings>() }.getOrNull()
    },
) {
    val topic = selection.audioPackTopicOrNull() ?: AudioPackTopic.Essentials
    val styles = remember(topic) { topic.stylesInPack() }
    var selectedStyle by remember(topic) { mutableStateOf(topic.primaryStyle()) }
    var isPlaying by remember { mutableStateOf(true) }
    val keepPrefsOnDismiss = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    val context = LocalContext.current
    val settings = audioSettings

    // Snapshot prefs to restore unless the user chooses "Use in Ambient".
    val previousEnabled = remember(settings) { settings?.enabled?.value ?: false }
    val previousStyle = remember(settings) { settings?.stylePreference?.value }
    val previousCharacter = remember(settings) { settings?.character?.value }

    val zenAudio = remember(settings) {
        settings?.let { ZenAudioEngine(context, it) }
    }

    fun syntheticArtwork(style: MusicStyle): Artwork {
        val suffix = topic.packSuffix ?: "essentials"
        return Artwork(
            id = "sound.$suffix.${style.name}",
            title = context.getString(topic.labelRes),
            sourceId = "sound",
            kind = ArtworkKind.Photo,
        )
    }

    DisposableEffect(zenAudio, settings) {
        onDispose {
            zenAudio?.stop()
            zenAudio?.destroy()
            if (settings != null && !keepPrefsOnDismiss.get()) {
                settings.setEnabled(previousEnabled)
                settings.setStylePreference(previousStyle)
                previousCharacter?.let { settings.setCharacter(it) }
            }
        }
    }

    LaunchedEffect(settings, selectedStyle, isPlaying, zenAudio) {
        if (settings == null || zenAudio == null) return@LaunchedEffect
        settings.setEnabled(true)
        settings.setStylePreference(selectedStyle)
        when (topic) {
            AudioPackTopic.HearthWeather, AudioPackTopic.DawnChorus,
            AudioPackTopic.TempleResonance, AudioPackTopic.WindGarden,
            AudioPackTopic.CosmicDrift,
            -> settings.setCharacter(AmbientAudioCharacter.Atmosphere)
            else -> Unit
        }
        zenAudio.setArtwork(syntheticArtwork(selectedStyle))
        if (isPlaying) {
            zenAudio.start()
            zenAudio.triggerTransition()
        } else {
            zenAudio.stop(abandonFocus = false)
        }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sound_player_screen"),
    ) {
        Image(
            painter = painterResource(PackCovers.audio(topic)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
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
                onClick = onDismiss,
                modifier = Modifier.testTag("sound_player_back"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(topic.labelRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.testTag("sound_player_title"),
            )
            val neuralPack = when (topic) {
                AudioPackTopic.AtmospheresNature -> fr.geoking.arthur.shared.audio.NeuralAudioPack.ATMOSPHERES_NATURE
                AudioPackTopic.MuseumsClassical -> fr.geoking.arthur.shared.audio.NeuralAudioPack.MUSEUMS_CLASSICAL
                AudioPackTopic.GenartDigital -> fr.geoking.arthur.shared.audio.NeuralAudioPack.GENART_DIGITAL
                else -> null
            }

            if (neuralPack != null) {
                val downloader = remember {
                    runCatching { GlobalContext.get().get<fr.geoking.arthur.audio.engines.NeuralModelDownloader>() }.getOrNull()
                }
                val downloadStates by (downloader?.downloadStates ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
                val scope = rememberCoroutineScope()
                val modelState = downloadStates[neuralPack.engineType] ?: fr.geoking.arthur.audio.engines.ModelDownloadState.Idle

                val prompts = remember(neuralPack) {
                    fr.geoking.arthur.shared.audio.NeuralPromptCatalog.promptsForPack(neuralPack)
                }
                var selectedPromptIndex by remember { mutableStateOf(0) }
                val currentPrompt = prompts.getOrElse(selectedPromptIndex) { prompts.first() }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = when (modelState) {
                            is fr.geoking.arthur.audio.engines.ModelDownloadState.Ready -> stringResource(R.string.neural_model_ready)
                            is fr.geoking.arthur.audio.engines.ModelDownloadState.Downloading -> stringResource(R.string.neural_model_downloading, modelState.progressPercent)
                            else -> stringResource(R.string.neural_procedural_fallback)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (modelState) {
                            is fr.geoking.arthur.audio.engines.ModelDownloadState.Ready -> Color(0xFF81C784)
                            is fr.geoking.arthur.audio.engines.ModelDownloadState.Downloading -> Color(0xFFFFB74D)
                            else -> Color(0xFFE0E0E0)
                        },
                        modifier = Modifier.testTag("sound_player_model_status"),
                    )

                    if (modelState !is fr.geoking.arthur.audio.engines.ModelDownloadState.Ready &&
                        modelState !is fr.geoking.arthur.audio.engines.ModelDownloadState.Downloading
                    ) {
                        TextButton(
                            onClick = {
                                downloader?.let { d ->
                                    scope.launch { d.downloadModel(neuralPack.engineType) }
                                }
                            },
                            modifier = Modifier.testTag("sound_player_download_model"),
                        ) {
                            Text(
                                text = stringResource(R.string.neural_model_download),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF81D4FA),
                            )
                        }
                    }
                }

                Text(
                    text = "${currentPrompt.title} — ${currentPrompt.promptText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .testTag("sound_player_styles_line"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .horizontalScroll(rememberScrollState())
                        .testTag("sound_player_prompts_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    prompts.forEachIndexed { idx, prompt ->
                        FilterChip(
                            selected = selectedPromptIndex == idx,
                            onClick = { selectedPromptIndex = idx },
                            label = { Text(prompt.title) },
                            modifier = Modifier.testTag("sound_player_prompt_$idx"),
                        )
                    }
                }
            } else {
                val stylesLine = styles.joinToString(" · ") { style ->
                    context.getString(style.labelRes())
                }
                Text(
                    text = stylesLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .testTag("sound_player_styles_line"),
                )

                if (styles.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        styles.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(stringResource(style.labelRes())) },
                                modifier = Modifier.testTag("sound_player_style_${style.name}"),
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                    .testTag("sound_player_play_pause"),
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

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (settings != null) {
                        keepPrefsOnDismiss.set(true)
                        selection.applySoundPack(settings, styleOverride = selectedStyle)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("sound_player_use_in_ambient"),
            ) {
                Text(
                    text = stringResource(R.string.sound_player_use_in_ambient),
                    color = Color.White,
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("sound_player_done"),
            ) {
                Text(stringResource(R.string.sound_player_done))
            }
        }
    }
}
