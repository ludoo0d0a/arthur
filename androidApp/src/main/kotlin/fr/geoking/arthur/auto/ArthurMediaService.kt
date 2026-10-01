package fr.geoking.arthur.auto

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.ZenAudioEngine
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.DeveloperSettings
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.QuoteRepository
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.StillImagePrefetcher
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
 *
 * Bootstrap: disk cache → full catalog query → prefetch neighbors; auto advances use
 * unseen-first picks and pause on the 3rd auto-rotated image.
 */
@OptIn(UnstableApi::class)
class ArthurMediaService : MediaLibraryService() {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()
    private val imageCache: ArtworkImageCache by inject()
    private val invalidStore: InvalidArtworkStore by inject()
    private val quoteSettings: QuoteSettings by inject()
    private val quoteRepository: QuoteRepository by inject()
    private val developerSettings: DeveloperSettings by inject()
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private lateinit var zenAudio: ZenAudioEngine
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var librarySession: MediaLibrarySession? = null
    private lateinit var player: AmbientMediaPlayer
    private var catalog: List<Artwork> = emptyList()
    private var ambientPool: List<Artwork> = emptyList()
    private var current: Artwork? = null
    private var generation: Long = 0L
    private var playing: Boolean = false
    private var consecutiveAutoRotations: Int = 0
    private var seenIds: Set<String> = emptySet()
    private var cacheCount: Int = 0
    private var liveCount: Int = 0
    private var queryLaunched: Boolean = false
    private var rotationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        player = AmbientMediaPlayer(
            Looper.getMainLooper(),
            object : AmbientMediaPlayer.Callbacks {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean) {
                    setPlaying(playWhenReady)
                }

                override fun onSkip(delta: Int) {
                    scope.launch { advance(delta, userInitiated = true) }
                }

                override fun onPlayMediaId(mediaId: String) {
                    if (ArthurMediaBrowse.isFolder(mediaId)) return
                    val art = catalog.firstOrNull { it.id == mediaId } ?: return
                    scope.launch {
                        current = art
                        generation += 1
                        seenIds = seenIds + art.id
                        publishArtwork(art)
                        prefetchNeighbors()
                        setPlaying(true)
                    }
                }
            },
        )
        librarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setId("ArthurMedia")
            .build()
        zenAudio = ZenAudioEngine(this, ambientAudioSettings)
        scope.launch {
            runCatching { bootstrapCatalog() }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        librarySession

    /**
     * Keep browse-only / no FGS behavior (Play Console demo video not yet available).
     * Media3 would otherwise promote a media-playback foreground service while "playing".
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // no-op
    }

    private suspend fun bootstrapCatalog() {
        val cached = withContext(Dispatchers.IO) {
            imageCache.loadCachedArtworks(null).distinctBy { it.id }
        }
        cacheCount = cached.size
        if (cached.isNotEmpty()) {
            catalog = cached
            rebuildAmbientPool()
            current = resolveAmbientArtwork(ambientPool.ifEmpty { catalog }, null)
            seenIds = current?.id?.let { setOf(it) }.orEmpty()
            current?.let { publishArtwork(it) }
            notifyBrowseChanged()
            setPlaying(true)
        }

        queryLaunched = true
        val live = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList()).distinctBy { it.id }
        }
        liveCount = live.size
        if (live.isNotEmpty()) {
            val keepId = current?.id
            catalog = live
            rebuildAmbientPool(seedId = keepId)
            current = keepId?.let { id -> ambientPool.firstOrNull { it.id == id } }
                ?: resolveAmbientArtwork(ambientPool.ifEmpty { live }, null)
            seenIds = current?.id?.let { setOf(it) }.orEmpty()
            notifyBrowseChanged()
            current?.let { publishArtwork(it) }
        } else if (catalog.isEmpty()) {
            return
        }
        prefetchNeighbors()
        if (!playing) setPlaying(true)
    }

    private fun notifyBrowseChanged() {
        val session = librarySession ?: return
        session.notifyChildrenChanged(ROOT, ArthurMediaBrowse.rootSourceIds(catalog).size, null)
        for (sourceId in ArthurMediaBrowse.rootSourceIds(catalog)) {
            val children = ArthurMediaBrowse.childrenOf(ArthurMediaBrowse.folderId(sourceId), catalog)
            session.notifyChildrenChanged(ArthurMediaBrowse.folderId(sourceId), children.size, null)
        }
    }

    private suspend fun renewCatalog() {
        queryLaunched = true
        val live = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList()).distinctBy { it.id }
        }
        liveCount = live.size
        if (live.isNotEmpty()) {
            val keepId = current?.id
            catalog = live
            rebuildAmbientPool(seedId = keepId)
            if (keepId == null || ambientPool.none { it.id == keepId }) {
                current = resolveAmbientArtwork(ambientPool.ifEmpty { live }, null)
            }
            notifyBrowseChanged()
        }
    }

    private suspend fun prefetchNeighbors() {
        val pool = rotationPool()
        if (pool.isEmpty()) return
        val index = pool.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val currentArt = pool.getOrNull(index)
        val nextArt = pool.getOrNull(AmbientAlbumArt.advanceIndex(index, pool.size))
        val prevArt = pool.getOrNull(Math.floorMod(index - 1, pool.size))
        withContext(Dispatchers.IO) {
            currentArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
            nextArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
            prevArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
        }
    }

    private fun setPlaying(value: Boolean) {
        playing = value
        if (value) {
            consecutiveAutoRotations = 0
            if (ambientAudioSettings.enabled.value) {
                current?.let { zenAudio.setArtwork(it) }
                zenAudio.start()
            } else {
                zenAudio.stop()
            }
        } else {
            zenAudio.stop()
        }
        player.setPlaying(value)
        if (value) startRotation() else rotationJob?.cancel()
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(rotationSettings.autoIntervalMs.value)
                if (playing) {
                    consecutiveAutoRotations += 1
                    advance(+1, userInitiated = false)
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
        seenIds = seenIds + next.id
        if (!next.remoteUrl.isNullOrBlank()) {
            rotationSettings.recordRecentStillId(next.id)
        }
        publishArtwork(next)
        zenAudio.setArtwork(next)
        zenAudio.triggerTransition()
        prefetchNeighbors()
        if (userInitiated) {
            consecutiveAutoRotations = 0
            if (playing) startRotation() else setPlaying(true)
        }

        val unseenCount = pool.count { !invalidStore.isInvalid(it.id) && it.id !in seenIds }
        if (unseenCount <= 1) {
            scope.launch { runCatching { renewCatalog() } }
        }
    }

    private fun rebuildAmbientPool(seedId: String? = current?.id) {
        val generative = catalog.filter { it.isGenerative }
        val base = generative.ifEmpty { catalog }
        val seed = seedId?.let { id -> base.firstOrNull { it.id == id } } ?: current
        ambientPool = AmbientAlbumArt.sampleRotationPool(
            pool = base,
            maxSize = AmbientAlbumArt.MAX_AUTO_ROTATION_POOL,
            seed = seed,
            isPreferred = { art ->
                art.isGenerative ||
                    !art.localPath.isNullOrBlank() ||
                    imageCache.hasImage(art.id)
            },
        )
    }

    private fun rotationPool(): List<Artwork> = ambientPool.ifEmpty {
        val generative = catalog.filter { it.isGenerative }
        generative.ifEmpty { catalog }
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
        val uri = AmbientAlbumArt.contentUri(packageName, art.id, gen)

        val quote = if (quoteSettings.showQuotes.value) {
            runCatching { quoteRepository.nextQuote() }.getOrNull()
        } else {
            null
        }
        val quoteText = if (quote != null) {
            "\u201C${quote.text}\u201D" + if (quote.author.isNotBlank()) " \u2014 ${quote.author}" else ""
        } else {
            null
        }

        val pool = rotationPool()
        val isDevMode = developerSettings.verbose.value
        val index = pool.indexOfFirst { it.id == art.id }.let { if (it < 0) 0 else it }
        val slidePos = if (isDevMode && pool.isNotEmpty()) {
            "[${index + 1}/${pool.size}] cache:$cacheCount live:$liveCount seen:${seenIds.size}"
        } else {
            null
        }

        val subtitle = buildString {
            if (art.attribution.isNotBlank()) {
                append(art.attribution)
            }
            if (quoteText != null) {
                if (isNotEmpty()) append(" • ")
                append(quoteText)
            }
            if (slidePos != null) {
                if (isNotEmpty()) append(" ")
                append(slidePos)
            }
        }

        val description = if (isDevMode) {
            buildString {
                append("query=")
                append(if (queryLaunched) "all" else "pending")
                append(" pool=")
                append(pool.size)
                append(" cache=")
                append(cacheCount)
                append(" live=")
                append(liveCount)
                append(" seen=")
                append(seenIds.size)
                append(" auto=")
                append(consecutiveAutoRotations)
                append(" playing=")
                append(playing)
            }
        } else {
            null
        }

        val queueUris = pool.associate { item ->
            item.id to AmbientAlbumArt.contentUri(packageName, item.id, 0L)
        }
        player.publish(
            art = art,
            artworkUri = uri,
            subtitle = subtitle,
            description = description,
            genre = if (art.isGenerative) "generative" else art.kind.name,
            queue = pool,
            queueUris = queueUris,
            playing = playing,
            durationMs = rotationSettings.autoIntervalMs.value,
            playlistTitle = getString(R.string.ambient_title),
        )
    }

    private fun buildChildren(parentId: String): List<MediaItem> {
        if (parentId == ROOT) {
            return ArthurMediaBrowse.rootSourceIds(catalog).map { sourceId ->
                val title = ArthurMediaBrowse.folderTitle(sourceId)
                val extras = if (ArthurMediaBrowse.usesPreviewGrid(sourceId)) {
                    ArthurMediaBrowse.previewGridExtras()
                } else {
                    Bundle.EMPTY
                }
                MediaItem.Builder()
                    .setMediaId(ArthurMediaBrowse.folderId(sourceId))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setSubtitle(
                                catalog.count { it.sourceId == sourceId }.toString() + " pieces",
                            )
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(extras)
                            .build(),
                    )
                    .build()
            }
        }

        return ArthurMediaBrowse.childrenOf(parentId, catalog).map { art ->
            val icon = AmbientAlbumArt.contentUri(packageName, art.id, 0L)
            MediaItem.Builder()
                .setMediaId(art.id)
                .setUri(icon)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(art.title)
                        .setSubtitle(art.attribution)
                        .setArtworkUri(icon)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build(),
                )
                .build()
        }
    }

    private fun mediaItemForId(mediaId: String): MediaItem? {
        if (mediaId == ROOT) {
            return MediaItem.Builder()
                .setMediaId(ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setExtras(ArthurMediaBrowse.rootExtras())
                        .build(),
                )
                .build()
        }
        if (ArthurMediaBrowse.isFolder(mediaId)) {
            val sourceId = ArthurMediaBrowse.sourceIdFromFolder(mediaId) ?: return null
            val extras = if (ArthurMediaBrowse.usesPreviewGrid(sourceId)) {
                ArthurMediaBrowse.previewGridExtras()
            } else {
                Bundle.EMPTY
            }
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ArthurMediaBrowse.folderTitle(sourceId))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setExtras(extras)
                        .build(),
                )
                .build()
        }
        val art = catalog.firstOrNull { it.id == mediaId } ?: return null
        val icon = AmbientAlbumArt.contentUri(packageName, art.id, 0L)
        return MediaItem.Builder()
            .setMediaId(art.id)
            .setUri(icon)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(art.title)
                    .setSubtitle(art.attribution)
                    .setArtworkUri(icon)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = mediaItemForId(ROOT)!!
            val rootParams = MediaLibraryService.LibraryParams.Builder()
                .setExtras(ArthurMediaBrowse.rootExtras())
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, rootParams))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = mediaItemForId(mediaId)
                ?: return Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE),
                )
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val all = buildChildren(parentId)
            val from = (page * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            val pageItems = ImmutableList.copyOf(all.subList(from, to))
            return Futures.immediateFuture(LibraryResult.ofItemList(pageItems, params))
        }
    }

    override fun onDestroy() {
        runCatching { rotationJob?.cancel() }
        runCatching { zenAudio.destroy() }
        runCatching { scope.cancel() }
        runCatching {
            librarySession?.release()
            librarySession = null
        }
        runCatching { player.release() }
        super.onDestroy()
    }

    companion object {
        const val ROOT = ArthurMediaBrowse.ROOT
    }
}
