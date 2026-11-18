package com.dlanuza.mixxxdroid.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dlanuza.mixxxdroid.player.AudioPlayerManager

class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                MainViewModel::class.java
            )
        ) {
            // Creamos una instancia independiente de AudioPlayerManager para cada Deck
            val playerA = AudioPlayerManager(context.applicationContext)
            val playerB = AudioPlayerManager(context.applicationContext)

            return MainViewModel(
                context = context.applicationContext,
                playerA = playerA,
                playerB = playerB
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}