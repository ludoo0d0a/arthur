package fr.geoking.arthur.audio.stem

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

enum class DirectorState {
    STOPPED,
    PLAYING,
    PAUSED
}

data class TrackPlaybackStatus(
    val trackId: String,
    val name: String,
    val category: StemCategory,
    val isPlaying: Boolean,
    val volume: Float
)

/**
 * MusicDirector manages synchronized multi-track Opus stem playback via Media3 ExoPlayers.
 * Generates continuous local dynamic ambient music arrangements without large model dependencies.
 */
class MusicDirector(
    private val context: Context,
    private val arrangementLoader: (Context) -> StemArrangement = { StemArrangementLoader.loadFromAssets(it) },
    private val playerFactory: (Context) -> Player = { ExoPlayer.Builder(it).build() }
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var currentArrangement: StemArrangement? = null
    private val players = mutableMapOf<String, Player>()
    private val trackVolumes = mutableMapOf<String, Float>()

    private val _directorState = MutableStateFlow(DirectorState.STOPPED)
    val directorState: StateFlow<DirectorState> = _directorState.asStateFlow()

    private val _trackStatuses = MutableStateFlow<List<TrackPlaybackStatus>>(emptyList())
    val trackStatuses: StateFlow<List<TrackPlaybackStatus>> = _trackStatuses.asStateFlow()

    private var generativeLoopJob: Job? = null

    fun loadArrangement(arrangement: StemArrangement) {
        stop()
        currentArrangement = arrangement
        setupPlayers(arrangement)
    }

    fun loadDefaultArrangement() {
        try {
            val arrangement = arrangementLoader(context)
            loadArrangement(arrangement)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupPlayers(arrangement: StemArrangement) {
        releasePlayers()

        for (track in arrangement.tracks) {
            val player = playerFactory(context)
            val assetUri = Uri.parse("asset:///stems/${track.filename}")
            val mediaItem = MediaItem.fromUri(assetUri)
            player.setMediaItem(mediaItem)
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.volume = track.defaultGain
            player.prepare()

            players[track.id] = player
            trackVolumes[track.id] = track.defaultGain
        }
        updateStatuses()
    }

    fun play(randomSeed: Long = System.currentTimeMillis()) {
        val arrangement = currentArrangement ?: run {
            loadDefaultArrangement()
            currentArrangement ?: return
        }

        val random = Random(randomSeed)
        _directorState.value = DirectorState.PLAYING

        // Perform initial dynamic stem selection based on probabilities
        for (track in arrangement.tracks) {
            val player = players[track.id] ?: continue
            val shouldActivate = !track.isOptional || random.nextFloat() <= track.activationProbability
            if (shouldActivate) {
                player.volume = track.defaultGain
                trackVolumes[track.id] = track.defaultGain
            } else {
                player.volume = 0.0f
                trackVolumes[track.id] = 0.0f
            }
        }

        // Synchronize and start all track players simultaneously
        players.values.forEach { player ->
            player.seekTo(0)
            player.play()
        }

        startGenerativeLoop(arrangement.loopDurationSeconds)
        updateStatuses()
    }

    private fun startGenerativeLoop(loopDurationSec: Double) {
        generativeLoopJob?.cancel()
        generativeLoopJob = scope.launch {
            val loopMs = (loopDurationSec * 1000).toLong().coerceAtLeast(1000L)
            while (_directorState.value == DirectorState.PLAYING) {
                delay(loopMs)
                // On each loop iteration boundary, randomly mutate dynamic optional stem layers
                mutateDynamicStems()
            }
        }
    }

    private fun mutateDynamicStems() {
        val arrangement = currentArrangement ?: return
        val random = Random(System.currentTimeMillis())

        for (track in arrangement.tracks) {
            if (track.isOptional) {
                val player = players[track.id] ?: continue
                val active = random.nextFloat() <= track.activationProbability
                val targetVol = if (active) track.defaultGain else 0.0f

                // Smoothly adjust volume
                trackVolumes[track.id] = targetVol
                player.volume = targetVol
            }
        }
        updateStatuses()
    }

    fun pause() {
        if (_directorState.value == DirectorState.PLAYING) {
            _directorState.value = DirectorState.PAUSED
            generativeLoopJob?.cancel()
            players.values.forEach { it.pause() }
            updateStatuses()
        }
    }

    fun resume() {
        if (_directorState.value == DirectorState.PAUSED) {
            _directorState.value = DirectorState.PLAYING
            players.values.forEach { it.play() }
            currentArrangement?.let { startGenerativeLoop(it.loopDurationSeconds) }
            updateStatuses()
        }
    }

    fun stop() {
        _directorState.value = DirectorState.STOPPED
        generativeLoopJob?.cancel()
        players.values.forEach { player ->
            player.pause()
            player.seekTo(0)
        }
        updateStatuses()
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        val player = players[trackId] ?: return
        val clamped = volume.coerceIn(0.0f, 1.0f)
        player.volume = clamped
        trackVolumes[trackId] = clamped
        updateStatuses()
    }

    private fun updateStatuses() {
        val arrangement = currentArrangement ?: return
        val isPlaying = _directorState.value == DirectorState.PLAYING
        val list = arrangement.tracks.map { track ->
            val vol = trackVolumes[track.id] ?: track.defaultGain
            TrackPlaybackStatus(
                trackId = track.id,
                name = track.name,
                category = track.category,
                isPlaying = isPlaying && vol > 0f,
                volume = vol
            )
        }
        _trackStatuses.value = list
    }

    fun release() {
        stop()
        releasePlayers()
    }

    private fun releasePlayers() {
        players.values.forEach { it.release() }
        players.clear()
        trackVolumes.clear()
    }
}
