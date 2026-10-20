#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cmath>
#include <cstdlib>

#define LOG_TAG "ExecuTorchMidiJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Native synthesizer state for ExecuTorch MIDI & FluidSynth synthesis bridge
struct NativeSynthContext {
    std::string soundFontPath;
    std::string modelPath;
    bool isInitialized = false;
    double sampleRate = 44100.0;

    // Internal polyphonic oscillator state for software FluidSynth fallback synthesis
    struct ActiveVoice {
        int note = 60;
        int velocity = 64;
        int channel = 0;
        double phase = 0.0;
        double freq = 440.0;
        double envelope = 1.0;
        bool active = true;
    };
    std::vector<ActiveVoice> activeVoices;
};

static NativeSynthContext g_synthContext;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jstring soundFontPath,
    jstring modelPath
) {
    const char* sfPath = soundFontPath ? env->GetStringUTFChars(soundFontPath, nullptr) : "";
    const char* mPath = modelPath ? env->GetStringUTFChars(modelPath, nullptr) : "";

    g_synthContext.soundFontPath = sfPath;
    g_synthContext.modelPath = mPath;
    g_synthContext.isInitialized = true;
    g_synthContext.activeVoices.clear();

    LOGI("Native ExecuTorch MIDI Bridge initialized with SoundFont: %s, Model: %s", sfPath, mPath);

    if (soundFontPath) env->ReleaseStringUTFChars(soundFontPath, sfPath);
    if (modelPath) env->ReleaseStringUTFChars(modelPath, mPath);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeSendMidiNoteOn(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint channel,
    jint key,
    jint velocity
) {
    if (!g_synthContext.isInitialized) return;

    double freq = 440.0 * std::pow(2.0, (key - 69) / 12.0);
    NativeSynthContext::ActiveVoice voice;
    voice.note = key;
    voice.velocity = velocity;
    voice.channel = channel;
    voice.phase = 0.0;
    voice.freq = freq;
    voice.envelope = velocity / 127.0;
    voice.active = true;

    g_synthContext.activeVoices.push_back(voice);
    LOGI("MIDI Note On: channel=%d, key=%d, velocity=%d", channel, key, velocity);
}

JNIEXPORT void JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeSendMidiNoteOff(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint channel,
    jint key
) {
    if (!g_synthContext.isInitialized) return;

    for (auto& voice : g_synthContext.activeVoices) {
        if (voice.active && voice.channel == channel && voice.note == key) {
            voice.active = false;
        }
    }
    LOGI("MIDI Note Off: channel=%d, key=%d", channel, key);
}

JNIEXPORT jint JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeRenderPcmShorts(
    JNIEnv* env,
    jobject /* thiz */,
    jshortArray outBuffer,
    jint frameCount
) {
    if (!g_synthContext.isInitialized || !outBuffer || frameCount <= 0) return 0;

    jshort* buffer = env->GetShortArrayElements(outBuffer, nullptr);
    if (!buffer) return 0;

    int totalSamples = frameCount * 2; // Stereo
    for (int i = 0; i < totalSamples; i++) {
        buffer[i] = 0;
    }

    double dt = 1.0 / g_synthContext.sampleRate;

    for (int frame = 0; frame < frameCount; frame++) {
        double leftSample = 0.0;
        double rightSample = 0.0;

        for (auto& voice : g_synthContext.activeVoices) {
            if (!voice.active && voice.envelope <= 0.001) continue;

            double sample = std::sin(voice.phase) * voice.envelope * 0.25;
            leftSample += sample;
            rightSample += sample;

            voice.phase += 2.0 * M_PI * voice.freq * dt;
            if (voice.phase >= 2.0 * M_PI) voice.phase -= 2.0 * M_PI;

            if (!voice.active) {
                voice.envelope *= 0.9995; // Gentle release decay
            }
        }

        // Clip protection
        if (leftSample > 1.0) leftSample = 1.0;
        if (leftSample < -1.0) leftSample = -1.0;
        if (rightSample > 1.0) rightSample = 1.0;
        if (rightSample < -1.0) rightSample = -1.0;

        buffer[frame * 2] = static_cast<jshort>(leftSample * 32767.0);
        buffer[frame * 2 + 1] = static_cast<jshort>(rightSample * 32767.0);
    }

    // Clean up finished voices
    auto& voices = g_synthContext.activeVoices;
    voices.erase(
        std::remove_if(voices.begin(), voices.end(), [](const NativeSynthContext::ActiveVoice& v) {
            return !v.active && v.envelope <= 0.001;
        }),
        voices.end()
    );

    env->ReleaseShortArrayElements(outBuffer, buffer, 0);
    return frameCount;
}

JNIEXPORT jintArray JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeGenerateMidiTokens(
    JNIEnv* env,
    jobject /* thiz */,
    jfloatArray seedTokens,
    jint maxTokens
) {
    int tokenCount = maxTokens > 0 ? maxTokens : 64;
    std::vector<jint> generatedTokens(tokenCount);

    // Simulated ExecuTorch MIDI model inference sequence (note on, note off, pitch, velocity, delta time)
    for (int i = 0; i < tokenCount; i++) {
        generatedTokens[i] = static_cast<jint>(60 + (i % 12));
    }

    jintArray resultArray = env->NewIntArray(tokenCount);
    if (resultArray) {
        env->SetIntArrayRegion(resultArray, 0, tokenCount, generatedTokens.data());
    }

    return resultArray;
}

JNIEXPORT void JNICALL
Java_fr_geoking_arthur_audio_engines_ExecuTorchMidiBridge_nativeRelease(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    g_synthContext.activeVoices.clear();
    g_synthContext.isInitialized = false;
    LOGI("Native ExecuTorch MIDI Bridge released.");
}

} // extern "C"
