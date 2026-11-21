package fr.geoking.arthur.genart

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp

/**
 * Live-render cost controls shared by [GenartEffectCanvas] and soft-glow engines.
 *
 * TV ambient uses [GenartQuality.Medium]: fewer primitives + smaller offscreen
 * buffers keep 1080p/4K panels fluid without a separate reduced-motion mode.
 */
internal object GenartPerf {
    /**
     * Fraction of the display size used for the live Canvas/AGSL buffer.
     * Output is upscaled with [GenartStillFx.FILL_SCALE] so the frame still fills.
     */
    fun liveRenderScale(quality: GenartQuality, minSidePx: Float): Float {
        val large = minSidePx >= 1080f
        return when (quality) {
            GenartQuality.Low -> if (large) 0.50f else 0.62f
            GenartQuality.Medium -> if (large) 0.62f else 0.78f
            GenartQuality.High -> if (large) 0.85f else 1f
        }
    }
}

/**
 * Soft glow layers are expensive on TV GPUs. Keep full blur only at High;
 * Medium uses a lighter radius; Low skips the offscreen blur pass.
 */
internal fun Modifier.genartSoftBlur(quality: GenartQuality, radius: Dp): Modifier =
    when (quality) {
        GenartQuality.High -> blur(radius)
        GenartQuality.Medium -> blur(radius * 0.45f)
        GenartQuality.Low -> this
    }
