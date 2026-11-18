package com.dlanuza.mixxxdroid.player

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.ByteOrder
import kotlin.math.abs

internal class BackgroundWaveformExtractor(private val context: Context) {

    companion object {
        const val TARGET_SAMPLES = 1200
        private const val TIMEOUT_US = 5_000L
        private const val TAG = "WaveformExtractor"
    }

    data class WaveformProgress(
        val samples: FloatArray,
        val progress: Float,
        val completed: Boolean
    )

    // Cuando algo falla se emite un arreglo VACÍO (no uno de ceros):
    //  - la UI muestra el texto "WAVEFORM" en vez de una línea plana,
    //  - AudioEngineWaveformCache.save() ignora los arreglos vacíos, así que un fallo
    //    pasajero ya no queda guardado para siempre en caché.
    private fun failed() = WaveformProgress(FloatArray(0), 0f, true)

    fun extract(uriString: String): Flow<WaveformProgress> = flow {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            try {
                extractor.setDataSource(context, Uri.parse(uriString), null)
            } catch (e: Exception) {
                Log.e(TAG, "Error al abrir el origen de datos: $uriString", e)
                emit(failed())
                return@flow
            }

            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }

            if (trackIndex == null) {
                Log.e(TAG, "No se encontró pista de audio válida en el archivo.")
                emit(failed())
                return@flow
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

            val newCodec = try {
                MediaCodec.createDecoderByType(mime)
            } catch (e: Exception) {
                Log.e(TAG, "Formato de audio no soportado ($mime): ${e.message}")
                emit(failed())
                return@flow
            }

            // Asignar ya a `codec` para que el finally lo libere si configure/start fallan.
            codec = newCodec
            try {
                newCodec.configure(format, null, null, 0)
                newCodec.start()
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar el decodificador ($mime): ${e.message}")
                emit(failed())
                return@flow
            }

            val activeCodec = newCodec

            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            val totalExpectedFrames = if (durationUs > 0) (durationUs * sampleRate / 1_000_000L) else 0L
            val samplesPerPoint = maxOf(1L, totalExpectedFrames / TARGET_SAMPLES)

            val peaks = FloatArray(TARGET_SAMPLES)
            var inputEnded = false
            var decoderEnded = false
            var totalFrames = 0L
            var currentPointFrames = 0L
            var currentPointPeak = 0
            var lastEmittedProgress = 0f

            val info = MediaCodec.BufferInfo()
            var pcmArray = ShortArray(8192)

            while (!decoderEnded) {
                // Si el usuario carga otra canción, parar YA (antes solo se cancelaba al emitir).
                currentCoroutineContext().ensureActive()

                if (!inputEnded) {
                    val inIdx = activeCodec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buffer = activeCodec.getInputBuffer(inIdx)
                        if (buffer != null) {
                            buffer.clear()
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                activeCodec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                activeCodec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = activeCodec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outIdx >= 0) {
                    val buffer = activeCodec.getOutputBuffer(outIdx)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)

                        // Orden de bytes explícito: sin esto los picos pueden salir mal.
                        val shortBuffer = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val totalShorts = shortBuffer.remaining()

                        if (pcmArray.size < totalShorts) {
                            pcmArray = ShortArray(totalShorts)
                        }
                        shortBuffer.get(pcmArray, 0, totalShorts)

                        var idx = 0
                        while (idx < totalShorts) {
                            var framePeak = 0
                            for (c in 0 until channelCount) {
                                if (idx < totalShorts) {
                                    val sample = abs(pcmArray[idx].toInt())
                                    if (sample > framePeak) framePeak = sample
                                    idx++
                                }
                            }

                            if (framePeak > currentPointPeak) currentPointPeak = framePeak

                            currentPointFrames++
                            totalFrames++

                            if (currentPointFrames >= samplesPerPoint) {
                                val point = ((totalFrames / samplesPerPoint).toInt() - 1).coerceIn(0, TARGET_SAMPLES - 1)
                                peaks[point] = currentPointPeak / 32768.0f
                                currentPointFrames = 0L
                                currentPointPeak = 0

                                val progress = if (totalExpectedFrames > 0) {
                                    (totalFrames.toFloat() / totalExpectedFrames).coerceIn(0f, 1f)
                                } else 0f

                                if (progress - lastEmittedProgress >= 0.05f) {
                                    lastEmittedProgress = progress
                                    emit(WaveformProgress(peaks.copyOf(), progress, false))
                                }
                            }
                        }
                    }

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decoderEnded = true
                    }
                    activeCodec.releaseOutputBuffer(outIdx, false)
                }
            }

            emit(WaveformProgress(peaks, 1.0f, true))
        } catch (e: CancellationException) {
            // Cancelación normal (se cargó otra canción): no es un error ni se emite nada.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando el waveform", e)
            emit(failed())
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }.flowOn(Dispatchers.IO)
}