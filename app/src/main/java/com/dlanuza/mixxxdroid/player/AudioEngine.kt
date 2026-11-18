package com.dlanuza.mixxxdroid.player

import android.content.Context
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Process
import android.util.Log
import java.io.File

internal class AudioEngine(
    private val context: Context,
    private val isScratchingProvider: () -> Boolean,
    // (muestras, cantidad válida). El arreglo se REUTILIZA: el receptor debe copiarlo.
    private val pushAudioCallback: (FloatArray, Int) -> Unit
) {
    companion object {
        private const val TAG = "MIXXX_ENGINE"
        private const val INPUT_BUFFER_TIMEOUT_US = 0L
        private const val OUTPUT_BUFFER_TIMEOUT_US = 0L
        private const val AUDIO_BUFFER_MULTIPLIER = 4
        private const val PROCESS_BUFFER_FRAMES = 2048
    }

    // =========================================================
    // WORKER & LOCK
    // =========================================================
    private val lock = Object()

    @Volatile private var running = true

    // =========================================================
    // COMMAND STATE
    // =========================================================
    @Volatile private var requestedUri: String? = null
    @Volatile private var requestedPlay = false
    @Volatile private var requestedPause = false
    @Volatile private var requestedStop = false
    @Volatile private var requestedResume = false
    @Volatile private var requestedSeekMs: Long? = null

    // =========================================================
    // DJ PARAMETERS
    // =========================================================
    @Volatile private var requestedTempo = 1.0f
    @Volatile private var requestedPitch = 0.0f
    @Volatile private var requestedKeylock = false

    @Volatile private var requestedVolume = 1.0f

    private var appliedTempo = -1.0f
    private var appliedPitch = -999.0f
    private var appliedKeylock = false

    private var appliedVolume = -1.0f

    // =========================================================
    // PLAYBACK STATE
    // =========================================================
    @Volatile private var currentUri: String? = null
    @Volatile private var prepared = false
    @Volatile private var playing = false
    @Volatile private var paused = false
    @Volatile private var ended = false
    @Volatile private var currentPositionMs = 0L
    @Volatile private var durationMs = 0L

    // =========================================================
    // MEDIA & HARDWARE
    // =========================================================
    private var bpmDetector: NativeBpmDetector? = null
    private var soundTouch: NativeSoundTouch? = null
    private var detectedBpm: Float = 0f
    private val scratchEngine = ScratchEngine()
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null

    private var softwareAudioFile: File? = null
    private var rawPcm = false

    private var sampleRate = 44100
    private var channelCount = 2

    private var inputEnded = false
    private var decoderEnded = false

    // =========================================================
    // PUBLIC CONTROLS
    // =========================================================
    fun prepare(uri: String) {
        Log.d(TAG, "REQUEST PREPARE -> $uri")
        resetRequests()
        requestedUri = uri
        signalWorker()
    }

    fun play(uri: String) {
        Log.d(TAG, "REQUEST PLAY -> uri=$uri currentUri=$currentUri")
        ended = false
        resetRequests()
        requestedPlay = true
        if (currentUri != uri) requestedUri = uri
        signalWorker()
    }

    // pause() y resume() NO borran un seek pendiente (antes llamaban a resetRequests()).
    fun pause() {
        Log.d(TAG, "REQUEST PAUSE -> position=$currentPositionMs")
        requestedPlay = false
        requestedResume = false
        requestedPause = true
        signalWorker()
    }

    fun resume() {
        Log.d(TAG, "REQUEST RESUME -> position=$currentPositionMs")
        requestedPause = false
        requestedStop = false
        requestedResume = true
        signalWorker()
    }

    fun stop() {
        Log.d(TAG, "REQUEST STOP -> position=$currentPositionMs")
        resetRequests()
        requestedStop = true
        signalWorker()
    }

    fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        Log.d(TAG, "REQUEST SEEK -> $safePosition")
        requestedSeekMs = safePosition
        signalWorker()
    }

    fun pauseAudioTrackOnly() {
        Log.d(TAG, "PAUSE AUDIOTRACK FOR SCRATCH")
        audioTrack?.pause()
    }

    fun resumeAudioTrackOnly() {
        Log.d(TAG, "RESUME AUDIOTRACK AFTER SCRATCH")
        if (playing) {
            audioTrack?.play()
        }
    }

    fun startScratch(positionMs: Long) {
        Log.d(TAG, "START SCRATCH -> positionMs=$positionMs")
        scratchEngine.start(positionMs)
        appliedTempo = -1.0f
        signalWorker()
    }

    fun updateScratch(speedFactor: Float) {
        scratchEngine.update(speedFactor)
        appliedTempo = -1.0f
        signalWorker()
    }

    fun stopScratch() {
        Log.d(TAG, "STOP SCRATCH")
        val finalPosition = scratchEngine.positionMs
        scratchEngine.stop()

        appliedTempo = -1.0f

        if (finalPosition > 0) {
            seekTo(finalPosition)
        }

        requestedResume = true
        signalWorker()
    }

    fun isScratching(): Boolean = scratchEngine.isActive
    fun isPlaying(): Boolean = playing
    fun isEnded(): Boolean = ended

    fun getCurrentPosition(): Long =
        if (scratchEngine.isActive) scratchEngine.positionMs else currentPositionMs

    fun getDuration(): Long = durationMs
    fun getDetectedBpm(): Float = detectedBpm

    fun setTempoAndPitch(tempo: Float, pitch: Float) {
        requestedTempo = tempo.coerceIn(0.8f, 1.2f)
        requestedPitch = pitch.coerceIn(-3.0f, 3.0f)
        signalWorker()
    }

    fun setKeylock(enabled: Boolean) {
        requestedKeylock = enabled
        signalWorker()
    }

    fun setAudioMode(mode: AudioMode) {
        soundTouchEngine.setAudioMode(mode)
    }

    fun setEq(lowDb: Float, midDb: Float, highDb: Float) {
        soundTouchEngine.setEq(lowDb, midDb, highDb)
    }

    private fun resetRequests() {
        requestedUri = null
        requestedPlay = false
        requestedPause = false
        requestedResume = false
        requestedStop = false
        requestedSeekMs = null
    }

    fun setVolume(volume: Float) {
        requestedVolume = volume.coerceIn(0f, 1f)
        signalWorker()
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        Log.d(TAG, "AUDIO THREAD START -> ${Thread.currentThread().name}")

        while (running) {
            try {
                processCommands()

                if (!prepared) {
                    waitForCommand()
                    continue
                }

                applyDjParameters()

                val managerIsScratching = runCatching { isScratchingProvider() }.getOrDefault(false)
                if (!playing && !scratchEngine.isActive && !managerIsScratching) {
                    waitForCommand()
                    continue
                }

                feedDecoder()
                drainDecoder()

                if (decoderEnded && playing) {
                    soundTouchEngine.drainSoundTouchAfterEos()
                }

                if (ended) {
                    requestedResume = false
                    playing = false
                    paused = false
                    waitForCommand()
                }

            } catch (e: InterruptedException) {
                // release() interrumpe el hilo a propósito: no es un error.
                if (!running) break
            } catch (t: Throwable) {
                Log.e(TAG, "AUDIO ENGINE ERROR", t)
                playing = false
                paused = false
                prepared = false
                releaseMedia()
                waitForCommand()
            }
        }

        releaseMedia()
        Log.d(TAG, "AUDIO THREAD END")
    }

    private fun processCommands() {
        val uri = requestedUri
        if (uri != null) {
            requestedUri = null
            Log.d(TAG, "PROCESS NEW URI -> $uri | requestedPlay=$requestedPlay")

            if (uri != currentUri) {
                val shouldPlay = requestedPlay
                prepareInternal(uri)

                if (shouldPlay) {
                    requestedPlay = false
                    audioTrack?.play()
                    playing = true
                    paused = false
                    ended = false
                    Log.d(TAG, "NEW TRACK STARTED -> uri=$uri position=$currentPositionMs")
                }
            }
        }

        if (requestedStop) {
            requestedStop = false
            performStop()
        }

        requestedSeekMs?.let { seek ->
            requestedSeekMs = null
            performSeek(seek)
        }

        if (requestedPause) {
            requestedPause = false
            audioTrack?.pause()
            playing = false
            paused = true
            Log.d(TAG, "PAUSED -> position=$currentPositionMs")
        }

        if (requestedResume && prepared && paused) {
            requestedResume = false
            audioTrack?.play()
            playing = true
            paused = false
            ended = false
            Log.d(TAG, "RESUMED -> position=$currentPositionMs")
        }

        if (requestedPlay && prepared) {
            requestedPlay = false
            audioTrack?.play()
            playing = true
            paused = false
            ended = false
            Log.d(TAG, "PLAYING -> position=$currentPositionMs")
        }
    }

    private fun prepareInternal(uriString: String) {
        Log.d(TAG, "PREPARE INTERNAL -> $uriString")
        releaseMedia()

        requestedTempo = 1.0f
        requestedPitch = 0.0f
        requestedKeylock = false

        val originalUri = Uri.parse(uriString)

        // Si algo falla, el extractor se libera (antes quedaba sin cerrar).
        val localExtractor = MediaExtractor()
        try {
            localExtractor.setDataSource(context, originalUri, null)
        } catch (t: Throwable) {
            localExtractor.release()
            throw t
        }

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until localExtractor.trackCount) {
            val trackFormat = localExtractor.getTrackFormat(i)
            val trackMime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (trackMime?.startsWith("audio/") == true) {
                audioTrackIndex = i
                audioFormat = trackFormat
                break
            }
        }

        if (audioTrackIndex < 0) {
            localExtractor.release()
            error("No audio track found")
        }

        var format = requireNotNull(audioFormat)
        var mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))

        if (mime.equals("audio/x-ms-wma", ignoreCase = true)) {
            Log.d(TAG, "WMA DETECTED -> USING SOFTWARE DECODER")
            localExtractor.release()

            val wavFile = SoftwareAudioDecoder.decodeToWav(context, originalUri)
            softwareAudioFile = wavFile
            val wavInfo = AudioEngineWavParser.parse(wavFile)

            sampleRate = wavInfo.sampleRate
            channelCount = wavInfo.channelCount
            val bytesPerFrame = channelCount * 2
            val bytesPerSecond = sampleRate.toLong() * bytesPerFrame.toLong()
            durationMs = wavInfo.durationMs

            rawPcmEngine.open(
                wavFile = wavFile,
                dataOffset = wavInfo.dataOffset,
                dataSize = wavInfo.dataSize,
                bytesPerSecond = bytesPerSecond,
                bytesPerFrame = bytesPerFrame
            )

            rawPcm = true
            extractor = null
            format = MediaFormat()
            mime = "audio/raw"
        } else {
            rawPcm = false
            localExtractor.selectTrack(audioTrackIndex)

            sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            durationMs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) / 1000L else 0L

            // A partir de aquí, releaseMedia() se encarga de liberarlo si algo falla.
            extractor = localExtractor
        }

        codec = if (rawPcm) {
            null
        } else {
            val newCodec = MediaCodec.createDecoderByType(mime)
            try {
                newCodec.configure(format, null, null, 0)
                newCodec.start()
            } catch (t: Throwable) {
                newCodec.release()
                throw t
            }
            newCodec
        }

        soundTouch = NativeSoundTouch().apply { create(channelCount, sampleRate) }
        bpmDetector = NativeBpmDetector().apply { create(channelCount, sampleRate) }

        createAudioTrack()

        inputEnded = false
        decoderEnded = false
        soundTouchEngine.reset()

        currentPositionMs = 0L
        ended = false
        paused = false
        prepared = true

        appliedTempo = -1.0f
        appliedPitch = -999.0f
        appliedKeylock = !requestedKeylock

        applyDjParameters()
        currentUri = uriString
    }

    private fun createAudioTrack() {
        audioTrack?.release()
        audioTrack = audioTrackEngine.create()
    }

    private fun applyDjParameters() {
        val st = soundTouch ?: return

        // NUEVO: Aplicar volumen físico al AudioTrack
        if (requestedVolume != appliedVolume) {
            appliedVolume = requestedVolume
            audioTrack?.setVolume(appliedVolume)
        }

        if (scratchEngine.isActive) {
            val scratchSpeed = scratchEngine.speedFactor.coerceIn(0.0f, 3.0f)
            st.setRate(scratchSpeed)
            return
        }

        val tempo = requestedTempo
        val pitch = requestedPitch
        val keylock = requestedKeylock

        if (tempo == appliedTempo && pitch == appliedPitch && keylock == appliedKeylock) return

        // Se reinicia SIEMPRE el otro parámetro: si no, al cambiar de modo (o tras un
        // scratch) quedan aplicados rate y tempo a la vez y la canción va al cuadrado.
        if (keylock) {
            st.setRate(1.0f)
            st.setTempo(tempo)
            st.setPitchSemitones(pitch)
        } else {
            st.setTempo(1.0f)
            st.setRate(tempo)
            st.setPitchSemitones(pitch)
        }

        appliedTempo = tempo
        appliedPitch = pitch
        appliedKeylock = keylock
    }

    private fun feedDecoder() {
        if (rawPcm) {
            rawPcmEngine.feed(inputEnded)
        } else {
            mediaCodecEngine.feed(codec = codec, extractor = extractor, inputEnded = inputEnded)
        }
    }

    private fun drainDecoder() {
        if (!rawPcm) {
            mediaCodecEngine.drain(codec = codec, playing = playing)
        }
    }

    private val rawPcmEngine = AudioEngineRawPcm(
        tag = TAG,
        processBufferFrames = PROCESS_BUFFER_FRAMES,
        processDecodedPcm = { b, i -> soundTouchEngine.processDecodedPcm(b, i) },
        setInputEnded = { inputEnded = it },
        setDecoderEnded = { decoderEnded = it }
    )

    private val mediaCodecEngine = AudioEngineMediaCodec(
        inputBufferTimeoutUs = INPUT_BUFFER_TIMEOUT_US,
        outputBufferTimeoutUs = OUTPUT_BUFFER_TIMEOUT_US,
        onInputEnded = { inputEnded = true },
        onOutputFormatChanged = { handleOutputFormat(it) },
        onDecodedPcm = { b, i -> soundTouchEngine.processDecodedPcm(b, i) },
        onDecoderEnded = { decoderEnded = true }
    )

    private val soundTouchEngine = AudioEngineSoundTouch(
        processBufferFrames = PROCESS_BUFFER_FRAMES,
        getSoundTouch = { soundTouch },
        getBpmDetector = { bpmDetector },
        getAudioTrack = { audioTrack },
        getChannelCount = { channelCount },
        getSampleRate = { sampleRate },
        getRequestedTempo = { requestedTempo },
        getDurationMs = { durationMs },
        getCurrentPositionMs = { currentPositionMs },
        setCurrentPositionMs = { currentPositionMs = it },
        getPlaying = { playing },
        getScratchEngine = { scratchEngine },
        setEnded = { ended = it },
        setDetectedBpm = { detectedBpm = it },
        getRequestedPause = { requestedPause },
        getRequestedStop = { requestedStop },
        getRequestedSeekMs = { requestedSeekMs },
        writeAudio = { track, data, count -> audioTrackEngine.write(track, data, count) },
        onPushAudioToNative = { samples, count ->
            pushAudioCallback(samples, count)
        }
    )

    private val audioTrackEngine = AudioEngineAudioTrack(
        tag = TAG,
        audioBufferMultiplier = AUDIO_BUFFER_MULTIPLIER,
        getSampleRate = { sampleRate },
        getChannelCount = { channelCount },
        getRunning = { running },
        getRequestedStop = { requestedStop },
        getRequestedSeekMs = { requestedSeekMs }
    )

    private fun handleOutputFormat(format: MediaFormat) {
        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        }
        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        }
    }

    private fun performSeek(positionMs: Long) {
        if (!prepared) return

        val wasPlaying = playing
        audioTrack?.apply { pause(); flush() }

        if (rawPcm) {
            currentPositionMs = rawPcmEngine.seek(positionMs, durationMs, sampleRate)
        } else {
            val localExtractor = extractor ?: return
            val localCodec = codec ?: return
            localCodec.flush()
            localExtractor.seekTo(positionMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            currentPositionMs = positionMs.coerceIn(0L, durationMs)
        }

        soundTouch?.clear()
        soundTouchEngine.reset()
        inputEnded = false
        decoderEnded = false
        ended = false

        paused = !wasPlaying
        playing = wasPlaying
        if (wasPlaying) audioTrack?.play()

        Log.d(TAG, "SEEK COMPLETE -> position=$currentPositionMs rawPcm=$rawPcm")
    }

    private fun performStop() {
        if (!prepared) return

        audioTrack?.apply { pause(); flush() }

        if (rawPcm) {
            rawPcmEngine.resetToStart()
        } else {
            try { codec?.flush() } catch (t: Throwable) { Log.w(TAG, "CODEC FLUSH ERROR", t) }
            extractor?.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }

        soundTouch?.clear()
        soundTouchEngine.reset()
        inputEnded = false
        decoderEnded = false
        ended = false
        currentPositionMs = 0L
        playing = false
        paused = false

        Log.d(TAG, "STOP COMPLETE")
    }

    private fun waitForCommand() {
        synchronized(lock) {
            val managerIsScratching = runCatching { isScratchingProvider() }.getOrDefault(false)
            if (running && requestedUri == null && !requestedPlay && !requestedPause &&
                !requestedResume && !requestedStop && requestedSeekMs == null &&
                !playing && !scratchEngine.isActive && !managerIsScratching) {
                lock.wait()
            }
        }
    }

    private fun signalWorker() {
        synchronized(lock) { lock.notifyAll() }
    }

    private fun releaseMedia() {
        playing = false
        paused = false
        prepared = false
        scratchEngine.stop()

        try {
            audioTrack?.apply { pause(); flush(); release() }
        } catch (t: Throwable) { Log.w(TAG, "AUDIOTRACK RELEASE ERROR", t) }
        audioTrack = null

        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null

        runCatching { extractor?.release() }
        extractor = null

        rawPcmEngine.close()

        runCatching { soundTouch?.destroy() }
        soundTouch = null

        // NUEVO: antes el detector de BPM nunca se destruía.
        runCatching { bpmDetector?.destroy() }
        bpmDetector = null

        SoftwareAudioDecoder.deleteTempFile(softwareAudioFile)
        softwareAudioFile = null

        rawPcm = false
        currentUri = null
        currentPositionMs = 0L
        durationMs = 0L
        ended = false
        inputEnded = false
        decoderEnded = false
        soundTouchEngine.reset()
    }

    fun release() {
        running = false
        signalWorker()
        worker.interrupt()
        // Esperar a que el hilo termine ANTES de que el manager libere lo nativo.
        runCatching { worker.join(1000) }
    }

    // Debe ir al FINAL de la clase: así todas las propiedades ya están inicializadas
    // cuando el hilo empieza a ejecutarse.
    private val worker = Thread({ audioLoop() }, "Mixxx-Audio").apply { start() }
}