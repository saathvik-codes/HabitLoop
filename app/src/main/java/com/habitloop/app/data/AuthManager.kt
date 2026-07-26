package com.habitloop.app.data

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
        val user = auth.currentUser ?: error("Sign in again before requesting a verification email.")
        user.sendEmailVerification().await()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
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
            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) =
                onError(readableError(error))

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) =
                onCodeSent(id)
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

    private suspend fun linkOrSignIn(credential: AuthCredential) {
        val current = auth.currentUser
        if (current?.isAnonymous == true) {
            try {
                current.linkWithCredential(credential).await()
                return
            } catch (_: FirebaseAuthUserCollisionException) {
                // This credential belongs to an existing account; sign into it below.
            }
        }
        auth.signInWithCredential(credential).await()
    }

    fun readableError(error: Throwable): String {
        val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
        return when {
            error is FirebaseNetworkException ->
                "No connection. Check your internet and try again. Your local habits are still safe."
            error is FirebaseTooManyRequestsException ->
                "Too many attempts. Please wait a few minutes before trying again."
            error is FirebaseAuthUserCollisionException || code == "ERROR_EMAIL_ALREADY_IN_USE" ->
                "An account already uses this email. Choose Sign in instead."
            error is FirebaseAuthWeakPasswordException || code == "ERROR_WEAK_PASSWORD" ->
                "Use a stronger password with at least 6 characters."
            error is FirebaseAuthInvalidUserException ||
                code in setOf("ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED") ->
                "No active account was found for this email."
            error is FirebaseAuthInvalidCredentialsException ||
                code in setOf("ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_LOGIN_CREDENTIALS") ->
                "The email or password is incorrect. Try again or reset your password."
            code == "ERROR_INVALID_EMAIL" ->
                "Enter a valid email address."
            code == "ERROR_OPERATION_NOT_ALLOWED" ->
                "Email and password sign-in is not enabled for this Firebase project yet."
            code == "ERROR_REQUIRES_RECENT_LOGIN" ->
                "For security, sign in again before making this change."
            else -> error.localizedMessage
                ?.takeIf { it.isNotBlank() }
                ?: "Sign-in could not be completed. Please try again."
        }
    }
}
