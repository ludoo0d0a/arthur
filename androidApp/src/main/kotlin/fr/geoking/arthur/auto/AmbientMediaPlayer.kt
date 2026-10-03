package fr.geoking.arthur.auto

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import fr.geoking.arthur.shared.domain.Artwork

/**
 * Media3 [Player] that mirrors Ambient rotation as a one-item (or queue) session —
 * play/pause/skip drive ambient state; there is no ExoPlayer decode path.
 */
@OptIn(UnstableApi::class)
internal class AmbientMediaPlayer(
    looper: Looper,
    private val callbacks: Callbacks,
) : SimpleBasePlayer(looper) {

    interface Callbacks {
        fun onPlayWhenReadyChanged(playWhenReady: Boolean)
        fun onSkip(delta: Int)
        fun onPlayMediaId(mediaId: String)
    }

    private var playWhenReady: Boolean = false
    private var currentIndex: Int = 0
    private var playlist: List<MediaItemData> = emptyList()
    private var playlistTitle: String? = null

    fun publish(
        art: Artwork,
        artworkUri: Uri,
        subtitle: String,
        title: String? = null,
        description: String? = null,
        genre: String? = null,
        queue: List<Artwork> = listOf(art),
        queueUris: Map<String, Uri> = emptyMap(),
        playing: Boolean,
        durationMs: Long = C.TIME_UNSET,
        playlistTitle: String? = null,
    ) {
        this.playWhenReady = playing
        this.playlistTitle = playlistTitle
        val displayTitle = title?.takeIf { it.isNotBlank() } ?: art.title
        val items = queue.ifEmpty { listOf(art) }
        currentIndex = items.indexOfFirst { it.id == art.id }.let { if (it < 0) 0 else it }
        playlist = items.map { item ->
            val uri = if (item.id == art.id) {
                artworkUri
            } else {
                queueUris[item.id] ?: artworkUri
            }
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(if (item.id == art.id) displayTitle else item.title)
                .setArtist(if (item.id == art.id) subtitle else item.attribution)
                .setSubtitle(if (item.id == art.id) subtitle else item.attribution)
                .setArtworkUri(uri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
            if (item.id == art.id) {
                if (!description.isNullOrBlank()) metaBuilder.setDescription(description)
                if (!genre.isNullOrBlank()) metaBuilder.setGenre(genre)
            }
            val mediaItem = MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(uri)
                .setMediaMetadata(metaBuilder.build())
                .build()
            val data = MediaItemData.Builder(item.id)
                .setMediaItem(mediaItem)
            if (durationMs != C.TIME_UNSET && durationMs > 0) {
                data.setDurationUs(durationMs * 1_000L)
            }
            data.build()
        }
        invalidateState()
    }

    fun setPlaying(playing: Boolean) {
        if (playWhenReady == playing) return
        playWhenReady = playing
        invalidateState()
    }

    override fun getState(): State {
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
            )
            .build()
        val builder = State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(playWhenReady, Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setPlaybackState(
                if (playlist.isEmpty()) Player.STATE_IDLE else Player.STATE_READY,
            )
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(
                if (playlist.isEmpty()) {
                    0
                } else {
                    currentIndex.coerceIn(0, playlist.lastIndex)
                },
            )
        if (!playlistTitle.isNullOrBlank()) {
            builder.setPlaylistMetadata(
                MediaMetadata.Builder().setTitle(playlistTitle).build(),
            )
        }
        return builder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        callbacks.onPlayWhenReadyChanged(playWhenReady)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        playWhenReady = false
        callbacks.onPlayWhenReadyChanged(false)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        @Player.Command seekCommand: Int,
    ): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            -> callbacks.onSkip(+1)
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            -> callbacks.onSkip(-1)
            else -> {
                if (mediaItemIndex in playlist.indices && mediaItemIndex != currentIndex) {
                    playlist.getOrNull(mediaItemIndex)?.mediaItem?.mediaId?.let {
                        callbacks.onPlayMediaId(it)
                    }
                }
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        val id = mediaItems.getOrNull(startIndex)?.mediaId
            ?: mediaItems.firstOrNull()?.mediaId
        if (!id.isNullOrBlank() && !ArthurMediaBrowse.isFolder(id)) {
            callbacks.onPlayMediaId(id)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
