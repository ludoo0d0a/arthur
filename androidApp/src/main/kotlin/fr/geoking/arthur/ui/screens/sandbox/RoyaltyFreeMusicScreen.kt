package fr.geoking.arthur.ui.screens.sandbox

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.remote.DownloadedMusicManager
import fr.geoking.arthur.audio.remote.DownloadedMusicTrack
import fr.geoking.arthur.shared.audio.remote.MusicProviderFactory
import fr.geoking.arthur.shared.audio.remote.RemoteAudioSource
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoyaltyFreeMusicScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadedManager = remember { DownloadedMusicManager(context) }

    var query by remember { mutableStateOf("zen calm ambient") }
    var selectedProviderId by remember { mutableStateOf("all") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<RemoteAudioTrack>>(emptyList()) }
    var downloadedTracks by remember { mutableStateOf(downloadedManager.getDownloadedTracks()) }

    // Track download state: true = downloading, false = done
    val downloadingState = remember { mutableStateMapOf<String, Boolean>() }

    // ExoPlayer for sample preview audio
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var playingUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    fun playPreview(url: String) {
        if (playingUrl == url) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        } else {
            playingUrl = url
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    val factory = remember {
        MusicProviderFactory(
            httpGet = { urlStr ->
                withContext(Dispatchers.IO) {
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Arthur-Ambient/1.0")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 15000
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
            },
            jamendoClientId = BuildConfig.JAMENDO_CLIENT_ID,
            freesoundApiKey = BuildConfig.FREESOUND_API_KEY,
            pixabayApiKey = BuildConfig.PIXABAY_API_KEY,
            fmaApiKey = BuildConfig.FMA_API_KEY,
        )
    }

    fun executeSearch() {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            searchResults = emptyList()
            val providersToQuery: List<RemoteAudioSource> = if (selectedProviderId == "all") {
                factory.createAll()
            } else {
                listOfNotNull(factory.provider(selectedProviderId))
            }

            val tracks = mutableListOf<RemoteAudioTrack>()
            withContext(Dispatchers.IO) {
                providersToQuery.forEach { provider ->
                    provider.fetchZenTracks(query, limit = 10).getOrNull()?.let {
                        tracks.addAll(it)
                    }
                }
            }
            searchResults = tracks
            isSearching = false
        }
    }

    LaunchedEffect(Unit) {
        executeSearch()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.screen_dev_royalty_free_music),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.dev_royalty_free_music_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // Search query field & button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.dev_search_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { executeSearch() },
                    enabled = !isSearching,
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            }

            // Provider filter chips
            val providersList = listOf(
                "all" to "All Providers",
                "jamendo" to "Jamendo",
                "freesound" to "Freesound",
                "archive_org" to "Archive.org",
                "pixabay_music" to "Pixabay",
                "fma" to "FMA",
                "incompetech" to "Incompetech",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                providersList.forEach { (pId, pLabel) ->
                    FilterChip(
                        selected = selectedProviderId == pId,
                        onClick = {
                            selectedProviderId = pId
                            executeSearch()
                        },
                        label = { Text(pLabel) },
                    )
                }
            }

            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Downloaded Music Pack Section Header
                item {
                    Text(
                        text = stringResource(R.string.dev_downloaded_tracks_title, downloadedTracks.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }

                if (downloadedTracks.isEmpty()) {
                    item {
                        Text(
                            text = "No downloaded tracks in pack yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(downloadedTracks, key = { "dl-${it.id}" }) { track ->
                        DownloadedTrackRow(
                            track = track,
                            isPlaying = playingUrl == track.id,
                            onPlayPause = {
                                val file = downloadedManager.getTrackFile(track.id)
                                if (file != null) {
                                    playPreview(file.absolutePath)
                                }
                            },
                            onDelete = {
                                downloadedManager.deleteTrack(track.id)
                                downloadedTracks = downloadedManager.getDownloadedTracks()
                            },
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (searchResults.isEmpty() && !isSearching) {
                    item {
                        Text(
                            text = stringResource(R.string.dev_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(searchResults, key = { "res-${it.id}" }) { track ->
                        val isDownloaded = downloadedManager.isDownloaded(track.id)
                        val isDownloading = downloadingState[track.id] == true
                        val isTrackPlaying = playingUrl == track.streamUrl && exoPlayer.isPlaying

                        SearchResultCard(
                            track = track,
                            isPlaying = isTrackPlaying,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            onPlayPause = { playPreview(track.streamUrl) },
                            onDownload = {
                                scope.launch {
                                    downloadingState[track.id] = true
                                    downloadedManager.downloadTrack(track)
                                    downloadingState[track.id] = false
                                    downloadedTracks = downloadedManager.getDownloadedTracks()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    track: RemoteAudioTrack,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onPlayPause: () -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${track.artist} · ${track.sourceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (isDownloaded) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.dev_downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    onClick = onDownload,
                ) {
                    Text(stringResource(R.string.dev_download))
                }
            }
        }
    }
}

@Composable
private fun DownloadedTrackRow(
    track: DownloadedMusicTrack,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${track.artist} · ${track.sourceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.dev_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
