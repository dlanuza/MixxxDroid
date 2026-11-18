package com.dlanuza.mixxxdroid.gui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // Temporizador para simular la carga y pasar al Home (2 segundos)
    LaunchedEffect(Unit) {
        delay(1500)
        onTimeout()
    }

    // Animación de rotación infinita para simular el giro del vinilo central
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)), // Fondo negro carbón pro
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ==========================================
            // ONDA DE AUDIO IZQUIERDA (Estilo Ecualizador)
            // ==========================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heightsLeft = listOf(12, 24, 38, 18, 42, 30, 48, 22, 36, 16, 28)
                heightsLeft.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(h.dp)
                            .background(Color(0xFF0EA5E9), CircleShape) // Azul cian
                    )
                }
            }

            // ==========================================
            // VINILO CENTRAL CON LA "M" (GIRATORIO)
            // ==========================================
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .rotate(angle) // Gira como un disco de verdad
                    .background(Color(0xFF121214), CircleShape)
                    .border(3.dp, Color(0xFF0EA5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Surcos internos del vinilo
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .border(1.dp, Color(0xFF27272A), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .border(1.dp, Color(0xFF27272A), CircleShape)
                )

                // Etiqueta central con la M de MixxxDroid
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF0EA5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }

            // ==========================================
            // ONDA DE AUDIO DERECHA (Estilo Ecualizador)
            // ==========================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heightsRight = listOf(28, 16, 36, 22, 48, 30, 42, 18, 38, 24, 12)
                heightsRight.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(h.dp)
                            .background(Color(0xFFFF9800), CircleShape) // Naranja / Ámbar
                    )
                }
            }
        }
    }
}