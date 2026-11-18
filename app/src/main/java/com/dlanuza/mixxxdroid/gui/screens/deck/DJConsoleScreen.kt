package com.dlanuza.mixxxdroid.gui.screens.deck

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlanuza.mixxxdroid.data.models.Song
import com.dlanuza.mixxxdroid.gui.components.DeckChip
import com.dlanuza.mixxxdroid.gui.components.EqKnob
import com.dlanuza.mixxxdroid.gui.components.JogWheel
import com.dlanuza.mixxxdroid.gui.components.PitchStepper
import com.dlanuza.mixxxdroid.gui.components.TempoSelector
import com.dlanuza.mixxxdroid.gui.components.Waveform
import com.dlanuza.mixxxdroid.gui.events.DeckEvent
import com.dlanuza.mixxxdroid.gui.state.DeckUiState
import com.dlanuza.mixxxdroid.player.AudioMode
import com.dlanuza.mixxxdroid.player.EqBand
import com.dlanuza.mixxxdroid.utils.formatTime
import com.dlanuza.mixxxdroid.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

// =========================================================
// COLORES CONSTANTES (DISEÑO PRO HARDWARE)
// =========================================================
private val Bg = Color(0xFF05070A)
private val Panel = Color(0xFF0E131A)
private val ToolsBg = Color(0xFF090D14)
private val Line = Color(0xFF1E2736)
private val PadBg = Color(0xFF161F2C)
private val PadBorder = Color(0xFF263346)
private val Green = Color(0xFF00E5A0)
private val Blue = Color(0xFF2495FF)
private val MainText = Color(0xFFF8FAFC)
private val Muted = Color(0xFF8B9BB4)
private val WaveformCardColor = Color(0xFF0B0F15)
private val ProgressTrackColor = Color(0xFF1B2433)
private val LoopInColor = Color(0xFFFFD740)
private val LoopOutColor = Color(0xFFFF5252)

/** Pestañas del área de herramientas de cada deck. */
private enum class DeckTab(val label: String) {
    TEMPO("TEMPO"),
    CUE("CUE"),
    LOOP("LOOP"),
    AUDIO("AUDIO")
}

