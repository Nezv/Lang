package app.lang.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette: seal-red (primary), jade (secondary), imperial gold (tertiary) on warm paper.
private val LightColors = lightColorScheme(
    primary = Color(0xFFA8322D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF2F6B5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F0DF),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCF3EF),
    surfaceContainer = Color(0xFFF7EDE9),
    surfaceContainerHigh = Color(0xFFF1E7E3),
    surfaceContainerHighest = Color(0xFFEBE1DD),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AA),
    onPrimary = Color(0xFF690004),
    primaryContainer = Color(0xFF8A1B18),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFF96D4C3),
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF145045),
    onSecondaryContainer = Color(0xFFB2F0DF),
    tertiary = Color(0xFFF2BF48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFDEA0),
    background = Color(0xFF1A1312),
    onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF1A1312),
    onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFF140D0C),
    surfaceContainerLow = Color(0xFF221A19),
    surfaceContainer = Color(0xFF271E1D),
    surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
)

/** Pleco-style tone colours: 1 red, 2 green, 3 blue, 4 purple, neutral grey. */
@Immutable
data class ToneColors(val tone1: Color, val tone2: Color, val tone3: Color, val tone4: Color, val neutral: Color) {
    fun forTone(tone: Int): Color = when (tone) {
        1 -> tone1
        2 -> tone2
        3 -> tone3
        4 -> tone4
        else -> neutral
    }
}

private val LightToneColors = ToneColors(Color(0xFFD32F2F), Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFF7B1FA2), Color(0xFF6D6D6D))
private val DarkToneColors = ToneColors(Color(0xFFFF8A80), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFCE93D8), Color(0xFFB0B0B0))

val LocalToneColors = staticCompositionLocalOf { LightToneColors }

@Composable
fun LangTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalToneColors provides if (darkTheme) DarkToneColors else LightToneColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}
