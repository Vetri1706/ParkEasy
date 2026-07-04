package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SlateIndigoDark,
    secondary = WarmCoralDark,
    tertiary = GeoPurpleContainer,
    background = DarkCharcoalDark,
    surface = LightLavenderDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2B2930)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPurplePrimary,
    secondary = GeoPurplePrimary,
    tertiary = GeoPurpleContainer,
    background = GeoBackground,
    surface = GeoSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GeoDarkText,
    onSurface = GeoDarkText,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoGrayLabel
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic color to enforce our premium Geometric Balance styling
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
