package fr.geoking.arthur.auto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import fr.geoking.arthur.fractal.CustomFractalParams
import fr.geoking.arthur.fractal.CustomFractalQuality
import fr.geoking.arthur.fractal.CustomFractalStillRenderer
import fr.geoking.arthur.genart.GenartCatalog
import fr.geoking.arthur.genart.GenartStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.isGenerative
import android.os.Looper
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.SafeBitmapDecoder
import fr.geoking.arthur.source.StillImageDownloader
import java.io.File
import kotlin.random.Random

/**
 * Bakes a single static frame for Auto album art (no Compose / no animation).
 * [generation] shifts the procedural seed so Ambient Rotation refreshes look distinct.
 */
object AmbientStillRenderer {
    /** Near-4K square bake (4K UHD short side) — avoids pixelation on Auto / TV / phone Ambient. */
    const val SIZE = 2160

    fun renderToFile(
        artwork: Artwork,
        generation: Long,
        file: File,
        imageCache: ArtworkImageCache? = null,
        invalidStore: InvalidArtworkStore? = null,
    ) {
        val bitmap = render(artwork, generation, imageCache, invalidStore)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
    }

    fun render(
        artwork: Artwork,
        generation: Long,
        imageCache: ArtworkImageCache? = null,
        invalidStore: InvalidArtworkStore? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val seed = artwork.id.hashCode().toLong() xor (generation * 0x9E3779B9L)
        runCatching {
            when {
                artwork.kind == ArtworkKind.Genart ->
                    drawGenart(canvas, artwork.id, generation)
                artwork.kind == ArtworkKind.CustomFractal || CustomFractalSource.isCustomId(artwork.id) ->
                    drawCustomFractal(canvas, artwork.id, generation)
                artwork.isGenerative ->
                    drawFractalField(canvas, artwork.id, seed)
                !artwork.localPath.isNullOrBlank() || !artwork.remoteUrl.isNullOrBlank() -> {
                    val (drawn, reason) = drawStillImageWithResult(
                        canvas,
                        artwork.id,
                        artwork.localPath,
                        artwork.remoteUrl,
                        imageCache,
                        invalidStore,
                    )
                    if (!drawn) drawStillPlaceholder(canvas, seed, isError = true, errorReason = reason)
                }
                else -> drawStillPlaceholder(canvas, seed, isError = true)
            }
        }.onFailure { e ->
            drawStillPlaceholder(canvas, seed, isError = true, errorReason = e.message)
        }
        return bitmap
    }

    fun renderPlaceholder(artwork: Artwork, generation: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val seed = artwork.id.hashCode().toLong() xor (generation * 0x9E3779B9L)
        drawStillPlaceholder(canvas, seed, isError = false)
        return bitmap
    }

    /** On-demand bake when only the media id is known (browse icon URI). */
    fun renderForId(artworkId: String, generation: Long): Bitmap {
        val kind = kindForId(artworkId)
        return render(
            Artwork(
                id = artworkId,
                title = artworkId,
                sourceId = "auto",
                kind = kind,
            ),
            generation,
        )
    }

    fun kindForId(artworkId: String): ArtworkKind = when {
        artworkId.startsWith("genart.") -> ArtworkKind.Genart
        CustomFractalSource.isCustomId(artworkId) -> ArtworkKind.CustomFractal
        artworkId.startsWith("fractal.") -> ArtworkKind.FractalPreset
        artworkId.startsWith("rijks-") -> ArtworkKind.Painting
        artworkId.startsWith("bundled-") -> ArtworkKind.Painting
        else -> ArtworkKind.Photo
    }

