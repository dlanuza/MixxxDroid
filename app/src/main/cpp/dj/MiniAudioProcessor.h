#pragma once

#include <cstdint>

class MiniAudioProcessor {
public:
    bool initialize(uint32_t sampleRate, uint32_t channels);
    void process(float* samples, uint32_t frameCount);

private:
    uint32_t sampleRate_ = 0;
    uint32_t channels_ = 0;
};