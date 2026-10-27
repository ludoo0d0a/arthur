package fr.geoking.arthur.audio.engines

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import fr.geoking.arthur.audio.engines.llm.PromptEncoder
import fr.geoking.arthur.audio.engines.llm.NeuralTokenLm
import fr.geoking.arthur.audio.engines.llm.WeightBank
import fr.geoking.arthur.shared.audio.ExecuTorchMidiPresets
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * On-device local AI music generation engine executing MIDI generation via ExecuTorch
 * and synthesizing real-time audio through FluidSynth & SoundFont into PCM/WAV and AudioTrack.
 */
class ExecuTorchMidiEngine(
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
    private val bridge: ExecuTorchMidiBridge = ExecuTorchMidiBridge(),
) : NeuralMusicEngine {

    override val engineType: NeuralEngineType = NeuralEngineType.EXECUTORCH_MIDI
    override val pack: NeuralAudioPack = NeuralAudioPack.EXECUTORCH_MIDI

    override fun isModelReady(): Boolean = downloader.isModelAvailable(engineType)

    override suspend fun generateTrack(
        prompt: NeuralPromptDef,
        randomSeed: Long,
        durationSeconds: Int,
    ): File? = withContext(Dispatchers.IO) {
        val targetDuration = durationSeconds.coerceIn(5, 120)
        val cachedFile = cacheManager.getCachedTrack(prompt.id, randomSeed)
        if (cachedFile != null) return@withContext cachedFile

        if (!isModelReady()) return@withContext null

        // SoundFont is the required artifact; optional .pte supplies extra weight conditioning.
        val modelFile = downloader.getModelFile(engineType).takeIf { it.exists() && it.length() > 0 }
        val soundFontFile = downloader.getSoundFontFile(
            ExecuTorchMidiPresets.DEFAULT_SOUNDFONT.soundFontFileName,
        )
        if (!soundFontFile.exists() || soundFontFile.length() <= 0L) return@withContext null

        // Prompt → conditioning → MIDI token LM (same LLM path as the other neural packs).
        val weightSource = modelFile ?: soundFontFile
        val weights = WeightBank.load(weightSource) ?: return@withContext null
        val conditioning = PromptEncoder.encode(prompt.promptText, prompt.title, randomSeed, weights)
        val seedTokens = PromptEncoder.toSeedTokens(conditioning, randomSeed)
        val llmMidi = NeuralTokenLm.generate(
            engineType = NeuralEngineType.EXECUTORCH_MIDI,
            conditioning = conditioning,
            weights = weights,
            seed = randomSeed,
            tokenCount = 32,
        ).map { it.coerceIn(48, 84) }.toIntArray()

        bridge.init(
            soundFontPath = soundFontFile.absolutePath,
            modelPath = modelFile?.absolutePath,
        )

        val outputFile = cacheManager.createCacheFile(prompt.id, randomSeed)
        try {
            val sampleRate = 44100
            val numChannels = 2
            val totalFrames = sampleRate * targetDuration
            val frameChunkSize = 1024

            // Prefer bridge native tokens when the .pte is present; otherwise LLM MIDI.
            val bridgeTokens = bridge.generateMidiTokens(seedTokens = seedTokens, maxTokens = 32)
            val midiTokens = when {
                modelFile != null && bridgeTokens != null && bridgeTokens.isNotEmpty() ->
                    IntArray(bridgeTokens.size) { i ->
                        // Blend native + LLM so the prompt still steers pitch.
                        ((bridgeTokens[i] + llmMidi[i % llmMidi.size]) / 2).coerceIn(48, 84)
                    }
                else -> llmMidi
            }

            // Seed a small chord so the first render chunk is never silent.
            bridge.sendMidiNoteOn(0, midiTokens[0], 90)
            if (midiTokens.size > 1) bridge.sendMidiNoteOn(1, midiTokens[1], 70)
            if (midiTokens.size > 2) bridge.sendMidiNoteOn(2, midiTokens[2], 60)

            var framesRendered = 0
            var peakAbs = 0
            FileOutputStream(outputFile).use { fos ->
                writeWavHeader(fos, sampleRate, numChannels, totalFrames)

                val shortBuffer = ShortArray(frameChunkSize * numChannels)
                val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                while (framesRendered < totalFrames) {
                    val framesToRender = (totalFrames - framesRendered).coerceAtMost(frameChunkSize)
                    val rendered = bridge.renderPcmShorts(shortBuffer, framesToRender)
                    if (rendered <= 0) break

                    byteBuffer.clear()
                    val sampleCount = rendered * numChannels
                    for (i in 0 until sampleCount) {
                        val s = shortBuffer[i]
                        val abs = if (s < 0) -s.toInt() else s.toInt()
                        if (abs > peakAbs) peakAbs = abs
                        byteBuffer.putShort(s)
                    }
                    fos.write(byteBuffer.array(), 0, sampleCount * 2)

                    // Advance melody from LLM MIDI sequence (after first chunk).
                    val chunkIndex = framesRendered / frameChunkSize
                    if (chunkIndex > 0 && chunkIndex % 8 == 0) {
                        val prevNote = midiTokens[chunkIndex % midiTokens.size]
                        bridge.sendMidiNoteOff(0, prevNote)
                        val newNote = midiTokens[(chunkIndex + 1) % midiTokens.size]
                        bridge.sendMidiNoteOn(0, newNote, 80)
                    }

                    framesRendered += rendered
                }
            }

            bridge.release()

            val completeEnough = framesRendered >= totalFrames * 9 / 10
            val audible = peakAbs >= AudioCacheManager.MIN_AUDIBLE_PEAK
            if (completeEnough && audible) {
                outputFile
            } else {
                runCatching { outputFile.delete() }
                null
            }
        } catch (e: Exception) {
            bridge.release()
            runCatching { if (outputFile.exists()) outputFile.delete() }
            null
        }
    }

    /**
     * Streams real-time synthesized PCM audio directly into Android [AudioTrack].
     */
    fun playDirectToAudioTrack(
        randomSeed: Long = System.currentTimeMillis(),
        durationSeconds: Int = 120,
        onFrameRendered: ((frames: Int) -> Unit)? = null,
    ) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        val frameChunkSize = 512
        val shortBuffer = ShortArray(frameChunkSize * 2)
        var totalFrames = sampleRate * durationSeconds

        while (totalFrames > 0 && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            val rendered = bridge.renderPcmShorts(shortBuffer, frameChunkSize)
            if (rendered <= 0) break

            audioTrack.write(shortBuffer, 0, rendered * 2)
            onFrameRendered?.invoke(rendered)
            totalFrames -= rendered
        }

        audioTrack.stop()
        audioTrack.release()
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        totalFrames: Int,
    ) {
        val bytesPerSample = 2
        val pcmDataSize = totalFrames * channels * bytesPerSample
        val chunkSizeBytes = 36 + pcmDataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(chunkSizeBytes)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size
        header.putShort(1.toShort()) // AudioFormat (PCM = 1)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * bytesPerSample) // ByteRate
        header.putShort((channels * bytesPerSample).toShort()) // BlockAlign
        header.putShort(16.toShort()) // BitsPerSample
        header.put("data".toByteArray())
        header.putInt(pcmDataSize)

        out.write(header.array())
    }
}
