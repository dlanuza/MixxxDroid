package com.dlanuza.mixxxdroid.player

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

internal object SoftwareAudioDecoder {

    private const val TAG = "MIXXX_FFMPEG"

    fun decodeToWav(
        context: Context,
        uri: Uri
    ): File {

        Log.d(
            TAG,
            "WMA DECODE START -> $uri"
        )

        val inputFile = File(
            context.cacheDir,
            "mixxx_wma_input_${System.currentTimeMillis()}.wma"
        )

        val outputFile = File(
            context.cacheDir,
            "mixxx_wma_output_${System.currentTimeMillis()}.wav"
        )

        try {

            // =====================================================
            // COPIAR content:// -> archivo local
            // =====================================================

            context.contentResolver
                .openInputStream(uri)
                .use { input ->

                    requireNotNull(input) {
                        "No se pudo abrir WMA: $uri"
                    }

                    FileOutputStream(inputFile).use { output ->

                        val buffer = ByteArray(64 * 1024)

                        while (true) {

                            val read = input.read(buffer)

                            if (read < 0) {
                                break
                            }

                            if (read > 0) {
                                output.write(
                                    buffer,
                                    0,
                                    read
                                )
                            }
                        }

                        output.flush()
                    }
                }

            Log.d(
                TAG,
                "WMA COPIED -> " +
                        "${inputFile.absolutePath} " +
                        "size=${inputFile.length()}"
            )

            // =====================================================
            // WMA -> WAV PCM 16 BIT
            // =====================================================

            val command = buildString {

                append("-y ")

                append("-i ")
                append(quote(inputFile.absolutePath))
                append(" ")

                append("-vn ")

                append("-c:a pcm_s16le ")

                append(
                    quote(outputFile.absolutePath)
                )
            }

            Log.d(
                TAG,
                "FFMPEG COMMAND -> $command"
            )

            val session = FFmpegKit.execute(command)

            val returnCode = session.returnCode

            if (!ReturnCode.isSuccess(returnCode)) {

                Log.e(
                    TAG,
                    "FFMPEG FAILED -> code=$returnCode"
                )

                Log.e(
                    TAG,
                    "FFMPEG LOG -> ${session.allLogsAsString}"
                )

                throw IllegalStateException(
                    "FFmpeg no pudo decodificar WMA. " +
                            "returnCode=$returnCode"
                )
            }

            // =====================================================
            // COMPROBAR RESULTADO
            // =====================================================

            check(outputFile.exists()) {
                "FFmpeg no genero el WAV"
            }

            check(outputFile.length() > 44) {
                "El WAV generado esta vacio o es invalido"
            }

            Log.d(
                TAG,
                "WMA DECODE SUCCESS -> " +
                        "${outputFile.absolutePath} " +
                        "size=${outputFile.length()}"
            )

            // El WMA temporal ya no hace falta
            inputFile.delete()

            return outputFile

        } catch (t: Throwable) {

            Log.e(
                TAG,
                "WMA DECODE ERROR",
                t
            )

            inputFile.delete()
            outputFile.delete()

            throw t
        }
    }

    fun deleteTempFile(file: File?) {

        if (file == null) {
            return
        }

        try {

            if (file.exists()) {

                val deleted = file.delete()

                Log.d(
                    TAG,
                    "DELETE TEMP -> " +
                            "${file.absolutePath} " +
                            "success=$deleted"
                )
            }

        } catch (t: Throwable) {

            Log.w(
                TAG,
                "DELETE TEMP ERROR -> " +
                        "${file.absolutePath}",
                t
            )
        }
    }

    private fun quote(value: String): String {

        return "'" +
                value.replace(
                    "'",
                    "'\\''"
                ) +
                "'"
    }
}