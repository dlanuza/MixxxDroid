package com.dlanuza.mixxxdroid.player

import android.content.Context
import android.util.Log

class AudioPlayerManager(
    context: Context
) {

    companion object {
        private const val TAG = "MIXXX"

        // Se mantiene por compatibilidad. Para los Decks A y B usa el constructor
        // público directamente: cada deck necesita su propia instancia.
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(
                    context.applicationContext
                ).also {
                    INSTANCE = it
                }
            }
        }
    }

    // =========================================================
    // AUDIO ENGINE NORMAL (aislado por instancia de deck)
    // =========================================================

    private val audioEngine = AudioEngine(
        context = context.applicationContext,
        isScratchingProvider = { isScratching() },
        pushAudioCallback = { samples, count -> pushAudio(samples, count) }
    )

    // =========================================================
    // DJ ENGINE NATIVO
    // =========================================================

    private var nativeEngineHandle: Long = 0L

    // @Volatile: lo lee el hilo de audio y lo escribe el hilo principal.
    @Volatile
    private var isScratching = false

    private var baseTempo = 1.0f
    private var basePitch = 0.0f
    private var wasPlayingBeforeScratch = false

    init {
        try {
            System.loadLibrary("mixxx_audio")

            nativeEngineHandle =
                nativeInit(
                    sampleRate = 44100,
                    channelCount = 2
                )

            Log.d(TAG, "DjEngine inicializado. Handle=$nativeEngineHandle")

        } catch (e: UnsatisfiedLinkError) {

            Log.e(TAG, "Error cargando mixxx_audio", e)

            nativeEngineHandle = 0L
        }
    }

    // =========================================================
    // REPRODUCCIÓN
    // =========================================================

    fun prepare(uri: String) {
        Log.d(TAG, "PREPARE -> $uri")
        audioEngine.prepare(uri)
    }

    fun play(uri: String? = null) {
        Log.d(TAG, "PLAY -> $uri")

        if (uri != null) {
            audioEngine.play(uri)
        } else {
            audioEngine.resume()
        }
    }

    fun pause() {
        Log.d(TAG, "PAUSE")
        audioEngine.pause()
    }

    fun resume() {
        Log.d(TAG, "RESUME")
        audioEngine.resume()
    }

    fun stop() {
        Log.d(TAG, "STOP")
        audioEngine.stop()
    }

    fun seekTo(position: Long) {
        val safePosition = position.coerceAtLeast(0L)
        Log.d(TAG, "SEEK -> $safePosition")
        audioEngine.seekTo(safePosition)
    }

    // =========================================================
    // TEMPO / PITCH
    // =========================================================

    fun setTempoAndPitch(
        tempo: Float,
        semitones: Float
    ) {
        baseTempo = tempo
        basePitch = semitones

        Log.d(TAG, "TEMPO+PITCH -> tempo=$tempo pitch=$semitones")

        if (!isScratching) {
            audioEngine.setTempoAndPitch(tempo, semitones)
        }
    }

    fun setKeylock(enabled: Boolean) {
        Log.d(TAG, "KEYLOCK -> $enabled")
        audioEngine.setKeylock(enabled)
    }

    fun setAudioMode(mode: AudioMode) {
        Log.d(TAG, "AUDIO MODE -> $mode")
        audioEngine.setAudioMode(mode)
    }

    fun setEq(lowDb: Float, midDb: Float, highDb: Float) {
        audioEngine.setEq(lowDb, midDb, highDb)
    }

    // =========================================================
    // VOLUMEN
    // =========================================================

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        Log.d(TAG, "VOLUME -> $safeVolume")
        audioEngine.setVolume(safeVolume)
    }

    // =========================================================
    // ESTADO
    // =========================================================

    fun isPlaying(): Boolean =
        audioEngine.isPlaying()

    fun isEnded(): Boolean =
        audioEngine.isEnded()

    fun getCurrentPosition(): Long {

        if (
            isScratching &&
            nativeEngineHandle != 0L
        ) {
            return nativeGetCurrentPositionMs(nativeEngineHandle)
        }

        return audioEngine.getCurrentPosition()
    }

    fun getDuration(): Long =
        audioEngine.getDuration()

    fun getDetectedBpm(): Float =
        audioEngine.getDetectedBpm()

    // =========================================================
    // SCRATCH
    // =========================================================

    fun startScratch(positionMs: Long) {

        if (nativeEngineHandle == 0L) {
            Log.e(TAG, "SCRATCH -> DjEngine no inicializado")
            return
        }

        val safePosition = positionMs.coerceAtLeast(0L)

        Log.d(TAG, "START SCRATCH -> position=$safePosition")

        // Recordar si sonaba, para reanudar SOLO en ese caso al soltar.
        wasPlayingBeforeScratch = audioEngine.isPlaying()
        if (wasPlayingBeforeScratch) {
            audioEngine.pause()
        }

        isScratching = true

        nativeStartScratch(nativeEngineHandle, safePosition)
    }

    fun updateScratch(
        speedFactor: Float
    ) {

        if (!isScratching) {
            return
        }

        if (nativeEngineHandle == 0L) {
            return
        }

        val safeSpeed = speedFactor.coerceIn(-4.0f, 4.0f)

        nativeUpdateScratchSpeed(nativeEngineHandle, safeSpeed)
    }

    fun stopScratch() {

        if (!isScratching) {
            return
        }

        Log.d(TAG, "STOP SCRATCH")

        val finalPosition =
            if (nativeEngineHandle != 0L) {
                nativeGetCurrentPositionMs(nativeEngineHandle)
            } else {
                audioEngine.getCurrentPosition()
            }

        Log.d(TAG, "SCRATCH FINAL POSITION -> $finalPosition")

        if (nativeEngineHandle != 0L) {
            nativeStopScratch(nativeEngineHandle)
        }

        isScratching = false

        audioEngine.seekTo(finalPosition)

        // Restaurar el tempo Y el pitch que tenía el deck (antes el pitch volvía a 0).
        audioEngine.setTempoAndPitch(baseTempo, basePitch)

        if (wasPlayingBeforeScratch) {
            audioEngine.resume()
        }
        wasPlayingBeforeScratch = false
    }

    fun isScratching(): Boolean =
        isScratching

    fun getScratchPosition(): Long =
        getCurrentPosition()

    // =========================================================
    // PCM -> DJ ENGINE
    // =========================================================

    /**
     * @param count número de muestras válidas en [samples]. El arreglo puede ser
     * más grande y se reutiliza, así que el lado nativo debe copiar lo que necesite.
     */
    fun pushAudio(
        samples: FloatArray,
        count: Int = samples.size
    ) {

        if (nativeEngineHandle == 0L) {
            return
        }

        if (count <= 0) {
            return
        }

        nativePushAudio(
            nativeEngineHandle,
            samples,
            count
        )
    }

    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

        Log.d(TAG, "RELEASE")

        isScratching = false

        // AudioEngine.release() espera a que el hilo de audio termine,
        // así que después ya es seguro liberar el motor nativo.
        audioEngine.release()

        if (nativeEngineHandle != 0L) {

            nativeRelease(nativeEngineHandle)

            nativeEngineHandle = 0L
        }
    }

    // =========================================================
    // JNI
    // =========================================================

    private external fun nativeInit(
        sampleRate: Int,
        channelCount: Int
    ): Long

    private external fun nativeRelease(
        handle: Long
    )

    private external fun nativePushAudio(
        handle: Long,
        samples: FloatArray,
        sampleCount: Int
    ): Boolean

    private external fun nativeStartScratch(
        handle: Long,
        positionMs: Long
    )

    private external fun nativeUpdateScratchSpeed(
        handle: Long,
        speedFactor: Float
    )

    private external fun nativeStopScratch(
        handle: Long
    )

    private external fun nativeGetCurrentPositionMs(
        handle: Long
    ): Long
}