@Composable
fun DJConsoleScreen(
    viewModel: MainViewModel,
    onOpenLibraryForDeck: (String) -> Unit,
    onHomeClick: () -> Unit, // <-- Añadido parámetro para volver al Home
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            if (originalOrientation != null) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    val stateA = viewModel.deckUiState
    val songA = viewModel.deckASong

    val stateB = viewModel.deckBUiState
    val songB = viewModel.deckBSong

    var isSeekingA by remember { mutableStateOf(false) }
    var isSeekingB by remember { mutableStateOf(false) }

    val maxDurationFloatA = remember(stateA.duration) { maxOf(stateA.duration, 1L).toFloat() }
    val maxDurationFloatB = remember(stateB.duration) { maxOf(stateB.duration, 1L).toFloat() }

    // Volúmenes locales por canal
    var volumeA by remember { mutableFloatStateOf(0.8f) }
    var volumeB by remember { mutableFloatStateOf(0.8f) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Bg
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ==========================================
            // DECK A
            // ==========================================
            Box(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                DeckChannelPanel(
                    name = "A",
                    viewModel = viewModel,
                    isDeckA = true,
                    accent = Green,
                    song = songA,
                    state = stateA,
                    isSeeking = isSeekingA,
                    maxDurationFloat = maxDurationFloatA,
                    waveformSamples = viewModel.waveformSamplesA,
                    waveformLoading = viewModel.waveformLoadingA,
                    waveformProgress = viewModel.waveformProgressA,
                    onSeekStarted = {
                        isSeekingA = true
                        viewModel.onEvent(DeckEvent.SeekStarted)
                    },
                    onSeekChanged = { pos -> viewModel.onEvent(DeckEvent.SeekChanged(pos)) },
                    onSeekFinished = {
                        viewModel.onEvent(DeckEvent.SeekFinished)
                        isSeekingA = false
                    },
                    onPlayPause = {
                        val event = if (stateA.isPlaying) DeckEvent.Pause else DeckEvent.Play
                        viewModel.onEvent(event)
                    },
                    onStop = { viewModel.onEvent(DeckEvent.Stop) },
                    onTempoChange = { viewModel.onEvent(DeckEvent.TempoChanged(it)) },
                    onPitchChange = { viewModel.onEvent(DeckEvent.PitchChanged(it)) },
                    onKeylockChange = { viewModel.onEvent(DeckEvent.KeylockChanged(it)) },
                    onAudioModeChange = { viewModel.onEvent(DeckEvent.AudioModeChanged(it)) },
                    onScratchStart = { pos -> viewModel.onEvent(DeckEvent.ScratchStarted(pos)) },
                    onScratchUpdate = { factor -> viewModel.onEvent(DeckEvent.ScratchUpdated(factor)) },
                    onScratchStop = { viewModel.onEvent(DeckEvent.ScratchStopped) },
                    onOpenLibrary = { onOpenLibraryForDeck("A") }
                )
            }

            // ==========================================
            // MEZCLADOR CENTRAL (Con botón Home integrado)
            // ==========================================
            Box(
                Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
            ) {
                MixerPanel(
                    cross = viewModel.crossfader,
                    master = viewModel.masterVolume,
                    isAutomix = viewModel.isAutomixEnabled,
                    volA = volumeA,
                    volB = volumeB,
                    stateA = stateA,
                    stateB = stateB,
                    onCross = { viewModel.updateCrossfader(it) },
                    onVolA = {
                        volumeA = it
                        viewModel.onEvent(DeckEvent.VolumeChanged(it))
                    },
                    onVolB = {
                        volumeB = it
                        viewModel.onDeckBEvent(DeckEvent.VolumeChanged(it))
                    },
                    onEqA = { band, db -> viewModel.onEvent(DeckEvent.EqChanged(band, db)) },
                    onEqB = { band, db -> viewModel.onDeckBEvent(DeckEvent.EqChanged(band, db)) },
                    onMasterVolume = { viewModel.updateMasterVolume(it) },
                    onToggleAutomix = { viewModel.toggleAutomix(!viewModel.isAutomixEnabled) },
                    onHomeClick = onHomeClick // <-- Pasado al panel central
                )
            }

            // ==========================================
            // DECK B
            // ==========================================
            Box(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                DeckChannelPanel(
                    name = "B",
                    viewModel = viewModel,
                    isDeckA = false,
                    accent = Blue,
                    song = songB,
                    state = stateB,
                    isSeeking = isSeekingB,
                    maxDurationFloat = maxDurationFloatB,
                    waveformSamples = viewModel.waveformSamplesB,
                    waveformLoading = viewModel.waveformLoadingB,
                    waveformProgress = viewModel.waveformProgressB,
                    onSeekStarted = {
                        isSeekingB = true
                        viewModel.onDeckBEvent(DeckEvent.SeekStarted)
                    },
                    onSeekChanged = { pos -> viewModel.onDeckBEvent(DeckEvent.SeekChanged(pos)) },
                    onSeekFinished = {
                        viewModel.onDeckBEvent(DeckEvent.SeekFinished)
                        isSeekingB = false
                    },
                    onPlayPause = {
                        val event = if (stateB.isPlaying) DeckEvent.Pause else DeckEvent.Play
                        viewModel.onDeckBEvent(event)
                    },
                    onStop = { viewModel.onDeckBEvent(DeckEvent.Stop) },
                    onTempoChange = { viewModel.onDeckBEvent(DeckEvent.TempoChanged(it)) },
                    onPitchChange = { viewModel.onDeckBEvent(DeckEvent.PitchChanged(it)) },
                    onKeylockChange = { viewModel.onDeckBEvent(DeckEvent.KeylockChanged(it)) },
                    onAudioModeChange = { viewModel.onDeckBEvent(DeckEvent.AudioModeChanged(it)) },
                    onScratchStart = { pos -> viewModel.onDeckBEvent(DeckEvent.ScratchStarted(pos)) },
                    onScratchUpdate = { factor -> viewModel.onDeckBEvent(DeckEvent.ScratchUpdated(factor)) },
                    onScratchStop = { viewModel.onDeckBEvent(DeckEvent.ScratchStopped) },
                    onOpenLibrary = { onOpenLibraryForDeck("B") }
                )
            }
        }
    }
}

