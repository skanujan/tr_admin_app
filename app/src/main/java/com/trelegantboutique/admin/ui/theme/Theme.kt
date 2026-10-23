package com.trelegantboutique.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.trelegantboutique.admin.R

// The exact colours of the website admin panel (admin/layout-top.php):
// --maroon #1b4d3e, --maroon-deep #0f2f26, --gold #c9a14a, --ivory #f7f1e6,
// --ink #1e2b23, --muted #5f6f66, --line #dbe5dd, page background #f3ece0,
// sidebar text #e3cd93.
val DeepGreen = Color(0xFF1B4D3E)
val MaroonDeep = Color(0xFF0F2F26)
val Gold = Color(0xFFC9A14A)
val Cream = Color(0xFFF7F1E6)
val PageBg = Color(0xFFF3ECE0)
val Ink = Color(0xFF1E2B23)
val Muted = Color(0xFF5F6F66)
val Line = Color(0xFFDBE5DD)
val SideText = Color(0xFFE3CD93)
val DangerRed = Color(0xFFB02A3E)
val OkGreen = Color(0xFF1F8A4C)

/** Marcellus is the heading font of the website admin. */
val Marcellus = FontFamily(Font(R.font.marcellus, FontWeight.Normal))

private val Colors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBE0),
    onPrimaryContainer = MaroonDeep,
    secondary = Gold,
    onSecondary = MaroonDeep,
    secondaryContainer = Color(0xFFF3E6C4),
    onSecondaryContainer = MaroonDeep,
    background = PageBg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = DangerRed,
    surfaceTint = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceBright = Color.White,
    surfaceDim = Cream
)

private val base = Typography()
private val AppTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontFamily = Marcellus, color = DeepGreen),
    headlineMedium = base.headlineMedium.copy(fontFamily = Marcellus, color = DeepGreen),
    headlineSmall = base.headlineSmall.copy(fontFamily = Marcellus, color = DeepGreen),
    titleLarge = base.titleLarge.copy(fontFamily = Marcellus),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall.copy(color = Muted),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall
)

/** Card heading style used on every screen, like the website's green h3. */
val CardTitle = TextStyle(fontFamily = Marcellus, fontSize = 18.sp, color = DeepGreen)

@Composable
fun TrAdminTheme(content: @Composable () -> Unit) {
    // The website admin has one light look, so the app always uses it.
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
