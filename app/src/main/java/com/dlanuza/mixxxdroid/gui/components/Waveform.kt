package com.dlanuza.mixxxdroid.gui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Dibuja la forma de onda y permite colocar/activar Hot Cues tocando su posición. */
@Composable
fun Waveform(
    samples: FloatArray,
    modifier: Modifier = Modifier.fillMaxSize(),
    progress: () -> Float = { 0f },
    playedColor: Color = Color(0xFF00E5A0),
    unplayedColor: Color = Color(0xFF4A5568),
    playheadColor: Color = Color.White,
    cuePositions: List<Long?> = emptyList(),
    durationMs: Long = 0L,
    cueColors: List<Color> = listOf(Color(0xFFFF4545), Color(0xFF00D66B), Color(0xFF2495FF), Color(0xFFFFD740), Color(0xFFB56BFF), Color(0xFFFF8A32)),
    onPositionSelected: ((Long) -> Unit)? = null,
    onCueMarkerClick: ((Int) -> Unit)? = null
) {
    val maxPeak = remember(samples) { (samples.maxOrNull() ?: 1f).coerceAtLeast(0.05f) }
    Canvas(
        modifier = modifier.pointerInput(durationMs, onPositionSelected, onCueMarkerClick) {
            detectTapGestures { tap ->
                if (durationMs <= 0L || size.width <= 0f) return@detectTapGestures
                val ratio = (tap.x / size.width).coerceIn(0f, 1f)
                val markerIndex = cuePositions.indices.firstOrNull { index ->
                    val pos = cuePositions[index] ?: return@firstOrNull false
                    val markerX = pos.toFloat() / durationMs * size.width
                    kotlin.math.abs(markerX - tap.x) <= 18.dp.toPx()
                }
                if (markerIndex != null) onCueMarkerClick?.invoke(markerIndex)
                else onPositionSelected?.invoke((ratio * durationMs).toLong())
            }
        }
    ) {
        if (samples.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val barCount = minOf(samples.size, (width / 2.dp.toPx()).toInt().coerceAtLeast(1))
        val step = width / barCount
        val samplesPerBar = samples.size.toFloat() / barCount
        val strokeWidth = (step * 0.7f).coerceAtLeast(1f)
        val playedX = progress().coerceIn(0f, 1f) * width

        for (i in 0 until barCount) {
            val from = (i * samplesPerBar).toInt()
            val to = ((i + 1) * samplesPerBar).toInt().coerceIn(from + 1, samples.size)
            var peak = 0f
            for (j in from until to) if (samples[j] > peak) peak = samples[j]
            val half = ((peak / maxPeak).coerceIn(0f, 1f) * height / 2f).coerceAtLeast(1f)
            val x = i * step + step / 2f
            drawLine(if (x <= playedX) playedColor else unplayedColor, Offset(x, midY - half), Offset(x, midY + half), strokeWidth)
        }

        if (durationMs > 0L) {
            cuePositions.forEachIndexed { index, position ->
                if (position != null) {
                    val x = (position.toFloat() / durationMs * width).coerceIn(0f, width)
                    drawLine(cueColors.getOrElse(index) { Color.White }, Offset(x, 0f), Offset(x, height), 3.dp.toPx())
                }
            }
        }
        drawLine(playheadColor, Offset(playedX, 0f), Offset(playedX, height), 2.dp.toPx())
    }
}
