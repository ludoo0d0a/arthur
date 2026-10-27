package fr.geoking.arthur.audio.engines

import android.content.Context
import java.io.File

/**
 * Manages rendered neural audio tracks (cache + durable take persistence).
 * Does not synthesize placeholder audio — engines must produce real output or fail.
 */
class AudioCacheManager(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "neural_audio_cache").apply { mkdirs() }
    private val takesDir = File(context.filesDir, "neural_takes").apply { mkdirs() }

    fun getCachedTrack(promptId: String, seed: Long): File? {
        val file = File(cacheDir, "track_${promptId}_$seed.wav")
        if (!file.exists() || file.length() <= 44L) return null
        // Reject near-silent / truncated PCM left by failed renders.
        if (wavPeakAbsolute(file) < MIN_AUDIBLE_PEAK) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    fun createCacheFile(promptId: String, seed: Long): File {
        return File(cacheDir, "track_${promptId}_$seed.wav")
    }

    /** Copies a generated cache WAV into durable app storage for Neural takes. */
    fun persistTakeAudio(source: File, takeId: String): File {
        takesDir.mkdirs()
        val target = File(takesDir, "$takeId.wav")
        source.copyTo(target, overwrite = true)
        return target
    }

    fun clearCache() {
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
    }

    companion object {
        /** Peak |sample| below this is treated as effectively silent. */
        const val MIN_AUDIBLE_PEAK = 500

        /** Scans PCM after a standard 44-byte WAV header; returns max |int16| sample. */
        fun wavPeakAbsolute(file: File, maxFramesToScan: Int = 44100 * 2): Int {
            if (!file.exists() || file.length() <= 44L) return 0
            return runCatching {
                file.inputStream().use { input ->
                    val header = ByteArray(44)
                    if (input.read(header) < 44) return@use 0
                    val bytesPerFrame = 4 // stereo 16-bit
                    val toRead = minOf(
                        (file.length() - 44).toInt(),
                        maxFramesToScan * bytesPerFrame,
                    )
                    val buf = ByteArray(8192)
                    var remaining = toRead
                    var peak = 0
                    while (remaining > 0) {
                        val n = input.read(buf, 0, minOf(buf.size, remaining))
                        if (n <= 0) break
                        var i = 0
                        while (i + 1 < n) {
                            val sample = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
                            val signed = sample.toShort().toInt()
                            val abs = if (signed < 0) -signed else signed
                            if (abs > peak) peak = abs
                            i += 2
                        }
                        remaining -= n
                    }
                    peak
                }
            }.getOrDefault(0)
        }
    }
}
