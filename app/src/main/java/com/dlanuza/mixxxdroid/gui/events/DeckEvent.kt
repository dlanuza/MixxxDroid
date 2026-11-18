package com.dlanuza.mixxxdroid.gui.events

import com.dlanuza.mixxxdroid.player.AudioMode
import com.dlanuza.mixxxdroid.player.EqBand

sealed interface DeckEvent {

    // =========================================================
    // TEMPO
    // =========================================================

    data class TempoChanged(val value: Float) : DeckEvent
    data object TempoFinished : DeckEvent

    // =========================================================
    // PITCH
    // =========================================================

    data class PitchChanged(val value: Float) : DeckEvent
    data object PitchFinished : DeckEvent

    // =========================================================
    // SEEK
    // =========================================================

    data class SeekChanged(val position: Long) : DeckEvent
    data object SeekStarted : DeckEvent
    data object SeekFinished : DeckEvent

    // =========================================================
    // SCRATCH
    // =========================================================

    data class ScratchStarted(val positionMs: Long) : DeckEvent
    data class ScratchUpdated(val speedFactor: Float) : DeckEvent
    data object ScratchStopped : DeckEvent

    // =========================================================
    // PLAYBACK
    // =========================================================

    data object Play : DeckEvent
    data object Pause : DeckEvent
    data object Stop : DeckEvent

    // =========================================================
    // KEYLOCK
    // =========================================================

    data class KeylockChanged(val enabled: Boolean) : DeckEvent

    // =========================================================
    // VOLUME
    // =========================================================

    data class VolumeChanged(val volume: Float) : DeckEvent

    // =========================================================
    // ECUALIZADOR Y MODO DE AUDIO
    // =========================================================

    data class EqChanged(val band: EqBand, val db: Float) : DeckEvent
    data class AudioModeChanged(val mode: AudioMode) : DeckEvent
}