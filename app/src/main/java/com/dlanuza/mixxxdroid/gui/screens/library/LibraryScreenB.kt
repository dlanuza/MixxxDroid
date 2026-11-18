//package com.dlanuza.mixxxdroid.gui.screens.library
//
//import android.content.Intent
//import android.net.Uri
//import android.util.Log
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyListState
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.dlanuza.mixxxdroid.data.local.AppDatabase
//import com.dlanuza.mixxxdroid.data.local.SongEntity
//import com.dlanuza.mixxxdroid.data.repository.LibraryRepository
//import com.dlanuza.mixxxdroid.viewmodel.LibraryViewModel
//import kotlin.math.roundToInt
//import kotlinx.coroutines.launch
//
//private const val TAG_B = "LIBRARY_B"
//
//@Composable
//fun LibraryScreenB(
//    currentDeckASongId: Long? = null,
//    currentDeckBSongId: Long? = null,
//    onSongSelected: (SongEntity, List<SongEntity>) -> Unit
//) {
//    val context = LocalContext.current
//
//    LaunchedEffect(Unit) {
//        Log.d(TAG_B, "================================")
//        Log.d(TAG_B, "ABRIENDO BIBLIOTECA DEL DECK B")
//        Log.d(TAG_B, "================================")
//    }
//
//    // Room compartido con Deck A.
//    val database = remember {
//        AppDatabase.getInstance(context).also {
//            Log.d(TAG_B, "Instancia Room obtenida: ${it.hashCode()}")
//        }
//    }
//
//    val repository = remember {
//        LibraryRepository(context = context,dao = database.libraryDao()//No value passed for parameter 'deckId'.
//        ).also {
//            Log.d(TAG_B, "Repository B creado: ${it.hashCode()}")
//        }
//    }
//
//    // ViewModel independiente para la pantalla B.
//    val viewModel: LibraryViewModel = viewModel(
//        factory = remember {
//            object : ViewModelProvider.Factory {
//                @Suppress("UNCHECKED_CAST")
//                override fun <T : ViewModel> create(
//                    modelClass: Class<T>
//                ): T {
//                    Log.d(TAG_B, "Creando LibraryViewModel B")
//                    return LibraryViewModel(repository = repository) as T
//                }
//            }
//        }
//    )
//
//    val songs by viewModel.songs.collectAsStateWithLifecycle()
//
//    LaunchedEffect(viewModel) {
//        Log.d(TAG_B, "ViewModel B activo: ${viewModel.hashCode()}")
//    }
//
//    LaunchedEffect(songs) {
//        Log.d(TAG_B, "CAMBIO EN LISTA B")
//        Log.d(TAG_B, "Total canciones: ${songs.size}")
//        Log.d(TAG_B, "IDs: ${songs.map { it.id }}")
//        Log.d(TAG_B, "Títulos: ${songs.map { it.title }}")
//    }
//
//    LaunchedEffect(currentDeckASongId, currentDeckBSongId) {
//        Log.d(
//            TAG_B,
//            "Canción actual A: $currentDeckASongId | Canción actual B: $currentDeckBSongId"
//        )
//    }
//
//    var showClearDialog by remember {
//        mutableStateOf(false)
//    }
//
//    val folderPicker = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.OpenDocumentTree()
//    ) { uri: Uri? ->
//
//        if (uri == null) {
//            Log.d(TAG_B, "Selección de carpeta cancelada")
//            return@rememberLauncherForActivityResult
//        }
//
//        Log.d(TAG_B, "Carpeta seleccionada: $uri")
//
//        try {
//            context.contentResolver.takePersistableUriPermission(
//                uri,
//                Intent.FLAG_GRANT_READ_URI_PERMISSION
//            )
//        } catch (e: SecurityException) {
//            Log.e(TAG_B, "No se pudo conservar el permiso de carpeta", e)
//        }
//
//        val folderName = uri.lastPathSegment
//            ?.substringAfterLast(':')
//            ?.ifBlank { "Carpeta" }
//            ?: "Carpeta"
//
//        Log.d(TAG_B, "Agregando carpeta: $folderName")
//
//        viewModel.addFolder(
//            treeUri = uri,
//            folderName = folderName
//        )
//    }
//
//    if (showClearDialog) {
//        AlertDialog(
//            onDismissRequest = {
//                showClearDialog = false
//            },
//            title = {
//                Text("Vaciar biblioteca")
//            },
//            text = {
//                Text(
//                    "Se eliminarán todas las canciones y carpetas de la biblioteca. " +
//                            "Los archivos del teléfono NO se eliminarán."
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        Log.w(TAG_B, "SOLICITUD DE VACIAR BIBLIOTECA")
//
//                        showClearDialog = false
//                        viewModel.clearLibrary()
//                    }
//                ) {
//                    Text("Vaciar")
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    onClick = {
//                        showClearDialog = false
//                    }
//                ) {
//                    Text("Cancelar")
//                }
//            }
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = "Biblioteca",
//                    style = MaterialTheme.typography.headlineMedium
//                )
//
//                Text(
//                    text = "Cargando para: Deck B",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = Color(0xFF2196F3)
//                )
//            }
//
//            Button(
//                onClick = {
//                    Log.d(TAG_B, "Pulsado Agregar carpeta")
//                    folderPicker.launch(null)
//                },
//                enabled = !viewModel.isScanning
//            ) {
//                Text("Agregar carpeta")
//            }
//        }
//
//        TextButton(
//            onClick = {
//                Log.w(TAG_B, "Pulsado Vaciar biblioteca")
//                showClearDialog = true
//            },
//            enabled = songs.isNotEmpty() && !viewModel.isScanning
//        ) {
//            Text("Vaciar biblioteca")
//        }
//
//        if (viewModel.isScanning) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 16.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                CircularProgressIndicator()
//                Text(text = "Escaneando biblioteca...")
//            }
//        }
//
//        viewModel.scanError?.let { error ->
//            Log.e(TAG_B, "Error de escaneo: $error")
//
//            Text(
//                text = error,
//                color = MaterialTheme.colorScheme.error,
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
//        }
//
//        Text(
//            text = "${songs.size} canciones",
//            style = MaterialTheme.typography.titleMedium,
//            modifier = Modifier.padding(
//                top = 12.dp,
//                bottom = 8.dp
//            )
//        )
//
//        if (songs.isEmpty() && !viewModel.isScanning) {
//            Text(text = "No hay canciones en la biblioteca.")
//        } else {
//            val listState = rememberLazyListState()
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//            ) {
//                LazyColumn(
//                    state = listState,
//                    modifier = Modifier.fillMaxSize(),
//                    verticalArrangement = Arrangement.spacedBy(6.dp)
//                ) {
//                    items(
//                        items = songs,
//                        key = { song -> song.id }
//                    ) { song ->
//
//                        val isPlayingInA =
//                            song.id == currentDeckASongId
//
//                        val isPlayingInB =
//                            song.id == currentDeckBSongId
//
//                        SongItemB(
//                            song = song,
//                            isPlayingInA = isPlayingInA,
//                            isPlayingInB = isPlayingInB,
//                            onClick = {
//                                Log.d(
//                                    TAG_B,
//                                    "CANCIÓN SELECCIONADA EN DECK B: " +
//                                            "${song.title} | ID: ${song.id}"
//                                )
//
//                                Log.d(
//                                    TAG_B,
//                                    "Playlist enviada desde B: ${songs.size} canciones"
//                                )
//
//                                onSongSelected(song, songs)
//                            }
//                        )
//                    }
//                }
//
//                if (songs.size > 1) {
//                    LibraryScrollbarB(
//                        listState = listState,
//                        itemCount = songs.size,
//                        modifier = Modifier
//                            .align(Alignment.CenterEnd)
//                            .fillMaxHeight()
//                            .padding(vertical = 4.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun LibraryScrollbarB(
//    listState: LazyListState,
//    itemCount: Int,
//    modifier: Modifier = Modifier
//) {
//    if (itemCount <= 1) return
//
//    val coroutineScope = rememberCoroutineScope()
//    val density = LocalDensity.current
//
//    var containerHeightPx by remember {
//        mutableFloatStateOf(1f)
//    }
//
//    val thumbHeightDp = 48.dp
//
//    val thumbHeightPx = remember(density, thumbHeightDp) {
//        with(density) {
//            thumbHeightDp.toPx()
//        }
//    }
//
//    val maxIndex = (itemCount - 1).coerceAtLeast(1)
//
//    var dragY by remember {
//        mutableFloatStateOf(0f)
//    }
//
//    var isDragging by remember {
//        mutableStateOf(false)
//    }
//
//    val currentIndex = listState.firstVisibleItemIndex
//
//    val currentFraction = (
//            currentIndex.toFloat() / maxIndex.toFloat()
//            ).coerceIn(0f, 1f)
//
//    Box(
//        modifier = modifier
//            .width(32.dp)
//            .onSizeChanged { size ->
//                if (size.height > 0) {
//                    containerHeightPx = size.height.toFloat()
//                }
//            }
//            .pointerInput(itemCount, containerHeightPx) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        isDragging = true
//
//                        dragY = offset.y.coerceIn(
//                            0f,
//                            containerHeightPx
//                        )
//
//                        val fraction = (
//                                dragY / containerHeightPx
//                                ).coerceIn(0f, 1f)
//
//                        val targetIndex = (
//                                fraction * maxIndex
//                                ).roundToInt().coerceIn(0, maxIndex)
//
//                        coroutineScope.launch {
//                            listState.scrollToItem(targetIndex)
//                        }
//                    },
//                    onDrag = { change, dragAmount ->
//                        change.consume()
//
//                        dragY = (
//                                dragY + dragAmount.y
//                                ).coerceIn(0f, containerHeightPx)
//
//                        val fraction = (
//                                dragY / containerHeightPx
//                                ).coerceIn(0f, 1f)
//
//                        val targetIndex = (
//                                fraction * maxIndex
//                                ).roundToInt().coerceIn(0, maxIndex)
//
//                        coroutineScope.launch {
//                            listState.scrollToItem(targetIndex)
//                        }
//                    },
//                    onDragEnd = {
//                        isDragging = false
//                    },
//                    onDragCancel = {
//                        isDragging = false
//                    }
//                )
//            }
//    ) {
//        Box(
//            modifier = Modifier
//                .width(4.dp)
//                .fillMaxHeight()
//                .align(Alignment.Center)
//                .clip(MaterialTheme.shapes.small)
//                .background(MaterialTheme.colorScheme.surfaceVariant)
//        )
//
//        val maxThumbOffset = (
//                containerHeightPx - thumbHeightPx
//                ).coerceAtLeast(0f)
//
//        val thumbOffset = if (isDragging) {
//            (
//                    dragY / containerHeightPx
//                    ).coerceIn(0f, 1f) * maxThumbOffset
//        } else {
//            currentFraction * maxThumbOffset
//        }
//
//        Box(
//            modifier = Modifier
//                .width(if (isDragging) 10.dp else 7.dp)
//                .height(thumbHeightDp)
//                .offset {
//                    IntOffset(
//                        x = 0,
//                        y = thumbOffset.roundToInt()
//                    )
//                }
//                .align(Alignment.TopCenter)
//                .clip(MaterialTheme.shapes.small)
//                .background(MaterialTheme.colorScheme.primary)
//        )
//    }
//}
//
//@Composable
//private fun SongItemB(
//    song: SongEntity,
//    isPlayingInA: Boolean,
//    isPlayingInB: Boolean,
//    onClick: () -> Unit
//) {
//    val cardColors = when {
//        isPlayingInA -> CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
//                alpha = 0.4f
//            )
//        )
//
//        isPlayingInB -> CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
//                alpha = 0.4f
//            )
//        )
//
//        else -> CardDefaults.cardColors()
//    }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable {
//                onClick()
//            },
//        colors = cardColors
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = song.title,
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.weight(1f)
//                )
//
//                if (isPlayingInA) {
//                    Text(
//                        text = "[EN DECK A]",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = Color(0xFF4CAF50)
//                    )
//                } else if (isPlayingInB) {
//                    Text(
//                        text = "[EN DECK B]",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = Color(0xFF2196F3)
//                    )
//                }
//            }
//
//            if (song.relativePath.isNotEmpty()) {
//                Text(
//                    text = song.relativePath,
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//
//            Text(
//                text = song.artist,
//                style = MaterialTheme.typography.bodySmall
//            )
//        }
//    }
//}