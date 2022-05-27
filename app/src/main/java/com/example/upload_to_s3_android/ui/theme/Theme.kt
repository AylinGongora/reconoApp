package com.example.upload_to_s3_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Rojo,
    primaryVariant = Rojo,
    secondary = Teal200,
    background = Color.White,
    onBackground = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onSurface = Color.White

)

private val LightColorPalette = lightColors(
    primary = Rojo,
    primaryVariant = Rojo,
    secondary = Teal200,
    background = Color.White,
    onBackground = Color.White,

    /* Other default colors to override*/
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color.White

)

@Composable
fun Uploadtos3androidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}