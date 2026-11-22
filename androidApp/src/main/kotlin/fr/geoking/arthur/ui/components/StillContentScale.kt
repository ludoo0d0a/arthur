package fr.geoking.arthur.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor

/**
 * Hybrid contain/cover scale for ambient stills: fill the viewport when Cover would
 * crop at most [MaxCropFraction] of the overflowing axis; otherwise zoom from Fit
 * up to that crop cap so letterboxing shrinks without aggressive truncation.
 */
object StillContentScale : ContentScale {

    const val MaxCropFraction = 0.20f

    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val scale = scale(
            srcW = srcSize.width,
            srcH = srcSize.height,
            dstW = dstSize.width,
            dstH = dstSize.height,
        )
        return ScaleFactor(scale, scale)
    }

    /**
     * Uniform scale factor to draw [srcW]×[srcH] into [dstW]×[dstH].
     * Between Fit (contain) and Crop (cover), never cropping more than [maxCrop].
     */
    fun scale(
        srcW: Float,
        srcH: Float,
        dstW: Float,
        dstH: Float,
        maxCrop: Float = MaxCropFraction,
    ): Float {
        if (srcW <= 0f || srcH <= 0f || dstW <= 0f || dstH <= 0f) return 1f
        val scaleContain = minOf(dstW / srcW, dstH / srcH)
        val scaleCover = maxOf(dstW / srcW, dstH / srcH)
        val cropCap = maxCrop.coerceIn(0f, 0.99f)
        val maxScale = scaleContain / (1f - cropCap)
        return minOf(scaleCover, maxScale)
    }

    /** Fraction of the overflowing axis that full Cover would discard (0 = same ratio). */
    fun coverCropFraction(
        srcW: Float,
        srcH: Float,
        dstW: Float,
        dstH: Float,
    ): Float {
        if (srcW <= 0f || srcH <= 0f || dstW <= 0f || dstH <= 0f) return 0f
        val srcRatio = srcW / srcH
        val dstRatio = dstW / dstH
        return if (srcRatio > dstRatio) {
            1f - dstRatio / srcRatio
        } else {
            1f - srcRatio / dstRatio
        }
    }
}
