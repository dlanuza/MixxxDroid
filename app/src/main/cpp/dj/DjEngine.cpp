#include "DjEngine.h"

#include <algorithm>
#include <cmath>
#include <cstring>

#include <android/log.h>

#define LOG_TAG "DjEngine"

#define LOGD(...) \
    __android_log_print( \
        ANDROID_LOG_DEBUG, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

// =========================================================
// CONSTRUCTOR
// =========================================================

DjEngine::DjEngine() {
    LOGD("DjEngine creado");
}

// =========================================================
// DESTRUCTOR
// =========================================================

DjEngine::~DjEngine() {
    shutdown();
}

// =========================================================
// INITIALIZE
// =========================================================

bool DjEngine::initialize(
        int sampleRate,
        int channelCount
) {

    if (initialized_) {
        shutdown();
    }

    if (sampleRate <= 0 ||
        channelCount <= 0) {

        LOGE(
                "Formato inválido: %d Hz / %d canales",
                sampleRate,
                channelCount
        );

        return false;
    }

    sampleRate_ = sampleRate;
    channelCount_ = channelCount;

    bufferCapacityFrames_ =
            static_cast<int64_t>(
                    sampleRate_ *
                    BUFFER_SECONDS
            );

    audioBufferSize_ =
            static_cast<size_t>(
                    bufferCapacityFrames_ *
                    channelCount_
            );

    try {

        audioBuffer_.assign(
                audioBufferSize_,
                0.0f
        );

    } catch (...) {

        LOGE(
                "No se pudo reservar el buffer PCM"
        );

        return false;
    }

    clearBuffer();

    // =====================================================
    // OBOE
    // =====================================================

    oboe::AudioStreamBuilder builder;

    builder.setDirection(
                    oboe::Direction::Output
            )
            ->setPerformanceMode(
                    oboe::PerformanceMode::LowLatency
            )
            ->setSharingMode(
                    oboe::SharingMode::Exclusive
            )
            ->setFormat(
                    oboe::AudioFormat::Float
            )
            ->setChannelCount(
                    channelCount_
            )
            ->setSampleRate(
                    sampleRate_
            )
            ->setDataCallback(
                    this
            );

    oboe::Result result =
            builder.openStream(
                    outputStream_
            );

    if (result != oboe::Result::OK ||
        !outputStream_) {

        LOGE(
                "Error abriendo Oboe: %s",
                oboe::convertToText(
                        result
                )
        );

        outputStream_.reset();

        return false;
    }

    result =
            outputStream_->requestStart();

    if (result != oboe::Result::OK) {

        LOGE(
                "Error iniciando Oboe: %s",
                oboe::convertToText(
                        result
                )
        );

        outputStream_->close();
        outputStream_.reset();

        return false;
    }

    initialized_ = true;

    LOGD(
            "DjEngine inicializado: %d Hz / %d canales / %d segundos",
            sampleRate_,
            channelCount_,
            BUFFER_SECONDS
    );

    return true;
}

// =========================================================
// SHUTDOWN
// =========================================================

void DjEngine::shutdown() {

    initialized_ = false;

    isScratching_.store(
            false,
            std::memory_order_release
    );

    targetSpeedFactor_.store(
            0.0f,
            std::memory_order_release
    );

    if (outputStream_) {

        outputStream_->requestStop();
        outputStream_->close();

        outputStream_.reset();
    }

    audioBuffer_.clear();

    audioBufferSize_ = 0;
    bufferCapacityFrames_ = 0;

    bufferStartFrame_.store(
            0,
            std::memory_order_release
    );

    bufferEndFrame_.store(
            0,
            std::memory_order_release
    );

    writeFrame_.store(
            0,
            std::memory_order_release
    );

    readFrame_.store(
            0.0,
            std::memory_order_release
    );

    sampleRate_ = 0;
    channelCount_ = 0;

    LOGD("DjEngine apagado");
}

// =========================================================
// IS INITIALIZED
// =========================================================

bool DjEngine::isInitialized() const {
    return initialized_;
}

// =========================================================
// CLEAR BUFFER
// =========================================================

void DjEngine::clearBuffer() {

    if (!audioBuffer_.empty()) {

        std::fill(
                audioBuffer_.begin(),
                audioBuffer_.end(),
                0.0f
        );
    }

    bufferStartFrame_.store(
            0,
            std::memory_order_release
    );

    bufferEndFrame_.store(
            0,
            std::memory_order_release
    );

    writeFrame_.store(
            0,
            std::memory_order_release
    );

    readFrame_.store(
            0.0,
            std::memory_order_release
    );

    currentSpeedFactor_ = 0.0;
}

// =========================================================
// PUSH AUDIO
// =========================================================

bool DjEngine::pushAudio(
        const float* samples,
        size_t sampleCount
) {

    if (!initialized_ ||
        samples == nullptr ||
        sampleCount == 0 ||
        channelCount_ <= 0 ||
        bufferCapacityFrames_ <= 0) {

        return false;
    }

    const size_t frames =
            sampleCount /
            static_cast<size_t>(
                    channelCount_
            );

    if (frames == 0) {
        return false;
    }

    int64_t startFrame =
            writeFrame_.load(
                    std::memory_order_acquire
            );

    int64_t endFrame =
            startFrame +
            static_cast<int64_t>(
                    frames
            );

    // =====================================================
    // ESCRITURA EN RING BUFFER
    // =====================================================

    for (size_t frame = 0;
         frame < frames;
         ++frame) {

        const int64_t absoluteFrame =
                startFrame +
                static_cast<int64_t>(
                        frame
                );

        const int64_t bufferFrame =
                absoluteFrame %
                bufferCapacityFrames_;

        const size_t bufferIndex =
                static_cast<size_t>(
                        bufferFrame *
                        channelCount_
                );

        const size_t sourceIndex =
                frame *
                static_cast<size_t>(
                        channelCount_
                );

        for (int channel = 0;
             channel < channelCount_;
             ++channel) {

            audioBuffer_[
                    bufferIndex +
                    static_cast<size_t>(
                            channel
                    )
            ] =
                    samples[
                            sourceIndex +
                            static_cast<size_t>(
                                    channel
                            )
                    ];
        }
    }

    // =====================================================
    // ACTUALIZAR VENTANA DISPONIBLE
    // =====================================================

    int64_t newStartFrame =
            endFrame -
            bufferCapacityFrames_;

    if (newStartFrame < 0) {
        newStartFrame = 0;
    }

    bufferStartFrame_.store(
            newStartFrame,
            std::memory_order_release
    );

    bufferEndFrame_.store(
            endFrame,
            std::memory_order_release
    );

    writeFrame_.store(
            endFrame,
            std::memory_order_release
    );

    return true;
}

// =========================================================
// FRAME AVAILABLE
// =========================================================

bool DjEngine::isFrameAvailable(
        int64_t frame
) const {

    const int64_t start =
            bufferStartFrame_.load(
                    std::memory_order_acquire
            );

    const int64_t end =
            bufferEndFrame_.load(
                    std::memory_order_acquire
            );

    return frame >= start &&
           frame < end;
}

// =========================================================
// READ INTERPOLATED
// =========================================================

float DjEngine::readInterpolated(
        double framePosition,
        int channel
) const {

    if (channel < 0 ||
        channel >= channelCount_) {

        return 0.0f;
    }

    const int64_t frame0 =
            static_cast<int64_t>(
                    std::floor(
                            framePosition
                    )
            );

    const int64_t frame1 =
            frame0 + 1;

    if (!isFrameAvailable(frame0)) {
        return 0.0f;
    }

    const int64_t bufferFrame0 =
            frame0 %
            bufferCapacityFrames_;

    const size_t index0 =
            static_cast<size_t>(
                    bufferFrame0 *
                    channelCount_ +
                    channel
            );

    const float sample0 =
            audioBuffer_[index0];

    if (!isFrameAvailable(frame1)) {
        return sample0;
    }

    const int64_t bufferFrame1 =
            frame1 %
            bufferCapacityFrames_;

    const size_t index1 =
            static_cast<size_t>(
                    bufferFrame1 *
                    channelCount_ +
                    channel
            );

    const float sample1 =
            audioBuffer_[index1];

    const float fraction =
            static_cast<float>(
                    framePosition -
                    static_cast<double>(
                            frame0
                    )
            );

    return sample0 +
           (sample1 - sample0) *
           fraction;
}

// =========================================================
// START SCRATCH
// =========================================================

void DjEngine::startScratch(
        int64_t positionMs
) {

    if (!initialized_ ||
        sampleRate_ <= 0) {

        return;
    }

    const int64_t requestedFrame =
            (
                    positionMs *
                    static_cast<int64_t>(
                            sampleRate_
                    )
            ) /
            1000LL;

    const int64_t start =
            bufferStartFrame_.load(
                    std::memory_order_acquire
            );

    const int64_t end =
            bufferEndFrame_.load(
                    std::memory_order_acquire
            );

    if (end <= start) {

        LOGE(
                "Scratch sin PCM disponible"
        );

        return;
    }

    const int64_t safeFrame =
            std::clamp(
                    requestedFrame,
                    start,
                    end - 1
            );

    readFrame_.store(
            static_cast<double>(
                    safeFrame
            ),
            std::memory_order_release
    );

    currentSpeedFactor_ = 0.0;

    targetSpeedFactor_.store(
            0.0f,
            std::memory_order_release
    );

    isScratching_.store(
            true,
            std::memory_order_release
    );

    LOGD(
            "START SCRATCH position=%lld frame=%lld buffer=[%lld,%lld]",
            static_cast<long long>(
                    positionMs
            ),
            static_cast<long long>(
                    safeFrame
            ),
            static_cast<long long>(
                    start
            ),
            static_cast<long long>(
                    end
            )
    );
}

// =========================================================
// UPDATE SCRATCH SPEED
// =========================================================

void DjEngine::updateScratchSpeed(
        float speedFactor
) {

    if (!isScratching()) {
        return;
    }

    const float safeSpeed =
            std::clamp(
                    speedFactor,
                    -4.0f,
                    4.0f
            );

    targetSpeedFactor_.store(
            safeSpeed,
            std::memory_order_release
    );
}

// =========================================================
// STOP SCRATCH
// =========================================================

void DjEngine::stopScratch() {

    targetSpeedFactor_.store(
            0.0f,
            std::memory_order_release
    );

    currentSpeedFactor_ = 0.0;

    isScratching_.store(
            false,
            std::memory_order_release
    );

    LOGD(
            "STOP SCRATCH"
    );
}

// =========================================================
// CURRENT POSITION
// =========================================================

int64_t DjEngine::getCurrentPositionMs() const {

    if (sampleRate_ <= 0) {
        return 0;
    }

    const double frame =
            readFrame_.load(
                    std::memory_order_acquire
            );

    const double positionMs =
            (
                    frame *
                    1000.0
            ) /
            static_cast<double>(
                    sampleRate_
            );

    return static_cast<int64_t>(
            std::max(
                    0.0,
                    positionMs
            )
    );
}

// =========================================================
// OBOE CALLBACK
// =========================================================

oboe::DataCallbackResult DjEngine::onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames
) {

    if (!audioData ||
        numFrames <= 0 ||
        channelCount_ <= 0) {

        return oboe::DataCallbackResult::Continue;
    }

    float* output =
            static_cast<float*>(
                    audioData
            );

    const size_t outputSamples =
            static_cast<size_t>(
                    numFrames
            ) *
            static_cast<size_t>(
                    channelCount_
            );

    // =====================================================
    // SI NO HAY SCRATCH -> SILENCIO
    // =====================================================

    if (!isScratching_.load(
            std::memory_order_acquire
    )) {

        std::fill(
                output,
                output + outputSamples,
                0.0f
        );

        return oboe::DataCallbackResult::Continue;
    }

    // =====================================================
    // SUAVIZAR VELOCIDAD
    // =====================================================

    const float target =
            targetSpeedFactor_.load(
                    std::memory_order_acquire
            );

    currentSpeedFactor_ +=
            (
                    static_cast<double>(
                            target
                    ) -
                    currentSpeedFactor_
            ) *
            static_cast<double>(
                    smoothingAlpha_
            );

    double position =
            readFrame_.load(
                    std::memory_order_acquire
            );

    // =====================================================
    // GENERAR AUDIO SCRATCH
    // =====================================================

    for (int32_t frame = 0;
         frame < numFrames;
         ++frame) {

        const int64_t frameInteger =
                static_cast<int64_t>(
                        std::floor(
                                position
                        )
                );

        const int64_t start =
                bufferStartFrame_.load(
                        std::memory_order_acquire
                );

        const int64_t end =
                bufferEndFrame_.load(
                        std::memory_order_acquire
                );

        // =================================================
        // FUERA DEL BUFFER
        // =================================================

        if (frameInteger < start ||
            frameInteger >= end) {

            for (int channel = 0;
                 channel < channelCount_;
                 ++channel) {

                output[
                        static_cast<size_t>(
                                frame *
                                channelCount_ +
                                channel
                        )
                ] = 0.0f;
            }

            /*
             * No hacemos loop circular.
             * El scratch se queda detenido
             * al llegar al límite disponible.
             */
            position =
                    std::clamp(
                            position,
                            static_cast<double>(
                                    start
                            ),
                            static_cast<double>(
                                    std::max(
                                            start,
                                            end - 1
                                    )
                            )
                    );

        } else {

            for (int channel = 0;
                 channel < channelCount_;
                 ++channel) {

                output[
                        static_cast<size_t>(
                                frame *
                                channelCount_ +
                                channel
                        )
                ] =
                        readInterpolated(
                                position,
                                channel
                        );
            }
        }

        // =================================================
        // AVANZAR / RETROCEDER
        // =================================================

        position += currentSpeedFactor_;
    }

    readFrame_.store(
            position,
            std::memory_order_release
    );

    return oboe::DataCallbackResult::Continue;
}