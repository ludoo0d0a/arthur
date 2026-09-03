package fr.geoking.arthur.auto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.isGenerative
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bakes a single static frame for Auto album art (no Compose / no animation).
 * [generation] shifts the procedural seed so Ambient Rotation refreshes look distinct.
 */
object AmbientStillRenderer {
    const val SIZE = 720

    fun renderToFile(artwork: Artwork, generation: Long, file: File) {
        val bitmap = render(artwork, generation)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
    }

    fun render(artwork: Artwork, generation: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val seed = artwork.id.hashCode().toLong() xor (generation * 0x9E3779B9L)
        when {
            artwork.kind == ArtworkKind.Genart -> drawGenart(canvas, artwork.id, seed)
            artwork.isGenerative -> drawFractalField(canvas, artwork.id, seed)
            else -> drawStillPlaceholder(canvas, seed)
        }
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
        artworkId.startsWith("fractal.") -> ArtworkKind.FractalPreset
        artworkId.startsWith("rijks-") -> ArtworkKind.Painting
        artworkId.startsWith("bundled-") -> ArtworkKind.Painting
        else -> ArtworkKind.Photo
    }

    private fun drawStillPlaceholder(canvas: Canvas, seed: Long) {
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
    }

    private fun drawGenart(canvas: Canvas, artworkId: String, seed: Long) {
        val rnd = Random(seed)
        canvas.drawColor(Color.rgb(2, 6, 23))
        canvas.drawPaint(
            Paint().apply {
                shader = RadialGradient(
                    SIZE * 0.5f,
                    SIZE * 0.45f,
                    SIZE * 0.85f,
                    Color.rgb(11, 18, 32),
                    Color.rgb(2, 6, 23),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        val count = when {
            artworkId.contains("particles") -> 64
            artworkId.contains("tunnel") -> 28
            else -> 40
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until count) {
            val t = (i + rnd.nextFloat()) / count
            val angle = t * 2f * PI.toFloat() * (1.5f + rnd.nextFloat())
            val radius = SIZE * (0.08f + 0.38f * t)
            val x = SIZE * 0.5f + cos(angle) * radius
            val y = SIZE * 0.48f + sin(angle * 0.9f) * radius * 0.85f
            paint.color = Color.argb(
                90 + rnd.nextInt(120),
                80 + rnd.nextInt(140),
                120 + rnd.nextInt(100),
                180 + rnd.nextInt(60),
            )
            canvas.drawCircle(x, y, 3f + rnd.nextFloat() * 8f, paint)
        }
    }

    private fun drawFractalField(canvas: Canvas, artworkId: String, seed: Long) {
        val rnd = Random(seed)
        val pixels = IntArray(SIZE * SIZE)
        val maxIter = 48
        val cx = when {
            artworkId.contains("julia") -> -0.4 + rnd.nextDouble() * 0.1
            artworkId.contains("burning") -> -0.45
            artworkId.contains("tricorn") -> 0.0
            else -> -0.55 + rnd.nextDouble() * 0.08
        }
        val cy = when {
            artworkId.contains("julia") -> 0.6
            else -> 0.0 + rnd.nextDouble() * 0.05
        }
        val scale = 2.6 / SIZE
        for (py in 0 until SIZE) {
            for (px in 0 until SIZE) {
                var zx = (px - SIZE / 2.0) * scale + cx
                var zy = (py - SIZE / 2.0) * scale + cy
                var iter = 0
                while (iter < maxIter && zx * zx + zy * zy < 4.0) {
                    val xt = if (artworkId.contains("burning")) {
                        zx * zx - zy * zy + cx
                    } else {
                        zx * zx - zy * zy + cx
                    }
                    val yt = if (artworkId.contains("tricorn")) {
                        -2.0 * zx * zy + cy
                    } else if (artworkId.contains("burning")) {
                        2.0 * kotlin.math.abs(zx * zy) + cy
                    } else {
                        2.0 * zx * zy + cy
                    }
                    zx = if (artworkId.contains("burning")) kotlin.math.abs(xt) else xt
                    zy = yt
                    iter++
                }
                pixels[py * SIZE + px] = if (iter >= maxIter) {
                    Color.rgb(2, 6, 23)
                } else {
                    val h = (iter * 7 + (seed and 0xFF).toInt()) % 360
                    Color.HSVToColor(floatArrayOf(h.toFloat(), 0.55f, 0.35f + iter / maxIter.toFloat() * 0.55f))
                }
            }
        }
        canvas.drawBitmap(pixels, 0, SIZE, 0, 0, SIZE, SIZE, false, null)
    }
}
