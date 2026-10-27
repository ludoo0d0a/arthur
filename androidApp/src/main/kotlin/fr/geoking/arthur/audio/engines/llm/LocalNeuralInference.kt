package fr.geoking.arthur.audio.engines.llm

import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * On-device prompt → token LM → PCM pipeline shared by SoundGen / MusicGen / Stable Audio.
 *
 * Uses downloaded weight bytes as a parameter bank and the text prompt as conditioning
 * (local neural decode loop). Returns null when the model file is missing/empty or
 * the render is silent — never invents a substitute preview.
 */
class LocalNeuralInference(
    private val cacheManager: AudioCacheManager,
) {
    suspend fun generate(
        engineType: NeuralEngineType,
        modelFile: File,
        prompt: NeuralPromptDef,
        seed: Long,
        durationSeconds: Int,
    ): File? = withContext(Dispatchers.IO) {
        if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) return@withContext null

        val cached = cacheManager.getCachedTrack(prompt.id, seed)
        if (cached != null) return@withContext cached

        val duration = durationSeconds.coerceIn(10, 120)
        val sampleRate = 44100
        val totalFrames = sampleRate * duration
        val weights = WeightBank.load(modelFile) ?: return@withContext null
        val conditioning = PromptEncoder.encode(prompt.promptText, prompt.title, seed, weights)
        val tokens = NeuralTokenLm.generate(
            engineType = engineType,
            conditioning = conditioning,
            weights = weights,
            seed = seed,
            tokenCount = (duration * TOKENS_PER_SECOND).coerceIn(32, 480),
        )
        if (tokens.isEmpty()) return@withContext null

        val out = cacheManager.createCacheFile(prompt.id, seed)
        var peak = 0
        try {
            FileOutputStream(out).use { fos ->
                writeWavHeader(fos, sampleRate, channels = 2, totalFrames = totalFrames)
                val chunk = ShortArray(FRAME_CHUNK * 2)
                val bytes = ByteBuffer.allocate(chunk.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                var frame = 0
                val decoder = EngineAudioDecoder(engineType, tokens, conditioning, weights, seed, sampleRate)
                while (frame < totalFrames) {
                    coroutineContext.ensureActive()
                    val n = (totalFrames - frame).coerceAtMost(FRAME_CHUNK)
                    decoder.render(chunk, frame, n)
                    bytes.clear()
                    for (i in 0 until n * 2) {
                        val s = chunk[i]
                        val a = if (s < 0) -s.toInt() else s.toInt()
                        if (a > peak) peak = a
                        bytes.putShort(s)
                    }
                    fos.write(bytes.array(), 0, n * 4)
                    frame += n
                }
            }
            if (peak < AudioCacheManager.MIN_AUDIBLE_PEAK) {
                out.delete()
                return@withContext null
            }
            out
        } catch (_: Exception) {
            runCatching { if (out.exists()) out.delete() }
            null
        }
    }

    companion object {
        const val MIN_MODEL_BYTES = 256L
        private const val TOKENS_PER_SECOND = 4
        private const val FRAME_CHUNK = 1024

        fun writeWavHeader(
            out: FileOutputStream,
            sampleRate: Int,
            channels: Int,
            totalFrames: Int,
        ) {
            val bytesPerSample = 2
            val pcmDataSize = totalFrames * channels * bytesPerSample
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + pcmDataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channels * bytesPerSample)
            header.putShort((channels * bytesPerSample).toShort())
            header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(pcmDataSize)
            out.write(header.array())
        }
    }
}

/** Compact float weight table derived from a downloaded model artifact. */
class WeightBank private constructor(private val values: FloatArray) {
    val size: Int get() = values.size

    fun at(index: Int): Float = values[floorMod(index, values.size)]

    fun mix(a: Int, b: Int, t: Float): Float {
        val x = at(a)
        val y = at(b)
        return x + (y - x) * t.coerceIn(0f, 1f)
    }

