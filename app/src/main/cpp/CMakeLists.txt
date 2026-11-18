cmake_minimum_required(VERSION 3.22.1)

project("mixxx_audio")

find_package(oboe REQUIRED CONFIG)

set(SOUNDTOUCH_DIR
        ${CMAKE_CURRENT_SOURCE_DIR}/soundtouch
)

set(SOUNDTOUCH_SOURCES
        ${SOUNDTOUCH_DIR}/source/SoundTouch/AAFilter.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/BPMDetect.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/cpu_detect_x86.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/FIFOSampleBuffer.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/FIRFilter.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/InterpolateCubic.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/InterpolateLinear.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/InterpolateShannon.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/PeakFinder.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/RateTransposer.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/SoundTouch.cpp
        ${SOUNDTOUCH_DIR}/source/SoundTouch/TDStretch.cpp
)

add_library(
        soundtouch
        STATIC
        ${SOUNDTOUCH_SOURCES}
)

target_include_directories(
        soundtouch
        PUBLIC
        ${SOUNDTOUCH_DIR}/include
        ${SOUNDTOUCH_DIR}/source/SoundTouch
)

target_compile_definitions(
        soundtouch
        PRIVATE
        SOUNDTOUCH_INTEGER_SAMPLES
)

add_library(
        mixxx_audio
        SHARED
        native_soundtouch.cpp
        NativeBpmDetector.cpp
        MiniAudioCore.cpp
        dj/dj_engine_jni.cpp
        dj/DjEngine.cpp
        dj/MiniAudioProcessor.cpp
)

target_compile_definitions(
        mixxx_audio
        PRIVATE
        SOUNDTOUCH_INTEGER_SAMPLES
)

target_include_directories(
        mixxx_audio
        PRIVATE
        ${SOUNDTOUCH_DIR}/include
        ${SOUNDTOUCH_DIR}/source/SoundTouch
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/dj
)

find_library(
        log-lib
        log
)

target_link_libraries(
        mixxx_audio
        PRIVATE
        soundtouch
        oboe::oboe
        ${log-lib}
)

# Compatibilidad con paginas de memoria de 16 KB
target_link_options(
        mixxx_audio
        PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)