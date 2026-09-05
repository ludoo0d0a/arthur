package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MuseumSearchKind
import fr.geoking.arthur.shared.source.StockPhotoCategory
import java.io.File

/** Persists selected stock-photo topic for Pexels / Unsplash queries. */
class StockPhotoSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var category: StockPhotoCategory
        get() = StockPhotoCategory.fromQuery(
            prefs.getString(KEY_CATEGORY, StockPhotoCategory.Random.query).orEmpty(),
        )
        set(value) {
            prefs.edit().putString(KEY_CATEGORY, value.query).apply()
        }

    companion object {
        private const val PREFS = "arthur_stock_photo"
        private const val KEY_CATEGORY = "category"
    }
}

/**
 * In-memory Painting / Sculpture facet for museum Remote Sources.
 * Driven by Control Plane category chips; not persisted (defaults to All).
 */
class MuseumSearchSettings {
    @Volatile
    var kind: MuseumSearchKind = MuseumSearchKind.All
}

/**
 * Disk cache of stock stills + baked genart frames.
 * Stock entries are keyed by photo topic; genart is capped at [MAX_GENART] (LRU).
 */
class ArtworkImageCache(context: Context) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.cacheDir, "artwork").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun imageFile(artworkId: String): File = File(dir, "${sanitize(artworkId)}.img")

    fun hasImage(artworkId: String): Boolean =
        imageFile(artworkId).let { it.exists() && it.length() > MIN_BYTES }

    fun putImage(artworkId: String, bytes: ByteArray) {
        if (bytes.size < MIN_BYTES) return
        imageFile(artworkId).writeBytes(bytes)
    }

    fun localPathOrNull(artworkId: String): String? =
        imageFile(artworkId).takeIf { it.exists() && it.length() > MIN_BYTES }?.absolutePath

    fun remember(artworks: List<Artwork>, category: String) {
        val byId = readLines().associateBy { it.substringBefore(SEP) }.toMutableMap()
        for (art in artworks) {
            byId[art.id] = encode(art, category = category, lastAccess = now())
        }
        writeAll(byId.values)
    }

    /** Bake / touch a genart still; keeps at most [MAX_GENART] genart entries (LRU). */
    fun rememberGenart(artwork: Artwork) {
        require(artwork.kind == ArtworkKind.Genart || artwork.sourceId == GenartSource.ID)
        val byId = readLines().associateBy { it.substringBefore(SEP) }.toMutableMap()
        byId[artwork.id] = encode(
            artwork.copy(kind = ArtworkKind.Genart, sourceId = GenartSource.ID),
            category = CATEGORY_GENART,
            lastAccess = now(),
        )
        trimGenartLocked(byId)
        writeAll(byId.values)
    }

    fun touchGenart(artworkId: String) {
        val byId = readLines().associateBy { it.substringBefore(SEP) }.toMutableMap()
        val line = byId[artworkId] ?: return
        val parts = line.split(SEP)
        if (parts.size < 8 || parts[6] != CATEGORY_GENART) return
        byId[artworkId] = parts.toMutableList().also { it[7] = now().toString() }.joinToString(SEP)
        writeAll(byId.values)
    }

    /** Offline photos for [category] + [sourceId] that already have bytes on disk. */
    fun loadCached(category: String, sourceId: String): List<Artwork> =
        decodeEntries()
            .filter { it.category.equals(category, ignoreCase = true) }
            .filter { it.sourceId == sourceId }
            .mapNotNull { it.toArtworkOrNull() }

    /**
     * Offline stock for a UX topic. [StockPhotoCategory.Random] merges every remote
     * topic bucket so ambient still has photos without a network.
     */
    fun loadCachedStock(category: StockPhotoCategory, sourceId: String): List<Artwork> {
        if (category != StockPhotoCategory.Random) {
            return loadCached(category.query, sourceId)
        }
        val topics = listOf(category) + StockPhotoCategory.remoteSearchTopics
        return topics.flatMap { loadCached(it.query, sourceId) }
            .distinctBy { it.id }
    }

    /** Cached genart stills with [localPath] set (max [MAX_GENART]). */
    fun loadCachedGenart(): List<Artwork> =
        decodeEntries()
            .filter { it.category == CATEGORY_GENART }
            .sortedByDescending { it.lastAccess }
            .take(MAX_GENART)
            .mapNotNull { it.toArtworkOrNull() }

    private fun trimGenartLocked(byId: MutableMap<String, String>) {
        val genartIds = byId.values.mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size >= 8 && parts[6] == CATEGORY_GENART) {
                parts[0] to (parts[7].toLongOrNull() ?: 0L)
            } else {
                null
            }
        }.sortedBy { it.second }
        val overflow = genartIds.size - MAX_GENART
        if (overflow <= 0) return
        for ((id, _) in genartIds.take(overflow)) {
            byId.remove(id)
            imageFile(id).delete()
        }
    }

    private fun encode(art: Artwork, category: String, lastAccess: Long): String =
        listOf(
            art.id,
            art.title.replace(SEP, " "),
            art.attribution.replace(SEP, " "),
            art.sourceId,
            art.kind.name,
            art.remoteUrl.orEmpty(),
            category,
            lastAccess.toString(),
        ).joinToString(SEP)

    private fun decodeEntries(): List<CacheEntry> =
        readLines().mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size < 7) return@mapNotNull null
            CacheEntry(
                id = parts[0],
                title = parts[1],
                attribution = parts[2],
                sourceId = parts[3],
                kind = parts[4],
                remoteUrl = parts[5].ifBlank { null },
                category = parts[6],
                lastAccess = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
            )
        }

    private fun CacheEntry.toArtworkOrNull(): Artwork? {
        val path = localPathOrNull(id) ?: return null
        val kind = runCatching { ArtworkKind.valueOf(kind) }.getOrDefault(ArtworkKind.Photo)
        return Artwork(
            id = id,
            title = title,
            attribution = attribution,
            sourceId = sourceId,
            kind = kind,
            remoteUrl = remoteUrl,
            localPath = path,
        )
    }

    private fun readLines(): List<String> =
        prefs.getString(KEY_CATALOG, "").orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }

    private fun writeAll(lines: Collection<String>) {
        prefs.edit().putString(KEY_CATALOG, lines.joinToString("\n")).apply()
    }

    private fun now(): Long = System.currentTimeMillis()

    private data class CacheEntry(
        val id: String,
        val title: String,
        val attribution: String,
        val sourceId: String,
        val kind: String,
        val remoteUrl: String?,
        val category: String,
        val lastAccess: Long,
    )

    companion object {
        /** Bake budget for Auto stills — keep ≥ shipped GenartSource catalog size. */
        const val MAX_GENART = 64
        const val CATEGORY_GENART = "genart"

        private const val PREFS = "arthur_artwork_cache"
        private const val KEY_CATALOG = "catalog"
        private const val SEP = "\t"
        private const val MIN_BYTES = 1_000

        fun sanitize(artworkId: String): String =
            artworkId.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
