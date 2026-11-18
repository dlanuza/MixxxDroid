package com.dlanuza.mixxxdroid.player

internal class NativeBpmDetector {

    private var handle: Long = 0L

    init {
        System.loadLibrary("mixxx_audio")
    }

    fun create(
        channels: Int,
        sampleRate: Int
    ) {
        check(handle == 0L) {
            "BPM detector already created"
        }

        handle = nativeCreate(
            channels,
            sampleRate
        )

        check(handle != 0L) {
            "Unable to create BPM detector"
        }
    }

    fun putSamples(
        samples: ShortArray,
        sampleCount: Int
    ) {
        checkHandle()

        nativePutSamples(
            handle,
            samples,
            sampleCount
        )
    }

    fun getBpm(): Float {
        checkHandle()

        return nativeGetBpm(handle)
    }

    fun destroy() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private fun checkHandle() {
        check(handle != 0L) {
            "BPM detector is not initialized"
        }
    }

    private external fun nativeCreate(
        channels: Int,
        sampleRate: Int
    ): Long

    private external fun nativePutSamples(
        handle: Long,
        samples: ShortArray,
        sampleCount: Int
    )

    private external fun nativeGetBpm(
        handle: Long
    ): Float

    private external fun nativeDestroy(
        handle: Long
    )
}