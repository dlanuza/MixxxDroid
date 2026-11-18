package com.dlanuza.mixxxdroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val treeUri: String,

    val name: String,

    // "A" o "B": deck al que pertenece esta carpeta.
    val deckId: String,

    val addedTimestamp: Long = System.currentTimeMillis()
)