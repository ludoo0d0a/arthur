package fr.geoking.arthur.fractal

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Shared AGSL host matching the tbahlai/agsl pattern: cache size + palette once,
 * update motion uniforms only in [onDrawFrame].
 *
 * Callers supply size ([Modifier.fillMaxSize] or a reduced [Modifier.requiredSize]).
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun AgslShaderBox(
    shader: RuntimeShader,
    modifier: Modifier = Modifier,
    /** Re-run size/color setup when this key changes (e.g. palette seed). */
    cacheKey: Any? = null,
    onConfigure: (size: Size, shader: RuntimeShader) -> Unit,
    onDrawFrame: (size: Size, shader: RuntimeShader) -> Unit,
) {
    Box(
        modifier = modifier.drawWithCache {
            @Suppress("UNUSED_EXPRESSION")
            cacheKey
            onConfigure(size, shader)
            val brush = ShaderBrush(shader)
            onDrawBehind {
                onDrawFrame(size, shader)
                drawRect(brush = brush)
            }
        },
    )
}
