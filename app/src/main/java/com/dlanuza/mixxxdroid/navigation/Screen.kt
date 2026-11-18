package com.dlanuza.mixxxdroid.navigation

sealed class Screen(val route: String) {

    data object Home : Screen("home")

    // Biblioteca Deck A
    data object Library : Screen("library")

    // Biblioteca Deck B
    data object LibraryB : Screen("library_b")

    data object Deck : Screen("deck")

    data object DJConsole : Screen("dj_console")
}