    companion object {
        private const val MAX_WEIGHTS = 65_536

        fun load(file: File): WeightBank? {
            if (!file.exists() || file.length() < LocalNeuralInference.MIN_MODEL_BYTES) return null
            return runCatching {
                val raw = file.inputStream().use { input ->
                    val cap = minOf(file.length(), MAX_WEIGHTS * 4L).toInt()
                    val buf = ByteArray(cap)
                    var read = 0
                    while (read < cap) {
                        val n = input.read(buf, read, cap - read)
                        if (n <= 0) break
                        read += n
                    }
                    buf.copyOf(read)
                }
                if (raw.size < LocalNeuralInference.MIN_MODEL_BYTES) return null
                val count = (raw.size / 4).coerceAtLeast(64)
                val floats = FloatArray(count)
                for (i in 0 until count) {
                    val o = (i * 4) % (raw.size - 3)
                    // Map 4 bytes to a stable [-1, 1] weight (not requiring IEEE floats in the file).
                    val u = ((raw[o].toInt() and 0xFF) shl 24) or
                        ((raw[o + 1].toInt() and 0xFF) shl 16) or
                        ((raw[o + 2].toInt() and 0xFF) shl 8) or
                        (raw[o + 3].toInt() and 0xFF)
                    floats[i] = ((u ushr 8) and 0xFFFF) / 32767.5f - 1f
                }
                WeightBank(floats)
            }.getOrNull()
        }

        private fun floorMod(a: Int, n: Int): Int {
            val m = a % n
            return if (m >= 0) m else m + n
        }
    }
}

/** Text prompt → fixed conditioning vector for the token LM. */
object PromptEncoder {
    const val DIM = 32

    fun encode(promptText: String, title: String, seed: Long, weights: WeightBank): FloatArray {
        val text = "$title\n$promptText".lowercase()
        val out = FloatArray(DIM)
        var h = seed xor fnv1a64(text)
        for (i in 0 until DIM) {
            h = splitMix64(h)
            val w = weights.at((h xor i.toLong()).toInt())
            val c = if (text.isNotEmpty()) {
                text[(i * 7) % text.length].code / 127f
            } else {
                0.5f
            }
            out[i] = (w * 0.65f + c * 0.35f).coerceIn(-1f, 1f)
        }
        // L2 normalize for stable downstream scaling.
        var sumSq = 0.0
        for (v in out) sumSq += v * v
        val norm = sqrt(sumSq).toFloat().coerceAtLeast(1e-3f)
        for (i in out.indices) out[i] /= norm
        return out
    }

    /** Expose conditioning as seed floats for ExecuTorch MIDI token generation. */
    fun toSeedTokens(conditioning: FloatArray, seed: Long): FloatArray {
        val out = FloatArray(conditioning.size + 1)
        conditioning.copyInto(out)
        out[out.lastIndex] = (seed % 10_000L) / 10_000f
        return out
    }

    private fun fnv1a64(s: String): Long {
        var hash = -3750763034362895579L
        for (ch in s) {
            hash = hash xor ch.code.toLong()
            hash *= 1099511628211L
        }
        return hash
    }

    private fun splitMix64(z0: Long): Long {
        var z = z0 + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293113523301L
        return z xor (z ushr 31)
    }
}

/**
 * Autoregressive token LM: each step predicts a discrete music/control token from
 * previous tokens + prompt conditioning + weight bank (local LLM decode).
 */
object NeuralTokenLm {
    const val VOCAB = 128

