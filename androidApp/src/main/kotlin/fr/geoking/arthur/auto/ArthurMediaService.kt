package fr.geoking.arthur.auto

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 * Auto Canvas (Media): Ambient Rotation as browse tree + now-playing artwork.
 */
class ArthurMediaService : MediaBrowserServiceCompat() {
    private val contentEngine: ContentEngine by inject()
    private lateinit var session: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "ArthurMedia").apply {
            setCallback(object : MediaSessionCompat.Callback() {})
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1f)
                    .build(),
            )
            isActive = true
        }
        sessionToken = session.sessionToken
        publishFirstArtwork()
    }

    private fun publishFirstArtwork() {
        val catalog = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            )
        }
        val first = catalog.firstOrNull() ?: return
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, first.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, first.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, first.attribution)
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
        val catalog = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            )
        }
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