// =====================================================================
// DECK CHANNEL PANEL
// =====================================================================

@Composable
private fun DeckChannelPanel(
    name: String,
    viewModel: MainViewModel,
    isDeckA: Boolean,
    accent: Color,
    song: Song?,
    state: DeckUiState,
    isSeeking: Boolean,
    maxDurationFloat: Float,
    waveformSamples: FloatArray,
    waveformLoading: Boolean,
    waveformProgress: Float,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onTempoChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onKeylockChange: (Boolean) -> Unit,
    onAudioModeChange: (AudioMode) -> Unit,
    onScratchStart: (Long) -> Unit,
    onScratchUpdate: (Float) -> Unit,
    onScratchStop: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    var tab by rememberSaveable(name) { mutableStateOf(DeckTab.TEMPO) }

    val hotCues = if (isDeckA) viewModel.hotCuesA else viewModel.hotCuesB
    val selectedHotCue = if (isDeckA) viewModel.selectedHotCueA else viewModel.selectedHotCueB
    val repeatEnabled = if (isDeckA) viewModel.repeatSongA else viewModel.repeatSongB
    val loopActive = if (isDeckA) viewModel.loopActiveA else viewModel.loopActiveB
    val selectedLoopBeats = if (isDeckA) viewModel.loopBeatsA else viewModel.loopBeatsB

    val loopInPosition = if (isDeckA) viewModel.loopStartA else viewModel.loopStartB
    val loopOutPosition = if (isDeckA) viewModel.loopEndA else viewModel.loopEndB

    val activeTabs = buildSet {
        if (abs(state.tempo - 1f) > 0.001f || state.pitch != 0f || state.isKeylockEnabled) add(DeckTab.TEMPO)
        if (hotCues.any { it != null }) add(DeckTab.CUE)
        if (loopActive) add(DeckTab.LOOP)
        if (repeatEnabled || state.audioMode != AudioMode.NORMAL) add(DeckTab.AUDIO)
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CABECERA DE SELECCIÓN DE PISTA CON 🎵 +
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ToolsBg)
                    .border(
                        BorderStroke(1.dp, if (song == null) accent.copy(alpha = 0.6f) else Line),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onOpenLibrary() }
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(accent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = Bg, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎵 + ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = song?.title ?: "AGREGAR / CARGAR (DECK $name)",
                            color = if (song != null) MainText else accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = song?.artist ?: "Toca para buscar canciones en el dispositivo",
                        color = Muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (state.bpm > 0f) "${state.bpm.roundToInt()}" else "--",
                        color = MainText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("BPM", color = Muted, fontSize = 8.sp)
                }
            }

            // WAVEFORM VISUALIZER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = WaveformCardColor),
                border = BorderStroke(1.dp, Line)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (waveformSamples.isEmpty()) {
                        Text(
                            text = if (waveformLoading) "ANALIZANDO AUDIO..." else "WAVEFORM DISPONIBLE",
                            color = Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Waveform(
                            samples = waveformSamples,
                            progress = {
                                if (state.duration > 0L) state.currentPosition.toFloat() / state.duration else 0f
                            },
                            playedColor = accent,
                            cuePositions = listOf(loopInPosition, loopOutPosition),
                            durationMs = state.duration,
                            cueColors = listOf(LoopInColor, LoopOutColor)
                        )
                    }
                }
            }

            if (waveformLoading) {
                LinearProgressIndicator(
                    progress = { waveformProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent,
                    trackColor = ProgressTrackColor
                )
            }

            // TIEMPOS Y SEEKBAR
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = formatTime(state.currentPosition), color = MainText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = formatTime(state.duration), color = Muted, fontSize = 9.sp)
                }

                Slider(
                    value = state.currentPosition.coerceIn(0L, maxOf(state.duration, 1L)).toFloat(),
                    onValueChange = { newPos ->
                        if (!isSeeking) onSeekStarted()
                        onSeekChanged(newPos.toLong())
                    },
                    onValueChangeFinished = { onSeekFinished() },
                    valueRange = 0f..maxDurationFloat,
                    enabled = state.duration > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Line
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                )
            }

            // HERRAMIENTAS Y JOGWHEEL
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PANEL DE PESTAÑAS (TEMPO, CUE, LOOP, AUDIO)
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .background(ToolsBg, RoundedCornerShape(10.dp))
                        .border(1.dp, Line, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    DeckTabBar(
                        selected = tab,
                        activeTabs = activeTabs,
                        accent = accent,
                        onSelect = { tab = it }
                    )

                    HorizontalDivider(color = Line)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = tab,
                            animationSpec = tween(120),
                            label = "deckTools$name"
                        ) { current ->
                            when (current) {
                                DeckTab.TEMPO -> TempoTab(
                                    state = state,
                                    accent = accent,
                                    onTempoChange = onTempoChange,
                                    onPitchChange = onPitchChange,
                                    onKeylockChange = onKeylockChange,
                                    onReset = {
                                        onTempoChange(1f)
                                        onPitchChange(0f)
                                    }
                                )

                                DeckTab.CUE -> CueTab(
                                    accent = accent,
                                    hotCues = hotCues,
                                    selectedHotCue = selectedHotCue,
                                    onPad = { index, cuePosition ->
                                        viewModel.selectHotCue(isDeckA, index)
                                        if (cuePosition == null) {
                                            viewModel.setHotCuePosition(isDeckA, state.currentPosition)
                                        } else {
                                            viewModel.triggerHotCue(isDeckA, index)
                                        }
                                    },
                                    onRandom = { viewModel.randomizeHotCues(isDeckA) },
                                    onClear = { viewModel.clearHotCues(isDeckA) }
                                )

                                DeckTab.LOOP -> LoopTab(
                                    accent = accent,
                                    loopIn = loopInPosition,
                                    loopOut = loopOutPosition,
                                    loopActive = loopActive,
                                    selectedBeats = selectedLoopBeats,
                                    onLoopIn = { viewModel.setLoopIn(isDeckA) },
                                    onLoopOut = { viewModel.setLoopOut(isDeckA) },
                                    onBeatLoop = { beats -> viewModel.setBeatLoop(isDeckA, beats) },
                                    onToggle = { viewModel.toggleLoop(isDeckA) },
                                    onReset = { viewModel.clearLoop(isDeckA) }
                                )

                                DeckTab.AUDIO -> AudioTab(
                                    accent = accent,
                                    state = state,
                                    repeatEnabled = repeatEnabled,
                                    onAudioModeChange = onAudioModeChange,
                                    onToggleRepeat = { viewModel.toggleRepeatSong(isDeckA) }
                                )
                            }
                        }
                    }
                }

                // JOGWHEEL / SCRATCH
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    JogWheel(
                        currentPositionMs = state.currentPosition,
                        durationMs = state.duration,
                        isPlaying = state.isPlaying,
                        onScratchStart = { _ -> onScratchStart(state.currentPosition) },
                        onScratchUpdate = { factor -> onScratchUpdate(factor) },
                        onScratchStop = { onScratchStop() }
                    )
                }
            }

            // BOTONES DE TRANSPORTE (PLAY / PAUSE / STOP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isPlaying) Color(0xFF1E2736) else accent,
                        contentColor = if (state.isPlaying) accent else Bg
                    )
                ) {
                    Text(
                        text = if (state.isPlaying) "⏸ PAUSE" else "▶ PLAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Text("⏹ STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =====================================================================
// ÁREA DE HERRAMIENTAS: BARRA DE PESTAÑAS
// =====================================================================

@Composable
private fun DeckTabBar(
    selected: DeckTab,
    activeTabs: Set<DeckTab>,
    accent: Color,
    onSelect: (DeckTab) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        DeckTab.entries.forEach { tab ->
            val isSelected = tab == selected

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) accent else Muted,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    if (tab in activeTabs) {
                        Spacer(Modifier.width(2.dp))
                        Box(
                            Modifier
                                .size(4.dp)
                                .background(accent, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .background(
                            if (isSelected) accent else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

// =====================================================================
// CONTENIDO DE CADA PESTAÑA
// =====================================================================

@Composable
private fun TempoTab(
    state: DeckUiState,
    accent: Color,
    onTempoChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onKeylockChange: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    val canReset = abs(state.tempo - 1f) > 0.001f || state.pitch != 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TempoSelector(tempo = state.tempo, accent = accent, onTempoChange = onTempoChange)
        PitchStepper(pitch = state.pitch, accent = accent, onPitchChange = onPitchChange)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DeckChip(
                text = "KEYLOCK",
                selected = state.isKeylockEnabled,
                accent = accent,
                outlinedWhenSelected = true,
                modifier = Modifier.weight(1.4f)
            ) { onKeylockChange(!state.isKeylockEnabled) }

            DeckChip(
                text = "RESET",
                selected = false,
                accent = accent,
                enabled = canReset,
                modifier = Modifier.weight(1f)
            ) { onReset() }
        }
    }
}

@Composable
private fun CueTab(
    accent: Color,
    hotCues: List<Long?>,
    selectedHotCue: Int?,
    onPad: (index: Int, cuePosition: Long?) -> Unit,
    onRandom: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0..1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (column in 0..2) {
                    val index = row * 3 + column
                    val cuePosition = hotCues.getOrNull(index)
                    CuePad(
                        index = index,
                        positionMs = cuePosition,
                        selected = selectedHotCue == index,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { onPad(index, cuePosition) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DeckChip(
                text = "RANDOM",
                selected = false,
                accent = accent,
                modifier = Modifier.weight(1.4f)
            ) { onRandom() }

            DeckChip(
                text = "CLEAR",
                selected = false,
                accent = accent,
                enabled = hotCues.any { it != null },
                modifier = Modifier.weight(1f)
            ) { onClear() }
        }
    }
}

@Composable
private fun CuePad(
    index: Int,
    positionMs: Long?,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val isSet = positionMs != null

    Column(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(if (isSet) accent.copy(alpha = 0.2f) else PadBg)
            .border(1.dp, if (selected || isSet) accent else PadBorder, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${index + 1}",
            color = if (isSet) accent else MainText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = if (positionMs == null) "SET" else formatTime(positionMs),
            color = Muted,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun LoopTab(
    accent: Color,
    loopIn: Long?,
    loopOut: Long?,
    loopActive: Boolean,
    selectedBeats: Int?,
    onLoopIn: () -> Unit,
    onLoopOut: () -> Unit,
    onBeatLoop: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DeckChip(
                text = if (loopIn != null) "IN: ${formatTime(loopIn)}" else "LOOP IN",
                selected = loopIn != null,
                accent = LoopInColor,
                outlinedWhenSelected = true,
                modifier = Modifier.weight(1f)
            ) { onLoopIn() }

            DeckChip(
                text = if (loopOut != null) "OUT: ${formatTime(loopOut)}" else "LOOP OUT",
                selected = loopOut != null,
                accent = LoopOutColor,
                outlinedWhenSelected = true,
                modifier = Modifier.weight(1f)
            ) { onLoopOut() }
        }

        Text("BEAT LOOPS", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(4, 8, 16).forEach { beats ->
                DeckChip(
                    text = "${beats}B",
                    selected = selectedBeats == beats,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { onBeatLoop(beats) }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DeckChip(
                text = "ACTIVE",
                selected = loopActive,
                accent = accent,
                outlinedWhenSelected = true,
                modifier = Modifier.weight(1f)
            ) { onToggle() }

            DeckChip(
                text = "CLEAR",
                selected = false,
                accent = accent,
                enabled = loopIn != null || loopOut != null || loopActive,
                modifier = Modifier.weight(1f)
            ) { onReset() }
        }
    }
}

@Composable
private fun AudioTab(
    accent: Color,
    state: DeckUiState,
    repeatEnabled: Boolean,
    onAudioModeChange: (AudioMode) -> Unit,
    onToggleRepeat: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("MODO DE AUDIO", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)

        AudioModeControls(
            currentMode = state.audioMode,
            onModeSelected = onAudioModeChange
        )

        Text("REPRODUCCIÓN", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)

        DeckChip(
            text = "REPEAT SONG",
            selected = repeatEnabled,
            accent = accent,
            outlinedWhenSelected = true,
            modifier = Modifier.fillMaxWidth()
        ) { onToggleRepeat() }
    }
}

// =====================================================================
// MIXER PANEL CENTRAL
// =====================================================================

@Composable
private fun MixerPanel(
    cross: Float,
    master: Float,
    isAutomix: Boolean,
    volA: Float,
    volB: Float,
    stateA: DeckUiState,
    stateB: DeckUiState,
    onCross: (Float) -> Unit,
    onVolA: (Float) -> Unit,
    onVolB: (Float) -> Unit,
    onEqA: (EqBand, Float) -> Unit,
    onEqB: (EqBand, Float) -> Unit,
    onMasterVolume: (Float) -> Unit,
    onToggleAutomix: () -> Unit,
    onHomeClick: () -> Unit // <-- Añadido parámetro de navegación
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CABECERA CON MIXER, BOTÓN DE CASA Y AUTOMIX
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MIXER", color = MainText, fontSize = 11.sp, fontWeight = FontWeight.Black)

                // Botón de Inicio (Casita) en el centro de la cabecera
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Ir al Inicio",
                        tint = MainText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onToggleAutomix,
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutomix) Green else ToolsBg,
                        contentColor = if (isAutomix) Bg else Muted
                    )
                ) {
                    Text(
                        text = if (isAutomix) "🤖 AUTOMIX ON" else "🤖 AUTOMIX",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Line)

            ChannelEq(label = "CH A", accent = Green, state = stateA, onEq = onEqA)
            ChannelEq(label = "CH B", accent = Blue, state = stateB, onEq = onEqB)

            HorizontalDivider(color = Line)

            // VOLÚMENES DE CANAL
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = volA.coerceIn(0f, 1f),
                        onValueChange = onVolA,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp),
                        colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", color = Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = volB.coerceIn(0f, 1f),
                        onValueChange = onVolB,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp),
                        colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
                    )
                }
            }

            // VOLUMEN MASTER
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("MASTER", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = master,
                    onValueChange = onMasterVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MainText,
                        activeTrackColor = MainText
                    )
                )
            }

            // CROSSFADER
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CROSSFADER", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    ResetLink(enabled = cross != 0f) { onCross(0f) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = cross.coerceIn(-1f, 1f),
                        onValueChange = onCross,
                        valueRange = -1f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MainText,
                            activeTrackColor = Green,
                            inactiveTrackColor = Blue
                        )
                    )
                    Text("B", color = Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChannelEq(
    label: String,
    accent: Color,
    state: DeckUiState,
    onEq: (EqBand, Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            ResetLink(
                enabled = state.eqLowDb != 0f || state.eqMidDb != 0f || state.eqHighDb != 0f
            ) {
                onEq(EqBand.HIGH, 0f)
                onEq(EqBand.MID, 0f)
                onEq(EqBand.LOW, 0f)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EqKnob("HIGH", state.eqHighDb, accent, { onEq(EqBand.HIGH, it) })
            EqKnob("MID", state.eqMidDb, accent, { onEq(EqBand.MID, it) })
            EqKnob("LOW", state.eqLowDb, accent, { onEq(EqBand.LOW, it) })
        }
    }
}

@Composable
private fun ResetLink(enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = "RESET",
        color = if (enabled) MainText else Muted.copy(alpha = 0.4f),
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}