package fr.geoking.arthur.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.isGenerative
import java.io.File

/**
 * Auto Canvas album-art helpers: content:// URIs, cache files, Ambient Rotation default interval.
 * AA shows a single static image (IU-1 / SA-1); refresh by publishing a new URI.
 */
object AmbientAlbumArt {
    /** Default rotation interval (20s). User preference lives in [fr.geoking.arthur.source.RotationSettings]. */
    const val ROTATION_INTERVAL_MS = 20_000L
    /**
     * Ambient playlist size across phone / TV / Auto / Media canvases.
     * Cache-preferred items are sampled first; the rest fill up to this cap so rotation
     * can walk the full list (e.g. debug counter 1/30) before renewing from APIs.
     */
    const val MAX_AUTO_ROTATION_POOL = 30
    /**
     * Hard cap after mid-session catalog appends (~3 museum pages of [SOURCE_CATALOG_PAGE]).
     */
    const val MAX_PLAYLIST_SIZE = 60
    /**
     * Typical museum/stock Source [load] page (`DEFAULT_LIMIT`). Prefetch the next page
     * when fewer than [POOL_RENEW_LEAD] items remain — around the 19th/20th of a full page.
     */
    const val SOURCE_CATALOG_PAGE = 20
    const val POOL_RENEW_LEAD = 2
    const val PATH_ART = "art"
    const val AUTHORITY_SUFFIX = ".albumart"

    /**
     * Caps [pool] to [maxSize] for Ambient auto-rotation.
     * Generative artwork pools preserve all items in the pool since no remote network preloading is required.
     * Prefers [seed] and [isPreferred] items (e.g. already on disk) so the first slide avoids a network wait.
     */
    fun sampleRotationPool(
        pool: List<Artwork>,
        maxSize: Int = MAX_AUTO_ROTATION_POOL,
        seed: Artwork? = null,
        isPreferred: (Artwork) -> Boolean = { false },
    ): List<Artwork> {
        if (pool.isEmpty() || maxSize <= 0) return emptyList()
        val effectiveMaxSize = if (pool.all { it.isGenerative }) pool.size else maxSize
        val preferred = pool.filter(isPreferred)
        val others = pool.filterNot(isPreferred)
        val ordered = ArrayList<Artwork>(effectiveMaxSize.coerceAtMost(pool.size))
        val seedInPool = seed?.takeIf { candidate -> pool.any { it.id == candidate.id } }
        if (seedInPool != null) {
            ordered.add(seedInPool)
        } else {
            (preferred.firstOrNull() ?: others.firstOrNull())?.let { ordered.add(it) }
        }
        for (art in preferred + others) {
            if (ordered.size >= effectiveMaxSize) break
            if (ordered.none { it.id == art.id }) ordered.add(art)
        }
        return ordered
    }

    /**
     * True when the viewer is within [POOL_RENEW_LEAD] of the playlist end
     * (e.g. 19th/20th of a [SOURCE_CATALOG_PAGE]-sized pool — `DEFAULT_LIMIT - 2`).
     */
    fun shouldPrefetchNextPool(seenCount: Int, poolSize: Int): Boolean {
        if (poolSize < 2 || seenCount <= 0) return false
        val remaining = poolSize - seenCount
        return remaining <= POOL_RENEW_LEAD
    }

    /**
     * Appends distinct [incoming] artworks after [current], dropping oldest entries
     * (never [keepId]) if the playlist would exceed [maxSize].
     */
    fun appendToRotationPool(
        current: List<Artwork>,
        incoming: List<Artwork>,
        maxSize: Int = MAX_PLAYLIST_SIZE,
        keepId: String? = null,
    ): List<Artwork> {
        if (incoming.isEmpty()) return current
        val existingIds = current.mapTo(HashSet(current.size)) { it.id }
        val additions = incoming.filter { it.id !in existingIds }.distinctBy { it.id }
        if (additions.isEmpty()) return current
        val combined = current + additions
        if (combined.size <= maxSize) return combined
        val overflow = combined.size - maxSize
        val dropIds = LinkedHashSet<String>(overflow)
        for (art in combined) {
            if (dropIds.size >= overflow) break
            if (art.id != keepId) dropIds.add(art.id)
        }
        return combined.filterNot { it.id in dropIds }
    }

    fun authority(packageName: String): String = packageName + AUTHORITY_SUFFIX

    fun contentUri(packageName: String, artworkId: String, generation: Long): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(authority(packageName))
            .appendPath(PATH_ART)
            .appendPath(artworkId)
            .appendPath(generation.toString())
            .build()

    fun cacheDir(context: Context): File =
        File(context.cacheDir, "albumart").also { it.mkdirs() }

    fun cacheFile(context: Context, artworkId: String, generation: Long): File =
        File(cacheDir(context), fileName(artworkId, generation))

    fun fileName(artworkId: String, generation: Long): String =
        "${sanitize(artworkId)}__$generation.png"

    fun sanitize(artworkId: String): String =
        artworkId.replace(Regex("[^A-Za-z0-9._-]"), "_")

    fun advanceIndex(currentIndex: Int, poolSize: Int): Int {
        if (poolSize <= 0) return 0
        return Math.floorMod(currentIndex + 1, poolSize)
    }

    /**
     * Steps [delta] through [pool], skipping ids for which [isInvalid] is true.
     * If every item is invalid, still returns one step from [currentIndex].
     */
    fun nextValidIndex(
        poolSize: Int,
        currentIndex: Int,
        delta: Int,
        isInvalidAt: (Int) -> Boolean,
    ): Int {
        if (poolSize <= 0) return 0
        var idx = currentIndex
        repeat(poolSize) {
            idx = if (delta >= 0) {
                advanceIndex(idx, poolSize)
            } else {
                Math.floorMod(idx - 1, poolSize)
            }
            if (!isInvalidAt(idx)) return idx
        }
        return if (delta >= 0) advanceIndex(currentIndex, poolSize) else Math.floorMod(currentIndex - 1, poolSize)
    }

    fun parseUri(uri: Uri): Pair<String, Long>? =
        parsePathSegments(uri.pathSegments)

    fun parsePathSegments(segments: List<String>): Pair<String, Long>? {
        if (segments.size < 3) return null
        if (segments[0] != PATH_ART) return null
        val id = segments[1]
        val generation = segments[2].toLongOrNull() ?: return null
        return id to generation
    }
}
