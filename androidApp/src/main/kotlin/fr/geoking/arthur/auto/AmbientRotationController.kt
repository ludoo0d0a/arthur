package fr.geoking.arthur.auto

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import fr.geoking.arthur.audio.AmbientAudioFocusHandler
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
import fr.geoking.arthur.source.Quote
import fr.geoking.arthur.source.QuoteRepository
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.StillImagePrefetcher
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.allowsGenerativeAmbientFallback
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sourceIdsForAmbientLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Shared Ambient rotation / audio / still-bake state for Auto Car App screens
 * ([ArtworkPaneScreen] and [MediaAmbientPlaybackScreen]).
 */
internal class AmbientRotationController(
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    val packSelection: PackSelection,
    private val initialArtworkId: String?,
    private val limitAutoRotation: Boolean = true,
    private val onInvalidate: () -> Unit,
    private val onArtworkChanged: ((Artwork, Long, Boolean) -> Unit)? = null,
) : KoinComponent {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()
    private val imageCache: ArtworkImageCache by inject()
    private val invalidStore: InvalidArtworkStore by inject()
    private val quoteSettings: QuoteSettings by inject()
    private val quoteRepository: QuoteRepository by inject()
    private val developerSettings: DeveloperSettings by inject()
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var zenAudio: ZenAudioEngine? = null
    private val audioFocusHandler = AmbientAudioFocusHandler(
        isPlaying = { isPlaying },
        setPlaying = { setPlaying(it) },
    )

    private val renewSourceIds: List<String> = packSelection.sourceIdsForAmbientLoad().orEmpty()

    var catalog: List<Artwork> = emptyList()
        private set
    var current: Artwork? = null
        private set
    var currentQuote: Quote? = null
        private set
    var generation: Long = 0L
        private set
    var isPlaying: Boolean = true
        private set
    var consecutiveAutoRotations: Int = 0
        private set
    var seenIds: Set<String> = emptySet()
        private set
    var cacheCount: Int = 0
        private set
    var liveCount: Int = 0
        private set
    var queryLaunched: Boolean = false
        private set
    var loaded: Boolean = false
        private set

    @Volatile
    var renderedBitmap: android.graphics.Bitmap? = null
        private set
    @Volatile
    var renderedArtId: String? = null
        private set
    @Volatile
    var renderedGen: Long = -1L
        private set

    private var rotationJob: Job? = null
    private var renderJob: Job? = null
    private var advanceJob: Job? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    rotationJob?.cancel()
                    renderJob?.cancel()
                    advanceJob?.cancel()
                    zenAudio?.destroy()
                    zenAudio = null
                    scope.cancel()
                }
            },
        )
        scope.launch {
            runCatching { bootstrapRotation() }
                .onFailure {
                    loaded = true
                    onInvalidate()
                }
        }
    }

    private fun audioEngine(): ZenAudioEngine {
        return zenAudio ?: ZenAudioEngine(appContext, ambientAudioSettings).also { engine ->
            engine.onAudioFocusChanged = { event ->
                scope.launch { audioFocusHandler.onFocusEvent(event) }
            }
            zenAudio = engine
        }
    }

    private suspend fun bootstrapRotation() {
        val cachedRaw = withContext(Dispatchers.IO) {
            imageCache.loadCachedArtworks(renewSourceIds.takeIf { it.isNotEmpty() })
        }
        val cachedPool = samplePool(
            resolvePackPool(cachedRaw, packSelection).distinctBy { it.id }.ifEmpty {
                if (packSelection.allowsGenerativeAmbientFallback()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            contentEngine.catalog(
                                PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
                            )
                        }.getOrDefault(emptyList())
                    }.let { resolvePackPool(it, packSelection).distinctBy { art -> art.id } }
                } else {
                    emptyList()
                }
            },
        )
        cacheCount = cachedPool.size
        if (cachedPool.isNotEmpty()) {
            applyPool(cachedPool, preferInitial = true)
            loaded = true
            onInvalidate()
        }

        queryLaunched = true
        val liveRaw = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = renewSourceIds, artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList())
        }
        val livePool = samplePool(resolvePackPool(liveRaw, packSelection).distinctBy { it.id })
        liveCount = livePool.size
        if (livePool.isNotEmpty()) {
            applyPool(livePool, preferInitial = catalog.isEmpty())
        } else if (catalog.isEmpty() && packSelection.allowsGenerativeAmbientFallback()) {
            applyPool(cachedPool, preferInitial = true)
        }

        loaded = true
        prefetchNeighbors()
        syncAudio()
        notifyArtworkChanged()
        if (isPlaying) startRotation()
        onInvalidate()
    }

    private fun samplePool(pool: List<Artwork>, seed: Artwork? = current): List<Artwork> {
        if (pool.isEmpty()) return emptyList()
        return AmbientAlbumArt.sampleRotationPool(
            pool = pool,
            maxSize = AmbientAlbumArt.MAX_AUTO_ROTATION_POOL,
            seed = seed,
            isPreferred = { art ->
                art.isGenerative ||
                    !art.localPath.isNullOrBlank() ||
                    imageCache.hasImage(art.id)
            },
        )
    }

    private fun applyPool(pool: List<Artwork>, preferInitial: Boolean) {
        val previousId = current?.id
        catalog = pool
        current = when {
            preferInitial && initialArtworkId != null ->
                pool.firstOrNull { it.id == initialArtworkId } ?: resolveAmbientArtwork(pool, null)
            previousId != null ->
                pool.firstOrNull { it.id == previousId } ?: resolveAmbientArtwork(pool, null)
            else -> resolveAmbientArtwork(pool, null)
        }
        seenIds = current?.id?.let { setOf(it) }.orEmpty()
        scope.launch { updateQuoteForCurrent() }
        scheduleAsyncRender()
        notifyArtworkChanged()
    }

    private suspend fun renewCatalog() {
        queryLaunched = true
        val liveRaw = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = renewSourceIds, artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList())
        }
        val currentPoolIds = catalog.map { it.id }.toSet()
        val rawResolved = resolvePackPool(liveRaw, packSelection).distinctBy { it.id }
        val distinctLive = rawResolved.filter { it.id !in currentPoolIds }
        val candidatePool = if (distinctLive.isNotEmpty()) {
            (catalog + distinctLive).distinctBy { it.id }
        } else {
            rawResolved.ifEmpty { catalog }
        }
        val livePool = samplePool(candidatePool)
        liveCount = livePool.size
        if (livePool.isNotEmpty()) {
            val keepId = current?.id
            catalog = livePool
            if (keepId == null || livePool.none { it.id == keepId }) {
                current = resolveAmbientArtwork(livePool, null)
                notifyArtworkChanged()
            }
        }
    }

    private suspend fun prefetchNeighbors() {
        val pool = catalog
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

    private fun scheduleAsyncRender() {
        val art = current ?: return
        val gen = generation
        if (art.id == renderedArtId && gen == renderedGen && renderedBitmap != null) {
            return
        }
        renderJob?.cancel()
        renderJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    AmbientStillRenderer.render(art, gen, imageCache, invalidStore)
                }.getOrNull()
            }
            if (isActive && bitmap != null) {
                renderedBitmap = bitmap
                renderedArtId = art.id
                renderedGen = gen
                onInvalidate()
            }
        }
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(rotationSettings.autoIntervalMs.value)
                if (isPlaying) {
                    advance(+1, isAuto = true)
                }
            }
        }
    }

    private suspend fun updateQuoteForCurrent() {
        if (quoteSettings.showQuotes.value && current != null) {
            currentQuote = withContext(Dispatchers.IO) {
                runCatching { quoteRepository.nextQuote() }.getOrNull()
            }
        } else {
            currentQuote = null
        }
    }

    fun syncAudio() {
        if (isPlaying && ambientAudioSettings.enabled.value) {
            val engine = audioEngine()
            current?.let { engine.setArtwork(it) }
            engine.start()
        } else {
            zenAudio?.stop()
        }
    }

    fun advance(delta: Int, isAuto: Boolean = false) {
        if (!isAuto) {
            consecutiveAutoRotations = 0
            if (!isPlaying) {
                isPlaying = true
                syncAudio()
                notifyArtworkChanged()
            }
            advanceJob?.cancel()
            advanceJob = scope.launch {
                applyManualAdvance(delta)
            }
        } else {
            consecutiveAutoRotations += 1
            if (limitAutoRotation && consecutiveAutoRotations >= 2) {
                isPlaying = false
                consecutiveAutoRotations = 0
                rotationJob?.cancel()
                syncAudio()
                notifyArtworkChanged()
            }
            advanceJob?.cancel()
            advanceJob = scope.launch {
                applyAutoAdvance()
            }
        }
    }

    private suspend fun applyManualAdvance(delta: Int) {
        if (catalog.isEmpty()) return
        val index = catalog.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val nextIndex = AmbientAlbumArt.nextValidIndex(
            poolSize = catalog.size,
            currentIndex = index,
            delta = delta,
            isInvalidAt = { catalog[it].id.let(invalidStore::isInvalid) },
        )
        showArtwork(catalog[nextIndex])
        if (isPlaying) startRotation()
    }

    private suspend fun applyAutoAdvance() {
        applyManualAdvance(delta = +1)
    }

    private suspend fun showArtwork(art: Artwork) {
        current = art
        generation += 1
        seenIds = seenIds + art.id
        if (!art.remoteUrl.isNullOrBlank()) {
            rotationSettings.recordRecentStillId(art.id)
        }
        if (ambientAudioSettings.enabled.value) {
            val engine = audioEngine()
            engine.setArtwork(art)
            engine.triggerTransition()
        }
        updateQuoteForCurrent()
        scheduleAsyncRender()
        prefetchNeighbors()
        notifyArtworkChanged()
        onInvalidate()

        val unseenCount = catalog.count { !invalidStore.isInvalid(it.id) && it.id !in seenIds }
        if (unseenCount <= 1) {
            scope.launch { runCatching { renewCatalog() } }
        }
    }

    fun togglePlay() {
        isPlaying = !isPlaying
        consecutiveAutoRotations = 0
        syncAudio()
        if (isPlaying) startRotation() else rotationJob?.cancel()
        notifyArtworkChanged()
        onInvalidate()
    }

    fun setPlaying(value: Boolean) {
        if (isPlaying == value) return
        isPlaying = value
        consecutiveAutoRotations = 0
        syncAudio()
        if (isPlaying) startRotation() else rotationJob?.cancel()
        notifyArtworkChanged()
        onInvalidate()
    }

    fun debugSnapshot(): AmbientRotationDebug {
        val unseen = catalog.count { art ->
            !invalidStore.isInvalid(art.id) && art.id !in seenIds
        }
        return AmbientRotationDebug(
            querySourceIds = renewSourceIds,
            cacheCount = cacheCount,
            liveCount = liveCount,
            poolSize = catalog.size,
            seenCount = seenIds.size,
            unseenCount = unseen,
            consecutiveAutoRotations = consecutiveAutoRotations,
            isPlaying = isPlaying,
            currentArtworkId = current?.id,
            generation = generation,
            queryLaunched = queryLaunched,
        )
    }

    fun isDevMode(): Boolean = developerSettings.verbose.value

    fun showQuotes(): Boolean = quoteSettings.showQuotes.value

    fun imageCache(): ArtworkImageCache = imageCache

    fun invalidStore(): InvalidArtworkStore = invalidStore

    private fun notifyArtworkChanged() {
        val art = current ?: return
        onArtworkChanged?.invoke(art, generation, isPlaying)
    }
}
