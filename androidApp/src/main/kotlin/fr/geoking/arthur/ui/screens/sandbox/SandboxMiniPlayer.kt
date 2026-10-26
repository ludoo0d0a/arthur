package fr.geoking.arthur.ui.screens.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.geoking.arthur.R

data class StemTrackState(
    val id: String,
    val name: String,
    val isMuted: Boolean = false,
    val volume: Float = 0.8f,
)

data class GeneratedResult(
    val id: String,
    val prompt: String,
    val engineName: String,
    val format: MusicOutputFormat,
    val durationSeconds: Int,
    val filePath: String,
    val fileSizeMb: Float,
    val stems: List<StemTrackState> = emptyList(),
)

@Composable
fun SandboxMiniPlayer(
    result: GeneratedResult,
    isPlaying: Boolean,
    playbackPositionSec: Float,
    onPlayPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    onStemMuteToggle: (stemId: String) -> Unit = {},
    onStemVolumeChange: (stemId: String, volume: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var isLooping by remember { mutableStateOf(true) }
    var masterVolume by remember { mutableFloatStateOf(0.9f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sandbox_mini_player"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Mini Player POC",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = result.prompt,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = result.format.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("sandbox_mini_player_format_badge"),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Details subtitle
            Text(
                text = "${result.engineName} · ${result.durationSeconds}s · ${String.format("%.1f MB", result.fileSizeMb)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("sandbox_mini_player_details"),
            )

            Spacer(Modifier.height(12.dp))

            // Seek bar and timestamps
            val maxSec = result.durationSeconds.toFloat().coerceAtLeast(1f)
            val currentClamped = playbackPositionSec.coerceIn(0f, maxSec)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(currentClamped),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("sandbox_mini_player_current_time"),
                )
                Slider(
                    value = currentClamped,
                    onValueChange = onSeek,
                    valueRange = 0f..maxSec,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("sandbox_mini_player_seekbar"),
                )
                Text(
                    text = formatTime(maxSec),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("sandbox_mini_player_total_time"),
                )
            }

            // Controls Row (Play/Pause, Stop, Loop, Master Volume)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("sandbox_mini_player_play_pause"),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.testTag("sandbox_mini_player_stop"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = { isLooping = !isLooping },
                        modifier = Modifier.testTag("sandbox_mini_player_loop"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Toggle Loop",
                            tint = if (isLooping) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                    }
                }

                // Volume slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(130.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_car_sound_on),
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Slider(
                        value = masterVolume,
                        onValueChange = { masterVolume = it },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("sandbox_mini_player_volume"),
                    )
                }
            }

            // Stems Multi-track Mixer Section (shown if format == STEMS or stems list non-empty)
            if (result.format == MusicOutputFormat.MULTI_TRACK_STEMS || result.stems.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Stem Mixer (Feasibility Control)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("sandbox_mini_player_stem_mixer_title"),
                )
                Spacer(Modifier.height(8.dp))

                result.stems.forEach { stem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("sandbox_stem_row_${stem.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stem.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(100.dp),
                            )
                            Slider(
                                value = if (stem.isMuted) 0f else stem.volume,
                                onValueChange = { vol -> onStemVolumeChange(stem.id, vol) },
                                enabled = !stem.isMuted,
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("sandbox_stem_slider_${stem.id}"),
                            )
                        }

                        OutlinedButton(
                            onClick = { onStemMuteToggle(stem.id) },
                            modifier = Modifier.testTag("sandbox_stem_mute_${stem.id}"),
                        ) {
                            Text(
                                text = if (stem.isMuted) "Unmute" else "Mute",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (stem.isMuted) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}