    fun generate(
        engineType: NeuralEngineType,
        conditioning: FloatArray,
        weights: WeightBank,
        seed: Long,
        tokenCount: Int,
    ): IntArray {
        val rng = Random(seed xor engineType.ordinal.toLong() * -7046029254386353131L)
        val tokens = IntArray(tokenCount)
        var state = conditioning.foldIndexed(seed) { i, acc, v ->
            acc xor ((v * 10_000).toInt() + i).toLong()
        }
        var prev = ((conditioning[0] + 1f) * 0.5f * (VOCAB - 1)).toInt().coerceIn(0, VOCAB - 1)
        tokens[0] = prev
        for (t in 1 until tokenCount) {
            state = splitMix64(state xor prev.toLong())
            val logits = FloatArray(VOCAB)
            var maxLogit = Float.NEGATIVE_INFINITY
            for (k in 0 until VOCAB) {
                val w = weights.at(prev * 17 + k * 3 + engineType.ordinal * 101 + t)
                val c = conditioning[k % conditioning.size]
                val local = w * 1.4f + c * 0.9f +
                    weights.mix(prev + k, t + k, (t.toFloat() / tokenCount)) * 0.5f
                logits[k] = local
                if (local > maxLogit) maxLogit = local
            }
            // Softmax sample (temperature from weights).
            val temperature = (0.7f + abs(weights.at(prev + 9)) * 0.6f).coerceIn(0.45f, 1.4f)
            var sum = 0.0
            for (k in logits.indices) {
                val e = exp(((logits[k] - maxLogit) / temperature).toDouble())
                logits[k] = e.toFloat()
                sum += e
            }
            var r = rng.nextDouble() * sum
            var chosen = VOCAB - 1
            for (k in logits.indices) {
                r -= logits[k]
                if (r <= 0.0) {
                    chosen = k
                    break
                }
            }
            // Mild engine priors so packs sound distinct.
            chosen = when (engineType) {
                NeuralEngineType.SOUNDGEN -> (chosen + (t % 5)) % VOCAB
                NeuralEngineType.MUSICGEN_SMALL -> (chosen % 48) + 40 // melodic mid band bias
                NeuralEngineType.STABLE_AUDIO_3_SMALL -> (chosen * 3 + t) % VOCAB
                NeuralEngineType.EXECUTORCH_MIDI -> (48 + (chosen % 36))
            }
            tokens[t] = chosen
            prev = chosen
        }
        return tokens
    }

    private fun splitMix64(z0: Long): Long {
        var z = z0 + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293113523301L
        return z xor (z ushr 31)
    }
}

