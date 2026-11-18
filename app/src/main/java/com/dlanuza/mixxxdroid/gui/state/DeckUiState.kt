// com/dlanuza/mixxxdroid/gui/state/DeckUiState.kt
package com.dlanuza.mixxxdroid.gui.state

import com.dlanuza.mixxxdroid.player.AudioMode

data class DeckUiState(
    val tempo: Float = 1.0f,
    val pitch: Float = 0.0f,
    val isKeylockEnabled: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bpm: Float = 0f,

    // Ajustes del deck (se conservan al cargar otra canción)
    val eqLowDb: Float = 0f,
    val eqMidDb: Float = 0f,
    val eqHighDb: Float = 0f,
    val audioMode: AudioMode = AudioMode.NORMAL
)