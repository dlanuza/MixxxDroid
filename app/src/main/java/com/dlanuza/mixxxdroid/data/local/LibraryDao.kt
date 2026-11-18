package com.dlanuza.mixxxdroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // ---------------------------------------------------------
    // CANCIONES
    // ---------------------------------------------------------

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    /** Solo las canciones de las carpetas asignadas a un deck. */
    @Query(
        "SELECT s.* FROM songs s " +
                "INNER JOIN folders f ON s.folderId = f.id " +
                "WHERE f.deckId = :deckId " +
                "ORDER BY s.title COLLATE NOCASE ASC"
    )
    fun getSongsByDeck(deckId: String): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM songs " +
                "WHERE folderId = :folderId " +
                "ORDER BY title COLLATE NOCASE ASC"
    )
    fun getSongsByFolder(folderId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    // ---------------------------------------------------------
    // CARPETAS
    // ---------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    /** Evita agregar dos veces la misma carpeta EN EL MISMO deck. */
    @Query(
        "SELECT * FROM folders " +
                "WHERE treeUri = :treeUri AND deckId = :deckId " +
                "LIMIT 1"
    )
    suspend fun findFolder(treeUri: String, deckId: String): FolderEntity?

    // ---------------------------------------------------------
    // LIMPIEZA
    // ---------------------------------------------------------

    @Query("DELETE FROM songs WHERE folderId = :folderId")
    suspend fun clearFolderSongs(folderId: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    /** Vacía SOLO el deck indicado; el otro no se toca. */
    @Query(
        "DELETE FROM songs WHERE folderId IN " +
                "(SELECT id FROM folders WHERE deckId = :deckId)"
    )
    suspend fun clearSongsByDeck(deckId: String)

    @Query("DELETE FROM folders WHERE deckId = :deckId")
    suspend fun clearFoldersByDeck(deckId: String)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()
}