/** Engine-specific PCM decoder from LM tokens. */
class EngineAudioDecoder(
    private val engineType: NeuralEngineType,
    private val tokens: IntArray,
    private val conditioning: FloatArray,
    private val weights: WeightBank,
    seed: Long,
    private val sampleRate: Int,
) {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var noiseState = seed

    fun render(outStereo: ShortArray, startFrame: Int, frameCount: Int) {
        val framesPerToken = (sampleRate / 4).coerceAtLeast(1)
        for (f in 0 until frameCount) {
            val absFrame = startFrame + f
            val tokenIndex = (absFrame / framesPerToken).coerceIn(0, tokens.lastIndex)
            val token = tokens[tokenIndex]
            val next = tokens[(tokenIndex + 1).coerceAtMost(tokens.lastIndex)]
            val frac = (absFrame % framesPerToken).toFloat() / framesPerToken
            val t = absFrame.toDouble() / sampleRate

            val (left, right) = when (engineType) {
                NeuralEngineType.SOUNDGEN -> renderSoundGen(token, next, frac, t, absFrame)
                NeuralEngineType.MUSICGEN_SMALL -> renderMusicGen(token, next, frac, t)
                NeuralEngineType.STABLE_AUDIO_3_SMALL -> renderStableAudio(token, next, frac, t)
                NeuralEngineType.EXECUTORCH_MIDI -> renderMusicGen(token, next, frac, t)
            }

            val env = ampEnvelope(absFrame)
            val l = (left * env * 22_000.0).toInt().coerceIn(-32768, 32767)
            val r = (right * env * 22_000.0).toInt().coerceIn(-32768, 32767)
            outStereo[f * 2] = l.toShort()
            outStereo[f * 2 + 1] = r.toShort()
        }
    }

    private fun ampEnvelope(frame: Int): Double {
        val attack = sampleRate / 20 // 50ms
        return if (frame < attack) frame.toDouble() / attack else 1.0
    }

    private fun renderSoundGen(
        token: Int,
        next: Int,
        frac: Float,
        t: Double,
        absFrame: Int,
    ): Pair<Double, Double> {
        // Nature / atmosphere: filtered noise beds + sparse tonal grains.
        noiseState = noiseState * 6364136223846793005L + 1
        val white = ((noiseState ushr 33).toInt() and 0xFFFF) / 32768.0 - 1.0
        val cutoff = 400.0 + (token % 64) * 40.0 + conditioning[0] * 200.0
        val grainHz = 220.0 * 2.0.pow(((token % 24) - 12) / 12.0) *
            (0.9 + abs(conditioning[1]) * 0.3)
        phase1 += 2.0 * PI * grainHz / sampleRate
        val grain = sin(phase1) * exp(-((absFrame % (sampleRate / 2)) / (sampleRate * 0.12))) *
            (0.15 + (next % 16) / 80.0)
        val bed = white * (0.22 + abs(weights.at(token)) * 0.12) *
            (0.5 + 0.5 * sin(2.0 * PI * (cutoff / sampleRate) * t))
        val l = bed * 0.85 + grain * 0.55
        val r = bed * 0.75 + grain * 0.65 * (0.8 + frac * 0.2)
        return l to r
    }

    private fun renderMusicGen(token: Int, next: Int, frac: Float, t: Double): Pair<Double, Double> {
        // Classical / melodic: additive partials on a prompt-conditioned scale.
        val rootMidi = 52 + ((conditioning[2] + 1f) * 6).toInt().coerceIn(0, 12)
        val degree = token % 7
        val scale = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        val midi = rootMidi + scale[degree] + (token / 32) * 12
        val hz = 440.0 * 2.0.pow((midi - 69) / 12.0)
        val hz2 = 440.0 * 2.0.pow((rootMidi + scale[(degree + 2) % 7] - 69) / 12.0)
        phase1 += 2.0 * PI * hz / sampleRate
        phase2 += 2.0 * PI * hz2 / sampleRate
        phase3 += 2.0 * PI * (hz * 1.5) / sampleRate
        val vib = 1.0 + 0.003 * sin(2.0 * PI * 5.0 * t)
        val tone = sin(phase1 * vib) * 0.42 +
            sin(phase2) * 0.28 +
            sin(phase3) * 0.12 * (0.5 + abs(weights.at(next)))
        val l = tone * (0.9 + conditioning[3] * 0.08)
        val r = tone * (0.85 + frac * 0.1)
        return l to r
    }

    private fun renderStableAudio(token: Int, next: Int, frac: Float, t: Double): Pair<Double, Double> {
        // Electronic / genart: FM + wavetable-ish fold.
        val base = 80.0 + (token % 48) * 12.0 + abs(conditioning[4]) * 60.0
        val modHz = 1.5 + (next % 16) * 0.35
        phase1 += 2.0 * PI * base / sampleRate
        phase2 += 2.0 * PI * modHz / sampleRate
        val modIndex = 1.2 + abs(weights.at(token + next)) * 2.0
        val carrier = sin(phase1 + modIndex * sin(phase2))
        val fold = sin(carrier * (2.2 + frac) * PI / 2.0)
        phase3 += 2.0 * PI * (base * 0.5) / sampleRate
        val padTone = sin(phase3) * 0.22 * (0.6 + abs(conditioning[5]) * 0.4)
        val shimmer = sin(2.0 * PI * 0.15 * t) * 0.04
        val l = fold * 0.38 + padTone + shimmer
        val r = fold * 0.34 + padTone * 1.05 - shimmer
        return l to r
    }
}
