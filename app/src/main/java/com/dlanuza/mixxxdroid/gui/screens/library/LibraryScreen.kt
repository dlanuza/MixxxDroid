package com.dlanuza.mixxxdroid.gui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dlanuza.mixxxdroid.data.local.AppDatabase
import com.dlanuza.mixxxdroid.data.local.SongEntity
import com.dlanuza.mixxxdroid.data.repository.LibraryRepository
import com.dlanuza.mixxxdroid.viewmodel.LibraryViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Paleta neutra oscura (Cero tonos morados, estilo profesional y limpio)
private val DeckAColor = Color(0xFF10B981) // Verde esmeralda vivo
private val DeckBColor = Color(0xFF0EA5E9) // Azul cian vivo
private val CardBgColor = Color(0xFF18181B)  // Gris oscuro neutro (Zinc 900)
private val CardPlayingAColor = Color(0xFF064E3B) // Fondo verde oscuro activo
private val CardPlayingBColor = Color(0xFF0369A1) // Fondo azul oscuro activo

// =====================================================================
// PANTALLA ÚNICA DE BIBLIOTECA (sirve para Deck A y Deck B)
// =====================================================================

@Composable
fun LibraryScreen(
    deckId: String, // "A" o "B"
    currentDeckASongId: Long? = null,
    currentDeckBSongId: Long? = null,
    onHomeClick: () -> Unit, // Acción para volver al Home con la casita
    onSongSelected: (SongEntity, List<SongEntity>) -> Unit
) {
    val context = LocalContext.current
    val accent = if (deckId == "A") DeckAColor else DeckBColor

    val viewModel: LibraryViewModel = viewModel(
        key = "library_$deckId",
        factory = remember(deckId) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val dao = AppDatabase.getInstance(appContext).libraryDao()
                    return LibraryViewModel(
                        LibraryRepository(appContext, dao, deckId)
                    ) as T
                }
            }
        }
    )

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtrado reactivo de canciones por título o artista
    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val folderName = uri.lastPathSegment
            ?.substringAfterLast(':')
            ?.ifBlank { "Carpeta" } ?: "Carpeta"

        viewModel.addFolder(treeUri = uri, folderName = folderName)
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Vaciar biblioteca del Deck $deckId") },
            text = {
                Text(
                    "Se eliminarán las canciones y carpetas del Deck $deckId. " +
                            "El otro deck no se ve afectado y los archivos del teléfono NO se eliminan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearLibrary()
                }) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Fondo general negro/carbón neutro muy limpio
            .padding(12.dp)
    ) {
        // Cabecera compacta con Botón Home (Casita) y Título
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Inicio (Casita)
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Ir al Inicio",
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Deck $deckId activo",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }

            Button(
                onClick = { folderPicker.launch(null) },
                enabled = !viewModel.isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("+ Carpeta", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de Búsqueda con diseño neutro oscuro
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                Text(
                    "Buscar título o artista...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA) // Gris neutro claro
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = Color(0xFF27272A),
                focusedContainerColor = Color(0xFF121214),
                unfocusedContainerColor = Color(0xFF121214),
                cursorColor = accent
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFA1A1AA)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) "${songs.size} canciones" else "${filteredSongs.size} de ${songs.size} encontradas",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFA1A1AA),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            TextButton(
                onClick = { showClearDialog = true },
                enabled = songs.isNotEmpty() && !viewModel.isScanning,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Vaciar", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
            }
        }

        if (viewModel.isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
                Text("Escaneando biblioteca...", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }

        viewModel.scanError?.let { error ->
            Text(
                text = error,
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (songs.isEmpty() && !viewModel.isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay canciones en el Deck $deckId. Agrega una carpeta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA)
                )
            }
        } else if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin resultados para \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA)
                )
            }
        } else {
            val listState = rememberLazyListState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = filteredSongs, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            isPlayingInA = song.id == currentDeckASongId,
                            isPlayingInB = song.id == currentDeckBSongId,
                            onClick = { onSongSelected(song, filteredSongs) }
                        )
                    }
                }

                if (filteredSongs.size > 1) {
                    LibraryScrollbar(
                        listState = listState,
                        itemCount = filteredSongs.size,
                        accentColor = accent,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// SCROLLBAR RÁPIDO
// =====================================================================

@Composable
private fun LibraryScrollbar(
    listState: LazyListState,
    itemCount: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 1) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    val thumbHeightDp = 36.dp
    val thumbHeightPx = remember(density, thumbHeightDp) {
        with(density) { thumbHeightDp.toPx() }
    }

    val maxIndex = (itemCount - 1).coerceAtLeast(1)

    var dragY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val currentIndex = listState.firstVisibleItemIndex
    val currentFraction = (currentIndex.toFloat() / maxIndex.toFloat())
        .coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(24.dp)
            .onSizeChanged { size ->
                if (size.height > 0) {
                    containerHeightPx = size.height.toFloat()
                }
            }
            .pointerInput(itemCount, containerHeightPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragY = offset.y.coerceIn(0f, containerHeightPx)

                        val fraction = (dragY / containerHeightPx).coerceIn(0f, 1f)
                        val targetIndex = (fraction * maxIndex)
                            .roundToInt()
                            .coerceIn(0, maxIndex)

                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        dragY = (dragY + dragAmount.y)
                            .coerceIn(0f, containerHeightPx)

                        val fraction = (dragY / containerHeightPx).coerceIn(0f, 1f)
                        val targetIndex = (fraction * maxIndex)
                            .roundToInt()
                            .coerceIn(0, maxIndex)

                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .clip(MaterialTheme.shapes.small)
                .background(Color(0xFF27272A))
        )

        val maxThumbOffset = (containerHeightPx - thumbHeightPx)
            .coerceAtLeast(0f)

        val thumbOffset = if (isDragging) {
            (dragY / containerHeightPx).coerceIn(0f, 1f) * maxThumbOffset
        } else {
            currentFraction * maxThumbOffset
        }

        Box(
            modifier = Modifier
                .width(if (isDragging) 8.dp else 5.dp)
                .height(thumbHeightDp)
                .offset { IntOffset(x = 0, y = thumbOffset.roundToInt()) }
                .align(Alignment.TopCenter)
                .clip(MaterialTheme.shapes.small)
                .background(accentColor)
        )
    }
}

// =====================================================================
// ITEM DE CANCIÓN
// =====================================================================

@Composable
private fun SongItem(
    song: SongEntity,
    isPlayingInA: Boolean,
    isPlayingInB: Boolean,
    onClick: () -> Unit
) {
    val cardBg = when {
        isPlayingInA -> CardPlayingAColor
        isPlayingInB -> CardPlayingBColor
        else -> CardBgColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                if (isPlayingInA) {
                    Text(
                        text = "[DECK A]",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeckAColor
                    )
                } else if (isPlayingInB) {
                    Text(
                        text = "[DECK B]",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeckBColor
                    )
                }
            }

            if (song.relativePath.isNotEmpty()) {
                Text(
                    text = song.relativePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1AA)
                )
            }

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD4D4D8)
            )
        }
    }
}