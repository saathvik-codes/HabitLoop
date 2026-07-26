package com.habitloop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.habitloop.app.R

val characterAvatars = listOf(
    "focused" to R.drawable.avatar_focused,
    "cheerful" to R.drawable.avatar_cheerful,
    "calm" to R.drawable.avatar_calm,
    "adventurous" to R.drawable.avatar_adventurous,
    "flame" to R.drawable.avatar_flame,
    "sprout" to R.drawable.avatar_sprout
)

@Composable
fun ProfileAvatar(
    name: String,
    avatarStyle: String,
    color: Long,
    photoUri: String?,
    size: Dp
) {
    Surface(Modifier.size(size), shape = CircleShape, color = Color(color)) {
        when {
            !photoUri.isNullOrBlank() -> AsyncImage(
                model = photoUri,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(CircleShape)
            )
            avatarStyle.startsWith("character:") -> {
                val id = avatarStyle.removePrefix("character:")
                val res = characterAvatars.firstOrNull { it.first == id }?.second ?: R.drawable.avatar_cheerful
                Image(
                    painter = painterResource(res),
                    contentDescription = "HabitLoop character avatar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.clip(CircleShape)
                )
            }
            else -> Box(contentAlignment = Alignment.Center) {
                Text(
                    name.trim().take(1).uppercase().ifBlank { "H" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }
    }
}
