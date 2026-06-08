package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraColorScheme =
  darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.Black,
    secondary = SurfaceColor,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    error = DangerRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Color.Transparent.toArgb()
      window.navigationBarColor = Color.Transparent.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
  }

  MaterialTheme(colorScheme = AuraColorScheme, typography = Typography, content = content)
}
