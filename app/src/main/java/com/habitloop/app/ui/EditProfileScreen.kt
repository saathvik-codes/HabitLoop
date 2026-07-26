package com.habitloop.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.habitloop.app.data.UserPrefs
import com.habitloop.app.data.FirebaseSync

private val avatarColors = listOf(
    0xFF84A98C, 0xFFF6B89E, 0xFF7AA7C7, 0xFF9A86C8, 0xFFE3B653, 0xFF52796F
)

@Composable
fun EditProfileScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(UserPrefs.getName(context).orEmpty()) }
    var color by remember { mutableStateOf(UserPrefs.getAvatarColor(context)) }
    var avatarStyle by remember { mutableStateOf(UserPrefs.getAvatarSymbol(context)) }
    var photoUri by remember { mutableStateOf(UserPrefs.getProfilePhotoUri(context)) }
    var instagram by remember { mutableStateOf(UserPrefs.getInstagram(context)) }
    var discord by remember { mutableStateOf(UserPrefs.getDiscord(context)) }
    var shareSocials by remember { mutableStateOf(UserPrefs.sharesSocialsInJams(context)) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            photoUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Customize profile", style = MaterialTheme.typography.headlineSmall)
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                ProfileAvatar(name, avatarStyle, color, photoUri, 112.dp)
            }
            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AddAPhoto, null, Modifier.padding(end = 8.dp))
                Text(if (photoUri == null) "Choose profile photo" else "Change profile photo")
            }
            if (photoUri != null) {
                OutlinedButton(
                    onClick = { photoUri = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Use a HabitLoop character instead") }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
            )
        }
        item {
            Text("HabitLoop characters", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
            Text(
                "Choose a mood that feels like you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
            )
            characterAvatars.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (id, res) ->
                        val selected = avatarStyle == "character:$id" && photoUri == null
                        Card(
                            modifier = Modifier.weight(1f).clickable {
                                avatarStyle = "character:$id"
                                photoUri = null
                            },
                            shape = RoundedCornerShape(22.dp),
                            border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = Color(color).copy(alpha = .16f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = "$id character avatar",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(78.dp).clip(CircleShape)
                                )
                                Text(
                                    id.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Character background", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                avatarColors.forEach { option ->
                    Box(
                        Modifier.size(42.dp).background(Color(option), CircleShape).clickable { color = option },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == option) Icon(Icons.Filled.Check, null, tint = Color.White)
                    }
                }
            }
            Text("Jam connections", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 26.dp))
            Text(
                "Optional. These handles are shown only to people participating in the same Community Jam.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedTextField(
                instagram, { instagram = it.removePrefix("@").filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(30) },
                label = { Text("Instagram username") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            OutlinedTextField(
                discord, { discord = it.take(40) },
                label = { Text("Discord username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Share inside joined Jams", style = MaterialTheme.typography.titleMedium)
                    Text("Off by default. Turn off anytime.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(shareSocials, { shareSocials = it })
            }
            Button(
                onClick = {
                    UserPrefs.setName(context, name.trim())
                    UserPrefs.setAvatarColor(context, color)
                    UserPrefs.setAvatarSymbol(context, avatarStyle)
                    UserPrefs.setProfilePhotoUri(context, photoUri)
                    UserPrefs.setJamSocials(context, instagram, discord, shareSocials)
                    FirebaseSync.pushProfile(name.trim(), avatarStyle, color)
                    onSaved()
                },
                enabled = name.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            ) { Text("Save profile") }
        }
    }
}
