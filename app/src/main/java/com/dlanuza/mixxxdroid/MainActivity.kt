package com.dlanuza.mixxxdroid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dlanuza.mixxxdroid.navigation.AppNavigation
import com.dlanuza.mixxxdroid.player.MusicService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Evita que la pantalla se apague o se atenué mientras usas la app de DJ
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Activa el modo edge-to-edge nativo
        enableEdgeToEdge()

        // 3. Ocultar completamente la barra de estado y la barra de navegación (Modo Inmersivo)
        hideSystemBars()

        // 4. Iniciar el servicio en primer plano para permitir audio en segundo plano
        startMusicService()

        setContent {
            SafeAppContent()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Vuelve a ocultar las barras si el usuario interactúa o regresa a la app
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Permite que las barras aparezcan temporalmente si el usuario desliza desde el borde y se vuelvan a ocultar solas
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Oculta la barra de estado y la barra de navegación inferior
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun startMusicService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
private fun SafeAppContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        AppNavigation()
    }
}