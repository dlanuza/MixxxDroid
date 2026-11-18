#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

#include <oboe/Oboe.h>

class DjEngine : public oboe::AudioStreamDataCallback {

public:

    DjEngine();

    ~DjEngine() override;

    bool initialize(
            int sampleRate,
            int channelCount
    );

    void shutdown();

    bool isInitialized() const;

    // =========================================================
    // AUDIO BUFFER
    // =========================================================

    bool pushAudio(
            const float* samples,
            size_t sampleCount
    );

    // =========================================================
    // SCRATCH
    // =========================================================

    void startScratch(
            int64_t positionMs
    );

    void updateScratchSpeed(
            float speedFactor
    );

    void stopScratch();

    bool isScratching() const {
        return isScratching_.load(
                std::memory_order_acquire
        );
    }

    int64_t getCurrentPositionMs() const;

    // =========================================================
    // OBOE CALLBACK
    // =========================================================

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* audioStream,
            void* audioData,
            int32_t numFrames
    ) override;

private:

    static constexpr int BUFFER_SECONDS = 30;

    // =========================================================
    // BUFFER
    // =========================================================

    void clearBuffer();

    bool isFrameAvailable(
            int64_t frame
    ) const;

    float readInterpolated(
            double framePosition,
            int channel
    ) const;

private:

    // =========================================================
    // OBOE
    // =========================================================

    std::shared_ptr<oboe::AudioStream> outputStream_;

    // =========================================================
    // PCM RING BUFFER
    // =========================================================

    std::vector<float> audioBuffer_;

    size_t audioBufferSize_ = 0;

    int64_t bufferCapacityFrames_ = 0;

    std::atomic<int64_t>
            bufferStartFrame_{0};

    std::atomic<int64_t>
            bufferEndFrame_{0};

    std::atomic<int64_t>
            writeFrame_{0};

    // =========================================================
    // SCRATCH CURSOR
    // =========================================================

    std::atomic<double>
            readFrame_{0.0};

    std::atomic<bool>
            isScratching_{false};

    std::atomic<float>
            targetSpeedFactor_{0.0f};

    double currentSpeedFactor_ = 0.0;

    float smoothingAlpha_ = 0.08f;

    // =========================================================
    // STATE
    // =========================================================

    bool initialized_ = false;

    int sampleRate_ = 0;

    int channelCount_ = 0;
};