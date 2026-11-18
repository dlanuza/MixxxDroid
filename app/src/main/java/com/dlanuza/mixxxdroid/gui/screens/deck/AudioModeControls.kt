package com.dlanuza.mixxxdroid.gui.screens.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlanuza.mixxxdroid.player.AudioMode

private val CardColor = Color(0xFF1E1E1E)
private val CardColor2 = Color(0xFF252525)
private val AccentColor = Color(0xFF00E5FF)

@Composable
fun AudioModeControls(
    currentMode: AudioMode,
    onModeSelected: (AudioMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AudioModeButton(
            text = "NORMAL",
            selected = currentMode == AudioMode.NORMAL,
            onClick = { onModeSelected(AudioMode.NORMAL) },
            modifier = Modifier.weight(1f)
        )

        AudioModeButton(
            text = "MUSIC",
            selected = currentMode == AudioMode.INSTRUMENTAL,
            onClick = { onModeSelected(AudioMode.INSTRUMENTAL) },
            modifier = Modifier.weight(1f)
        )

        AudioModeButton(
            text = "VOCAL",
            selected = currentMode == AudioMode.VOCALS_ONLY,
            onClick = { onModeSelected(AudioMode.VOCALS_ONLY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AudioModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(7.dp),
        contentPadding = PaddingValues(
            horizontal = 2.dp,
            vertical = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AccentColor else CardColor2,
            contentColor = if (selected) Color.Black else Color.LightGray
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}