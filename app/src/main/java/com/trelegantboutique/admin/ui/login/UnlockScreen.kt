package com.trelegantboutique.admin.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.trelegantboutique.admin.auth.BiometricHelper
import com.trelegantboutique.admin.ui.theme.DeepGreen

/**
 * Shown every time the app is opened (or resumed from the background) while
 * a session token is already stored. Nothing from the account is visible
 * behind this screen until the fingerprint check succeeds, or the person
 * chooses to fall back to signing out and re-entering their password.
 */
@Composable
fun UnlockScreen(onUnlocked: () -> Unit, onUsePasswordInstead: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }

    fun showPrompt() {
        if (activity == null) return
        error = null
        BiometricHelper.prompt(
            activity = activity,
            onSuccess = onUnlocked,
            onError = { message -> error = message }
        )
    }

    LaunchedEffect(Unit) { showPrompt() }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = DeepGreen, modifier = androidx.compose.ui.Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Use your fingerprint to open TR Elegant Admin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { showPrompt() }) { Text("Try again") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUsePasswordInstead) { Text("Sign out and use password instead") }
        }
    }
}
