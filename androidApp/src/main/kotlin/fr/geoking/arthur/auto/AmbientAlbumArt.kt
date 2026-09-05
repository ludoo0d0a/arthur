package fr.geoking.arthur.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Auto Canvas album-art helpers: content:// URIs, cache files, Ambient Rotation default interval.
 * AA shows a single static image (IU-1 / SA-1); refresh by publishing a new URI.
 */
object AmbientAlbumArt {
    /** Default rotation interval (20s). User preference lives in [fr.geoking.arthur.source.RotationSettings]. */
    const val ROTATION_INTERVAL_MS = 20_000L
    const val PATH_ART = "art"
    const val AUTHORITY_SUFFIX = ".albumart"

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
