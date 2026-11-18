/*package com.dlanuza.mixxxdroid.gui.screens.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlanuza.mixxxdroid.data.models.Song
import com.dlanuza.mixxxdroid.gui.components.DualThumbwheelModule
import com.dlanuza.mixxxdroid.gui.components.JogWheel
import com.dlanuza.mixxxdroid.gui.components.Waveform
import com.dlanuza.mixxxdroid.gui.events.DeckEvent
import com.dlanuza.mixxxdroid.player.AudioMode
import com.dlanuza.mixxxdroid.utils.formatTime
import com.dlanuza.mixxxdroid.viewmodel.MainViewModel
import kotlin.math.roundToInt

// =========================================================
// COLORES CONSTANTES
// =========================================================
private val BackgroundColor = Color(0xFF121212)
private val CardColor = Color(0xFF1E1E1E)
private val CardColor2 = Color(0xFF252525)
private val WaveformCardColor = Color(0xFF181818)
private val AccentColor = Color(0xFF00E5FF)
private val ActiveColor = Color(0xFF00FF88)
private val SecondaryText = Color(0xFFAAAAAA)
private val InactiveTrackColor = Color(0xFF444444)
private val ProgressTrackColor = Color(0xFF333333)

@Composable
fun DeckScreen(
    viewModel: MainViewModel
) {
    val state = viewModel.deckUiState
    val song = viewModel.deckASong

    var isSeeking by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val maxDurationFloat = remember(state.duration) {
        maxOf(state.duration, 1L).toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER
        DeckHeader(isPlaying = state.isPlaying)

        // TRACK CARD
        TrackCard(song = song, bpm = state.bpm)

        // WAVEFORM
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WaveformCardColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.waveformSamples.isEmpty()) {
                    Text(
                        text = if (viewModel.waveformLoading) "CARGANDO WAVEFORM..." else "WAVEFORM",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Waveform()
                }
            }
        }

        // PROGRESO WAVEFORM
        if (viewModel.waveformLoading) {
            LinearProgressIndicator(
                progress = { viewModel.waveformProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AccentColor,
                trackColor = ProgressTrackColor
            )
        }

        // JOG WHEEL (VINILO / SCRATCH)
        JogWheel(
            currentPositionMs = state.currentPosition,
            durationMs = state.duration,
            isPlaying = state.isPlaying,
            onScratchStart = { pos ->
                viewModel.onEvent(DeckEvent.ScratchStarted(pos.toLong()))
            },
            onScratchUpdate = { factor ->
                viewModel.onEvent(DeckEvent.ScratchUpdated(factor))
            },
            onScratchStop = {
                viewModel.onEvent(DeckEvent.ScratchStopped)
            }
        )

        // TIME DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.currentPosition),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(state.duration),
                color = SecondaryText,
                fontSize = 14.sp
            )
        }

        // SEEK SLIDER
        Slider(
            value = state.currentPosition.coerceIn(0L, maxOf(state.duration, 1L)).toFloat(),
            onValueChange = { newPosition ->
                if (!isSeeking) {
                    isSeeking = true
                    viewModel.onEvent(DeckEvent.SeekStarted)
                }
                viewModel.onEvent(DeckEvent.SeekChanged(newPosition.toLong()))
            },
            onValueChangeFinished = {
                viewModel.onEvent(DeckEvent.SeekFinished)
                isSeeking = false
            },
            valueRange = 0f..maxDurationFloat,
            enabled = state.duration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = InactiveTrackColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // CONTROLES DE REPRODUCCIÓN
        PlaybackControls(
            isPlaying = state.isPlaying,
            onPrevious = { viewModel.previousTrack() },
            onPlayPause = {
                val event = if (state.isPlaying) DeckEvent.Pause else DeckEvent.Play
                viewModel.onEvent(event)
            },
            onNext = { viewModel.nextTrack() },
            onStop = { viewModel.onEvent(DeckEvent.Stop) }
        )

        // CONTROLES DE MODO DE AUDIO (Usa la función pública definida en AudioModeControls.kt)
        AudioModeControls(
            currentMode = viewModel.audioMode,
            onModeSelected = { mode ->
                viewModel.changeAudioMode(mode)
            }
        )

        // DJ CONTROLS (TEMPO & PITCH)
        DjControls(
            tempo = state.tempo,
            pitch = state.pitch,
            keylockEnabled = state.isKeylockEnabled,
            onTempoChange = { viewModel.onEvent(DeckEvent.TempoChanged(it)) },
            onTempoReset = { viewModel.onEvent(DeckEvent.TempoChanged(1.0f)) },
            onPitchChange = { viewModel.onEvent(DeckEvent.PitchChanged(it)) },
            onPitchReset = { viewModel.onEvent(DeckEvent.PitchChanged(0.0f)) },
            onKeylockChange = { enabled ->
                viewModel.onEvent(DeckEvent.KeylockChanged(enabled))
            }
        )

        // TRACK INFO CARD
        TrackInfoCard(
            tempo = state.tempo,
            pitch = state.pitch,
            keylockEnabled = state.isKeylockEnabled,
            bpm = state.bpm
        )
    }
}

// =====================================================================
// HEADER
// =====================================================================

@Composable
private fun DeckHeader(
    isPlaying: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "DECK A",
            color = AccentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = if (isPlaying) "PLAYING" else "STOPPED",
            color = if (isPlaying) ActiveColor else SecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// =====================================================================
// TRACK CARD
// =====================================================================

@Composable
private fun TrackCard(
    song: Song?,
    bpm: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (song == null) {
                Text(
                    text = "NO HAY CANCION CARGADA",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = song.artist,
                    color = SecondaryText,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BPM",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (bpm > 0f) bpm.roundToInt().toString() else "--",
                        color = AccentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =====================================================================
// PLAYBACK CONTROLS
// =====================================================================

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PREVIOUS
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .height(58.dp)
                    .width(70.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(text = "⏮", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // PLAY / PAUSE
            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .height(58.dp)
                    .width(150.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color(0xFF303030) else ActiveColor,
                    contentColor = if (isPlaying) Color.White else Color.Black
                )
            ) {
                Text(
                    text = if (isPlaying) "❚❚  PAUSE" else "▶  PLAY",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // NEXT
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier
                    .height(58.dp)
                    .width(70.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(text = "⏭", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // STOP
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .height(48.dp)
                .width(110.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(
                text = "■  STOP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =====================================================================
// DJ CONTROLS
// =====================================================================

@Composable
private fun DjControls(
    tempo: Float,
    pitch: Float,
    keylockEnabled: Boolean,
    onTempoChange: (Float) -> Unit,
    onTempoReset: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchReset: () -> Unit,
    onKeylockChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "DJ CONTROLS",
                color = AccentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            DualThumbwheelModule(
                tempo = tempo,
                pitch = pitch,
                onTempoChange = onTempoChange,
                onPitchChange = onPitchChange,
                onTempoReset = onTempoReset,
                onPitchReset = onPitchReset
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KEYLOCK",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = keylockEnabled,
                    onCheckedChange = onKeylockChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = AccentColor
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (keylockEnabled) "ON" else "OFF",
                    color = if (keylockEnabled) ActiveColor else SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =====================================================================
// TRACK INFO
// =====================================================================

@Composable
private fun TrackInfoCard(
    tempo: Float,
    pitch: Float,
    keylockEnabled: Boolean,
    bpm: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "TRACK INFO",
                color = AccentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoRow(
                label = "Tempo",
                value = "${(tempo * 100).roundToInt()}%"
            )
            InfoRow(
                label = "Keylock",
                value = if (keylockEnabled) "ON" else "OFF"
            )
            InfoRow(
                label = "Pitch Adjust",
                value = "%.1f st".format(pitch)
            )
            InfoRow(
                label = "BPM",
                value = if (bpm > 0f) bpm.roundToInt().toString() else "--"
            )
        }
    }
}

// =====================================================================
// INFO ROW
// =====================================================================

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}*/