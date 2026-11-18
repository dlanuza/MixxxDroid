package com.dlanuza.mixxxdroid.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

internal class AudioEngineAudioTrack(
    private val tag: String,
    private val audioBufferMultiplier: Int,
    private val getSampleRate: () -> Int,
    private val getChannelCount: () -> Int,
    private val getRunning: () -> Boolean,
    private val getRequestedStop: () -> Boolean,
    private val getRequestedSeekMs: () -> Long?
) {

    fun create(): AudioTrack {

        val sampleRate =
            getSampleRate()

        val channelCount =
            getChannelCount()

        val channelMask =
            when (channelCount) {

                1 ->
                    AudioFormat.CHANNEL_OUT_MONO

                2 ->
                    AudioFormat.CHANNEL_OUT_STEREO

                else ->
                    throw IllegalArgumentException(
                        "Unsupported channel count: $channelCount"
                    )
            }

        val minBuffer =
            AudioTrack.getMinBufferSize(
                sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT
            )

        check(minBuffer > 0) {
            "Invalid AudioTrack min buffer: $minBuffer"
        }

        val requestedBuffer =
            maxOf(
                minBuffer * audioBufferMultiplier,
                8192
            )

        val audioFormat =
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                )
                .setChannelMask(
                    channelMask
                )
                .build()

        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_MEDIA
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC
                )
                .build()

        return AudioTrack.Builder()
            .setAudioAttributes(
                attributes
            )
            .setAudioFormat(
                audioFormat
            )
            .setBufferSizeInBytes(
                requestedBuffer
            )
            .setTransferMode(
                AudioTrack.MODE_STREAM
            )
            .build()
    }

    fun write(
        track: AudioTrack,
        data: ShortArray,
        count: Int
    ): Int {

        var offset = 0

        val channelCount =
            getChannelCount()

        val chunkSize =
            512 * channelCount

        while (
            offset < count &&
            getRunning()
        ) {

            if (
                getRequestedStop() ||
                getRequestedSeekMs() != null
            ) {
                break
            }

            val toWrite =
                minOf(
                    chunkSize,
                    count - offset
                )

            val written =
                track.write(
                    data,
                    offset,
                    toWrite,
                    AudioTrack.WRITE_BLOCKING
                )

            if (written < 0) {

                Log.e(
                    tag,
                    "AUDIOTRACK WRITE ERROR -> $written"
                )

                break
            }

            if (written == 0) {
                break
            }

            offset += written
        }

        return offset
    }

    fun release(
        track: AudioTrack?
    ) {

        if (track == null) {
            return
        }

        try {
            track.pause()
        } catch (_: Throwable) {
        }

        try {
            track.flush()
        } catch (_: Throwable) {
        }

        try {
            track.release()
        } catch (t: Throwable) {

            Log.w(
                tag,
                "AUDIOTRACK RELEASE ERROR",
                t
            )
        }
    }
}
