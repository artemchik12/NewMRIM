package org.artemchik.newmrim.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(primary = MailBlueLight, onPrimary = CardLight, primaryContainer = MailBlueDark, secondary = OnlineGreen, background = SurfaceDark, surface = CardDark, onBackground = CardLight, onSurface = CardLight)
private val LightColorScheme = lightColorScheme(primary = MailBlue, onPrimary = CardLight, primaryContainer = MailBlueLight, secondary = OnlineGreen, background = SurfaceLight, surface = CardLight, onBackground = SurfaceDark, onSurface = SurfaceDark)

@Composable
fun MrimTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val ctx = LocalContext.current; if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) }
        darkTheme -> DarkColorScheme; else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
