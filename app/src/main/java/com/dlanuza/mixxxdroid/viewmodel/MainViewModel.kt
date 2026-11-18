package com.dlanuza.mixxxdroid.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlanuza.mixxxdroid.data.models.Song
import com.dlanuza.mixxxdroid.gui.events.DeckEvent
import com.dlanuza.mixxxdroid.gui.components.DJFunctions
import com.dlanuza.mixxxdroid.gui.state.DeckUiState
import com.dlanuza.mixxxdroid.player.AudioMode
import com.dlanuza.mixxxdroid.player.AudioPlayerManager
import com.dlanuza.mixxxdroid.player.EqBand
import com.dlanuza.mixxxdroid.player.WaveformRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainViewModel(
    context: Context,
    private val playerA: AudioPlayerManager,
    private val playerB: AudioPlayerManager
) : ViewModel() {

    private val appContext = context.applicationContext
    private val waveformRepository = WaveformRepository(appContext)

    private val djFunctions = DJFunctions(playerA, playerB)

    var loopActiveA by mutableStateOf(false)
        private set
    var loopActiveB by mutableStateOf(false)
        private set

    var loopStartA by mutableStateOf<Long?>(null)
        private set
    var loopStartB by mutableStateOf<Long?>(null)
        private set

    var loopEndA by mutableStateOf<Long?>(null)
        private set
    var loopEndB by mutableStateOf<Long?>(null)
        private set

    var loopBeatsA by mutableStateOf(0)
        private set
    var loopBeatsB by mutableStateOf(0)
        private set

    var repeatSongA by mutableStateOf(false)
        private set
    var repeatSongB by mutableStateOf(false)
        private set

    var audioMode by mutableStateOf(AudioMode.NORMAL)
        private set

    var targetDeck by mutableStateOf("A")
        private set

    fun updateTargetDeck(deck: String) {
        targetDeck = deck
        Log.d("MIXXX_DEBUG", "Target deck actualizado a: $targetDeck")
    }

    var deckASong by mutableStateOf<Song?>(null)
        private set
    var deckUiState by mutableStateOf(DeckUiState())
        private set

    var deckBSong by mutableStateOf<Song?>(null)
        private set
    var deckBUiState by mutableStateOf(DeckUiState())
        private set

    private var playlistA: List<Song> = emptyList()
    private var currentSongIndexA = 0
    private var endHandledA = false

    private var playlistB: List<Song> = emptyList()
    private var currentSongIndexB = 0
    private var endHandledB = false

    // =========================================================
    // PROTECCIÓN CONTRA FALSOS FINES DE PISTA POR SCRATCH
    // =========================================================
    private var lastScratchStopTimeA = 0L
    private var lastScratchStopTimeB = 0L

    var waveformSamplesA by mutableStateOf(FloatArray(0))
        private set
    var waveformProgressA by mutableStateOf(0f)
        private set
    var waveformLoadingA by mutableStateOf(false)
        private set
    private var waveformJobA: Job? = null

    var waveformSamplesB by mutableStateOf(FloatArray(0))
        private set
    var waveformProgressB by mutableStateOf(0f)
        private set
    var waveformLoadingB by mutableStateOf(false)
        private set

    var hotCuesA by mutableStateOf<List<Long?>>(List(6) { null })
        private set
    var hotCuesB by mutableStateOf<List<Long?>>(List(6) { null })
        private set
    var selectedHotCueA by mutableStateOf(0)
        private set
    var selectedHotCueB by mutableStateOf(0)
        private set

    fun selectHotCue(isDeckA: Boolean, index: Int) {
        if (index !in 0..5) return
        if (isDeckA) selectedHotCueA = index else selectedHotCueB = index
    }

    fun setHotCuePosition(isDeckA: Boolean, positionMs: Long) {
        val duration = if (isDeckA) deckUiState.duration else deckBUiState.duration
        val index = if (isDeckA) selectedHotCueA else selectedHotCueB
        val updated = (if (isDeckA) hotCuesA else hotCuesB).toMutableList()
        updated[index] = positionMs.coerceIn(0L, duration.coerceAtLeast(0L))
        if (isDeckA) hotCuesA = updated.toList() else hotCuesB = updated.toList()
    }

    fun triggerHotCue(isDeckA: Boolean, index: Int) {
        if (index !in 0..5) return
        val position = (if (isDeckA) hotCuesA else hotCuesB)[index] ?: return
        val player = if (isDeckA) playerA else playerB
        val current = if (isDeckA) deckUiState else deckBUiState
        player.seekTo(position)
        player.resume()
        updateState(isDeckA, current.copy(currentPosition = position, isPlaying = true))
    }

    fun randomizeHotCues(isDeckA: Boolean) {
        val duration = if (isDeckA) deckUiState.duration else deckBUiState.duration
        if (duration <= 0L) return

        val margin = (duration * 0.05).toLong()
        val usableDuration = (duration - margin * 2).coerceAtLeast(1L)
        val points = List(6) { index ->
            val segmentStart = margin + usableDuration * index / 6L
            val segmentEnd = margin + usableDuration * (index + 1) / 6L
            val segmentSize = (segmentEnd - segmentStart).coerceAtLeast(1L)
            segmentStart + kotlin.random.Random.nextLong(segmentSize)
        }
        if (isDeckA) hotCuesA = points else hotCuesB = points
        if (isDeckA) selectedHotCueA = 0 else selectedHotCueB = 0
    }

    fun clearHotCues(isDeckA: Boolean) {
        if (isDeckA) {
            hotCuesA = List(6) { null }
            selectedHotCueA = 0
        } else {
            hotCuesB = List(6) { null }
            selectedHotCueB = 0
        }
    }

    fun setLoopIn(isDeckA: Boolean) {
        val position = djFunctions.setLoopIn(isDeckA) ?: return
        if (isDeckA) {
            loopStartA = position
            loopEndA = null
            loopActiveA = false
            loopBeatsA = 0
        } else {
            loopStartB = position
            loopEndB = null
            loopActiveB = false
            loopBeatsB = 0
        }
    }

    fun setLoopOut(isDeckA: Boolean): Boolean {
        val success = djFunctions.setLoopOut(isDeckA)
        if (success) refreshLoopUiState(isDeckA)
        return success
    }

    fun setBeatLoop(isDeckA: Boolean, beats: Int): Boolean {
        val bpm = if (isDeckA) deckUiState.bpm else deckBUiState.bpm
        val success = djFunctions.setBeatLoop(isDeckA, beats, bpm)
        if (success) refreshLoopUiState(isDeckA)
        return success
    }

    fun toggleLoop(isDeckA: Boolean): Boolean {
        val active = djFunctions.toggleLoop(isDeckA)
        refreshLoopUiState(isDeckA)
        return active
    }

    fun exitLoop(isDeckA: Boolean) {
        djFunctions.exitLoop(isDeckA)
        refreshLoopUiState(isDeckA)
    }

    fun clearLoop(isDeckA: Boolean) {
        djFunctions.clearLoop(isDeckA)
        refreshLoopUiState(isDeckA)
    }

    fun toggleRepeatSong(isDeckA: Boolean): Boolean {
        val enabled = djFunctions.toggleRepeatSong(isDeckA)
        if (isDeckA) repeatSongA = enabled else repeatSongB = enabled
        return enabled
    }

    fun setRepeatSong(isDeckA: Boolean, enabled: Boolean) {
        djFunctions.setRepeatSong(isDeckA, enabled)
        if (isDeckA) repeatSongA = enabled else repeatSongB = enabled
    }

    private fun refreshLoopUiState(isDeckA: Boolean) {
        if (isDeckA) {
            loopActiveA = djFunctions.isLoopActive(true)
            loopStartA = djFunctions.getLoopStart(true)
            loopEndA = djFunctions.getLoopEnd(true)
            loopBeatsA = djFunctions.getLoopBeats(true)
        } else {
            loopActiveB = djFunctions.isLoopActive(false)
            loopStartB = djFunctions.getLoopStart(false)
            loopEndB = djFunctions.getLoopEnd(false)
            loopBeatsB = djFunctions.getLoopBeats(false)
        }
    }

    private var waveformJobB: Job? = null

    private var positionJobA: Job? = null
    private var positionJobB: Job? = null

    private var isSeekingA = false
    private var wasPlayingBeforeSeekA = false
    private var isPlayStateTransitioningA = false

    private var isSeekingB = false
    private var wasPlayingBeforeSeekB = false
    private var isPlayStateTransitioningB = false

    // =========================================================
    // CROSSFADER (EQUAL POWER) Y MASTER
    // =========================================================
    var crossfader by mutableStateOf(0f)
        private set

    var masterVolume by mutableFloatStateOf(1f)
        private set

    var isAutoCrossfadeEnabled by mutableStateOf(false)
        private set

    private var channelVolumeA = 0.8f
    private var channelVolumeB = 0.8f

    private var autoCrossfadeTriggerPercent = 0.35f

    // =========================================================
    // AUTOMIX INTELIGENTE
    // =========================================================
    var isAutomixEnabled by mutableStateOf(false)
        private set

    var automixTransitionSeconds by mutableStateOf(6)
        private set

    private var isAutomixTransitioning = false

    fun toggleAutomix(enabled: Boolean) {
        isAutomixEnabled = enabled
        Log.d("MIXXX_DEBUG", "🤖 Automix ${if (enabled) "ACTIVADO" else "DESACTIVADO"}")
    }

    fun updateAutomixTransitionSeconds(seconds: Int) {
        automixTransitionSeconds = seconds.coerceIn(2, 15)
    }

    private fun applyVolumes() {
        val x = ((crossfader + 1.0f) / 2.0f).coerceIn(0f, 1f)
        val angle = x * (Math.PI / 2.0).toFloat()
        val factorA = cos(angle)
        val factorB = sin(angle)

        playerA.setVolume(channelVolumeA * factorA * masterVolume)
        playerB.setVolume(channelVolumeB * factorB * masterVolume)
    }

    fun updateCrossfader(value: Float) {
        crossfader = value.coerceIn(-1f, 1f)
        applyVolumes()
    }

    fun updateMasterVolume(value: Float) {
        masterVolume = value.coerceIn(0f, 1f)
        applyVolumes()
    }

    fun toggleAutoCrossfade(enabled: Boolean) {
        isAutoCrossfadeEnabled = enabled
        if (enabled) {
            autoCrossfadeTriggerPercent = ((15..55).random().toFloat() / 100f)
            Log.d("MIXXX_DEBUG", "🎲 Auto-Crossfade ACTIVADO al ${(autoCrossfadeTriggerPercent * 100).roundToInt()}%.")
        }
    }

    private fun triggerAutomixTransition(fromDeckA: Boolean) {
        if (isAutomixTransitioning) return
        isAutomixTransitioning = true

        viewModelScope.launch {
            if (fromDeckA) {
                Log.d("MIXXX_DEBUG", "🤖 Automix: Transición Deck A -> Deck B")

                val targetPlaylist = if (playlistB.isNotEmpty()) playlistB else playlistA
                if (targetPlaylist.isNotEmpty()) {
                    currentSongIndexB = (currentSongIndexA + 1) % targetPlaylist.size
                    val nextSong = targetPlaylist[currentSongIndexB]
                    loadDeckBForAutomix(nextSong, play = true)
                }

                val startCross = crossfader
                val targetCross = 1.0f
                val durationMs = automixTransitionSeconds * 1000L
                val intervalMs = 50L
                val steps = (durationMs / intervalMs).coerceAtLeast(1L)
                val stepDelta = (targetCross - startCross) / steps

                for (i in 1..steps) {
                    if (!isActive) break
                    val newCross = (startCross + stepDelta * i).coerceIn(-1f, 1f)
                    updateCrossfader(newCross)
                    delay(intervalMs)
                }
                updateCrossfader(1.0f)

                playerA.pause()
                updateState(true, deckUiState.copy(isPlaying = false))

                Log.d("MIXXX_DEBUG", "🤖 Automix: Transición A -> B completada con éxito.")
            } else {
                Log.d("MIXXX_DEBUG", "🤖 Automix: Transición Deck B -> Deck A")

                val targetPlaylist = if (playlistA.isNotEmpty()) playlistA else playlistB
                if (targetPlaylist.isNotEmpty()) {
                    currentSongIndexA = (currentSongIndexB + 1) % targetPlaylist.size
                    val nextSong = targetPlaylist[currentSongIndexA]
                    loadDeckAForAutomix(nextSong, play = true)
                }

                val startCross = crossfader
                val targetCross = -1.0f
                val durationMs = automixTransitionSeconds * 1000L
                val intervalMs = 50L
                val steps = (durationMs / intervalMs).coerceAtLeast(1L)
                val stepDelta = (targetCross - startCross) / steps

                for (i in 1..steps) {
                    if (!isActive) break
                    val newCross = (startCross + stepDelta * i).coerceIn(-1f, 1f)
                    updateCrossfader(newCross)
                    delay(intervalMs)
                }
                updateCrossfader(-1.0f)

                playerB.pause()
                updateState(false, deckBUiState.copy(isPlaying = false))

                Log.d("MIXXX_DEBUG", "🤖 Automix: Transición B -> A completada con éxito.")
            }
            isAutomixTransitioning = false
        }
    }

    private fun loadDeckBForAutomix(song: Song, play: Boolean = true) {
        endHandledB = false
        val keep = deckBUiState
        deckBSong = song
        deckBUiState = DeckUiState(
            tempo = 1.0f, pitch = 0.0f, isKeylockEnabled = false,
            isPlaying = play, currentPosition = 0L, duration = 0L,
            eqLowDb = keep.eqLowDb, eqMidDb = keep.eqMidDb, eqHighDb = keep.eqHighDb,
            audioMode = keep.audioMode
        )
        playerB.setTempoAndPitch(1.0f, 0.0f)
        playerB.setKeylock(false)
        playerB.setAudioMode(keep.audioMode)
        playerB.setEq(keep.eqLowDb, keep.eqMidDb, keep.eqHighDb)

        if (play) {
            playerB.play(song.uri)
        } else {
            playerB.prepare(song.uri)
        }

        loadWaveformB(song.uri)
        startPlayerMonitorB()
    }

    private fun loadDeckAForAutomix(song: Song, play: Boolean = true) {
        endHandledA = false
        val keep = deckUiState
        deckASong = song
        deckUiState = DeckUiState(
            tempo = 1.0f, pitch = 0.0f, isKeylockEnabled = false,
            isPlaying = play, currentPosition = 0L, duration = 0L,
            eqLowDb = keep.eqLowDb, eqMidDb = keep.eqMidDb, eqHighDb = keep.eqHighDb,
            audioMode = keep.audioMode
        )
        playerA.setTempoAndPitch(1.0f, 0.0f)
        playerA.setKeylock(false)
        playerA.setAudioMode(keep.audioMode)
        playerA.setEq(keep.eqLowDb, keep.eqMidDb, keep.eqHighDb)

        if (play) {
            playerA.play(song.uri)
        } else {
            playerA.prepare(song.uri)
        }

        loadWaveformA(song.uri)
        startPlayerMonitorA()
    }

    fun changeAudioMode(mode: AudioMode) {
        audioMode = mode
        deckUiState = deckUiState.copy(audioMode = mode)
        deckBUiState = deckBUiState.copy(audioMode = mode)
        playerA.setAudioMode(mode)
        playerB.setAudioMode(mode)
    }

    fun setPlaylistForDeckA(songs: List<Song>, selectedSong: Song, forcePlay: Boolean = false) {
        playlistA = songs.ifEmpty { listOf(selectedSong) }
        currentSongIndexA = playlistA.indexOfFirst { it.id == selectedSong.id }.coerceAtLeast(0)

        if (playlistB.isEmpty()) {
            playlistB = playlistA
            currentSongIndexB = (currentSongIndexA + 1) % playlistA.size
        }

        loadDeckA(selectedSong, forcePlay)
    }

    fun setPlaylistForDeckB(songs: List<Song>, selectedSong: Song, forcePlay: Boolean = false) {
        playlistB = songs.ifEmpty { listOf(selectedSong) }
        currentSongIndexB = playlistB.indexOfFirst { it.id == selectedSong.id }.coerceAtLeast(0)

        if (playlistA.isEmpty()) {
            playlistA = playlistB
            currentSongIndexA = (currentSongIndexB + 1) % playlistB.size
        }

        loadDeckB(selectedSong, forcePlay)
    }

    fun setPlaylist(songs: List<Song>, selectedSong: Song, forcePlay: Boolean = false) {
        if (targetDeck.equals("B", ignoreCase = true)) {
            setPlaylistForDeckB(songs, selectedSong, forcePlay)
        } else {
            setPlaylistForDeckA(songs, selectedSong, forcePlay)
        }
    }

    fun loadDeckA(song: Song, forcePlay: Boolean = false) {
        endHandledA = false
        val wasPlaying = forcePlay || playerA.isPlaying()
        val keep = deckUiState

        positionJobA?.cancel()
        positionJobA = null
        waveformJobA?.cancel()
        waveformJobA = null

        waveformSamplesA = FloatArray(0)
        waveformProgressA = 0f
        waveformLoadingA = false
        deckASong = song
        hotCuesA = List(6) { null }
        selectedHotCueA = 0
        djFunctions.resetDeck(true)
        refreshLoopUiState(true)

        if (playlistA.none { it.id == song.id }) {
            playlistA = listOf(song)
            currentSongIndexA = 0
        } else {
            currentSongIndexA = playlistA.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }

        deckUiState = DeckUiState(
            tempo = 1.0f, pitch = 0.0f, isKeylockEnabled = false,
            isPlaying = wasPlaying, currentPosition = 0L, duration = 0L,
            eqLowDb = keep.eqLowDb, eqMidDb = keep.eqMidDb, eqHighDb = keep.eqHighDb,
            audioMode = keep.audioMode
        )

        playerA.setTempoAndPitch(1.0f, 0.0f)
        playerA.setKeylock(false)
        playerA.setAudioMode(keep.audioMode)
        playerA.setEq(keep.eqLowDb, keep.eqMidDb, keep.eqHighDb)

        if (wasPlaying) playerA.play(song.uri) else playerA.prepare(song.uri)

        loadWaveformA(song.uri)
        startPlayerMonitorA()
    }

    fun loadDeckB(song: Song, forcePlay: Boolean = false) {
        endHandledB = false
        val wasPlaying = forcePlay || playerB.isPlaying()
        val keep = deckBUiState

        positionJobB?.cancel()
        positionJobB = null
        waveformJobB?.cancel()
        waveformJobB = null

        waveformSamplesB = FloatArray(0)
        waveformProgressB = 0f
        waveformLoadingB = false
        deckBSong = song
        hotCuesB = List(6) { null }
        selectedHotCueB = 0
        djFunctions.resetDeck(false)
        refreshLoopUiState(false)

        if (playlistB.none { it.id == song.id }) {
            playlistB = listOf(song)
            currentSongIndexB = 0
        } else {
            currentSongIndexB = playlistB.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }

        deckBUiState = DeckUiState(
            tempo = 1.0f, pitch = 0.0f, isKeylockEnabled = false,
            isPlaying = wasPlaying, currentPosition = 0L, duration = 0L,
            eqLowDb = keep.eqLowDb, eqMidDb = keep.eqMidDb, eqHighDb = keep.eqHighDb,
            audioMode = keep.audioMode
        )

        playerB.setTempoAndPitch(1.0f, 0.0f)
        playerB.setKeylock(false)
        playerB.setAudioMode(keep.audioMode)
        playerB.setEq(keep.eqLowDb, keep.eqMidDb, keep.eqHighDb)

        if (wasPlaying) {
            playerB.play(song.uri)
        } else {
            playerB.prepare(song.uri)
        }

        loadWaveformB(song.uri)
        startPlayerMonitorB()
    }

    private fun loadWaveformA(uriString: String) {
        waveformJobA = viewModelScope.launch {
            waveformLoadingA = true
            try {
                waveformRepository.getWaveform(uriString).collect { result ->
                    waveformSamplesA = result.samples
                    waveformProgressA = result.progress
                    if (result.completed) waveformLoadingA = false
                }
            } catch (t: Throwable) {
                waveformLoadingA = false
            }
        }
    }

    private fun loadWaveformB(uriString: String) {
        waveformJobB = viewModelScope.launch {
            waveformLoadingB = true
            try {
                waveformRepository.getWaveform(uriString).collect { result ->
                    waveformSamplesB = result.samples
                    waveformProgressB = result.progress
                    if (result.completed) waveformLoadingB = false
                }
            } catch (t: Throwable) {
                waveformLoadingB = false
            }
        }
    }

    private fun startPlayerMonitorA() {
        positionJobA?.cancel()
        positionJobA = viewModelScope.launch {
            while (isActive) {
                val realIsPlaying = playerA.isPlaying()
                val duration = playerA.getDuration()
                val bpm = playerA.getDetectedBpm()
                val effectiveIsPlaying =
                    if (isPlayStateTransitioningA) deckUiState.isPlaying else realIsPlaying
                val currentPos = playerA.getCurrentPosition()
                if (duration > 0L && currentPos < duration - 500L) endHandledA = false
                djFunctions.updatePlayback(isDeckA = true)

                if (!isSeekingA) {
                    deckUiState = deckUiState.copy(
                        currentPosition = currentPos,
                        duration = duration,
                        isPlaying = effectiveIsPlaying,
                        bpm = bpm
                    )
                } else {
                    deckUiState = deckUiState.copy(
                        duration = duration,
                        isPlaying = effectiveIsPlaying,
                        bpm = bpm
                    )
                }

                // PROTECCIÓN: No disparar automix ni fin de pista si se hizo scratch recientemente (< 1.5s)
                val isRecentlyScratchingA = (System.currentTimeMillis() - lastScratchStopTimeA) < 1500L

                // AUTOMIX INTELIGENTE (Deck A -> Deck B)
                val remainingMs = duration - currentPos
                val transitionMs = automixTransitionSeconds * 1000L
                if (isAutomixEnabled && duration > 0L && effectiveIsPlaying && remainingMs <= transitionMs && !isAutomixTransitioning && crossfader <= 0f && !isRecentlyScratchingA) {
                    triggerAutomixTransition(fromDeckA = true)
                }

                if (isAutoCrossfadeEnabled && duration > 0L && effectiveIsPlaying) {
                    val progress = currentPos.toFloat() / duration.toFloat()
                    if (progress >= autoCrossfadeTriggerPercent && crossfader < 1.0f) {
                        val nextCross = (crossfader + 0.08f).coerceAtMost(1.0f)
                        updateCrossfader(nextCross)
                        if (nextCross >= 1.0f) {
                            isAutoCrossfadeEnabled = false
                            Log.d("MIXXX_DEBUG", "🎲 Auto-Crossfade completado hacia el Deck B.")
                        }
                    }
                }

                if (playerA.isEnded() && !endHandledA && !isAutomixTransitioning && !isRecentlyScratchingA) {
                    if (djFunctions.isRepeatSongEnabled(true)) {
                        endHandledA = true
                        playerA.seekTo(0L)
                        playerA.resume()
                    } else {
                        endHandledA = true
                        if (!isAutomixEnabled && playlistA.isNotEmpty() && currentSongIndexA in 0 until playlistA.size - 1) {
                            currentSongIndexA++
                            val nextSong = playlistA[currentSongIndexA]
                            loadDeckA(nextSong, forcePlay = true)
                        } else if (!isAutomixEnabled) {
                            deckUiState = deckUiState.copy(isPlaying = false, currentPosition = duration)
                        }
                    }
                }
                delay(50L)
            }
        }
    }

    private fun startPlayerMonitorB() {
        positionJobB?.cancel()
        positionJobB = viewModelScope.launch {
            while (isActive) {
                val realIsPlaying = playerB.isPlaying()
                val duration = playerB.getDuration()
                val bpm = playerB.getDetectedBpm()
                val effectiveIsPlaying =
                    if (isPlayStateTransitioningB) deckBUiState.isPlaying else realIsPlaying
                val currentPos = playerB.getCurrentPosition()
                if (duration > 0L && currentPos < duration - 500L) endHandledB = false
                djFunctions.updatePlayback(isDeckA = false)

                if (!isSeekingB) {
                    deckBUiState = deckBUiState.copy(
                        currentPosition = currentPos,
                        duration = duration,
                        isPlaying = effectiveIsPlaying,
                        bpm = bpm
                    )
                } else {
                    deckBUiState = deckBUiState.copy(
                        duration = duration,
                        isPlaying = effectiveIsPlaying,
                        bpm = bpm
                    )
                }

                // PROTECCIÓN: No disparar automix ni fin de pista si se hizo scratch recientemente (< 1.5s)
                val isRecentlyScratchingB = (System.currentTimeMillis() - lastScratchStopTimeB) < 1500L

                // AUTOMIX INTELIGENTE (Deck B -> Deck A)
                val remainingMs = duration - currentPos
                val transitionMs = automixTransitionSeconds * 1000L
                if (isAutomixEnabled && duration > 0L && effectiveIsPlaying && remainingMs <= transitionMs && !isAutomixTransitioning && crossfader >= 0f && !isRecentlyScratchingB) {
                    triggerAutomixTransition(fromDeckA = false)
                }

                if (isAutoCrossfadeEnabled && duration > 0L && effectiveIsPlaying) {
                    val progress = currentPos.toFloat() / duration.toFloat()
                    if (progress >= autoCrossfadeTriggerPercent && crossfader > -1.0f) {
                        val nextCross = (crossfader - 0.08f).coerceAtLeast(-1.0f)
                        updateCrossfader(nextCross)
                        if (nextCross <= -1.0f) {
                            isAutoCrossfadeEnabled = false
                            Log.d("MIXXX_DEBUG", "🎲 Auto-Crossfade completado hacia el Deck A.")
                        }
                    }
                }

                if (playerB.isEnded() && !endHandledB && !isAutomixTransitioning && !isRecentlyScratchingB) {
                    if (djFunctions.isRepeatSongEnabled(false)) {
                        endHandledB = true
                        playerB.seekTo(0L)
                        playerB.resume()
                    } else {
                        endHandledB = true
                        if (!isAutomixEnabled && playlistB.isNotEmpty() && currentSongIndexB in 0 until playlistB.size - 1) {
                            currentSongIndexB++
                            val nextSong = playlistB[currentSongIndexB]
                            loadDeckB(nextSong, forcePlay = true)
                        } else if (!isAutomixEnabled) {
                            deckBUiState = deckBUiState.copy(isPlaying = false, currentPosition = duration)
                        }
                    }
                }
                delay(50L)
            }
        }
    }

    fun onEvent(event: DeckEvent) {
        handleDeckEvent(event, isDeckA = true)
    }

    fun onDeckBEvent(event: DeckEvent) {
        handleDeckEvent(event, isDeckA = false)
    }

    private fun handleDeckEvent(event: DeckEvent, isDeckA: Boolean) {
        val player = if (isDeckA) playerA else playerB
        val song = if (isDeckA) deckASong else deckBSong
        val state = if (isDeckA) deckUiState else deckBUiState

        when (event) {
            is DeckEvent.TempoChanged -> {
                val tempo = event.value.coerceIn(0.8f, 1.2f)
                updateState(isDeckA, state.copy(tempo = tempo))
                player.setTempoAndPitch(tempo, state.pitch)
            }
            is DeckEvent.PitchChanged -> {
                val pitch = event.value.roundToInt().coerceIn(-3, 3).toFloat()
                updateState(isDeckA, state.copy(pitch = pitch))
                player.setTempoAndPitch(state.tempo, pitch)
            }
            is DeckEvent.EqChanged -> {
                val db = event.db.coerceIn(-24f, 6f)
                val newState = when (event.band) {
                    EqBand.LOW -> state.copy(eqLowDb = db)
                    EqBand.MID -> state.copy(eqMidDb = db)
                    EqBand.HIGH -> state.copy(eqHighDb = db)
                }
                updateState(isDeckA, newState)
                player.setEq(newState.eqLowDb, newState.eqMidDb, newState.eqHighDb)
            }
            is DeckEvent.AudioModeChanged -> {
                updateState(isDeckA, state.copy(audioMode = event.mode))
                player.setAudioMode(event.mode)
            }
            is DeckEvent.VolumeChanged -> {
                val vol = event.volume.coerceIn(0f, 1f)
                if (isDeckA) channelVolumeA = vol else channelVolumeB = vol
                applyVolumes()
            }
            DeckEvent.SeekStarted -> {
                if (isDeckA) {
                    isSeekingA = true; wasPlayingBeforeSeekA = playerA.isPlaying()
                } else {
                    isSeekingB = true; wasPlayingBeforeSeekB = playerB.isPlaying()
                }
            }
            is DeckEvent.SeekChanged -> {
                val pos = event.position.coerceAtLeast(0L)
                updateState(isDeckA, state.copy(currentPosition = pos))
            }
            DeckEvent.SeekFinished -> {
                val pos = if (isDeckA) deckUiState.currentPosition else deckBUiState.currentPosition
                player.seekTo(pos)
                val wasPlaying = if (isDeckA) wasPlayingBeforeSeekA else wasPlayingBeforeSeekB
                if (wasPlaying && !player.isPlaying()) player.resume()
                if (isDeckA) {
                    wasPlayingBeforeSeekA = false; isSeekingA = false
                } else {
                    wasPlayingBeforeSeekB = false; isSeekingB = false
                }
            }
            is DeckEvent.ScratchStarted -> {
                val startPos = if (event.positionMs > 0L) event.positionMs else state.currentPosition
                player.startScratch(startPos)
            }
            is DeckEvent.ScratchUpdated -> {
                player.updateScratch(event.speedFactor)
                updateState(isDeckA, state.copy(currentPosition = player.getCurrentPosition()))
            }
            is DeckEvent.ScratchStopped -> {
                player.stopScratch()
                if (isDeckA) {
                    lastScratchStopTimeA = System.currentTimeMillis()
                } else {
                    lastScratchStopTimeB = System.currentTimeMillis()
                }
                updateState(isDeckA, state.copy(currentPosition = player.getCurrentPosition(), isPlaying = player.isPlaying()))
            }
            DeckEvent.Play -> {
                val currentSong = song ?: return
                viewModelScope.launch {
                    if (isDeckA) isPlayStateTransitioningA = true else isPlayStateTransitioningB = true
                    updateState(isDeckA, state.copy(isPlaying = true))
                    if (player.getCurrentPosition() <= 0L && !player.isPlaying()) {
                        player.play(currentSong.uri)
                    } else {
                        player.resume()
                    }
                    delay(150L)
                    if (isDeckA) isPlayStateTransitioningA = false else isPlayStateTransitioningB = false
                }
            }
            DeckEvent.Pause -> {
                viewModelScope.launch {
                    if (isDeckA) isPlayStateTransitioningA = true else isPlayStateTransitioningB = true
                    updateState(isDeckA, state.copy(isPlaying = false))
                    player.pause()
                    delay(100L)
                    if (isDeckA) isPlayStateTransitioningA = false else isPlayStateTransitioningB = false
                }
            }
            DeckEvent.Stop -> {
                player.stop()
                updateState(isDeckA, state.copy(isPlaying = false, currentPosition = 0L))
            }
            is DeckEvent.KeylockChanged -> {
                updateState(isDeckA, state.copy(isKeylockEnabled = event.enabled))
                player.setKeylock(event.enabled)
            }
            else -> {}
        }
    }

    private fun updateState(isDeckA: Boolean, newState: DeckUiState) {
        if (isDeckA) deckUiState = newState else deckBUiState = newState
    }

    override fun onCleared() {
        positionJobA?.cancel()
        positionJobB?.cancel()
        waveformJobA?.cancel()
        waveformJobB?.cancel()
        playerA.release()
        playerB.release()
        super.onCleared()
    }
}