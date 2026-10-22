package com.trelegantboutique.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches the site's own brand palette (deep green + gold on a warm cream),
// so the app doesn't look like a generic Material template bolted onto the
// business.
val DeepGreen = Color(0xFF1B4D3E)
val Gold = Color(0xFFD4AF37)
val Cream = Color(0xFFF7F1E6)
val DangerRed = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color.Black,
    background = Cream,
    surface = Color.White,
    error = DangerRed
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color.Black,
    secondary = DeepGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679)
)

@Composable
fun TrAdminTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
