package com.dlanuza.mixxxdroid.gui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2

private val CardColor2 = Color(0xFF252525)
private val AccentColor = Color(0xFF00E5FF)
private val VinylBaseColor = Color(0xFF141414)
private val VinylGrooveColor = Color(0xFF2B2B2B)
private val VinylCenterColor = Color(0xFF333333)

@Composable
fun JogWheel(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onScratchStart: (positionMs: Long) -> Unit,
    onScratchUpdate: (speedFactor: Float) -> Unit,
    onScratchStop: () -> Unit
) {
    val updatedPositionMs by rememberUpdatedState(currentPositionMs)
    val updatedOnScratchStart by rememberUpdatedState(onScratchStart)
    val updatedOnScratchUpdate by rememberUpdatedState(onScratchUpdate)
    val updatedOnScratchStop by rememberUpdatedState(onScratchStop)

    var isTouching by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Velocidad nominal del vinilo (33 RPM = ~200 grados por segundo)
    val nominalDegreesPerSecond = 200f

    // Animación continua cuando está en PLAY y NO se está tocando
    LaunchedEffect(isPlaying, isTouching) {
        if (isPlaying && !isTouching) {
            var lastFrameTimeNanos = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos != 0L) {
                        val deltaTimeSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                        rotationAngle = (rotationAngle + nominalDegreesPerSecond * deltaTimeSeconds) % 360f
                    }
                    lastFrameTimeNanos = frameTimeNanos
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            // 1. Captura INMEDIATA al posar el dedo (ACTION_DOWN)
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            isTouching = true
                            updatedOnScratchStart(updatedPositionMs)

                            val center = Offset(size.width / 2f, size.height / 2f)
                            var lastPosition = down.position
                            var lastAngle = atan2(lastPosition.y - center.y, lastPosition.x - center.x)
                            var lastTimeMs = down.uptimeMillis

                            // 2. Control dinámico durante el movimiento (ACTION_MOVE)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    // Se levantó el dedo de la pantalla
                                    break
                                }

                                val currentPosition = change.position
                                val currentTimeMs = change.uptimeMillis
                                val deltaTimeMs = currentTimeMs - lastTimeMs

                                val currentAngle = atan2(currentPosition.y - center.y, currentPosition.x - center.x)
                                var deltaAngleRad = currentAngle - lastAngle

                                // Normalización de cuadrante trigonométrico (-PI a PI)
                                if (deltaAngleRad > PI) deltaAngleRad -= (2 * PI).toFloat()
                                else if (deltaAngleRad < -PI) deltaAngleRad += (2 * PI).toFloat()

                                val deltaDegrees = Math.toDegrees(deltaAngleRad.toDouble()).toFloat()

                                // Actualizar rotación visual del vinilo
                                rotationAngle = (rotationAngle + deltaDegrees) % 360f

                                if (deltaTimeMs > 0) {
                                    // Cálculo de velocidad angular física (grados por segundo)
                                    val angularVelocity = deltaDegrees / (deltaTimeMs / 1000f)

                                    // speedFactor: 1.0f = velocidad normal hacia adelante, -1.0f = reversa normal
                                    val speedFactor = (angularVelocity / nominalDegreesPerSecond).coerceIn(-3.0f, 3.0f)
                                    updatedOnScratchUpdate(speedFactor)

                                    lastAngle = currentAngle
                                    lastTimeMs = currentTimeMs
                                }

                                change.consume()
                            }

                            // 3. Soltar el vinilo (ACTION_UP)
                            isTouching = false
                            updatedOnScratchStop()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = minOf(size.width, size.height) / 2 - 4.dp.toPx()

                    rotate(degrees = rotationAngle, pivot = center) {
                        // Fondo base del vinilo
                        drawCircle(color = VinylBaseColor, radius = maxRadius, center = center)

                        // Surcos del disco
                        var currentRadius = maxRadius * 0.88f
                        while (currentRadius > maxRadius * 0.35f) {
                            drawCircle(
                                color = VinylGrooveColor,
                                radius = currentRadius,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            currentRadius -= 6.dp.toPx()
                        }

                        // Centro del vinilo
                        drawCircle(
                            color = VinylCenterColor,
                            radius = maxRadius * 0.30f,
                            center = center
                        )

                        // Marcador de posición (línea guía)
                        drawLine(
                            color = AccentColor,
                            start = center,
                            end = Offset(center.x, center.y - maxRadius + 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Agujero central
                        drawCircle(
                            color = CardColor2,
                            radius = 6.dp.toPx(),
                            center = center
                        )
                    }
                }
            }

            // Etiqueta flotante indicadora
            Text(
                text = if (isTouching) "🎧 SCRATCHING" else if (isPlaying) "▶ PLAYING" else "📀 TOUCH TO SCRATCH",
                color = if (isTouching) AccentColor else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}