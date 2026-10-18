package fr.geoking.arthur.audio.engines

import android.content.Context
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.sin

/**
 * Manages rendered 2-minute max audio tracks with prompt + seed caching and fallback sample generation.
 */
class AudioCacheManager(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "neural_audio_cache").apply { mkdirs() }

    fun getCachedTrack(promptId: String, seed: Long): File? {
        val file = File(cacheDir, "track_${promptId}_$seed.wav")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun createCacheFile(promptId: String, seed: Long): File {
        return File(cacheDir, "track_${promptId}_$seed.wav")
    }

    /**
     * Synthesizes a clean sample ambient WAV track (up to 2 minutes) as a fallback preview
     * when AI model weights are downloading or unready.
     */
    suspend fun generateFallbackSampleWav(
        prompt: NeuralPromptDef,
        seed: Long,
        durationSeconds: Int = 120,
    ): File = withContext(Dispatchers.IO) {
        val cached = getCachedTrack(prompt.id, seed)
        if (cached != null) return@withContext cached

        val targetFile = createCacheFile(prompt.id, seed)
        val clampedDuration = durationSeconds.coerceIn(10, 120)
        val sampleRate = 22050
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * clampedDuration
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)

        FileOutputStream(targetFile).use { out ->
            // Write WAV Header
            out.write("RIFF".toByteArray())
            out.write(intToBytes(36 + dataSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToBytes(16)) // Chunk size
            out.write(shortToBytes(1.toShort())) // PCM format
            out.write(shortToBytes(numChannels.toShort()))
            out.write(intToBytes(sampleRate))
            out.write(intToBytes(sampleRate * numChannels * (bitsPerSample / 8)))
            out.write(shortToBytes((numChannels * (bitsPerSample / 8)).toShort()))
            out.write(shortToBytes(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intToBytes(dataSize))

            // Generate peaceful ambient harmonic chord tones based on seed and prompt
            val baseFreq = 110.0 + (seed % 12) * 10.0
            val harmonic1 = baseFreq * 1.5
            val harmonic2 = baseFreq * 2.0

            val bufferSize = 4096
            val buffer = ByteArray(bufferSize * 4)
            var sampleIndex = 0

            while (sampleIndex < totalSamples) {
                val samplesToWrite = minOf(bufferSize, totalSamples - sampleIndex)
                var byteIdx = 0

                for (i in 0 until samplesToWrite) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val envelope = sin(Math.PI * (sampleIndex + i) / totalSamples) // Smooth fade in and out

                    val waveLeft = (sin(2.0 * Math.PI * baseFreq * t) * 0.4 +
                            sin(2.0 * Math.PI * harmonic1 * t) * 0.3) * envelope
                    val waveRight = (sin(2.0 * Math.PI * baseFreq * t) * 0.4 +
                            sin(2.0 * Math.PI * harmonic2 * t) * 0.3) * envelope

                    val sampleL = (waveLeft * 16384).toInt().coerceIn(-32768, 32767).toShort()
                    val sampleR = (waveRight * 16384).toInt().coerceIn(-32768, 32767).toShort()

                    buffer[byteIdx++] = (sampleL.toInt() and 0xFF).toByte()
                    buffer[byteIdx++] = (sampleL.toInt() shr 8 and 0xFF).toByte()
                    buffer[byteIdx++] = (sampleR.toInt() and 0xFF).toByte()
                    buffer[byteIdx++] = (sampleR.toInt() shr 8 and 0xFF).toByte()
                }

                out.write(buffer, 0, samplesToWrite * 4)
                sampleIndex += samplesToWrite
            }
        }

        targetFile
    }

    fun clearCache() {
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 24 and 0xFF).toByte()
    )

    private fun shortToBytes(value: Short): ByteArray = byteArrayOf(
        (value.toInt() and 0xFF).toByte(),
        (value.toInt() shr 8 and 0xFF).toByte()
    )
}
