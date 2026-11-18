package com.dlanuza.mixxxdroid.gui.components

import com.dlanuza.mixxxdroid.player.AudioPlayerManager

/** Gestiona CUE, LOOP y REPEAT de forma independiente por deck. */
class DJFunctions(
    private val playerA: AudioPlayerManager,
    private val playerB: AudioPlayerManager
) {
    // =========================================================
    // INICIO: ESTADO DEL DECK
    // =========================================================

    private data class CueState(
        var position: Long = 0L,
        var active: Boolean = false
    )

    private data class LoopState(
        var start: Long? = null,
        var end: Long? = null,
        var active: Boolean = false,
        var beatCount: Int = 0,
        var lastJumpTime: Long = 0L
    )

    private data class RepeatState(
        var enabled: Boolean = false,
        var lastJumpTime: Long = 0L
    )

    private val cueA = CueState()
    private val cueB = CueState()

    private val loopA = LoopState()
    private val loopB = LoopState()

    private val repeatA = RepeatState()
    private val repeatB = RepeatState()

    // =========================================================
    // FIN: ESTADO DEL DECK
    // =========================================================


    // =========================================================
    // INICIO: SELECCIÓN DE REPRODUCTOR Y ESTADOS
    // =========================================================

    private fun player(isDeckA: Boolean): AudioPlayerManager =
        if (isDeckA) playerA else playerB

    private fun cue(isDeckA: Boolean): CueState =
        if (isDeckA) cueA else cueB

    private fun loop(isDeckA: Boolean): LoopState =
        if (isDeckA) loopA else loopB

    private fun repeatState(isDeckA: Boolean): RepeatState =
        if (isDeckA) repeatA else repeatB

    // =========================================================
    // FIN: SELECCIÓN DE REPRODUCTOR Y ESTADOS
    // =========================================================


    // =========================================================
    // INICIO: FUNCIONES CUE
    // =========================================================
    /** Acción de control del deck. */
    fun resetCue(isDeckA: Boolean) {
        cue(isDeckA).apply {
            position = 0L
            active = false
        }
    }

    fun getCuePosition(isDeckA: Boolean): Long =
        cue(isDeckA).position

    fun isCueActive(isDeckA: Boolean): Boolean =
        cue(isDeckA).active
    /** Acción de control del deck. */
    fun toggleCue(isDeckA: Boolean): Boolean {
        val deckPlayer = player(isDeckA)
        val state = cue(isDeckA)

        if (state.active) {
            deckPlayer.pause()
            state.active = false
        } else {
            if (deckPlayer.getDuration() <= 0L) return false
            deckPlayer.seekTo(state.position)
            deckPlayer.resume()
            state.active = true
        }
        return state.active
    }
    /** Acción de control del deck. */
    fun setCuePosition(isDeckA: Boolean, position: Long) {
        val deckPlayer = player(isDeckA)
        val duration = deckPlayer.getDuration()
        cue(isDeckA).position = position.coerceIn(0L, duration.coerceAtLeast(0L))
        cue(isDeckA).active = false
    }
    /** Acción de control del deck. */
    fun stopCue(isDeckA: Boolean) {
        player(isDeckA).pause()
        cue(isDeckA).active = false
    }

    // =========================================================
    // FIN: FUNCIONES CUE
    // =========================================================


    // =========================================================
    // INICIO: LOOP IN / OUT Y AUTO LOOP
    // =========================================================

    /** Guarda la posición actual como inicio del bucle. */
    fun setLoopIn(isDeckA: Boolean): Long? {
        val deckPlayer = player(isDeckA)
        val duration = deckPlayer.getDuration()
        if (duration <= 0L) return null

        val position = deckPlayer.getCurrentPosition().coerceIn(0L, duration)
        loop(isDeckA).apply {
            start = position
            end = null
            active = false
            beatCount = 0
            lastJumpTime = 0L
        }
        return position
    }

    /** Guarda LOOP OUT; solo acepta una posición posterior a LOOP IN. */
    fun setLoopOut(isDeckA: Boolean): Boolean {
        val deckPlayer = player(isDeckA)
        val state = loop(isDeckA)
        val start = state.start ?: return false
        val duration = deckPlayer.getDuration()
        if (duration <= 0L) return false

        val position = deckPlayer.getCurrentPosition().coerceIn(0L, duration)
        if (position <= start) return false

        state.end = position
        state.active = true
        state.beatCount = 0
        state.lastJumpTime = 0L
        return true
    }

    /** Crea un bucle de 4, 8 o 16 beats desde la posición actual. */
    fun setBeatLoop(isDeckA: Boolean, beats: Int, bpm: Float): Boolean {
        if (beats != 4 && beats != 8 && beats != 16) return false
        if (!bpm.isFinite() || bpm <= 0f) return false

        val deckPlayer = player(isDeckA)
        val duration = deckPlayer.getDuration()
        if (duration <= 0L) return false

        val start = deckPlayer.getCurrentPosition().coerceIn(0L, duration)
        val beatDurationMs = 60_000.0 / bpm.toDouble()
        val loopDurationMs = (beatDurationMs * beats).toLong()
        val end = start + loopDurationMs
        if (loopDurationMs <= 0L || end > duration || end <= start) return false

        loop(isDeckA).apply {
            this.start = start
            this.end = end
            active = true
            beatCount = beats
            lastJumpTime = 0L
        }
        return true
    }

    /** Activa o desactiva el último bucle definido. */
    fun toggleLoop(isDeckA: Boolean): Boolean {
        val state = loop(isDeckA)
        if (state.active) {
            state.active = false
            return false
        }
        if (state.start == null || state.end == null) return false
        state.active = true
        state.lastJumpTime = 0L
        return true
    }

    /** Desactiva el bucle conservando IN y OUT. */
    fun exitLoop(isDeckA: Boolean) {
        loop(isDeckA).apply {
            active = false
            beatCount = 0
            lastJumpTime = 0L
        }
    }

    /** Borra por completo IN, OUT y el estado del bucle. */
    fun clearLoop(isDeckA: Boolean) {
        loop(isDeckA).apply {
            start = null
            end = null
            active = false
            beatCount = 0
            lastJumpTime = 0L
        }
    }

    fun isLoopActive(isDeckA: Boolean): Boolean = loop(isDeckA).active
    fun getLoopStart(isDeckA: Boolean): Long? = loop(isDeckA).start
    fun getLoopEnd(isDeckA: Boolean): Long? = loop(isDeckA).end
    fun getLoopBeats(isDeckA: Boolean): Int = loop(isDeckA).beatCount

    // =========================================================
    // FIN: LOOP IN / OUT Y AUTO LOOP
    // =========================================================


    // INICIO: REPETIR CANCIÓN COMPLETA
    // =========================================================
    /** Acción de control del deck. */
    fun setRepeatSong(isDeckA: Boolean, enabled: Boolean) {
        repeatState(isDeckA).apply {
            this.enabled = enabled
            lastJumpTime = 0L
        }
    }

    fun toggleRepeatSong(isDeckA: Boolean): Boolean {
        val state = repeatState(isDeckA)
        state.enabled = !state.enabled
        state.lastJumpTime = 0L
        return state.enabled
    }

    fun isRepeatSongEnabled(isDeckA: Boolean): Boolean =
        repeatState(isDeckA).enabled

    // =========================================================
    // FIN: REPETIR CANCIÓN COMPLETA
    // =========================================================


    // =========================================================
    // INICIO: MONITOR DE LOOP Y REPEAT
    // =========================================================
    /** Acción de control del deck. */
    fun updatePlayback(isDeckA: Boolean) {
        val deckPlayer = player(isDeckA)
        if (!deckPlayer.isPlaying()) return

        val currentPosition = deckPlayer.getCurrentPosition()
        val duration = deckPlayer.getDuration()
        if (duration <= 0L) return

        val now = System.currentTimeMillis()

        // Si hay un LOOP activo, vuelve al inicio al alcanzar LOOP OUT.
        val loopState = loop(isDeckA)
        if (loopState.active) {
            val start = loopState.start
            val end = loopState.end

            if (start != null && end != null &&
                currentPosition >= end &&
                now - loopState.lastJumpTime >= 100L
            ) {
                loopState.lastJumpTime = now
                deckPlayer.seekTo(start)
            }
            return
        }

        // Si REPEAT está activo, vuelve al principio al terminar la canción.
        val repeat = repeatState(isDeckA)
        if (repeat.enabled &&
            currentPosition >= duration - 150L &&
            now - repeat.lastJumpTime >= 500L
        ) {
            repeat.lastJumpTime = now
            deckPlayer.seekTo(0L)
            deckPlayer.resume()
        }
    }

    // =========================================================
    // FIN: MONITOR DE LOOP Y REPEAT
    // =========================================================


    // =========================================================
    // INICIO: REINICIO AL CARGAR OTRA CANCIÓN
    // =========================================================
    /** Acción de control del deck. */
    fun resetDeck(isDeckA: Boolean) {
        resetCue(isDeckA)

        loop(isDeckA).apply {
            start = null
            end = null
            active = false
            beatCount = 0
            lastJumpTime = 0L
        }
    }

    // =========================================================
    // FIN: REINICIO AL CARGAR OTRA CANCIÓN
    // =========================================================
}