    private fun drawStillImageWithResult(
        canvas: Canvas,
        artworkId: String,
        localPath: String?,
        remoteUrl: String?,
        imageCache: ArtworkImageCache?,
        invalidStore: InvalidArtworkStore? = null,
    ): Pair<Boolean, String?> {
        // 1. Check local path
        val fromFile = localPath?.takeIf { it.isNotBlank() }?.let { path ->
            SafeBitmapDecoder.decodeFile(path, SafeBitmapDecoder.AMBIENT_STILL_MAX_SIDE)
        }
        if (fromFile != null) {
            drawBitmapCover(canvas, fromFile)
            fromFile.recycle()
            return Pair(true, null)
        }

        // 2. Check disk cache
        val cachedPath = imageCache?.localPathOrNull(artworkId)
        if (cachedPath != null) {
            val fromCache = SafeBitmapDecoder.decodeFile(cachedPath, SafeBitmapDecoder.AMBIENT_STILL_MAX_SIDE)
            if (fromCache != null) {
                drawBitmapCover(canvas, fromCache)
                fromCache.recycle()
                return Pair(true, null)
            }
        }

        val url = remoteUrl?.takeIf { it.isNotBlank() } ?: return Pair(false, "No image URL provided")

        // 3. Do not execute network requests on the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return Pair(false, "Download skipped on main thread")
        }

