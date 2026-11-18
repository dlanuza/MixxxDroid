package com.dlanuza.mixxxdroid.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class WaveformRepository(
    private val context: Context
) {

    fun getWaveform(
        uriString: String
    ): Flow<BackgroundWaveformExtractor.WaveformProgress> =
        flow {

            // 1. BUSCAR EN CACHE (Ejecutado en Dispatchers.IO)
            val cached = AudioEngineWaveformCache.load(
                context,
                uriString
            )

            if (cached != null) {
                emit(
                    BackgroundWaveformExtractor.WaveformProgress(
                        samples = cached,
                        progress = 1.0f,
                        completed = true
                    )
                )
                return@flow
            }

            // 2. NO HAY CACHE -> EXTRAER
            val extractor = BackgroundWaveformExtractor(context)
            var finalSamples: FloatArray? = null

            extractor
                .extract(uriString)
                .collect { progress ->
                    finalSamples = progress.samples
                    emit(progress)
                }

            // 3. GUARDAR RESULTADO (Ejecutado en Dispatchers.IO sin bloquear UI)
            finalSamples?.let { samples ->
                AudioEngineWaveformCache.save(
                    context,
                    uriString,
                    samples
                )
            }
        }.flowOn(Dispatchers.IO) // Garatiza que todo el trabajo de I/O y caché corra en el hilo de E/S
}