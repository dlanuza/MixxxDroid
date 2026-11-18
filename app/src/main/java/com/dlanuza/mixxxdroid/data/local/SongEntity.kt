package com.dlanuza.mixxxdroid.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",

    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        // ANTES: unique solo por uri. Eso impedía que la misma canción
        // existiera en la biblioteca del Deck A y del Deck B a la vez.
        // AHORA: única por (carpeta, uri).
        Index(
            value = ["folderId", "uri"],
            unique = true
        ),
        Index(
            value = ["folderId"]
        )
    ]
)
data class SongEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val uri: String,

    val title: String,

    val artist: String,

    val durationMs: Long,

    val sizeBytes: Long,

    val relativePath: String,

    val folderId: Long
)