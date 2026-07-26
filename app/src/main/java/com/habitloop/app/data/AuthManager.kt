package com.habitloop.app.data

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object AuthManager {
    private val auth by lazy { FirebaseAuth.getInstance() }

    suspend fun signInGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val current = auth.currentUser
        if (current?.isAnonymous == true) {
            try {
                current.linkWithCredential(credential).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(credential).await()
            }
        } else {
            auth.signInWithCredential(credential).await()
        }
        FirebaseSync.refreshSession()
    }

    suspend fun createEmail(email: String, password: String, displayName: String) {
        linkOrSignIn(EmailAuthProvider.getCredential(email.trim(), password))
        auth.currentUser?.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName.trim().take(24)).build()
        )?.await()
        auth.currentUser?.sendEmailVerification()?.await()
        FirebaseSync.refreshSession()
    }

    suspend fun signInEmail(email: String, password: String) {
        linkOrSignIn(EmailAuthProvider.getCredential(email.trim(), password))
        FirebaseSync.refreshSession()
    }

    suspend fun reloadAndIsEmailVerified(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun resendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = onAutoVerified(credential)
            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) = onError(readableError(error))
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) = onCodeSent(id)
        }
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber.trim())
                .setTimeout(60, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    suspend fun signInPhoneCredential(credential: PhoneAuthCredential) {
        linkOrSignIn(credential)
        FirebaseSync.refreshSession()
    }

    suspend fun verifyPhoneCode(verificationId: String, code: String) {
        signInPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code.trim()))
    }

    private suspend fun linkOrSignIn(credential: com.google.firebase.auth.AuthCredential) {
        val current = auth.currentUser
        if (current?.isAnonymous == true) {
            try {
                current.linkWithCredential(credential).await()
                return
            } catch (_: FirebaseAuthUserCollisionException) {
                // The number already belongs to a recoverable Firebase account.
            }
        }
        auth.signInWithCredential(credential).await()
    }

    fun readableError(error: Throwable): String {
        val message = error.localizedMessage.orEmpty()
        return when {
            "provider is disabled" in message.lowercase() || "operation is not allowed" in message.lowercase() ->
                "This sign-in method is not enabled in Firebase yet. Enable it under Authentication → Sign-in method."
            "password" in message.lowercase() -> "Check your password and try again."
            "email" in message.lowercase() -> "Check your email address and try again."
            "network" in message.lowercase() -> "No connection. Your local habits are still safe."
            "credential" in message.lowercase() -> "Google could not verify this sign-in. Please try again."
            else -> message.ifBlank { "Google authentication could not be completed." }
        }
    }
}
