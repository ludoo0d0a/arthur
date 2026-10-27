package fr.geoking.arthur.ui.screens.sandbox

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.engines.ModelDownloadState
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.shared.audio.NeuralEngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.util.UUID

enum class MusicOutputFormat(val displayName: String) {
    FULL_TRACK("Full Track (WAV / AAC)"),
    MULTI_TRACK_STEMS("Multi-track Stems (Opus)"),
    MIDI_SOUNDFONT("MIDI + SoundFont"),
    PROCEDURAL_DSP("Procedural DSP Sound"),
}

data class SandboxModelOption(
    val id: String,
    val name: String,
    val sizeLabel: String,
    val engineType: NeuralEngineType?,
)

val SandboxModelList = listOf(
    SandboxModelOption("musicgen", "MusicGen Small", "2.2 GB", NeuralEngineType.MUSICGEN_SMALL),
    SandboxModelOption("soundgen", "SoundGen", "3.4 GB", NeuralEngineType.SOUNDGEN),
    SandboxModelOption("stable_audio", "Stable Audio 3", "4.5 GB", NeuralEngineType.STABLE_AUDIO_3_SMALL),
    SandboxModelOption("executorch", "ExecuTorch MIDI", "~141 MB", NeuralEngineType.EXECUTORCH_MIDI),
    SandboxModelOption("custom_llm", "Custom Music LLM (POC)", "Local Model", null),
)

val PresetPrompts = listOf(
    "Atmospheric Forest Rain with Soft Piano",
    "Lush Baroque String Quartet Adagio",
    "Generative Multi-track Ambient Stems",
    "Cyberpunk Synthwave 120 BPM",
    "Tibetan Singing Bowl & Crystal Chimes",
    "Lo-Fi Chill Hop with Soft Chords",
)

