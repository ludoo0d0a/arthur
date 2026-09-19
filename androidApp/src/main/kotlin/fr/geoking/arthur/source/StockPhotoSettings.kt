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

    var deviantArtUsername: String
        get() = prefs.getString(KEY_DEVIANTART_USERNAME, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_DEVIANTART_USERNAME, value).apply()
        }

    var deviantArtPassword: String
        get() = prefs.getString(KEY_DEVIANTART_PASSWORD, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_DEVIANTART_PASSWORD, value).apply()
        }

    /**
     * In-memory content kind for multi-kind stock Sources (e.g. Unsplash supports both
     * Photo and Video). Driven by Control Plane category chips; not persisted.
     */
    @Volatile
    var contentKind: ArtworkKind = ArtworkKind.Photo

    companion object {
        private const val PREFS = "arthur_stock_photo"
        private const val KEY_CATEGORY = "category"
        private const val KEY_DEVIANTART_USERNAME = "deviantart_username"
        private const val KEY_DEVIANTART_PASSWORD = "deviantart_password"
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
class ArtworkImageCache(
    context: Context,
    private val invalidStore: InvalidArtworkStore? = null,
) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.cacheDir, "artwork").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun imageFile(artworkId: String): File = File(dir, "${sanitize(artworkId)}.img")

    fun hasImage(artworkId: String): Boolean =
        imageFile(artworkId).let { it.exists() && it.length() > MIN_BYTES }

    /** Bytes on disk and decodable bounds; purges corrupt files. */
    fun hasDecodableImage(artworkId: String): Boolean =
        validCachedFileOrNull(artworkId) != null

    fun putImage(artworkId: String, bytes: ByteArray) {
        if (bytes.size < MIN_BYTES) return
        imageFile(artworkId).writeBytes(bytes)
    }

    fun localPathOrNull(artworkId: String): String? =
        validCachedFileOrNull(artworkId)?.absolutePath

    /**
     * Returns a cached file with valid image bounds, or null after deleting corrupt bytes.
     * Does not mark the artwork permanently invalid (caller may re-download).
     */
    fun validCachedFileOrNull(artworkId: String): File? {
        val target = imageFile(artworkId)
        if (!target.exists() || target.length() <= MIN_BYTES) return null
        if (SafeBitmapDecoder.canDecodeBounds(target.absolutePath)) return target
        target.delete()
        return null
    }

    /** Deletes cached bytes and marks [artworkId] invalid. */
    fun purgeInvalid(artworkId: String) {
        imageFile(artworkId).delete()
        invalidStore?.markInvalid(artworkId)
    }

    /**
     * Ensures [artworkId] is on disk from [remoteUrl].
     * When [allowNetwork] is false (cache-only): return valid cache or throw [RemoteStillCacheOnlyMiss].
     */
    fun downloadAndCache(
        artworkId: String,
        remoteUrl: String,
        errorLogger: fr.geoking.arthur.shared.error.ErrorLogger? = null,
        sourceId: String = "image_download",
        allowNetwork: Boolean = true,
    ): File {
        validCachedFileOrNull(artworkId)?.let { return it }
        // Stale oversized non-image: drop before download / cache-only miss.
        imageFile(artworkId).takeIf { it.exists() }?.delete()

        if (!allowNetwork) {
            throw RemoteStillCacheOnlyMiss(artworkId)
        }

        return try {
            val downloaded = StillImageDownloader.downloadToFile(
                url = remoteUrl,
                targetFile = imageFile(artworkId),
                errorLogger = errorLogger,
                sourceId = sourceId,
                artworkId = artworkId,
            )
            if (!SafeBitmapDecoder.canDecodeBounds(downloaded.absolutePath)) {
                downloaded.delete()
                invalidStore?.markInvalid(artworkId)
                throw java.io.IOException("Downloaded image is not decodable")
            }
            invalidStore?.clear(artworkId)
            downloaded
        } catch (e: RemoteStillCacheOnlyMiss) {
            throw e
        } catch (e: Exception) {
            val httpCode = Regex("""HTTP (\d{3})""")
                .find(e.message.orEmpty())
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            if (StillImageDownloader.isNonRetryable(httpCode, e)) {
                invalidStore?.markInvalid(artworkId)
            }
            throw e
        }
    }

    fun remember(artworks: List<Artwork>, category: String) {
        val byId = readLines().associateBy { it.substringBefore(SEP) }.toMutableMap()
        for (art in artworks) {
            byId[art.id] = encode(art, category = category, lastAccess = now())
        }
        writeAll(byId.values)
    }

    fun rememberArtworks(artworks: List<Artwork>) {
        val byId = readLines().associateBy { it.substringBefore(SEP) }.toMutableMap()
        for (art in artworks) {
            byId[art.id] = encode(art, category = art.sourceId, lastAccess = now())
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

    /**
     * Retrieve cached items that already have bytes on disk matching optional [sourceIds] and [kind].
     */
    fun loadCachedArtworks(sourceIds: List<String>? = null, kind: ArtworkKind? = null): List<Artwork> =
        decodeEntries()
            .filter { entry -> sourceIds.isNullOrEmpty() || entry.sourceId in sourceIds }
            .mapNotNull { it.toArtworkOrNull() }
            .filter { art -> kind == null || art.kind == kind }

    /** Number of index entries that still have image bytes on disk. */
    fun entryCount(): Int =
        decodeEntries().count { hasImage(it.id) }

    /** Total bytes of files under the artwork cache directory. */
    fun totalBytes(): Long {
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /** Deletes every cached image file and clears the catalog index. */
    fun clearAll() {
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
        prefs.edit().remove(KEY_CATALOG).apply()
    }

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
        const val MAX_GENART = 120
        const val CATEGORY_GENART = "genart"

        private const val PREFS = "arthur_artwork_cache"
        private const val KEY_CATALOG = "catalog"
        private const val SEP = "\t"
        private const val MIN_BYTES = 1_000

        fun sanitize(artworkId: String): String =
            artworkId.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
