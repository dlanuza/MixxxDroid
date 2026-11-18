package com.dlanuza.mixxxdroid.player

import android.media.AudioTrack
import android.media.MediaCodec
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class AudioMode {
    NORMAL,
    INSTRUMENTAL,
    VOCALS_ONLY
}

internal class AudioEngineSoundTouch(
    private val processBufferFrames: Int,
    private val getSoundTouch: () -> NativeSoundTouch?,
    private val getBpmDetector: () -> NativeBpmDetector?,
    private val getAudioTrack: () -> AudioTrack?,
    private val getChannelCount: () -> Int,
    private val getSampleRate: () -> Int,
    private val getRequestedTempo: () -> Float,
    private val getDurationMs: () -> Long,
    private val getCurrentPositionMs: () -> Long,
    private val setCurrentPositionMs: (Long) -> Unit,
    private val getPlaying: () -> Boolean,
    private val getRequestedPause: () -> Boolean,
    private val getRequestedStop: () -> Boolean,
    private val getRequestedSeekMs: () -> Long?,
    private val getScratchEngine: () -> ScratchEngine,
    private val setEnded: (Boolean) -> Unit,
    private val setDetectedBpm: (Float) -> Unit,
    private val writeAudio: (AudioTrack, ShortArray, Int) -> Int,
    // Alimenta el motor C++ (DjEngine). Recibe (arreglo reutilizado, muestras válidas).
    private val onPushAudioToNative: ((FloatArray, Int) -> Unit)? = null
) {

    private var soundTouchFlushed = false
    private var bpmUpdateCounter = 0

    // Resto de milisegundos que aún no se ha sumado a la posición (evita truncar).
    private var positionRemainderMs = 0.0

    private val scratchBuffer = ScratchPcmBuffer(maxFrames = 44100 * 60)
    private var audioMode = AudioMode.NORMAL

    // Ecualizador de 3 bandas: se aplica a la SALIDA de SoundTouch, justo antes del AudioTrack.
    private val eq = ThreeBandEq()

    // BUFFERS REUTILIZABLES (0 alocaciones en tiempo de reproducción)
    private var inputBuffer = ShortArray(8192)
    private var floatBuffer = FloatArray(8192)
    private var outputBuffer = ShortArray(processBufferFrames * 2)

    private fun ensureInputCapacity(capacity: Int) {
        if (inputBuffer.size < capacity) {
            inputBuffer = ShortArray(capacity)
            floatBuffer = FloatArray(capacity)
        }
    }

    private fun ensureOutputCapacity(capacity: Int) {
        if (outputBuffer.size < capacity) {
            outputBuffer = ShortArray(capacity)
        }
    }

    private fun processVocalFilter(pcmBuffer: ShortArray, shortCount: Int, channelCount: Int) {
        if (audioMode == AudioMode.NORMAL || channelCount != 2) return

        for (i in 0 until shortCount step 2) {
            val left = pcmBuffer[i].toInt()
            val right = pcmBuffer[i + 1].toInt()

            val filteredValue = when (audioMode) {
                AudioMode.INSTRUMENTAL -> ((left - right) / 2).coerceIn(-32768, 32767).toShort()
                AudioMode.VOCALS_ONLY -> ((left + right) / 2).coerceIn(-32768, 32767).toShort()
                AudioMode.NORMAL -> continue
            }

            pcmBuffer[i] = filteredValue
            pcmBuffer[i + 1] = filteredValue
        }
    }

    fun processDecodedPcm(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        val st = getSoundTouch() ?: return
        val channelCount = getChannelCount()

        if (scratchBuffer.getChannelCount() != channelCount) {
            scratchBuffer.configure(channelCount)
        }

        val shortCount = info.size / 2
        if (shortCount <= 0) return

        // 1. Asegurar capacidad sin reasignar memoria
        ensureInputCapacity(shortCount)

        val duplicate = buffer.duplicate().apply {
            order(ByteOrder.LITTLE_ENDIAN)
            position(info.offset)
            limit(info.offset + info.size)
        }

        duplicate.asShortBuffer().get(inputBuffer, 0, shortCount)

        // 2. Aplicar filtro vocal/instrumental
        processVocalFilter(inputBuffer, shortCount, channelCount)

        // 3. Convertir a float y enviar al motor C++ (sin crear un arreglo nuevo por bloque)
        for (i in 0 until shortCount) {
            floatBuffer[i] = inputBuffer[i] / 32768.0f
        }
        onPushAudioToNative?.invoke(floatBuffer, shortCount)

        val frameCount = shortCount / channelCount
        if (frameCount <= 0) return

        val sourceStartFrame = if (info.presentationTimeUs >= 0L) {
            (info.presentationTimeUs.toDouble() * getSampleRate().toDouble() / 1_000_000.0).toLong()
        } else {
            scratchBuffer.getEndFrame()
        }

        scratchBuffer.append(inputBuffer, frameCount, sourceStartFrame)

        getBpmDetector()?.putSamples(inputBuffer, shortCount)

        bpmUpdateCounter++
        if (bpmUpdateCounter >= 20) {
            bpmUpdateCounter = 0
            val bpm = getBpmDetector()?.getBpm() ?: 0f
            if (bpm > 0f) {
                setDetectedBpm(bpm)
                Log.d("MIXXX_BPM", "LIVE BPM -> $bpm")
            }
        }

        st.putSamples(inputBuffer, shortCount)
        drainSoundTouch()
    }

    fun drainSoundTouch() {
        val st = getSoundTouch() ?: return
        val localAudioTrack = getAudioTrack() ?: return
        val channelCount = getChannelCount()

        val requiredOutputSize = processBufferFrames * channelCount
        ensureOutputCapacity(requiredOutputSize)

        while (st.numSamples() > 0) {
            if (shouldCancelPlayback()) break

            val frames = st.receiveSamples(outputBuffer)
            if (frames <= 0) break

            val shorts = frames * channelCount
            eq.process(outputBuffer, shorts, channelCount, getSampleRate())
            val writtenShorts = writeAudio(localAudioTrack, outputBuffer, shorts)

            if (writtenShorts > 0) {
                advancePosition(writtenShorts / channelCount)
            }

            if (shouldCancelPlayback() || writtenShorts < shorts) break
        }
    }

    private fun shouldCancelPlayback(): Boolean {
        return getRequestedPause() ||
                getRequestedStop() ||
                getRequestedSeekMs() != null ||
                !getPlaying()
    }

    fun drainSoundTouchAfterEos() {
        val st = getSoundTouch() ?: return

        if (!soundTouchFlushed) {
            st.flush()
            soundTouchFlushed = true
        }

        drainSoundTouch()

        if (st.numSamples() == 0) {
            val bpm = getBpmDetector()?.getBpm() ?: 0f
            Log.d("MIXXX_BPM", "FINAL BPM -> $bpm")

            setDetectedBpm(bpm)
            setEnded(true)
            setCurrentPositionMs(getDurationMs())
            getAudioTrack()?.pause()

            Log.d("MIXXX_ENGINE", "PLAYBACK ENDED")
        }
    }

    fun reset() {
        soundTouchFlushed = false
        bpmUpdateCounter = 0
        positionRemainderMs = 0.0
        scratchBuffer.clear()
        eq.reset()
    }

    fun setEq(lowDb: Float, midDb: Float, highDb: Float) {
        eq.setGains(lowDb, midDb, highDb)
    }

    fun setAudioMode(mode: AudioMode) {
        audioMode = mode
    }

    /**
     * `frames` son frames de SALIDA (ya procesados por SoundTouch).
     * Los frames de ORIGEN consumidos son salida x tempo, así que la posición de
     * la canción avanza a esa velocidad y el cabezal de la waveform queda sincronizado.
     */
    private fun advancePosition(frames: Int) {
        val sampleRate = getSampleRate()
        if (sampleRate <= 0) return

        positionRemainderMs +=
            frames.toDouble() * getRequestedTempo() * 1000.0 / sampleRate.toDouble()

        val wholeMs = positionRemainderMs.toLong()
        positionRemainderMs -= wholeMs.toDouble()

        setCurrentPositionMs(
            (getCurrentPositionMs() + wholeMs).coerceIn(0L, getDurationMs())
        )
    }
}