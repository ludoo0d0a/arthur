package fr.geoking.arthur.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves baked Ambient stills to Android Auto / AAOS via content:// URIs.
 * Renders on first open when the cache file is missing.
 */
class AlbumArtContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val ctx = context ?: throw FileNotFoundException("no context")
        val parsed = AmbientAlbumArt.parseUri(uri)
            ?: throw FileNotFoundException("bad album art uri: $uri")
        val (artworkId, generation) = parsed
        val file = AmbientAlbumArt.cacheFile(ctx, artworkId, generation)
        if (!file.exists()) {
            runCatching {
                ensureParent(file)
                val bitmap = AmbientStillRenderer.renderForId(artworkId, generation)
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                }
                bitmap.recycle()
            }
        }
        if (!file.exists()) {
            throw FileNotFoundException("Failed to render album art for $uri")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun ensureParent(file: File) {
        file.parentFile?.mkdirs()
    }
}
