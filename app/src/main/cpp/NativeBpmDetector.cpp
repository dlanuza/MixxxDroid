#include <jni.h>
#include <android/log.h>

#include <BPMDetect.h>

#define LOG_TAG "MIXXX_BPM"

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace soundtouch;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeBpmDetector_nativeCreate(
        JNIEnv *,
        jobject,
        jint channels,
        jint sampleRate) {

    try {

        auto *detector =
                new BPMDetect(
                        channels,
                        sampleRate
                );

        LOGD(
                "BPM DETECTOR CREATED -> channels=%d sampleRate=%d",
                channels,
                sampleRate
        );

        return reinterpret_cast<jlong>(
                detector
        );

    } catch (...) {

        LOGE("BPM CREATE FAILED");

        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeBpmDetector_nativePutSamples(
        JNIEnv *env,
        jobject,
        jlong handle,
        jshortArray samples,
        jint sampleCount) {

    auto *detector =
            reinterpret_cast<BPMDetect *>(
                    handle
            );

    if (!detector || !samples) {
        return;
    }

    jboolean copied = JNI_FALSE;

    auto *data =
            env->GetShortArrayElements(
                    samples,
                    &copied
            );

    if (!data) {
        return;
    }



    detector->inputSamples(
            data,
            sampleCount
    );

    env->ReleaseShortArrayElements(
            samples,
            data,
            JNI_ABORT
    );
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeBpmDetector_nativeGetBpm(
        JNIEnv *,
        jobject,
        jlong handle) {

    auto *detector =
            reinterpret_cast<BPMDetect *>(
                    handle
            );

    if (!detector) {
        return 0.0f;
    }

    return detector->getBpm();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_NativeBpmDetector_nativeDestroy(
        JNIEnv *,
        jobject,
        jlong handle) {

    auto *detector =
            reinterpret_cast<BPMDetect *>(
                    handle
            );

    delete detector;
}