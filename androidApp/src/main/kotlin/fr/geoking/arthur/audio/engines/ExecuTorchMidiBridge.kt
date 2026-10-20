package fr.geoking.arthur.audio.engines

/** Native C++/JNI interface for ExecuTorch MIDI inference and FluidSynth PCM audio rendering with JVM unit test fallback. */
class ExecuTorchMidiBridge {

    val hasNativeLib: Boolean = try {
        System.loadLibrary("executorch_midi_jni")
        true
    } catch (e: UnsatisfiedLinkError) {
        false
    } catch (e: Exception) {
        false
    }

    fun init(soundFontPath: String?, modelPath: String?): Boolean {
        return if (hasNativeLib) {
            try { nativeInit(soundFontPath, modelPath) } catch (e: UnsatisfiedLinkError) { true }
        } else {
            true
        }
    }

    fun sendMidiNoteOn(channel: Int, key: Int, velocity: Int) {
        if (hasNativeLib) {
            try { nativeSendMidiNoteOn(channel, key, velocity) } catch (_: UnsatisfiedLinkError) {}
        }
    }

    fun sendMidiNoteOff(channel: Int, key: Int) {
        if (hasNativeLib) {
            try { nativeSendMidiNoteOff(channel, key) } catch (_: UnsatisfiedLinkError) {}
        }
    }

    fun renderPcmShorts(outBuffer: ShortArray, frameCount: Int): Int {
        if (hasNativeLib) {
            return try {
                nativeRenderPcmShorts(outBuffer, frameCount)
            } catch (e: UnsatisfiedLinkError) {
                fallbackRenderPcmShorts(outBuffer, frameCount)
            }
        }
        return fallbackRenderPcmShorts(outBuffer, frameCount)
    }

    fun generateMidiTokens(seedTokens: FloatArray?, maxTokens: Int): IntArray? {
        if (hasNativeLib) {
            return try {
                nativeGenerateMidiTokens(seedTokens, maxTokens)
            } catch (e: UnsatisfiedLinkError) {
                fallbackGenerateMidiTokens(maxTokens)
            }
        }
        return fallbackGenerateMidiTokens(maxTokens)
    }

    fun release() {
        if (hasNativeLib) {
            try { nativeRelease() } catch (_: UnsatisfiedLinkError) {}
        }
    }

    private fun fallbackRenderPcmShorts(outBuffer: ShortArray, frameCount: Int): Int {
        val samples = (frameCount * 2).coerceAtMost(outBuffer.size)
        for (i in 0 until samples) {
            val t = i.toDouble() / 44100.0
            outBuffer[i] = (kotlin.math.sin(2.0 * Math.PI * 440.0 * t) * 16384.0).toInt().toShort()
        }
        return frameCount
    }

    private fun fallbackGenerateMidiTokens(maxTokens: Int): IntArray {
        val count = if (maxTokens > 0) maxTokens else 64
        return IntArray(count) { 60 + (it % 12) }
    }

    private external fun nativeInit(soundFontPath: String?, modelPath: String?): Boolean
    private external fun nativeSendMidiNoteOn(channel: Int, key: Int, velocity: Int)
    private external fun nativeSendMidiNoteOff(channel: Int, key: Int)
    private external fun nativeRenderPcmShorts(outBuffer: ShortArray, frameCount: Int): Int
    private external fun nativeGenerateMidiTokens(seedTokens: FloatArray?, maxTokens: Int): IntArray?
    private external fun nativeRelease()
}
