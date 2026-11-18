package com.dlanuza.mixxxdroid.player

import android.media.MediaCodec
import android.util.Log
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class AudioEngineRawPcm(
    private val tag: String,
    private val processBufferFrames: Int,
    private val processDecodedPcm: (
        ByteBuffer,
        MediaCodec.BufferInfo
    ) -> Unit,
    private val setInputEnded: (Boolean) -> Unit,
    private val setDecoderEnded: (Boolean) -> Unit
) {

    private var rawPcmFile: RandomAccessFile? = null

    private var rawPcmDataOffset = 0L
    private var rawPcmDataSize = 0L
    private var rawPcmBytesRead = 0L
    private var rawPcmBytesPerSecond = 0L
    private var rawPcmBytesPerFrame = 0

    private var rawPcmBuffer: ByteBuffer? = null
    private var rawPcmReadBuffer: ByteArray? = null

    fun open(
        wavFile: java.io.File,
        dataOffset: Long,
        dataSize: Long,
        bytesPerSecond: Long,
        bytesPerFrame: Int
    ) {

        close()

        rawPcmDataOffset =
            dataOffset

        rawPcmDataSize =
            dataSize

        rawPcmBytesRead =
            0L

        rawPcmBytesPerSecond =
            bytesPerSecond

        rawPcmBytesPerFrame =
            bytesPerFrame

        rawPcmFile =
            RandomAccessFile(
                wavFile,
                "r"
            )

        rawPcmFile!!.seek(
            rawPcmDataOffset
        )
    }

    fun feed(
        inputEnded: Boolean
    ): Boolean {

        if (inputEnded) {
            return false
        }

        val file =
            rawPcmFile ?: run {

                Log.e(
                    tag,
                    "RAW PCM ERROR -> file is null"
                )

                setInputEnded(true)
                setDecoderEnded(true)

                return false
            }

        if (
            rawPcmBytesRead >=
            rawPcmDataSize
        ) {

            setInputEnded(true)
            setDecoderEnded(true)

            Log.d(
                tag,
                "RAW PCM EOS -> bytes=$rawPcmBytesRead"
            )

            return false
        }

        if (rawPcmBytesPerFrame <= 0) {

            Log.e(
                tag,
                "RAW PCM ERROR -> invalid bytesPerFrame=$rawPcmBytesPerFrame"
            )

            setInputEnded(true)
            setDecoderEnded(true)

            return false
        }

        val remaining =
            rawPcmDataSize -
                    rawPcmBytesRead

        val desiredBytes =
            processBufferFrames *
                    rawPcmBytesPerFrame

        var bytesToRead =
            minOf(
                desiredBytes.toLong(),
                remaining
            ).toInt()

        bytesToRead -=
            bytesToRead %
                    rawPcmBytesPerFrame

        if (bytesToRead <= 0) {

            setInputEnded(true)
            setDecoderEnded(true)

            return false
        }

        val buffer: ByteBuffer =
            if (
                rawPcmBuffer == null ||
                rawPcmBuffer!!.capacity() < bytesToRead
            ) {

                ByteBuffer
                    .allocateDirect(
                        bytesToRead
                    )
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            } else {

                rawPcmBuffer!!.clear()

                rawPcmBuffer!!
            }

        rawPcmBuffer =
            buffer

        val temp: ByteArray =
            if (
                rawPcmReadBuffer == null ||
                rawPcmReadBuffer!!.size < bytesToRead
            ) {

                ByteArray(
                    bytesToRead
                )

            } else {

                rawPcmReadBuffer!!
            }

        rawPcmReadBuffer =
            temp

        val sourcePosition =
            rawPcmBytesRead

        try {

            file.readFully(
                temp,
                0,
                bytesToRead
            )

        } catch (t: Throwable) {

            Log.e(
                tag,
                "RAW PCM READ ERROR",
                t
            )

            setInputEnded(true)
            setDecoderEnded(true)

            return false
        }

        buffer.clear()

        buffer.put(
            temp,
            0,
            bytesToRead
        )

        buffer.flip()

        val sampleTimeUs =
            if (
                rawPcmBytesPerSecond > 0
            ) {

                (
                        sourcePosition.toDouble() *
                                1_000_000.0 /
                                rawPcmBytesPerSecond.toDouble()
                        ).toLong()

            } else {

                0L
            }

        val info =
            MediaCodec.BufferInfo()

        info.set(
            0,
            bytesToRead,
            sampleTimeUs,
            0
        )

        processDecodedPcm(
            buffer,
            info
        )

        rawPcmBytesRead +=
            bytesToRead.toLong()

//        Log.d(
//            tag,
//            "RAW PCM READ -> bytes=$bytesToRead total=$rawPcmBytesRead/$rawPcmDataSize"
//        )

        if (
            rawPcmBytesRead >=
            rawPcmDataSize
        ) {

            setInputEnded(true)
            setDecoderEnded(true)

            Log.d(
                tag,
                "RAW PCM REACHED EOS"
            )
        }

        return true
    }

    fun seek(
        positionMs: Long,
        durationMs: Long,
        sampleRate: Int
    ): Long {

        val file =
            rawPcmFile ?: return 0L

        if (
            rawPcmBytesPerSecond <= 0 ||
            rawPcmBytesPerFrame <= 0 ||
            sampleRate <= 0
        ) {
            return 0L
        }

        val safePosition =
            positionMs.coerceIn(
                0L,
                durationMs
            )

        val targetFrame =
            (
                    safePosition.toDouble() *
                            sampleRate.toDouble() /
                            1000.0
                    ).toLong()

        var targetByte =
            targetFrame *
                    rawPcmBytesPerFrame.toLong()

        targetByte =
            targetByte.coerceIn(
                0L,
                rawPcmDataSize
            )

        targetByte -=
            targetByte %
                    rawPcmBytesPerFrame.toLong()

        file.seek(
            rawPcmDataOffset +
                    targetByte
        )

        rawPcmBytesRead =
            targetByte

        return (
                targetByte.toDouble() *
                        1000.0 /
                        rawPcmBytesPerSecond.toDouble()
                ).toLong()
    }

    fun resetToStart() {

        val file =
            rawPcmFile ?: return

        file.seek(
            rawPcmDataOffset
        )

        rawPcmBytesRead =
            0L
    }

    fun close() {

        try {

            rawPcmFile?.close()

        } catch (t: Throwable) {

            Log.w(
                tag,
                "RAW PCM FILE CLOSE ERROR",
                t
            )
        }

        rawPcmFile = null

        rawPcmDataOffset = 0L
        rawPcmDataSize = 0L
        rawPcmBytesRead = 0L
        rawPcmBytesPerSecond = 0L
        rawPcmBytesPerFrame = 0

        rawPcmBuffer = null
        rawPcmReadBuffer = null
    }
}