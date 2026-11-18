package com.dlanuza.mixxxdroid.gui.screens.help

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Manual y Funciones", "Política de Privacidad")
    val context = LocalContext.current
    val privacyUrl = "https://dlanuza.pythonanywhere.com/politica-de-privacidad-mixxxdroid/"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y Documentación", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121214))
            )
        },
        containerColor = Color(0xFF09090B)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF121214),
                contentColor = Color(0xFF10B981)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color(0xFF10B981) else Color(0xFFA1A1AA)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                ManualContent()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sitio Web Oficial de Privacidad",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = privacyUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0EA5E9),
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    PrivacyPolicyWebView(url = privacyUrl)
                }
            }
        }
    }
}

@Composable
fun ManualContent() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ManualSectionCard(
            title = "Funciones Principales",
            icon = Icons.Default.Info,
            accentColor = Color(0xFF0EA5E9),
            content = """
- Consola DJ de Doble Deck (Deck A y Deck B): Permite cargar pistas independientes, reproducir, pausar y controlar el pitch.

- Motor de Audio Nativo: Soporte de scratch optimizado con búfer ampliado de hasta 30 segundos.

- Biblioteca Musical Integrada: Indexación automática de pistas de audio locales con selección rápida para cada deck.

- Control de Volumen y Mezcla: Sliders dedicados de nivel y ecualización básica.
            """.trimIndent()
        )

        ManualSectionCard(
            title = "Limitaciones Existentes",
            icon = Icons.Default.Warning,
            accentColor = Color(0xFFFF9800),
            content = """
- Orientación Fija: La aplicación está diseñada exclusivamente para ejecutarse en modo horizontal (Landscape) para garantizar la ergonomía de la tornamesa virtual.

- Formatos de Audio: Depende de los códecs soportados nativamente por Android (MP3, WAV, AAC, OGG). Archivos protegidos con DRM no pueden ser reproducidos.

- Rendimiento de Hardware: Dispositivos con especificaciones de memoria muy reducidas pueden experimentar una ligera latencia al realizar scratch intensivo a 30 segundos.
            """.trimIndent()
        )

        // NUEVA SECCIÓN DE DISCLAIMER LEGAL SOBRE EL CONTENIDO MUSICAL
        ManualSectionCard(
            title = "Aviso Legal y Derechos de Contenido",
            icon = Icons.Default.Lock,
            accentColor = Color(0xFF10B981),
            content = """
- Naturaleza del Software: MixxxDroid opera estrictamente como un reproductor multimedia y herramienta local de mezcla de audio para archivos almacenados por el propio usuario en su dispositivo.

- Ausencia de Vínculo Musical: Los desarrolladores de MixxxDroid no alojan, distribuyen, transmiten, comercializan ni poseen ninguna clase de derecho sobre la música, pistas o archivos de audio que el usuario reproduzca, gestione o mezcle a través de la aplicación.

- Responsabilidad del Usuario: El usuario es el único responsable de asegurar que cuenta con los derechos legales, licencias o autorizaciones pertinentes sobre el contenido de audio local que decida utilizar en la aplicación.
            """.trimIndent()
        )
    }
}

@Composable
fun ManualSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA1A1AA)
            )
        }
    }
}

@Composable
fun PrivacyPolicyWebView(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121214))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                }
            }
        )
    }
}