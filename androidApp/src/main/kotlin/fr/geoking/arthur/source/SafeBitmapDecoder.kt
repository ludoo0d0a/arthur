package fr.geoking.arthur.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

/**
 * Decodes bitmaps with [BitmapFactory.Options.inSampleSize] so full-res museum / stock
 * photos cannot allocate multi‑tens‑of‑MB bitmaps on phone Ambient / Auto stills.
 */
object SafeBitmapDecoder {
    const val DEFAULT_MAX_SIDE = 2048
    /** Match [fr.geoking.arthur.auto.AmbientStillRenderer.SIZE] so ambient stills stay near-4K. */
    const val AMBIENT_STILL_MAX_SIDE = 2160

    /** True when [path] has decodable image bounds (cheap check before full decode). */
    fun canDecodeBounds(path: String): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    fun decodeFile(path: String, maxSide: Int = DEFAULT_MAX_SIDE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }

    fun decodeByteArray(bytes: ByteArray, maxSide: Int = DEFAULT_MAX_SIDE): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        }.getOrNull()
    }

    internal fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (maxSide <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / sample > maxSide) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
