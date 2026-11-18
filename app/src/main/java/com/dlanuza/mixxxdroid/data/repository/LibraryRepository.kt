package com.dlanuza.mixxxdroid.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.dlanuza.mixxxdroid.data.local.FolderEntity
import com.dlanuza.mixxxdroid.data.local.LibraryDao
import com.dlanuza.mixxxdroid.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio de biblioteca de UN deck ("A" o "B").
 * Todas las lecturas y escrituras quedan limitadas a ese deck,
 * aunque la base de datos sea la misma.
 */
class LibraryRepository(
    private val context: Context,
    private val dao: LibraryDao,
    private val deckId: String
) {

    companion object {
        private const val BATCH_SIZE = 200

        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "wav", "flac", "m4a", "aac",
            "ogg", "opus", "aiff", "aif", "ape"
        )
    }

    private val contentResolver
        get() = context.contentResolver

    /** Solo las canciones de las carpetas de ESTE deck. */
    fun getAllSongs(): Flow<List<SongEntity>> = dao.getSongsByDeck(deckId)

    /**
     * Agrega una carpeta SAF a este deck y escanea sus archivos de audio,
     * incluyendo subcarpetas. No reproduce ningún archivo.
     */
    suspend fun addAndIndexFolder(
        treeUri: Uri,
        folderName: String
    ): Long = withContext(Dispatchers.IO) {

        val treeUriString = treeUri.toString()

        // Evitar agregar dos veces la misma carpeta en el MISMO deck.
        val existingFolder = dao.findFolder(treeUriString, deckId)
        if (existingFolder != null) {
            return@withContext existingFolder.id
        }

        val folderId = dao.insertFolder(
            FolderEntity(
                treeUri = treeUriString,
                name = folderName,
                deckId = deckId
            )
        )

        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            rootDocumentId
        )

        val batch = ArrayList<SongEntity>(BATCH_SIZE)

        scanFolder(
            folderId = folderId,
            parentUri = childrenUri,
            relativePath = "",
            batch = batch
        )

        if (batch.isNotEmpty()) {
            dao.insertSongs(batch)
            batch.clear()
        }

        folderId
    }

    /** Recorre una carpeta y todas sus subcarpetas. */
    private suspend fun scanFolder(
        folderId: Long,
        parentUri: Uri,
        relativePath: String,
        batch: ArrayList<SongEntity>
    ) {

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        contentResolver.query(parentUri, projection, null, null, null)?.use { cursor ->

            val documentIdColumn =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeColumn =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeColumn =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {

                if (documentIdColumn < 0 || nameColumn < 0) continue

                val documentId = cursor.getString(documentIdColumn)
                val name = cursor.getString(nameColumn) ?: continue

                val mimeType =
                    if (mimeTypeColumn >= 0 && !cursor.isNull(mimeTypeColumn)) {
                        cursor.getString(mimeTypeColumn)
                    } else {
                        null
                    }

                val isDirectory =
                    mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                if (isDirectory) {

                    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        parentUri,
                        documentId
                    )

                    val childPath =
                        if (relativePath.isEmpty()) name else "$relativePath/$name"

                    scanFolder(
                        folderId = folderId,
                        parentUri = childUri,
                        relativePath = childPath,
                        batch = batch
                    )

                } else {

                    if (!isAudioFile(name, mimeType)) continue

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                        parentUri,
                        documentId
                    )

                    val sizeBytes =
                        if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                            cursor.getLong(sizeColumn)
                        } else {
                            0L
                        }

                    batch.add(
                        SongEntity(
                            uri = fileUri.toString(),
                            title = name.substringBeforeLast('.', name),
                            artist = "Desconocido",
                            durationMs = 0L,
                            sizeBytes = sizeBytes,
                            relativePath = relativePath,
                            folderId = folderId
                        )
                    )

                    // Insertar por bloques para no acumular miles en memoria.
                    if (batch.size >= BATCH_SIZE) {
                        dao.insertSongs(batch)
                        batch.clear()
                    }
                }
            }
        }
    }

    private fun isAudioFile(fileName: String, mimeType: String?): Boolean {
        if (mimeType?.startsWith("audio/") == true) return true

        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in AUDIO_EXTENSIONS
    }

    /** Vacía SOLO la biblioteca de este deck. */
    suspend fun clearLibrary() = withContext(Dispatchers.IO) {
        dao.clearSongsByDeck(deckId)
        dao.clearFoldersByDeck(deckId)
    }
}