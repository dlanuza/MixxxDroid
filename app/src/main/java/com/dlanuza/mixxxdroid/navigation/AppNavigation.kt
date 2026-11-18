package com.dlanuza.mixxxdroid.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlanuza.mixxxdroid.data.local.SongEntity
import com.dlanuza.mixxxdroid.data.models.Song
import com.dlanuza.mixxxdroid.gui.screens.deck.DJConsoleScreen
import com.dlanuza.mixxxdroid.gui.screens.home.HomeScreen
import com.dlanuza.mixxxdroid.gui.screens.home.SplashScreen
import com.dlanuza.mixxxdroid.gui.screens.library.LibraryScreen
import com.dlanuza.mixxxdroid.viewmodel.MainViewModel
import com.dlanuza.mixxxdroid.viewmodel.MainViewModelFactory
import com.dlanuza.mixxxdroid.gui.screens.help.HelpScreen

private fun SongEntity.toSong() = Song(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    duration = durationMs
)

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    // ViewModel compartido por los dos decks (cada deck tiene su propio player).
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(
            context = context.applicationContext
        )
    )

    // Función auxiliar para volver al inicio limpiando la pila
    val navigateToHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash" // Inicia en la pantalla de carga del vinilo
    ) {
        // ==========================================
        // SPLASH SCREEN (Pantalla de carga inicial)
        // ==========================================
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo("splash") { inclusive = true } // Elimina el splash de la pila
                    }
                }
            )
        }

        // HOME
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenLibrary = { navController.navigate(Screen.Library.route) },
                onOpenDJConsole = { navController.navigate(Screen.DJConsole.route) },
                onOpenHelp = { navController.navigate("help") }
            )
        }

        // BIBLIOTECA DECK A
        composable(Screen.Library.route) {
            LibraryScreen(
                deckId = "A",
                currentDeckASongId = mainViewModel.deckASong?.id,
                currentDeckBSongId = mainViewModel.deckBSong?.id,
                onHomeClick = navigateToHome,
                onSongSelected = { songEntity, songsEntity ->
                    val isDeckAEmpty = mainViewModel.deckASong == null
                    val isDeckBPlaying = mainViewModel.deckBUiState.isPlaying
                    val forcePlay = isDeckAEmpty && !isDeckBPlaying

                    mainViewModel.setPlaylistForDeckA(
                        songs = songsEntity.map { it.toSong() },
                        selectedSong = songEntity.toSong(),
                        forcePlay = forcePlay
                    )

                    navController.popBackStack()
                }
            )
        }

        // BIBLIOTECA DECK B
        composable(Screen.LibraryB.route) {
            LibraryScreen(
                deckId = "B",
                currentDeckASongId = mainViewModel.deckASong?.id,
                currentDeckBSongId = mainViewModel.deckBSong?.id,
                onHomeClick = navigateToHome,
                onSongSelected = { songEntity, songsEntity ->
                    val isDeckBEmpty = mainViewModel.deckBSong == null
                    val isDeckAPlaying = mainViewModel.deckUiState.isPlaying
                    val forcePlay = isDeckBEmpty && !isDeckAPlaying

                    mainViewModel.setPlaylistForDeckB(
                        songs = songsEntity.map { it.toSong() },
                        selectedSong = songEntity.toSong(),
                        forcePlay = forcePlay
                    )

                    navController.popBackStack()
                }
            )
        }

        composable("help") {
            HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // CONSOLA DJ
        composable(Screen.DJConsole.route) {
            DJConsoleScreen(
                viewModel = mainViewModel,
                onOpenLibraryForDeck = { deckName ->
                    if (deckName.equals("A", ignoreCase = true)) {
                        navController.navigate(Screen.Library.route)
                    } else {
                        navController.navigate(Screen.LibraryB.route)
                    }
                },
                onHomeClick = navigateToHome
            )
        }
    }
}