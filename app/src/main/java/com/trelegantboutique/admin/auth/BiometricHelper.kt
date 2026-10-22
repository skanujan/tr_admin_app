package com.trelegantboutique.admin.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/** Thin wrapper around BiometricPrompt so screens can ask "can we even use a
 *  fingerprint/face on this phone?" and "show the system prompt" without
 *  repeating the BiometricManager boilerplate each time. */
object BiometricHelper {

    enum class Availability { AVAILABLE, NO_HARDWARE, NOT_ENROLLED, UNAVAILABLE }

    fun availability(activity: FragmentActivity): Availability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_ENROLLED
            else -> Availability.UNAVAILABLE
        }
    }

    fun prompt(
        activity: FragmentActivity,
        title: String = "Unlock TR Elegant Admin",
        subtitle: String = "Use your fingerprint to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
            override fun onAuthenticationFailed() {
                // A single bad scan - the system prompt stays open and lets
                // the person try again, so this is deliberately not an error.
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use password instead")
            .build()
        prompt.authenticate(info)
    }
}