        // 4. Download on background thread (Auto is never Wi‑Fi-gated)
        return try {
            val file = if (imageCache != null) {
                imageCache.downloadAndCache(artworkId, url, allowNetwork = true)
            } else {
                val tempFile = File.createTempFile("ambient_still_", ".tmp")
                try {
                    StillImageDownloader.downloadToFile(url, tempFile)
                } catch (e: Throwable) {
                    tempFile.delete()
                    throw e
                }
            }

            val bmp = SafeBitmapDecoder.decodeFile(
                file.absolutePath,
                SafeBitmapDecoder.AMBIENT_STILL_MAX_SIDE,
            ) ?: run {
                imageCache?.purgeInvalid(artworkId)
                invalidStore?.markInvalid(artworkId)
                throw java.io.IOException("Failed to decode image file")
            }
            drawBitmapCover(canvas, bmp)
            bmp.recycle()
            Pair(true, null)
        } catch (e: Throwable) {
            val httpCode = Regex("""HTTP (\d{3})""")
                .find(e.message.orEmpty())
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            if (StillImageDownloader.isNonRetryable(httpCode, e)) {
                invalidStore?.markInvalid(artworkId)
            }
            Pair(false, e.message ?: e.toString())
        }
    }

    /**
     * Full-bleed center-crop into the square bake so Auto Media host can
     * extract vibrant colors for its ambient backdrop (no framed gradient).
     */
    private fun drawBitmapCover(canvas: Canvas, bmp: Bitmap) {
        val scale = maxOf(SIZE.toFloat() / bmp.width, SIZE.toFloat() / bmp.height)
        val drawW = (bmp.width * scale).toInt().coerceAtLeast(1)
        val drawH = (bmp.height * scale).toInt().coerceAtLeast(1)
        val left = (SIZE - drawW) / 2
        val top = (SIZE - drawH) / 2
        val srcRect = Rect(0, 0, bmp.width, bmp.height)
        val dstRect = Rect(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bmp, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }

    internal fun extractGradientColors(bmp: Bitmap): Pair<Int, Int> {
        val w = bmp.width
        val h = bmp.height
        var topR = 0L
        var topG = 0L
        var topB = 0L
        var topCount = 0
        var botR = 0L
        var botG = 0L
        var botB = 0L
        var botCount = 0

        val stepX = (w / 12).coerceAtLeast(1)
        val stepY = (h / 12).coerceAtLeast(1)

        for (y in 0 until (h / 3) step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bmp.getPixel(x, y)
                topR += Color.red(pixel)
                topG += Color.green(pixel)
                topB += Color.blue(pixel)
                topCount++
            }
        }

        for (y in (2 * h / 3) until h step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bmp.getPixel(x, y)
                botR += Color.red(pixel)
                botG += Color.green(pixel)
                botB += Color.blue(pixel)
                botCount++
            }
        }

        val topAvg = if (topCount > 0) {
            Color.rgb((topR / topCount).toInt(), (topG / topCount).toInt(), (topB / topCount).toInt())
        } else {
            Color.rgb(30, 30, 45)
        }

        val botAvg = if (botCount > 0) {
            Color.rgb((botR / botCount).toInt(), (botG / botCount).toInt(), (botB / botCount).toInt())
        } else {
            Color.rgb(10, 10, 20)
        }

        val topColor = adjustToSpotifyTone(topAvg, targetValue = 0.32f)
        val botColor = adjustToSpotifyTone(botAvg, targetValue = 0.12f)
        return Pair(topColor, botColor)
    }

    private fun adjustToSpotifyTone(color: Int, targetValue: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceIn(0.25f, 0.75f)
        hsv[2] = targetValue
        return Color.HSVToColor(hsv)
    }

    private fun drawCustomFractal(canvas: Canvas, artworkId: String, generation: Long) {
        val params = CustomFractalParams.fromArtworkId(artworkId)
            ?: return drawStillPlaceholder(canvas, artworkId.hashCode().toLong() xor generation, isError = true)
        CustomFractalStillRenderer.draw(
            canvas = canvas,
            params = params,
            size = SIZE,
            generation = generation,
            quality = CustomFractalQuality.Medium,
        )
    }

    private fun drawStillPlaceholder(
        canvas: Canvas,
        seed: Long,
        isError: Boolean = false,
        errorReason: String? = null,
    ) {
        val rnd = Random(seed)
        val c1 = Color.rgb(12 + rnd.nextInt(20), 16 + rnd.nextInt(24), 32 + rnd.nextInt(40))
        val c2 = Color.rgb(8, 10, 24)
        canvas.drawPaint(
            Paint().apply {
                shader = RadialGradient(
                    SIZE * 0.5f,
                    SIZE * 0.42f,
                    SIZE * 0.7f,
                    c1,
                    c2,
                    Shader.TileMode.CLAMP,
                )
            },
        )
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 180, 190, 220)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(SIZE * 0.5f, SIZE * 0.48f, SIZE * 0.28f, accent)
        canvas.drawCircle(SIZE * 0.5f, SIZE * 0.48f, SIZE * 0.18f, accent)

        if (isError) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Artwork unavailable", SIZE * 0.5f, SIZE * 0.48f, textPaint)
            if (!errorReason.isNullOrBlank()) {
                val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(180, 255, 180, 180)
                    textSize = 20f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(errorReason, SIZE * 0.5f, SIZE * 0.54f, detailPaint)
            }
        }
    }

    private fun drawGenart(canvas: Canvas, artworkId: String, generation: Long) {
        val engine = GenartCatalog.engineForId(artworkId)
        if (engine == null) {
            drawStillPlaceholder(canvas, artworkId.hashCode().toLong() xor generation, isError = true)
            return
        }
        GenartStillRenderer.draw(
            canvas = canvas,
            engineId = engine,
            size = SIZE,
            generation = generation,
        )
    }

    private fun drawFractalField(canvas: Canvas, artworkId: String, seed: Long) {
        val rnd = Random(seed)
        val pixels = IntArray(SIZE * SIZE)
        val maxIter = 220
        val type = when {
            artworkId.contains("phoenix") -> "phoenix"
            artworkId.contains("julia") -> "julia"
            artworkId.contains("burning") -> "burningship"
            artworkId.contains("tricorn") -> "tricorn"
            artworkId.contains("multibrot") -> "multibrot"
            artworkId.contains("celtic") -> "celtic"
            artworkId.contains("buffalo") -> "buffalo"
            else -> "mandelbrot"
        }
        val (cx, cy, span) = when (type) {
            "julia" -> Triple(-0.4 + rnd.nextDouble() * 0.1, 0.6, 2.6)
            "burningship" -> Triple(-1.75, -0.04, 2.2)
            "tricorn" -> Triple(-0.2, rnd.nextDouble() * 0.04, 2.6)
            "multibrot" -> Triple(0.0, 0.0, 2.4)
            "celtic" -> Triple(-0.5, 0.0, 2.5)
            "buffalo" -> Triple(-0.65, -0.45, 2.4)
            "phoenix" -> Triple(0.0, 0.0, 2.8)
            else -> Triple(-0.55 + rnd.nextDouble() * 0.08, rnd.nextDouble() * 0.05, 2.6)
        }
        val scale = span / SIZE
        val hueBase = (seed and 0xFF).toInt()
        val isJuliaLike = type == "julia" || type == "phoenix"
        for (py in 0 until SIZE) {
            for (px in 0 until SIZE) {
                val cr = (px - SIZE / 2.0) * scale + cx
                val ci = (py - SIZE / 2.0) * scale + cy
                var zx = if (isJuliaLike) cr else 0.0
                var zy = if (isJuliaLike) ci else 0.0
                var prevX = 0.0
                var prevY = 0.0
                val jx = when (type) {
                    "julia" -> cx
                    "phoenix" -> 0.5667
                    else -> cr
                }
                val jy = when (type) {
                    "julia" -> cy
                    "phoenix" -> -0.5
                    else -> ci
                }
                var iter = 0
                var escaped = false
                var zr2 = 0.0
                var zi2 = 0.0
                while (iter < maxIter) {
                    zr2 = zx * zx
                    zi2 = zy * zy
                    if (zr2 + zi2 > 4.0) {
                        escaped = true
                        break
                    }
                    val nextX: Double
                    val nextY: Double
                    when (type) {
                        "tricorn" -> {
                            nextX = zr2 - zi2 + jx
                            nextY = -2.0 * zx * zy + jy
                        }
                        "burningship" -> {
                            nextX = kotlin.math.abs(zr2 - zi2 + jx)
                            nextY = 2.0 * kotlin.math.abs(zx * zy) + jy
                        }
                        "multibrot" -> {
                            nextX = zx * (zr2 - 3.0 * zi2) + jx
                            nextY = zy * (3.0 * zr2 - zi2) + jy
                        }
                        "celtic" -> {
                            nextX = kotlin.math.abs(zr2 - zi2) + jx
                            nextY = 2.0 * zx * zy + jy
                        }
                        "buffalo" -> {
                            nextX = kotlin.math.abs(zr2 - zi2) + jx
                            nextY = -kotlin.math.abs(2.0 * zx * zy) + jy
                        }
                        "phoenix" -> {
                            nextX = zr2 - zi2 + jx + jy * prevX
                            nextY = 2.0 * zx * zy + jy * prevY
                            prevX = zx
                            prevY = zy
                        }
                        else -> {
                            nextX = zr2 - zi2 + jx
                            nextY = 2.0 * zx * zy + jy
                        }
                    }
                    zx = nextX
                    zy = nextY
                    iter++
                }
                pixels[py * SIZE + px] = if (!escaped) {
                    Color.rgb(2, 6, 23)
                } else {
                    val power = if (type == "multibrot") 3.0 else 2.0
                    val logZn = kotlin.math.ln((zr2 + zi2).coerceAtLeast(1e-12)) / 2.0
                    val nu = kotlin.math.ln((logZn / kotlin.math.ln(2.0)).coerceAtLeast(1e-12)) /
                        kotlin.math.ln(power)
                    val continuous = (iter + 1.0 - nu).toFloat()
                    val t = (kotlin.math.ln(1.0 + continuous) / kotlin.math.ln(1.0 + maxIter))
                        .toFloat()
                    val cycles = 6.5f
                    val hue = ((t * cycles * 360f) + hueBase + continuous * 2.4f).mod(360f)
                    val sat = 0.62f + 0.28f * (1f - t)
                    val value = 0.42f + 0.55f * t
                    Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)))
                }
            }
        }
        canvas.drawBitmap(pixels, 0, SIZE, 0, 0, SIZE, SIZE, false, null)
    }
}
