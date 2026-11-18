#include <jni.h>
#include <android/log.h>

#include <SoundTouch.h>

#define LOG_TAG "MIXXX_NATIVE"

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace soundtouch;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeCreate(
        JNIEnv* env,
        jobject,
        jint channels,
        jint sampleRate) {

    try {

        auto* soundTouch =
                new SoundTouch();

        soundTouch->setChannels(
                static_cast<uint>(channels)
        );

        soundTouch->setSampleRate(
                static_cast<uint>(sampleRate)
        );

        LOGD(
                "CREATE -> channels=%d sampleRate=%d",
                channels,
                sampleRate
        );

        return reinterpret_cast<jlong>(
                soundTouch
        );

    } catch (...) {

        LOGE(
                "CREATE FAILED"
        );

        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeSetTempo(
        JNIEnv*,
jobject,
jlong handle,
        jfloat tempo) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return;
}

soundTouch->setTempo(
static_cast<float>(tempo)
);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeSetPitchSemitones(
        JNIEnv*,
jobject,
jlong handle,
        jfloat semitones) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return;
}

soundTouch->setPitchSemiTones(
static_cast<float>(semitones)
);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeSetRate(
        JNIEnv*,
jobject,
jlong handle,
        jfloat rate) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return;
}

soundTouch->setRate(
static_cast<float>(rate)
);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativePutSamples(
        JNIEnv* env,
jobject,
jlong handle,
        jshortArray samples,
jint sampleCount) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr ||
samples == nullptr) {

return;
}

jboolean copied = JNI_FALSE;

jshort* data =
        env->GetShortArrayElements(
                samples,
                &copied
        );

if (data == nullptr) {
return;
}

const uint channels =
        soundTouch->numChannels();

const uint frames =
        static_cast<uint>(
                sampleCount / channels
        );

soundTouch->putSamples(
reinterpret_cast<SAMPLETYPE*>(
data
),
frames
);

env->ReleaseShortArrayElements(
        samples,
        data,
        JNI_ABORT
);
}

extern "C"
JNIEXPORT jint JNICALL
        Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeReceiveSamples(
        JNIEnv* env,
        jobject,
        jlong handle,
jshortArray output) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr ||
output == nullptr) {

return 0;
}

const jsize length =
        env->GetArrayLength(
                output
        );

if (length <= 0) {
return 0;
}

jboolean copied = JNI_FALSE;

jshort* data =
        env->GetShortArrayElements(
                output,
                &copied
        );

if (data == nullptr) {
return 0;
}

const uint channels =
        soundTouch->numChannels();

const uint maxFrames =
        static_cast<uint>(
                length / channels
        );

const uint receivedFrames =
        soundTouch->receiveSamples(
                reinterpret_cast<SAMPLETYPE*>(
                        data
                ),
                maxFrames
        );

env->ReleaseShortArrayElements(
        output,
        data,
0
);

return static_cast<jint>(
        receivedFrames
);
}

extern "C"
JNIEXPORT jint JNICALL
        Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeNumSamples(
        JNIEnv*,
        jobject,
        jlong handle) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return 0;
}

return static_cast<jint>(
        soundTouch->numSamples()
);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeFlush(
        JNIEnv*,
jobject,
jlong handle) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return;
}

soundTouch->flush();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeClear(
        JNIEnv*,
jobject,
jlong handle) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

if (soundTouch == nullptr) {
return;
}

soundTouch->clear();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeSoundTouch_nativeDestroy(
        JNIEnv*,
jobject,
jlong handle) {

auto* soundTouch =
        reinterpret_cast<SoundTouch*>(
                handle
        );

delete soundTouch;
}