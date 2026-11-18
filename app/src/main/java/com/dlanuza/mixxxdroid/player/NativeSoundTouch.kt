package com.dlanuza.mixxxdroid.player

internal class NativeSoundTouch {

    private var handle: Long = 0L

    init {
        System.loadLibrary("mixxx_audio")
    }

    fun create(
        channels: Int,
        sampleRate: Int
    ) {

        check(handle == 0L) {
            "SoundTouch already created"
        }

        handle =
            nativeCreate(
                channels,
                sampleRate
            )

        check(handle != 0L) {
            "Unable to create SoundTouch"
        }
    }

    fun setTempo(
        tempo: Float
    ) {

        checkHandle()

        nativeSetTempo(
            handle,
            tempo
        )
    }

    fun setPitchSemitones(
        semitones: Float
    ) {

        checkHandle()

        nativeSetPitchSemitones(
            handle,
            semitones
        )
    }

    fun setRate(
        rate: Float
    ) {

        checkHandle()

        nativeSetRate(
            handle,
            rate
        )
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

    fun receiveSamples(
        output: ShortArray
    ): Int {

        checkHandle()

        return nativeReceiveSamples(
            handle,
            output
        )
    }

    fun numSamples(): Int {

        checkHandle()

        return nativeNumSamples(
            handle
        )
    }

    fun flush() {

        checkHandle()

        nativeFlush(
            handle
        )
    }

    fun clear() {

        checkHandle()

        nativeClear(
            handle
        )
    }

    fun destroy() {

        if (handle != 0L) {

            nativeDestroy(
                handle
            )

            handle = 0L
        }
    }

    private fun checkHandle() {

        check(handle != 0L) {
            "SoundTouch is not initialized"
        }
    }

    private external fun nativeCreate(
        channels: Int,
        sampleRate: Int
    ): Long

    private external fun nativeSetTempo(
        handle: Long,
        tempo: Float
    )

    private external fun nativeSetPitchSemitones(
        handle: Long,
        semitones: Float
    )

    private external fun nativeSetRate(
        handle: Long,
        rate: Float
    )

    private external fun nativePutSamples(
        handle: Long,
        samples: ShortArray,
        sampleCount: Int
    )

    private external fun nativeReceiveSamples(
        handle: Long,
        output: ShortArray
    ): Int

    private external fun nativeNumSamples(
        handle: Long
    ): Int

    private external fun nativeFlush(
        handle: Long
    )

    private external fun nativeClear(
        handle: Long
    )

    private external fun nativeDestroy(
        handle: Long
    )
}