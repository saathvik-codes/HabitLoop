package com.habitloop.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.habitloop.app.MainActivity
import com.habitloop.app.R
import com.habitloop.app.data.AuthManager
import com.habitloop.app.data.FirebaseSync
import com.habitloop.app.data.UserPrefs
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onBack: () -> Unit, onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPhone by remember { mutableStateOf(false) }
    var showEmail by remember { mutableStateOf(false) }
    val client = remember {
        GoogleSignIn.getClient(activity, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().requestProfile().build())
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.data == null) {
            loading = false
            message = "Google sign-in was cancelled."
        } else scope.launch {
            runCatching {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                AuthManager.signInGoogle(account)
                account.displayName?.take(24)?.let {
                    UserPrefs.setName(context, it)
                    FirebaseSync.pushProfile(it)
                }
            }.onSuccess { loading = false; onAuthenticated() }
                .onFailure { loading = false; message = googleError(it) }
        }
    }
    if (showPhone) {
        PhoneAuthDialog(activity, { showPhone = false }, onAuthenticated)
    }
    if (showEmail) {
        EmailAuthDialog({ showEmail = false }, onAuthenticated)
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Protect your progress", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.weight(.65f))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Filled.CloudDone, null, Modifier.padding(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text("One account. Every device.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
        Text("Continue with Google to protect your private backup, community circles and Growth Lab progress.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        message?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Text(it, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Button(
            onClick = {
                loading = true
                message = null
                client.signOut().addOnCompleteListener { launcher.launch(client.signInIntent) }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF202124)),
            border = BorderStroke(1.dp, Color(0xFFDADCE0))
        ) {
            Image(painterResource(R.drawable.ic_google_g), "Google", Modifier.size(22.dp))
            Text(if (loading) "Connecting…" else "Continue with Google", Modifier.padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
        }
        Text("HabitLoop never posts to Google. Disconnect anytime from Account & security.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        TextButton(onClick = { showPhone = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Use phone number instead")
        }
        TextButton(onClick = { showEmail = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Use email and password")
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun EmailAuthDialog(onDismiss: () -> Unit, onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var createMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(UserPrefs.getName(context).orEmpty()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var waitingForVerification by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val valid = email.contains("@") && password.length >= 6 && (!createMode || name.trim().length >= 2)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (waitingForVerification) "Verify your email" else if (createMode) "Create account" else "Sign in") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (waitingForVerification) {
                    Text("A verification link was sent to $email. Open the link, then return and confirm below.")
                } else {
                    if (createMode) {
                        OutlinedTextField(name, { name = it.take(24) }, label = { Text("Display name") }, singleLine = true, supportingText = { Text("2–24 characters") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(email, { email = it.trim().take(100) }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    OutlinedTextField(
                        password, { password = it.take(72) },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        supportingText = { Text("At least 6 characters") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                message?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && (waitingForVerification || valid),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        if (waitingForVerification) {
                            runCatching { AuthManager.reloadAndIsEmailVerified() }
                                .onSuccess { verified ->
                                    busy = false
                                    if (verified) onAuthenticated() else message = "The email is not verified yet. Open the link and try again."
                                }.onFailure { busy = false; message = AuthManager.readableError(it) }
                        } else {
                            runCatching {
                                if (createMode) {
                                    AuthManager.createEmail(email, password, name)
                                    UserPrefs.setName(context, name.trim())
                                    FirebaseSync.pushProfile(name.trim())
                                    waitingForVerification = true
                                } else {
                                    AuthManager.signInEmail(email, password)
                                    if (!AuthManager.reloadAndIsEmailVerified()) {
                                        AuthManager.resendEmailVerification()
                                        waitingForVerification = true
                                    }
                                }
                            }.onSuccess {
                                busy = false
                                if (!waitingForVerification) onAuthenticated()
                            }.onFailure { busy = false; message = AuthManager.readableError(it) }
                        }
                    }
                }
            ) { Text(if (busy) "Please wait…" else if (waitingForVerification) "I’ve verified" else if (createMode) "Create account" else "Sign in") }
        },
        dismissButton = {
            if (waitingForVerification) {
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
            } else {
                TextButton(onClick = { createMode = !createMode; message = null }, enabled = !busy) {
                    Text(if (createMode) "I already have an account" else "Create an account")
                }
            }
        }
    )
}

@Composable
private fun PhoneAuthDialog(activity: MainActivity, onDismiss: () -> Unit, onAuthenticated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (verificationId == null) "Verify phone number" else "Enter SMS code") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    if (verificationId == null) "Enter the complete international number, including the country code—for example, +91 99081 79816."
                    else "Enter the six-digit verification code sent by Firebase.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (verificationId == null) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { char -> char.isDigit() || char == '+' }.take(16) },
                        label = { Text("Phone number") },
                        placeholder = { Text("+91 99081 79816") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
                message?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && if (verificationId == null) phone.startsWith("+") && phone.length >= 8 else code.length == 6,
                onClick = {
                    busy = true
                    message = null
                    if (verificationId == null) {
                        AuthManager.startPhoneVerification(activity, phone, {
                            verificationId = it
                            busy = false
                        }, { credential ->
                            scope.launch {
                                runCatching { AuthManager.signInPhoneCredential(credential) }
                                    .onSuccess { busy = false; onAuthenticated() }
                                    .onFailure { busy = false; message = AuthManager.readableError(it) }
                            }
                        }, { busy = false; message = it })
                    } else scope.launch {
                        runCatching { AuthManager.verifyPhoneCode(verificationId!!, code) }
                            .onSuccess { busy = false; onAuthenticated() }
                            .onFailure { busy = false; message = AuthManager.readableError(it) }
                    }
                }
            ) { Text(if (busy) "Please wait…" else if (verificationId == null) "Send code" else "Verify") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

private fun googleError(error: Throwable): String = when ((error as? ApiException)?.statusCode) {
    10 -> "Google configuration is updating. Install the latest APK and try again in a few minutes."
    7 -> "Google could not connect. Check your internet connection."
    12501 -> "Google sign-in was cancelled."
    else -> AuthManager.readableError(error)
}
