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

    /** Continuous oscillator phase for the JVM fallback renderer (must not reset per chunk). */
    private var fallbackPhaseRadians = 0.0
    private var fallbackNativeOk = false

    fun init(soundFontPath: String?, modelPath: String?): Boolean {
        fallbackPhaseRadians = 0.0
        fallbackNativeOk = false
        return if (hasNativeLib) {
            try {
                fallbackNativeOk = nativeInit(soundFontPath, modelPath)
                fallbackNativeOk
            } catch (e: UnsatisfiedLinkError) {
                fallbackNativeOk = false
                true
            }
        } else {
            true
        }
    }

    fun sendMidiNoteOn(channel: Int, key: Int, velocity: Int) {
        if (hasNativeLib && fallbackNativeOk) {
            try {
                nativeSendMidiNoteOn(channel, key, velocity)
            } catch (_: UnsatisfiedLinkError) {
                fallbackNativeOk = false
            }
        }
    }

    fun sendMidiNoteOff(channel: Int, key: Int) {
        if (hasNativeLib && fallbackNativeOk) {
            try {
                nativeSendMidiNoteOff(channel, key)
            } catch (_: UnsatisfiedLinkError) {
                fallbackNativeOk = false
            }
        }
    }

    fun renderPcmShorts(outBuffer: ShortArray, frameCount: Int): Int {
        if (hasNativeLib && fallbackNativeOk) {
            return try {
                val rendered = nativeRenderPcmShorts(outBuffer, frameCount)
                if (rendered > 0) rendered else fallbackRenderPcmShorts(outBuffer, frameCount)
            } catch (e: UnsatisfiedLinkError) {
                fallbackNativeOk = false
                fallbackRenderPcmShorts(outBuffer, frameCount)
            }
        }
        return fallbackRenderPcmShorts(outBuffer, frameCount)
    }

    fun generateMidiTokens(seedTokens: FloatArray?, maxTokens: Int): IntArray? {
        if (hasNativeLib && fallbackNativeOk) {
            return try {
                nativeGenerateMidiTokens(seedTokens, maxTokens)
            } catch (e: UnsatisfiedLinkError) {
                fallbackNativeOk = false
                fallbackGenerateMidiTokens(maxTokens)
            }
        }
        return fallbackGenerateMidiTokens(maxTokens)
    }

    fun release() {
        if (hasNativeLib) {
            try {
                nativeRelease()
            } catch (_: UnsatisfiedLinkError) {
            }
        }
        fallbackNativeOk = false
        fallbackPhaseRadians = 0.0
    }

    private fun fallbackRenderPcmShorts(outBuffer: ShortArray, frameCount: Int): Int {
        val frames = frameCount.coerceAtMost(outBuffer.size / 2)
        val twoPi = 2.0 * Math.PI
        val phaseStep = twoPi * 440.0 / 44100.0
        for (frame in 0 until frames) {
            val sample = (kotlin.math.sin(fallbackPhaseRadians) * 16384.0).toInt().toShort()
            outBuffer[frame * 2] = sample
            outBuffer[frame * 2 + 1] = sample
            fallbackPhaseRadians += phaseStep
            if (fallbackPhaseRadians >= twoPi) fallbackPhaseRadians -= twoPi
        }
        return frames
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