val DurationChoices = listOf(15, 30, 60, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLlmSandboxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    downloader: NeuralModelDownloader? = remember {
        runCatching { GlobalContext.get().get<NeuralModelDownloader>() }.getOrNull()
    },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Default to Custom Music LLM so Generate works offline without a multi‑GB download.
    var selectedModel by remember {
        mutableStateOf(SandboxModelList.first { it.id == "custom_llm" })
    }
    var promptText by remember { mutableStateOf("Atmospheric Forest Rain with Soft Piano") }
    var selectedFormat by remember { mutableStateOf(MusicOutputFormat.FULL_TRACK) }
    var selectedDurationSec by remember { mutableIntStateOf(30) }

    val downloadStates by (
        downloader?.downloadStates
            ?: remember { MutableStateFlow(emptyMap()) }
        ).collectAsState()

    val engineType = selectedModel.engineType
    val currentModelState = engineType?.let { downloadStates[it] } ?: ModelDownloadState.Ready(
        engineType ?: NeuralEngineType.MUSICGEN_SMALL,
        java.io.File("/tmp/dummy"),
    )
    val isModelReady = currentModelState is ModelDownloadState.Ready || selectedModel.engineType == null

    val generator = remember(downloader) {
        downloader?.let { SandboxAudioGenerator(context, it) }
    }

    // Generation state
    var isGenerating by remember { mutableStateOf(false) }
    var generationStage by remember { mutableStateOf("") }
    var generationProgress by remember { mutableFloatStateOf(0f) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var generatedResult by remember { mutableStateOf<GeneratedResult?>(null) }

    // Mini Player State
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPositionSec by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("music_llm_sandbox_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Music LLM Generation Sandbox (POC)") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("music_sandbox_back"),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Feasibility testing sandbox for downloading LLM/neural music models, custom prompt tokenization, stem arrangement generation, and interactive mini player testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("sandbox_description"),
            )

            // 1. Model Selection & Downloader Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_model_section"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. AI Engine / LLM Weights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("sandbox_model_chips"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SandboxModelList.forEach { model ->
                            FilterChip(
                                selected = selectedModel.id == model.id,
                                onClick = { selectedModel = model },
                                label = { Text("${model.name} (${model.sizeLabel})") },
                                modifier = Modifier.testTag("sandbox_model_chip_${model.id}"),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Model Download Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Model Status: " + when (currentModelState) {
                                    is ModelDownloadState.Ready -> "Ready for inference"
                                    is ModelDownloadState.Downloading ->
                                        "Downloading (${(currentModelState as ModelDownloadState.Downloading).progressPercent}%)"
                                    is ModelDownloadState.Error -> "Download Error"
                                    is ModelDownloadState.Idle -> "Not Downloaded"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (currentModelState) {
                                    is ModelDownloadState.Ready -> MaterialTheme.colorScheme.primary
                                    is ModelDownloadState.Downloading -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.testTag("sandbox_model_status_text"),
                            )
                        }

                        if (engineType != null) {
                            if (currentModelState is ModelDownloadState.Ready) {
                                Button(
                                    onClick = { downloader?.deleteModel(engineType) },
                                    modifier = Modifier.testTag("sandbox_delete_model_btn"),
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Delete")
                                }
                            } else if (currentModelState is ModelDownloadState.Idle || currentModelState is ModelDownloadState.Error) {
                                Button(
                                    onClick = {
                                        downloader?.let { d ->
                                            scope.launch { d.downloadModel(engineType) }
                                        }
                                    },
                                    modifier = Modifier.testTag("sandbox_download_model_btn"),
                                ) {
                                    Text("Download Model")
                                }
                            }
                        }
                    }

                    if (currentModelState is ModelDownloadState.Downloading) {
                        val state = currentModelState as ModelDownloadState.Downloading
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (state.totalBytes > 0) state.progressPercent / 100f else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sandbox_model_download_progress"),
                        )
                    }
                }
            }

            // 2. Prompt & Parameters Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_prompt_section"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Add Prompt & Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Prompt (Music / Sound Description)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_prompt_input"),
                        singleLine = false,
                        maxLines = 3,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState())
                            .testTag("sandbox_preset_chips"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PresetPrompts.forEachIndexed { idx, preset ->
                            FilterChip(
                                selected = promptText == preset,
                                onClick = { promptText = preset },
                                label = { Text(preset) },
                                modifier = Modifier.testTag("sandbox_preset_chip_$idx"),
                            )
                        }
                    }
                }
            }

            // 3. Output Format & Duration Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_format_section"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Target Format & Duration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Output Format:",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState())
                            .testTag("sandbox_format_chips"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MusicOutputFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.displayName) },
                                modifier = Modifier.testTag("sandbox_format_chip_${fmt.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Target Duration:",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("sandbox_duration_chips"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DurationChoices.forEach { sec ->
                            FilterChip(
                                selected = selectedDurationSec == sec,
                                onClick = { selectedDurationSec = sec },
                                label = { Text("${sec}s") },
                                modifier = Modifier.testTag("sandbox_duration_chip_$sec"),
                            )
                        }
                    }
                }
            }

            // 4. Generation Button — real on-device inference (no fake path)
            Button(
                onClick = {
                    if (promptText.isBlank() || generator == null) return@Button
                    if (!isModelReady && selectedModel.engineType != null) {
                        generationError = "Download the model first"
                        return@Button
                    }
                    scope.launch {
                        isGenerating = true
                        isPlaying = false
                        playbackPositionSec = 0f
                        generationError = null
                        generatedResult = null
                        generationProgress = 0.1f
                        generationStage = "1/3 Encoding prompt..."
                        delay(150)
                        generationProgress = 0.35f
                        generationStage = "2/3 Running LLM / neural decode..."

                        val outcome = withContext(Dispatchers.IO) {
                            generator.generate(
                                model = selectedModel,
                                format = selectedFormat,
                                promptText = promptText,
                                durationSeconds = selectedDurationSec,
                            )
                        }

                        outcome.fold(
                            onSuccess = { file ->
                                generationStage = "3/3 Writing WAV..."
                                generationProgress = 1f
                                val stems = if (selectedFormat == MusicOutputFormat.MULTI_TRACK_STEMS) {
                                    listOf(
                                        StemTrackState("s1", "Melody / Lead", volume = 0.9f),
                                        StemTrackState("s2", "Harmony / Chords", volume = 0.8f),
                                        StemTrackState("s3", "Bass / Sub", volume = 0.85f),
                                        StemTrackState("s4", "Rhythm / Nature FX", volume = 0.7f),
                                    )
                                } else {
                                    emptyList()
                                }
                                generatedResult = GeneratedResult(
                                    id = UUID.randomUUID().toString(),
                                    prompt = promptText,
                                    engineName = selectedModel.name,
                                    format = selectedFormat,
                                    durationSeconds = selectedDurationSec,
                                    filePath = file.absolutePath,
                                    fileSizeMb = (file.length() / (1024f * 1024f)).coerceAtLeast(0.01f),
                                    stems = stems,
                                )
                                isPlaying = true
                            },
                            onFailure = { err ->
                                generationError = err.message ?: "Generation failed"
                                generationProgress = 0f
                            },
                        )
                        isGenerating = false
                    }
                },
                enabled = promptText.isNotBlank() && !isGenerating && generator != null &&
                    (isModelReady || selectedModel.engineType == null),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_generate_btn"),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(" Generating...")
                } else {
                    Text("Generate Music / Stems")
                }
            }

            if (isGenerating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_generation_progress_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = generationStage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { generationProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            generationError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("sandbox_generation_error"),
                )
            }

            // 5. Result Display & Mini Player (real MediaPlayer)
            val res = generatedResult
            if (res != null) {
                Spacer(Modifier.height(8.dp))
                SandboxMiniPlayer(
                    result = res,
                    isPlaying = isPlaying,
                    playbackPositionSec = playbackPositionSec,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onStop = {
                        isPlaying = false
                        playbackPositionSec = 0f
                    },
                    onSeek = { pos -> playbackPositionSec = pos },
                    onPlaybackPositionTick = { playbackPositionSec = it },
                    onPlaybackEnded = {
                        isPlaying = false
                        playbackPositionSec = 0f
                    },
                    onStemMuteToggle = { stemId ->
                        val updatedStems = res.stems.map {
                            if (it.id == stemId) it.copy(isMuted = !it.isMuted) else it
                        }
                        generatedResult = res.copy(stems = updatedStems)
                    },
                    onStemVolumeChange = { stemId, vol ->
                        val updatedStems = res.stems.map {
                            if (it.id == stemId) it.copy(volume = vol) else it
                        }
                        generatedResult = res.copy(stems = updatedStems)
                    },
                )
            }
        }
    }
}
