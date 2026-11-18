cmake_minimum_required(VERSION 3.22.1)

project("mixxx_audio")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ============================================================
# SOUNDTOUCH
# ============================================================

include(FetchContent)

FetchContent_Declare(
        soundtouch
        GIT_REPOSITORY https://github.com/RTE-Dev/soundtouch.git
        GIT_TAG 2.3.2
)

# ------------------------------------------------------------
# Configuracion SoundTouch
# ------------------------------------------------------------

set(SOUNDSTRETCH OFF CACHE BOOL "" FORCE)
set(OPENMP OFF CACHE BOOL "" FORCE)

# Evitamos que SoundTouch introduzca flags problematicos
# para ARM64.
set(NEON OFF CACHE BOOL "" FORCE)

# ------------------------------------------------------------
# SoundTouch usando samples de 16 bits.
# ------------------------------------------------------------

set(INTEGER_SAMPLES ON CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(soundtouch)

# ============================================================
# JNI
# ============================================================

add_library(
        mixxx_audio
        SHARED
        native_soundtouch.cpp
)

# ============================================================
# HEADERS
# ============================================================

target_include_directories(
        mixxx_audio
        PRIVATE
        ${soundtouch_SOURCE_DIR}/include
)

# ============================================================
# LIBRERIAS ANDROID
# ============================================================

find_library(
        log-lib
        log
)

# ============================================================
# LINK
# ============================================================

target_link_libraries(
        mixxx_audio
        PRIVATE
        SoundTouch
        ${log-lib}
        android
)