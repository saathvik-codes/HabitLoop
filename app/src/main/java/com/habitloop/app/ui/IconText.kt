package com.habitloop.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Real vector icon + text, replacing raw emoji glyphs. Emoji render
 * inconsistently across devices/fonts (different weight, alignment, and
 * sometimes an ugly fallback glyph) — a proper icon always matches the
 * app's own visual language and theme color.
 */
@Composable
fun IconText(
    icon: ImageVector,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    tint: androidx.compose.ui.graphics.Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = style, modifier = Modifier.padding(start = 6.dp))
    }
}
