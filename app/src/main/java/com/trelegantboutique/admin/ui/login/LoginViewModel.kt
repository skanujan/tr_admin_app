package com.trelegantboutique.admin.ui.login

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.ApiResult
import com.trelegantboutique.admin.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

class LoginViewModel(private val repo: ApiRepository, private val session: SessionStore) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun submit() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (email.isEmpty() || password.isEmpty()) {
            _state.value = _state.value.copy(error = "Enter both email and password.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            when (val result = repo.login(email, password, deviceName)) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (body.ok && body.token != null && body.admin != null) {
                        session.token = body.token
                        session.adminName = body.admin.name
                        session.adminEmail = body.admin.email
                        session.perms = body.perms?.toSet() ?: emptySet()
                        _state.value = _state.value.copy(loading = false, loggedIn = true)
                        registerPushTokenBestEffort()
                    } else {
                        _state.value = _state.value.copy(loading = false, error = body.error ?: "Sign in failed.")
                    }
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    /** Best-effort: hands the device's Firebase push token to the server so
     *  future notifications can reach it. Silently does nothing if Firebase
     *  hasn't been set up yet (no google-services.json) or the fetch fails -
     *  signing in should never be blocked by push setup. */
    private fun registerPushTokenBestEffort() {
        viewModelScope.launch {
            try {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                repo.registerPush(token)
            } catch (e: Throwable) {
                // No Firebase project configured yet, or no network - fine, skip.
            }
        }
    }
}
