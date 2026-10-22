package com.trelegantboutique.admin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Where the signed-in session lives on the phone: the API token, the admin's
 * name/email/permission list, and the server address (so the app can point at
 * a staging site or be moved to a new domain without a rebuild). Everything is
 * stored in EncryptedSharedPreferences, which is itself protected by a key the
 * Android Keystore holds in hardware - not plain text in app storage - same
 * idea as how the website never stores a plain password, only its hash.
 *
 * This file only stores and retrieves the token; it never decides whether the
 * person unlocking the phone with their fingerprint should see it - that
 * check happens once, in LoginScreen/BiometricGate, before this is read.
 */
class SessionStore(context: Context) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "tr_admin_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null) ?: com.trelegantboutique.admin.BuildConfig.DEFAULT_API_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var adminName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var adminEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var perms: Set<String>
        get() = prefs.getStringSet(KEY_PERMS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_PERMS, value).apply()

    /** True once biometric unlock has been set up and accepted once for this
     *  install - until then the app always asks for email + password first. */
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun can(key: String): Boolean = perms.contains("*") || perms.contains(key)

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_NAME).remove(KEY_EMAIL).remove(KEY_PERMS).apply()
        // Deliberately keeps apiBaseUrl and biometricEnabled - signing out
        // shouldn't make you re-type the server address or redo Fingerprint setup.
    }

    companion object {
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_NAME = "admin_name"
        private const val KEY_EMAIL = "admin_email"
        private const val KEY_PERMS = "perms"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
