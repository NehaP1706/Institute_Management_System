package com.ims.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Colors (matching Figma dark-teal palette) ──────────────────────────
val Primary     = Color(0xFF00BFA5)   // teal accent
val PrimaryDark = Color(0xFF00897B)
val Background  = Color(0xFF0D1B2A)   // deep navy
val Surface     = Color(0xFF122636)
val SurfaceVar  = Color(0xFF1A3448)
val OnPrimary   = Color(0xFF000000)
val OnBackground= Color(0xFFFFFFFF)
val OnSurface   = Color(0xFFE0E0E0)
val Error       = Color(0xFFCF6679)
val CardBg      = Color(0xFF1C3347)
val Divider     = Color(0xFF2A4A60)

private val DarkColorScheme = darkColorScheme(
    primary         = Primary,
    onPrimary       = OnPrimary,
    primaryContainer= PrimaryDark,
    background      = Background,
    onBackground    = OnBackground,
    surface         = Surface,
    onSurface       = OnSurface,
    surfaceVariant  = SurfaceVar,
    error           = Error
)

@Composable
fun IMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
