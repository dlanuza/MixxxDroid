package com.dlanuza.mixxxdroid.gui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlanuza.mixxxdroid.player.AudioMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ChipBg = Color(0xFF1F2733)
private val ChipBorder = Color(0xFF2B3644)
private val ChipText = Color(0xFFC9D3DF)
private val DarkText = Color(0xFF080B10)
private val MutedText = Color(0xFF9BA9B9)
private val KnobTrack = Color(0xFF26313F)

// =====================================================================
// CHIP BASE
// =====================================================================

/**
 * Botón compacto.
 *  - selected + outlinedWhenSelected = false -> relleno del color del deck (selector).
 *  - selected + outlinedWhenSelected = true  -> borde y texto del color (interruptor).
 */
@Composable
fun DeckChip(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlinedWhenSelected: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)

    val background = when {
        selected && !outlinedWhenSelected -> accent
        selected -> accent.copy(alpha = 0.18f)
        else -> ChipBg
    }
    val borderColor = if (selected) accent else ChipBorder
    val textColor = when {
        selected && !outlinedWhenSelected -> DarkText
        selected -> accent
        else -> ChipText
    }

    Box(
        modifier = modifier
            .height(26.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// =====================================================================
// TEMPO: 80 / 90 / 100 / 110 / 120
// =====================================================================

private val TEMPO_STEPS = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f)

@Composable
fun TempoSelector(
    tempo: Float,
    accent: Color,
    onTempoChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Comparar con tolerancia: 1.1f nunca es exactamente igual a 1.1000000238f.
    val selectedIndex = TEMPO_STEPS.indexOfFirst { abs(it - tempo) < 0.005f }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TEMPO", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${(tempo * 100).roundToInt()}%", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TEMPO_STEPS.forEachIndexed { index, step ->
                DeckChip(
                    text = "${(step * 100).roundToInt()}",
                    selected = index == selectedIndex,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) { onTempoChange(step) }
            }
        }
    }
}

// =====================================================================
// PITCH: -3 .. +3 semitonos
// =====================================================================

@Composable
fun PitchStepper(
    pitch: Float,
    accent: Color,
    onPitchChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val semitones = pitch.roundToInt()
    val label = if (semitones > 0) "+$semitones" else "$semitones"

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeckChip(
            text = "−",
            selected = false,
            accent = accent,
            modifier = Modifier.width(30.dp),
            enabled = semitones > -3
        ) { onPitchChange((semitones - 1).toFloat()) }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PITCH $label st",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        DeckChip(
            text = "+",
            selected = false,
            accent = accent,
            modifier = Modifier.width(30.dp),
            enabled = semitones < 3
        ) { onPitchChange((semitones + 1).toFloat()) }
    }
}

// =====================================================================
// MODO DE AUDIO: Normal / Vocal
// =====================================================================

@Composable
fun AudioModeSelector(
    mode: AudioMode,
    accent: Color,
    onModeChange: (AudioMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DeckChip(
            text = "NORMAL",
            selected = mode == AudioMode.NORMAL,
            accent = accent,
            modifier = Modifier.weight(1f)
        ) { onModeChange(AudioMode.NORMAL) }

        DeckChip(
            text = "VOCAL",
            selected = mode == AudioMode.VOCALS_ONLY,
            accent = accent,
            modifier = Modifier.weight(1f)
        ) { onModeChange(AudioMode.VOCALS_ONLY) }
    }
}

// =====================================================================
// PERILLA DE EQ (-24 dB .. +6 dB, centro = 0 dB)
// =====================================================================

private const val EQ_MIN_DB = -24f
private const val EQ_MAX_DB = 6f

// Mitad izquierda: -24..0 dB. Mitad derecha: 0..+6 dB. El 0 dB queda a las 12 en punto.
private fun dbToFraction(db: Float): Float =
    if (db <= 0f) 0.5f + 0.5f * (db / -EQ_MIN_DB)
    else 0.5f + 0.5f * (db / EQ_MAX_DB)

private fun fractionToDb(fraction: Float): Float =
    if (fraction <= 0.5f) (2f * fraction - 1f) * -EQ_MIN_DB
    else (2f * fraction - 1f) * EQ_MAX_DB

private fun formatDb(db: Float): String = when {
    db <= EQ_MIN_DB + 0.1f -> "KILL"
    abs(db) < 0.5f -> "0"
    db > 0f -> "+${db.roundToInt()}"
    else -> "${db.roundToInt()}"
}

/**
 * Arrastra en vertical para cambiar el valor. Doble toque: vuelve a 0 dB.
 */
@Composable
fun EqKnob(
    label: String,
    valueDb: Float,
    accent: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    knobSize: Dp = 36.dp
) {
    val currentDb by rememberUpdatedState(valueDb)
    val onChange by rememberUpdatedState(onValueChange)
    val fraction = dbToFraction(valueDb.coerceIn(EQ_MIN_DB, EQ_MAX_DB))

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(knobSize)
                .pointerInput(Unit) {
                    val travelPx = 140.dp.toPx()
                    // Se acumula la posición del dedo aparte del valor mostrado:
                    // si no, el "imán" del 0 dB impediría salir del centro.
                    var dragFraction = 0.5f
                    detectVerticalDragGestures(
                        onDragStart = { dragFraction = dbToFraction(currentDb) },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragFraction = (dragFraction - dragAmount / travelPx).coerceIn(0f, 1f)
                            var db = fractionToDb(dragFraction)
                            if (abs(db) < 0.8f) db = 0f
                            onChange(db)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onChange(0f) })
                }
        ) {
            val strokeWidth = 4.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            // Recorrido completo (270°, de las 7:30 a las 4:30).
            drawArc(
                color = KnobTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Tramo activo: desde las 12 en punto hasta el valor actual.
            drawArc(
                color = accent,
                startAngle = 270f,
                sweepAngle = (fraction - 0.5f) * 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Aguja.
            val angle = Math.toRadians((270f + (fraction - 0.5f) * 270f).toDouble())
            val radius = diameter / 2f
            drawLine(
                color = Color.White,
                start = Offset(
                    center.x + (cos(angle) * radius * 0.30).toFloat(),
                    center.y + (sin(angle) * radius * 0.30).toFloat()
                ),
                end = Offset(
                    center.x + (cos(angle) * radius * 0.80).toFloat(),
                    center.y + (sin(angle) * radius * 0.80).toFloat()
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Text(label, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(formatDb(valueDb), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}