package com.dlanuza.mixxxdroid.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlanuza.mixxxdroid.data.local.SongEntity
import com.dlanuza.mixxxdroid.data.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    // mutableStateOf para que Compose se entere cuando cambian.
    // Con un `var` normal la UI nunca mostraba "Escaneando..." ni los errores.
    var isScanning by mutableStateOf(false)
        private set

    var scanError by mutableStateOf<String?>(null)
        private set

    val songs: StateFlow<List<SongEntity>> =
        repository.getAllSongs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addFolder(
        treeUri: Uri,
        folderName: String
    ) {
        viewModelScope.launch {
            isScanning = true
            scanError = null

            try {
                repository.addAndIndexFolder(
                    treeUri = treeUri,
                    folderName = folderName
                )
            } catch (e: Exception) {
                scanError = e.message ?: "Error al escanear la carpeta"
            } finally {
                isScanning = false
            }
        }
    }

    fun clearError() {
        scanError = null
    }

    fun clearLibrary() {
        viewModelScope.launch {
            repository.clearLibrary()
        }
    }
}