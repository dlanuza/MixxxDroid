#include "MiniAudioProcessor.h"

bool MiniAudioProcessor::initialize(uint32_t sampleRate, uint32_t channels) {
    if (sampleRate == 0 || channels == 0) {
        return false;
    }

    sampleRate_ = sampleRate;
    channels_ = channels;

    return true;
}

void MiniAudioProcessor::process(float* samples, uint32_t frameCount) {
    if (!samples || frameCount == 0) {
        return;
    }
    // Procesamiento transparente
}