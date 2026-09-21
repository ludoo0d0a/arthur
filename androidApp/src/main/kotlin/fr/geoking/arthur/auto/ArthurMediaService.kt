package fr.geoking.arthur.auto

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.RotationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Auto Canvas (Media): Ambient Rotation as browse tree + now-playing **static** album art.
 * AA forbids animated graphics (SA-1 / IU-1); generative pieces are baked to stills and
 * refreshed on the user-configured rotation interval.
 *
 * Browse: root → source folders → playable art (≤2 levels). Genart/fractal folders use a
 * grid content style so still previews are the primary affordance.
 */
class ArthurMediaService : MediaBrowserServiceCompat() {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()
    private val imageCache: ArtworkImageCache by inject()
    private val invalidStore: InvalidArtworkStore by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSessionCompat
    private var catalog: List<Artwork> = emptyList()
    private var current: Artwork? = null
    private var generation: Long = 0L
    private var playing: Boolean = false
    private var consecutiveAutoRotations: Int = 0
    private var rotationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "ArthurMedia").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        setPlaying(true)
                    }

                    override fun onPause() {
                        setPlaying(false)
                    }

                    override fun onSkipToNext() {
                        scope.launch { advance(+1, userInitiated = true) }
                    }

                    override fun onSkipToPrevious() {
                        scope.launch { advance(-1, userInitiated = true) }
                    }

                    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                        if (mediaId == null || ArthurMediaBrowse.isFolder(mediaId)) return
                        val art = catalog.firstOrNull { it.id == mediaId } ?: return
                        scope.launch {
                            current = art
                            generation += 1
                            publishArtwork(art)
                            setPlaying(true)
                        }
                    }
                },
            )
            isActive = true
        }
        sessionToken = session.sessionToken
        scope.launch {
            runCatching {
                catalog = withContext(Dispatchers.IO) {
                    runCatching {
                        contentEngine.catalog(
                            PreparedRotation(
                                sourceIds = emptyList(),
                                artworkIds = emptyList(),
                            ),
                        )
                    }.getOrDefault(emptyList())
                }
                notifyChildrenChanged(ROOT)
                for (sourceId in ArthurMediaBrowse.rootSourceIds(catalog)) {
                    notifyChildrenChanged(ArthurMediaBrowse.folderId(sourceId))
                }
                current = resolveAmbientArtwork(catalog, null)
                current?.let { publishArtwork(it) }
                setPlaying(true)
            }
        }
    }

    private fun setPlaying(value: Boolean) {
        playing = value
        if (value) {
            consecutiveAutoRotations = 0
        }
        publishPlayback(
            if (value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
        )
        if (value) startRotation() else rotationJob?.cancel()
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(rotationSettings.intervalMs.value)
                if (playing) {
                    consecutiveAutoRotations += 1
                    if (consecutiveAutoRotations >= 3) {
                        consecutiveAutoRotations = 0
                        advance(+1, userInitiated = false)
                        setPlaying(false)
                    } else {
                        advance(+1, userInitiated = false)
                    }
                }
            }
        }
    }

    private suspend fun advance(delta: Int, userInitiated: Boolean) {
        val pool = rotationPool()
        if (pool.isEmpty()) return
        val index = pool.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val nextIndex = AmbientAlbumArt.nextValidIndex(
            poolSize = pool.size,
            currentIndex = index,
            delta = delta,
            isInvalidAt = { pool[it].id.let(invalidStore::isInvalid) },
        )
        val next = pool[nextIndex]
        current = next
        generation += 1
        publishArtwork(next)
        if (userInitiated) {
            consecutiveAutoRotations = 0
            if (playing) startRotation() else setPlaying(true)
        }
    }

    private fun rotationPool(): List<Artwork> {
        val generative = catalog.filter { it.isGenerative }
        return generative.ifEmpty { catalog }
    }

    private suspend fun publishArtwork(art: Artwork) {
        val gen = generation
        withContext(Dispatchers.IO) {
            val file = AmbientAlbumArt.cacheFile(this@ArthurMediaService, art.id, gen)
            if (!file.exists()) {
                runCatching {
                    AmbientStillRenderer.renderToFile(art, gen, file, imageCache, invalidStore)
                }
            }
        }
        val uri = AmbientAlbumArt.contentUri(packageName, art.id, gen).toString()
        runCatching {
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, art.id)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, art.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, art.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, art.attribution)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, art.attribution)
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_GENRE,
                        if (art.isGenerative) "generative" else art.kind.name,
                    )
                    .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, uri)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, rotationSettings.intervalMs.value)
                    .build(),
            )
            val pool = rotationPool()
            session.setQueue(
                pool.mapIndexed { index, item ->
                    val icon = AmbientAlbumArt.contentUri(packageName, item.id, 0L)
                    val desc = MediaDescriptionCompat.Builder()
                        .setMediaId(item.id)
                        .setTitle(item.title)
                        .setSubtitle(item.attribution)
                        .setIconUri(icon)
                        .build()
                    MediaSessionCompat.QueueItem(desc, index.toLong())
                },
            )
            session.setQueueTitle(getString(R.string.ambient_title))
        }
    }

    private fun publishPlayback(state: Int) {
        runCatching {
            session.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                    )
                    .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT, ArthurMediaBrowse.rootExtras())

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        val children = runCatching { buildChildren(parentId).toMutableList() }.getOrDefault(mutableListOf())
        result.sendResult(children)
    }

    private fun buildChildren(parentId: String): List<MediaBrowserCompat.MediaItem> {
        if (parentId == ROOT) {
            return ArthurMediaBrowse.rootSourceIds(catalog).map { sourceId ->
                val title = ArthurMediaBrowse.folderTitle(sourceId)
                val extras = if (ArthurMediaBrowse.usesPreviewGrid(sourceId)) {
                    ArthurMediaBrowse.previewGridExtras()
                } else {
                    null
                }
                val desc = MediaDescriptionCompat.Builder()
                    .setMediaId(ArthurMediaBrowse.folderId(sourceId))
                    .setTitle(title)
                    .setSubtitle(catalog.count { it.sourceId == sourceId }.toString() + " pieces")
                    .setExtras(extras)
                    .build()
                MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
            }
        }

        val items = ArthurMediaBrowse.childrenOf(parentId, catalog)
        return items.map { art ->
            val icon = AmbientAlbumArt.contentUri(packageName, art.id, 0L)
            val desc = MediaDescriptionCompat.Builder()
                .setMediaId(art.id)
                .setTitle(art.title)
                .setSubtitle(art.attribution)
                .setIconUri(icon)
                .build()
            MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }
    }

    override fun onDestroy() {
        runCatching { rotationJob?.cancel() }
        runCatching { scope.cancel() }
        runCatching { session.release() }
        super.onDestroy()
    }

    companion object {
        const val ROOT = ArthurMediaBrowse.ROOT
    }
}
