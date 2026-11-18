package com.dlanuza.mixxxdroid.gui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SongItem(
    title: String,
    artist: String
) {
    Text("$title - $artist")
}