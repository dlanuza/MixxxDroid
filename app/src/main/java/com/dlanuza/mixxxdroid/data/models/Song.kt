package com.dlanuza.mixxxdroid.data.models

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: String
)