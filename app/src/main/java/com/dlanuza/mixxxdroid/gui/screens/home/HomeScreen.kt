package com.dlanuza.mixxxdroid.gui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onOpenLibrary: () -> Unit,
    onOpenDJConsole: () -> Unit,
    onOpenHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)), // Fondo negro carbón neutro profesional
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // CABECERA CREATIVA
            // ==========================================
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "MIXXXDROID STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA1A1AA),
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "DJ Control Station",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }



            // ==========================================
            // TARJETAS DE ACCESO PRINCIPAL (ESTILO HARDWARE)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta Consola DJ (Estilo Azul Pro)
                HomeActionCard(
                    title = "Consola DJ",
                    subtitle = "Mezcla, efectos y jog wheels",
                    icon = Icons.Default.PlayArrow,
                    accentColor = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDJConsole
                )

                // Tarjeta Biblioteca (Estilo Verde Esmeralda)
                HomeActionCard(
                    title = "Biblioteca",
                    subtitle = "Gestiona pistas y carpetas",
                    icon = Icons.Default.List, // Usando List para evitar errores de importación
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLibrary
                )
            }

            // ==========================================
            // PIE / ESTADO
            // ==========================================
            Text(
                text = "dlanuza",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF52525B)
            )

            TextButton(onClick = onOpenHelp) { // <-- Usando el callback onOpenHelp en lugar de navController
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Manual y Privacidad",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1AA)
                )
            }
        }
    }
}

// =====================================================================
// COMPONENTE DE TARJETA CREATIVA
// =====================================================================

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121214) // Fondo sólido oscuro
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA),
                    maxLines = 1
                )
            }
        }
    }
}