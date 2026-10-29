package fr.geoking.arthur.ui.screens.sandbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.remote.DownloadedMusicManager
import fr.geoking.arthur.audio.remote.DownloadedMusicTrack
import fr.geoking.arthur.audio.remote.MusicDownloadProgress
import fr.geoking.arthur.audio.remote.MusicDownloadService
import fr.geoking.arthur.audio.remote.MusicDownloadTracker
import fr.geoking.arthur.shared.audio.remote.MusicProviderFactory
import fr.geoking.arthur.shared.audio.remote.RemoteAudioSource
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.ui.screens.verticalScrollbar
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoyaltyFreeMusicScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadedManager = remember { DownloadedMusicManager(context) }
    val httpClient = remember {
        runCatching { GlobalContext.get().get<HttpClient>() }.getOrNull()
    }
    val errorLogger = remember {
        runCatching { GlobalContext.get().get<ErrorLogger>() }.getOrNull()
    }

    var query by remember { mutableStateOf("zen calm ambient") }
    var selectedProviderId by remember { mutableStateOf("all") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<RemoteAudioTrack>>(emptyList()) }
    var providerErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var downloadedTracks by remember { mutableStateOf(downloadedManager.getDownloadedTracks()) }
    val downloadStates by MusicDownloadTracker.states.collectAsState()
    var pendingDownloadTrack by remember { mutableStateOf<RemoteAudioTrack?>(null) }
    val listState = rememberLazyListState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Download proceeds regardless — notification is best-effort on API 33+.
        pendingDownloadTrack?.let { track ->
            MusicDownloadService.enqueue(context, track)
            pendingDownloadTrack = null
        }
    }

    fun enqueueDownload(track: RemoteAudioTrack) {
        val needsNotificationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            pendingDownloadTrack = track
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            MusicDownloadService.enqueue(context, track)
        }
    }

    // ExoPlayer for sample preview audio
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var playingUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(downloadStates) {
        val completedIds = downloadStates.filterValues { it is MusicDownloadProgress.Completed }.keys
        if (completedIds.isNotEmpty()) {
            downloadedTracks = downloadedManager.getDownloadedTracks()
        }
    }

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

    val factory = remember(httpClient) {
        MusicProviderFactory(
            httpGet = { urlStr ->
                val client = httpClient
                    ?: error("HttpClient unavailable — open debug bar after app restart")
                val sourceId = sourceIdForMusicUrl(urlStr)
                val response = client.get(urlStr) {
                    header("User-Agent", "Arthur-Ambient/1.0 (fr.geoking.arthur)")
                    header("X-Source-Id", sourceId)
                }
                val body = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    error("HTTP ${response.status.value}: ${body.take(160)}")
                }
                body
            },
            jamendoClientId = BuildConfig.JAMENDO_CLIENT_ID,
            freesoundApiKey = BuildConfig.FREESOUND_API_KEY,
            pixabayApiKey = BuildConfig.PIXABAY_API_KEY,
        )
    }
    val missingApiKeys = remember(factory) { factory.missingApiKeys() }

    fun executeSearch() {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            searchResults = emptyList()
            providerErrors = emptyList()
            val providersToQuery: List<RemoteAudioSource> = if (selectedProviderId == "all") {
                factory.createAll()
            } else {
                listOfNotNull(factory.provider(selectedProviderId))
            }

            val tracks = mutableListOf<RemoteAudioTrack>()
            val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                providersToQuery.forEach { provider ->
                    provider.fetchZenTracks(query, limit = 10).fold(
                        onSuccess = { tracks.addAll(it) },
                        onFailure = { err ->
                            val msg = err.message ?: err::class.simpleName ?: "error"
                            errors += "${provider.displayName}: $msg"
                            errorLogger?.log(
                                sourceId = provider.id,
                                category = ErrorClassifier.classify(null, err),
                                message = "Music search failed: ${provider.displayName}",
                                details = msg,
                                throwable = err,
                            )
                        },
                    )
                }
            }
            // Best-effort file size for tracks that did not expose one in API metadata.
            val withSizes = withContext(Dispatchers.IO) {
                enrichFileSizes(tracks, downloadedManager)
            }
            searchResults = withSizes
            providerErrors = errors
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
            if (missingApiKeys.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            R.string.dev_music_missing_api_keys,
                            missingApiKeys.joinToString(", "),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (providerErrors.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.dev_music_provider_errors),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        providerErrors.forEach { msg ->
                            Text(
                                text = "• $msg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(
                        listState,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    ),
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
                        val isDownloaded = downloadedManager.isDownloaded(track.id) ||
                            downloadStates[track.id] is MusicDownloadProgress.Completed
                        val downloadProgress = downloadStates[track.id]
                        val isDownloading = downloadProgress is MusicDownloadProgress.Downloading
                        val progressFraction =
                            (downloadProgress as? MusicDownloadProgress.Downloading)?.progress
                        val isTrackPlaying = playingUrl == track.streamUrl && exoPlayer.isPlaying

                        SearchResultCard(
                            track = track,
                            isPlaying = isTrackPlaying,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            downloadProgress = progressFraction,
                            onPlayPause = { playPreview(track.streamUrl) },
                            onDownload = { enqueueDownload(track) },
                        )
                    }
                }
            }
        }
    }
}

private suspend fun enrichFileSizes(
    tracks: List<RemoteAudioTrack>,
    manager: DownloadedMusicManager,
): List<RemoteAudioTrack> = coroutineScope {
    tracks.map { track ->
        async(Dispatchers.IO) {
            if (track.fileSizeBytes != null && track.fileSizeBytes!! > 0L) {
                track
            } else {
                val size = manager.probeContentLength(track.streamUrl, track.sourceId)
                if (size != null) track.copy(fileSizeBytes = size) else track
            }
        }
    }.awaitAll()
}

private fun sourceIdForMusicUrl(url: String): String = when {
    "jamendo.com" in url -> "jamendo"
    "freesound.org" in url -> "freesound"
    "archive.org" in url -> "archive_org"
    "pixabay.com" in url -> "pixabay_music"
    "incompetech.com" in url -> "incompetech"
    else -> "music_remote"
}

@Composable
private fun SearchResultCard(
    track: RemoteAudioTrack,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float? = null,
    onPlayPause: () -> Unit,
    onDownload: () -> Unit,
) {
    val context = LocalContext.current
    val sizeLabel = remember(track.fileSizeBytes) {
        track.fileSizeBytes?.takeIf { it > 0L }?.let { Formatter.formatShortFileSize(context, it) }
    }
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
                    text = buildString {
                        append(track.artist)
                        append(" · ")
                        append(track.sourceId)
                        if (sizeLabel != null) {
                            append(" · ")
                            append(sizeLabel)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isDownloading) {
                if (downloadProgress != null && downloadProgress > 0f) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
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
