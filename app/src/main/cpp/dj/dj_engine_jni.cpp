#include <jni.h>
#include <android/log.h>

#include <new>
#include <cstddef>
#include <cstdint>

#include "DjEngine.h"

#define LOG_TAG "DjEngineJNI"

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGD(...) \
    __android_log_print( \
        ANDROID_LOG_DEBUG, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

static inline DjEngine* getEngine(
        jlong handle
) {
    return reinterpret_cast<DjEngine*>(
            handle
    );
}

extern "C" {

// =========================================================
// INIT
// =========================================================

JNIEXPORT jlong JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeInit(
        JNIEnv* env,
        jobject thiz,
        jint sampleRate,
        jint channelCount
) {

    auto* engine =
            new (std::nothrow) DjEngine();

    if (!engine) {

        LOGE(
                "No se pudo reservar DjEngine"
        );

        return 0;
    }

    if (!engine->initialize(
            static_cast<int>(
                    sampleRate
            ),
            static_cast<int>(
                    channelCount
            )
    )) {

        LOGE(
                "No se pudo inicializar DjEngine"
        );

        delete engine;

        return 0;
    }

    LOGD(
            "DjEngine inicializado: %d Hz / %d canales",
            sampleRate,
            channelCount
    );

    return reinterpret_cast<jlong>(
            engine
    );
}

// =========================================================
// RELEASE
// =========================================================

JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeRelease(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine) {
        return;
    }

    engine->shutdown();

    delete engine;

    LOGD(
            "DjEngine liberado"
    );
}

// =========================================================
// PUSH AUDIO
// =========================================================

JNIEXPORT jboolean JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativePushAudio(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle,
        jfloatArray samples,
        jint sampleCount
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine ||
        !samples ||
        sampleCount <= 0) {

        return JNI_FALSE;
    }

    const jsize arrayLength =
            env->GetArrayLength(
                    samples
            );

    if (arrayLength <= 0) {
        return JNI_FALSE;
    }

    if (sampleCount > arrayLength) {
        sampleCount = arrayLength;
    }

    if (sampleCount <= 0) {
        return JNI_FALSE;
    }

    jfloat* pcmData =
            env->GetFloatArrayElements(
                    samples,
                    nullptr
            );

    if (!pcmData) {
        return JNI_FALSE;
    }

    const bool result =
            engine->pushAudio(
                    pcmData,
                    static_cast<size_t>(
                            sampleCount
                    )
            );

    env->ReleaseFloatArrayElements(
            samples,
            pcmData,
            JNI_ABORT
    );

    return result
           ? JNI_TRUE
           : JNI_FALSE;
}

// =========================================================
// START SCRATCH
// =========================================================

JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeStartScratch(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle,
        jlong positionMs
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine) {
        return;
    }

    engine->startScratch(
            static_cast<int64_t>(
                    positionMs
            )
    );
}

// =========================================================
// UPDATE SCRATCH
// =========================================================

JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeUpdateScratchSpeed(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle,
        jfloat speedFactor
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine) {
        return;
    }

    engine->updateScratchSpeed(
            static_cast<float>(
                    speedFactor
            )
    );
}

// =========================================================
// STOP SCRATCH
// =========================================================

JNIEXPORT void JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeStopScratch(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine) {
        return;
    }

    engine->stopScratch();
}

// =========================================================
// CURRENT POSITION
// =========================================================

JNIEXPORT jlong JNICALL
Java_com_dlanuza_mixxxdroid_player_AudioPlayerManager_nativeGetCurrentPositionMs(
        JNIEnv* env,
        jobject thiz,
        jlong engineHandle
) {

    auto* engine =
            getEngine(
                    engineHandle
            );

    if (!engine) {
        return 0;
    }

    return static_cast<jlong>(
            engine->getCurrentPositionMs()
    );
}

} // extern "C"