package com.dlanuza.mixxxdroid.gui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun DualThumbwheelModule(
    tempo: Float,
    pitch: Float,
    onTempoChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onTempoReset: () -> Unit,
    onPitchReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF17181C),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DJ CONTROLS",
            color = Color(0xFF8F939B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbwheel(
                label = "TEMPO",
                value = tempo,
                min = 0.8f,
                max = 1.2f,
                step = 0.1f,
                sensitivity = 45f,
                accent = Color(0xFF00D9FF),
                format = { "${(it * 100f).roundToInt()}%" },
                onChange = onTempoChange,
                onReset = onTempoReset
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(205.dp)
                    .background(Color(0xFF303239))
            )

            Thumbwheel(
                label = "PITCH",
                value = pitch,
                min = -3f,
                max = 3f,
                step = 1f,
                sensitivity = 30f,
                accent = Color(0xFF00E58A),
                format = {
                    val n = it.roundToInt()
                    if (n > 0) "+$n" else "$n"
                },
                onChange = onPitchChange,
                onReset = onPitchReset
            )
        }
    }
}

@Composable
private fun Thumbwheel(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    sensitivity: Float,
    accent: Color,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    var current by remember { mutableFloatStateOf(value) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!dragging) current = value
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier.width(135.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF9699A1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = format(current),
            color = accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(178.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF08090B),
                            Color(0xFF15171B),
                            Color(0xFF08090B)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(5.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        min,
                        max,
                        step,
                        sensitivity
                    ) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragDistance = 0f
                            },

                            onVerticalDrag = { _, amount ->
                                dragDistance += amount

                                while (dragDistance >= sensitivity) {
                                    val next =
                                        (current + step).coerceAtMost(max)

                                    current = next
                                    onChange(current)
                                    dragDistance -= sensitivity

                                    if (current >= max) {
                                        dragDistance = 0f
                                        break
                                    }
                                }

                                while (dragDistance <= -sensitivity) {
                                    val next =
                                        (current - step).coerceAtLeast(min)

                                    current = next
                                    onChange(current)
                                    dragDistance += sensitivity

                                    if (current <= min) {
                                        dragDistance = 0f
                                        break
                                    }
                                }
                            },

                            onDragEnd = {
                                dragging = false
                                dragDistance = 0f
                            },

                            onDragCancel = {
                                dragging = false
                                dragDistance = 0f
                            }
                        )
                    }
            ) {
                val centerY = size.height / 2f
                val faceHeight = 45.dp.toPx()

                val index = (
                        (current - min) / step
                        ).roundToInt()

                val maxIndex = (
                        (max - min) / step
                        ).roundToInt()

                /*
                 * CINTA VERTICAL
                 *
                 * -1 = valor anterior
                 *  0 = valor seleccionado
                 * +1 = valor siguiente
                 */
                for (relative in -1..1) {
                    val i = index + relative

                    if (i !in 0..maxIndex) continue

                    val displayedValue =
                        min + i * step

                    val y =
                        centerY + relative * faceHeight

                    val top =
                        y - faceHeight / 2f

                    val isSelected =
                        relative == 0

                    val faceColor =
                        if (isSelected)
                            Color(0xFF292D34)
                        else
                            Color(0xFF181B20)

                    // Cara principal
                    drawRoundRect(
                        color = faceColor,
                        topLeft = Offset(
                            8.dp.toPx(),
                            top
                        ),
                        size = Size(
                            size.width - 16.dp.toPx(),
                            faceHeight
                        ),
                        cornerRadius = CornerRadius(
                            8.dp.toPx()
                        )
                    )

                    // Brillo superior
                    if (isSelected) {
                        drawRoundRect(
                            color = accent.copy(alpha = 0.08f),
                            topLeft = Offset(
                                8.dp.toPx(),
                                top
                            ),
                            size = Size(
                                size.width - 16.dp.toPx(),
                                5.dp.toPx()
                            ),
                            cornerRadius = CornerRadius(
                                8.dp.toPx()
                            )
                        )
                    }

                    // Separador superior
                    drawLine(
                        color = Color.Black.copy(alpha = 0.8f),
                        start = Offset(
                            10.dp.toPx(),
                            top
                        ),
                        end = Offset(
                            size.width - 10.dp.toPx(),
                            top
                        ),
                        strokeWidth = 1.dp.toPx()
                    )

                    val layout = textMeasurer.measure(
                        text = format(displayedValue),
                        style = TextStyle(
                            color =
                            if (isSelected)
                                accent
                            else
                                Color.White.copy(alpha = 0.5f),
                            fontSize =
                            if (isSelected)
                                16.sp
                            else
                                12.sp,
                            fontWeight =
                            if (isSelected)
                                FontWeight.ExtraBold
                            else
                                FontWeight.Medium
                        )
                    )

                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            (size.width - layout.size.width) / 2f,
                            y - layout.size.height / 2f
                        )
                    )
                }

                /*
                 * ZONA OSCURA SUPERIOR
                 */
                drawRect(
                    color = Color.Black.copy(alpha = 0.82f),
                    topLeft = Offset.Zero,
                    size = Size(
                        size.width,
                        27.dp.toPx()
                    )
                )

                /*
                 * ZONA OSCURA INFERIOR
                 */
                drawRect(
                    color = Color.Black.copy(alpha = 0.82f),
                    topLeft = Offset(
                        0f,
                        size.height - 27.dp.toPx()
                    ),
                    size = Size(
                        size.width,
                        27.dp.toPx()
                    )
                )

                /*
                 * SOMBRA CENTRAL
                 */
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(
                        0f,
                        centerY - faceHeight / 2f
                    ),
                    size = Size(
                        size.width,
                        faceHeight
                    )
                )

                /*
                 * MARCADOR CENTRAL
                 */
                drawLine(
                    color = accent.copy(alpha = 0.95f),
                    start = Offset(
                        2.dp.toPx(),
                        centerY
                    ),
                    end = Offset(
                        size.width - 2.dp.toPx(),
                        centerY
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // Indicador izquierdo
                drawLine(
                    color = accent,
                    start = Offset(
                        3.dp.toPx(),
                        centerY
                    ),
                    end = Offset(
                        9.dp.toPx(),
                        centerY - 5.dp.toPx()
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                drawLine(
                    color = accent,
                    start = Offset(
                        3.dp.toPx(),
                        centerY
                    ),
                    end = Offset(
                        9.dp.toPx(),
                        centerY + 5.dp.toPx()
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // Indicador derecho
                drawLine(
                    color = accent,
                    start = Offset(
                        size.width - 3.dp.toPx(),
                        centerY
                    ),
                    end = Offset(
                        size.width - 9.dp.toPx(),
                        centerY - 5.dp.toPx()
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                drawLine(
                    color = accent,
                    start = Offset(
                        size.width - 3.dp.toPx(),
                        centerY
                    ),
                    end = Offset(
                        size.width - 9.dp.toPx(),
                        centerY + 5.dp.toPx()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                current =
                    if (min >= 0f) 1f else 0f

                dragDistance = 0f
                dragging = false
                onReset()
            },
            modifier = Modifier.height(30.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF292C32)
            )
        ) {
            Text(
                text = "RESET",
                color = Color(0xFFD5D7DB),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}