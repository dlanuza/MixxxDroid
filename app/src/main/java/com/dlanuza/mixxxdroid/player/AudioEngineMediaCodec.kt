package com.dlanuza.mixxxdroid.player

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

internal class AudioEngineMediaCodec(
    private val inputBufferTimeoutUs: Long,
    private val outputBufferTimeoutUs: Long,

    private val onInputEnded: () -> Unit,

    private val onOutputFormatChanged: (
        MediaFormat
    ) -> Unit,

    private val onDecodedPcm: (
        java.nio.ByteBuffer,
        MediaCodec.BufferInfo
    ) -> Unit,

    private val onDecoderEnded: () -> Unit
) {

    fun feed(
        codec: MediaCodec?,
        extractor: MediaExtractor?,
        inputEnded: Boolean
    ) {

        if (inputEnded) {
            return
        }

        val localCodec =
            codec ?: return

        val localExtractor =
            extractor ?: return

        val inputIndex =
            localCodec.dequeueInputBuffer(
                inputBufferTimeoutUs
            )

        if (inputIndex < 0) {
            return
        }

        val inputBuffer =
            localCodec.getInputBuffer(
                inputIndex
            ) ?: return

        inputBuffer.clear()

        val sampleSize =
            localExtractor.readSampleData(
                inputBuffer,
                0
            )

        if (sampleSize < 0) {

            localCodec.queueInputBuffer(
                inputIndex,
                0,
                0,
                0L,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )

            onInputEnded()

            return
        }

        val sampleTime =
            localExtractor.sampleTime

        localCodec.queueInputBuffer(
            inputIndex,
            0,
            sampleSize,
            sampleTime,
            0
        )

        localExtractor.advance()
    }

    fun drain(
        codec: MediaCodec?,
        playing: Boolean
    ) {

        if (!playing) {
            return
        }

        val localCodec =
            codec ?: return

        while (playing) {

            val info =
                MediaCodec.BufferInfo()

            val outputIndex =
                localCodec.dequeueOutputBuffer(
                    info,
                    outputBufferTimeoutUs
                )

            when {

                outputIndex ==
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {

                    break
                }

                outputIndex ==
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                    onOutputFormatChanged(
                        localCodec.outputFormat
                    )
                }

                outputIndex >= 0 -> {

                    if (info.size > 0) {

                        val outputBuffer =
                            localCodec.getOutputBuffer(
                                outputIndex
                            )

                        if (outputBuffer != null) {

                            onDecodedPcm(
                                outputBuffer,
                                info
                            )
                        }
                    }

                    val eos =
                        (
                                info.flags and
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                ) != 0

                    localCodec.releaseOutputBuffer(
                        outputIndex,
                        false
                    )

                    if (eos) {

                        onDecoderEnded()

                        break
                    }
                }
            }
        }
    }
}
