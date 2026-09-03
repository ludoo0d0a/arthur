package fr.geoking.arthur.auto

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 * Auto Canvas (Media): full Content Engine catalog as browse tree;
 * now-playing prefers live generative Artwork.
 */
class ArthurMediaService : MediaBrowserServiceCompat() {
    private val contentEngine: ContentEngine by inject()
    private lateinit var session: MediaSessionCompat
    private var catalog: List<Artwork> = emptyList()
    private var current: Artwork? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "ArthurMedia").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        publishPlayback(PlaybackStateCompat.STATE_PLAYING)
                    }

                    override fun onPause() {
                        publishPlayback(PlaybackStateCompat.STATE_PAUSED)
                    }

                    override fun onSkipToNext() {
                        playRelative(+1)
                    }

                    override fun onSkipToPrevious() {
                        playRelative(-1)
                    }

                    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                        val art = catalog.firstOrNull { it.id == mediaId } ?: return
                        current = art
                        publishMetadata(art)
                        publishPlayback(PlaybackStateCompat.STATE_PLAYING)
                    }
                },
            )
            isActive = true
        }
        sessionToken = session.sessionToken
        catalog = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = emptyList(),
                    artworkIds = emptyList(),
                ),
            )
        }
        current = resolveAmbientArtwork(catalog, null)
        current?.let {
            publishMetadata(it)
            publishPlayback(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun playRelative(delta: Int) {
        val generative = catalog.filter { it.isGenerative }
        val pool = generative.ifEmpty { catalog }
        if (pool.isEmpty()) return
        val index = pool.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val next = pool[Math.floorMod(index + delta, pool.size)]
        current = next
        publishMetadata(next)
        publishPlayback(PlaybackStateCompat.STATE_PLAYING)
    }

    private fun publishMetadata(art: Artwork) {
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, art.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, art.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, art.attribution)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_GENRE,
                    if (art.isGenerative) "generative" else art.kind.name,
                )
                .build(),
        )
    }

    private fun publishPlayback(state: Int) {
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        val items = catalog.map { art ->
            val desc = MediaDescriptionCompat.Builder()
                .setMediaId(art.id)
                .setTitle(art.title)
                .setSubtitle(art.attribution)
                .build()
            MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }.toMutableList()
        result.sendResult(items)
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    companion object {
        const val ROOT = "arthur_root"
    }
}
