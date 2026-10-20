package fr.geoking.arthur.audio.engines

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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

        // Optional .pte when present; SoundFont is the downloaded artifact (public FluidR3).
        val modelFile = downloader.getModelFile(engineType).takeIf { it.exists() && it.length() > 0 }
        val soundFontFile = downloader.getSoundFontFile(
            ExecuTorchMidiPresets.DEFAULT_SOUNDFONT.soundFontFileName,
        )

        bridge.init(
            soundFontPath = if (soundFontFile.exists()) soundFontFile.absolutePath else null,
            modelPath = modelFile?.absolutePath,
        )

        val outputFile = cacheManager.createCacheFile(prompt.id, randomSeed)
        try {
            val sampleRate = 44100
            val numChannels = 2
            val totalFrames = sampleRate * targetDuration
            val frameChunkSize = 1024

            // Generate MIDI sequence tokens using ExecuTorch model or procedural MIDI generator
            val midiTokens = bridge.generateMidiTokens(
                seedTokens = floatArrayOf(randomSeed.toFloat()),
                maxTokens = 128,
            ) ?: IntArray(64) { 60 + (it % 12) }

            // Trigger initial MIDI notes
            for ((index, note) in midiTokens.withIndex()) {
                val channel = index % 4
                val velocity = 60 + (index % 40)
                bridge.sendMidiNoteOn(channel, note, velocity)
            }

            // Render PCM audio into WAV file
            FileOutputStream(outputFile).use { fos ->
                writeWavHeader(fos, sampleRate, numChannels, totalFrames)

                val shortBuffer = ShortArray(frameChunkSize * numChannels)
                val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                var framesRendered = 0
                while (framesRendered < totalFrames) {
                    val framesToRender = (totalFrames - framesRendered).coerceAtMost(frameChunkSize)
                    val rendered = bridge.renderPcmShorts(shortBuffer, framesToRender)
                    if (rendered <= 0) break

                    byteBuffer.clear()
                    for (i in 0 until rendered * numChannels) {
                        byteBuffer.putShort(shortBuffer[i])
                    }
                    fos.write(byteBuffer.array(), 0, rendered * numChannels * 2)

                    // Periodically release and trigger new MIDI notes
                    if ((framesRendered / frameChunkSize) % 8 == 0) {
                        val prevNote = midiTokens[(framesRendered / frameChunkSize) % midiTokens.size]
                        bridge.sendMidiNoteOff(0, prevNote)

                        val newNote = 55 + ((framesRendered / frameChunkSize) % 24)
                        bridge.sendMidiNoteOn(0, newNote, 70)
                    }

                    framesRendered += rendered
                }
            }

            bridge.release()
            outputFile
        } catch (e: Exception) {
            bridge.release()
            // Fallback sample WAV generation
            cacheManager.generateFallbackSampleWav(prompt, randomSeed, targetDuration)
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
