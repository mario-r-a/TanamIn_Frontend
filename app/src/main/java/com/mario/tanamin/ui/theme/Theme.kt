package com.mario.tanamin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mario.tanamin.data.dto.ThemeResponse

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val DefaultColorScheme = lightColorScheme(
    primary = Color(0xFFFFB86C),
    onPrimary = Color(0xFF222B45),
    secondary = Color(0xFF66BB6A),
    onSecondary = Color(0xFF222B45),
    tertiary = Color(0xFFFFE3A3),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF222B45),
    surface = Color.White,
    onSurface = Color(0xFF222B45)
)

@Composable
fun TanaminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Changed default to false to prioritize our custom theme
    activeTheme: ThemeResponse? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        activeTheme != null -> {
            try {
                lightColorScheme(
                    primary = safeParseColor(activeTheme.primary, Color(0xFFFFB86C)),
                    onPrimary = safeParseColor(activeTheme.text, Color(0xFF222B45)),
                    secondary = safeParseColor(activeTheme.secondary, Color(0xFF66BB6A)),
                    onSecondary = safeParseColor(activeTheme.text, Color(0xFF222B45)),
                    tertiary = safeParseColor(activeTheme.subprimary, Color(0xFFFFE3A3)),
                    background = safeParseColor(activeTheme.background, Color(0xFFF5F5F5)),
                    onBackground = safeParseColor(activeTheme.text, Color(0xFF222B45)),
                    surface = safeParseColor(activeTheme.subbackground, Color.White),
                    onSurface = safeParseColor(activeTheme.text, Color(0xFF222B45)),
                )
            } catch (e: Exception) {
                DefaultColorScheme
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> DefaultColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun safeParseColor(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}