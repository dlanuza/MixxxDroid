package com.dlanuza.mixxxdroid.player

import java.io.File
import java.io.RandomAccessFile

internal object AudioEngineWavParser {

    data class Result(
        val sampleRate: Int,
        val channelCount: Int,
        val bitsPerSample: Int,
        val dataOffset: Long,
        val dataSize: Long,
        val durationMs: Long
    )

    fun parse(
        wavFile: File
    ): Result {

        check(wavFile.exists()) {
            "WAV file does not exist: ${wavFile.absolutePath}"
        }

        val file =
            RandomAccessFile(
                wavFile,
                "r"
            )

        try {

            val riff =
                ByteArray(4)

            file.readFully(riff)

            check(
                String(
                    riff,
                    Charsets.US_ASCII
                ) == "RIFF"
            ) {
                "Invalid WAV: missing RIFF"
            }

            readLittleEndianInt(file)

            val wave =
                ByteArray(4)

            file.readFully(wave)

            check(
                String(
                    wave,
                    Charsets.US_ASCII
                ) == "WAVE"
            ) {
                "Invalid WAV: missing WAVE"
            }

            var foundFmt = false
            var foundData = false

            var wavChannels = 0
            var wavSampleRate = 0
            var wavBitsPerSample = 0
            var wavAudioFormat = 0

            var dataOffset = 0L
            var dataSize = 0L

            while (
                file.filePointer + 8 <= file.length()
            ) {

                val chunkIdBytes =
                    ByteArray(4)

                file.readFully(
                    chunkIdBytes
                )

                val chunkId =
                    String(
                        chunkIdBytes,
                        Charsets.US_ASCII
                    )

                val chunkSize =
                    readLittleEndianInt(file)
                        .toLong() and 0xffffffffL

                val chunkDataStart =
                    file.filePointer

                when (chunkId) {

                    "fmt " -> {

                        check(chunkSize >= 16L) {
                            "Invalid WAV fmt chunk"
                        }

                        wavAudioFormat =
                            readLittleEndianShort(file)

                        wavChannels =
                            readLittleEndianShort(file)

                        wavSampleRate =
                            readLittleEndianInt(file)

                        // byteRate
                        readLittleEndianInt(file)

                        // blockAlign
                        readLittleEndianShort(file)

                        wavBitsPerSample =
                            readLittleEndianShort(file)

                        foundFmt = true

                        file.seek(
                            chunkDataStart +
                                    chunkSize
                        )
                    }

                    "data" -> {

                        dataOffset =
                            chunkDataStart

                        dataSize =
                            chunkSize.coerceAtMost(
                                file.length() -
                                        chunkDataStart
                            )

                        foundData = true

                        file.seek(
                            chunkDataStart +
                                    dataSize
                        )

                        if (foundFmt) {
                            break
                        }
                    }

                    else -> {

                        file.seek(
                            chunkDataStart +
                                    chunkSize
                        )
                    }
                }

                if (
                    chunkSize % 2L != 0L &&
                    file.filePointer < file.length()
                ) {

                    file.seek(
                        file.filePointer + 1L
                    )
                }
            }

            check(foundFmt) {
                "WAV fmt chunk not found"
            }

            check(foundData) {
                "WAV data chunk not found"
            }

            check(wavAudioFormat == 1) {
                "Unsupported WAV format: $wavAudioFormat"
            }

            check(wavBitsPerSample == 16) {
                "Unsupported WAV bit depth: $wavBitsPerSample"
            }

            check(
                wavChannels == 1 ||
                        wavChannels == 2
            ) {
                "Unsupported WAV channels: $wavChannels"
            }

            check(wavSampleRate > 0) {
                "Invalid WAV sample rate: $wavSampleRate"
            }

            val bytesPerFrame =
                wavChannels * 2

            val bytesPerSecond =
                wavSampleRate.toLong() *
                        bytesPerFrame.toLong()

            val alignedDataSize =
                dataSize -
                        (
                                dataSize %
                                        bytesPerFrame
                                )

            val durationMs =
                (
                        alignedDataSize.toDouble() *
                                1000.0 /
                                bytesPerSecond.toDouble()
                        ).toLong()

            return Result(
                sampleRate = wavSampleRate,
                channelCount = wavChannels,
                bitsPerSample = wavBitsPerSample,
                dataOffset = dataOffset,
                dataSize = alignedDataSize,
                durationMs = durationMs
            )

        } finally {

            file.close()
        }
    }

    private fun readLittleEndianInt(
        file: RandomAccessFile
    ): Int {

        val b0 = file.read()
        val b1 = file.read()
        val b2 = file.read()
        val b3 = file.read()

        check(
            b0 >= 0 &&
                    b1 >= 0 &&
                    b2 >= 0 &&
                    b3 >= 0
        ) {
            "Unexpected end of WAV file"
        }

        return (
                (b0 and 0xFF) or
                        ((b1 and 0xFF) shl 8) or
                        ((b2 and 0xFF) shl 16) or
                        ((b3 and 0xFF) shl 24)
                )
    }

    private fun readLittleEndianShort(
        file: RandomAccessFile
    ): Int {

        val b0 = file.read()
        val b1 = file.read()

        check(
            b0 >= 0 &&
                    b1 >= 0
        ) {
            "Unexpected end of WAV file"
        }

        return (
                (b0 and 0xFF) or
                        ((b1 and 0xFF) shl 8)
                )
    }
}