package com.prk.kirinmusic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.prk.kirinmusic.viewmodel.MainViewModel
import com.prk.kirinmusic.viewmodel.ThemeColor

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

// --- Generador de Esquemas Material You (Simulación de Tonal Palettes) ---

/**
 * Genera un esquema de colores Material 3 completo basado en un color semilla.
 * Simula la generación de Tonal Palettes (Primary, Secondary, Tertiary, Neutral, Neutral Variant)
 * ajustando la luminosidad (Lightness) y saturación en el espacio HSL.
 */
private fun generateMaterialScheme(
    seed: Color,
    isDark: Boolean
): ColorScheme {
    val hsl = floatArrayOf(0f, 0f, 0f)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1]

    // Helper para obtener color por tono (0-100 lightness)
    fun getTone(tone: Int, s: Float = sat, h: Float = hue): Color {
        val l = tone / 100f
        // HSL Lightness va de 0 a 1.
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l.coerceIn(0f, 1f))))
    }

    // Primary Palette
    val primary40 = getTone(40)
    val primary80 = getTone(80)
    val primary90 = getTone(90)
    val primary30 = getTone(30)
    val primary20 = getTone(20)
    val primary10 = getTone(10)

    // Secondary Palette (Mismo Hue, menos saturación)
    val secSat = (sat * 0.4f).coerceIn(0f, 1f)
    val secondary40 = getTone(40, s = secSat)
    val secondary80 = getTone(80, s = secSat)
    val secondary90 = getTone(90, s = secSat)
    val secondary30 = getTone(30, s = secSat)
    val secondary20 = getTone(20, s = secSat)
    val secondary10 = getTone(10, s = secSat)

    // Tertiary Palette (Hue desplazado 60 grados, saturación media)
    val tertHue = (hue + 60f) % 360f
    val tertSat = (sat * 0.6f).coerceIn(0f, 1f)
    val tertiary40 = getTone(40, s = tertSat, h = tertHue)
    val tertiary80 = getTone(80, s = tertSat, h = tertHue)
    val tertiary90 = getTone(90, s = tertSat, h = tertHue)
    val tertiary30 = getTone(30, s = tertSat, h = tertHue)
    val tertiary20 = getTone(20, s = tertSat, h = tertHue)
    val tertiary10 = getTone(10, s = tertSat, h = tertHue)

    // Neutral Palette (Fondo/Superficie, muy baja saturación)
    val neuSat = (sat * 0.04f).coerceIn(0f, 1f)
    val neutral99 = getTone(99, s = neuSat) // Light Background
    val neutral98 = getTone(98, s = neuSat)
    val neutral96 = getTone(96, s = neuSat)
    val neutral94 = getTone(94, s = neuSat)
    val neutral92 = getTone(92, s = neuSat)
    val neutral90 = getTone(90, s = neuSat)
    val neutral22 = getTone(22, s = neuSat)
    val neutral17 = getTone(17, s = neuSat)
    val neutral12 = getTone(12, s = neuSat)
    val neutral10 = getTone(10, s = neuSat) // Dark Background (Tone 10 or 6)
    val neutral6 = getTone(6, s = neuSat)
    val neutral4 = getTone(4, s = neuSat)   // Very Dark Surface

    // Neutral Variant (Outline, SurfaceVariant, saturación baja)
    val nvSat = (sat * 0.1f).coerceIn(0f, 1f)
    val nv90 = getTone(90, s = nvSat)
    val nv80 = getTone(80, s = nvSat)
    val nv60 = getTone(60, s = nvSat)
    val nv50 = getTone(50, s = nvSat)
    val nv30 = getTone(30, s = nvSat)

    return if (isDark) {
        darkColorScheme(
            primary = primary80,
            onPrimary = primary20,
            primaryContainer = primary30,
            onPrimaryContainer = primary90,
            inversePrimary = primary40,

            secondary = secondary80,
            onSecondary = secondary20,
            secondaryContainer = secondary30,
            onSecondaryContainer = secondary90,

            tertiary = tertiary80,
            onTertiary = tertiary20,
            tertiaryContainer = tertiary30,
            onTertiaryContainer = tertiary90,

            background = neutral6, // Slightly darker than surface
            onBackground = neutral90,
            surface = neutral6,
            onSurface = neutral90,
            surfaceVariant = nv30,
            onSurfaceVariant = nv80,
            outline = nv60,
            outlineVariant = nv30,
            
            // Surface Containers
            surfaceContainerLowest = neutral4,
            surfaceContainerLow = neutral10,
            surfaceContainer = neutral12,
            surfaceContainerHigh = neutral17,
            surfaceContainerHighest = neutral22,

            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = primary40,
            onPrimary = Color.White,
            primaryContainer = primary90,
            onPrimaryContainer = primary10,
            inversePrimary = primary80,

            secondary = secondary40,
            onSecondary = Color.White,
            secondaryContainer = secondary90,
            onSecondaryContainer = secondary10,

            tertiary = tertiary40,
            onTertiary = Color.White,
            tertiaryContainer = tertiary90,
            onTertiaryContainer = tertiary10,

            background = neutral98,
            onBackground = neutral10,
            surface = neutral98,
            onSurface = neutral10,
            surfaceVariant = nv90,
            onSurfaceVariant = nv30,
            outline = nv50,
            outlineVariant = nv80,
            
            // Surface Containers
            surfaceContainerLowest = neutral99, // or 100
            surfaceContainerLow = neutral96,
            surfaceContainer = neutral94,
            surfaceContainerHigh = neutral92,
            surfaceContainerHighest = neutral90,

            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

// --- Definición de Temas usando la lógica Material You ---

// 1. Blue
private val BlueLight = generateMaterialScheme(Color(0xFF0061F2), false)
private val BlueDark = generateMaterialScheme(Color(0xFF0061F2), true)

// 2. Green
private val GreenLight = generateMaterialScheme(Color(0xFF008C3A), false)
private val GreenDark = generateMaterialScheme(Color(0xFF008C3A), true)

// 3. Red
private val RedLight = generateMaterialScheme(Color(0xFFD32F2F), false)
private val RedDark = generateMaterialScheme(Color(0xFFD32F2F), true)

// 4. Purple
private val PurpleLight = generateMaterialScheme(Color(0xFF6200EA), false)
private val PurpleDark = generateMaterialScheme(Color(0xFF6200EA), true)

// 5. Orange
private val OrangeLight = generateMaterialScheme(Color(0xFFFF6D00), false)
private val OrangeDark = generateMaterialScheme(Color(0xFFFF6D00), true)


@Composable
fun MusicPlayerTheme(
    viewModel: MainViewModel? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDynamicColor = viewModel?.isDynamicTheme ?: dynamicColor

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        viewModel != null -> {
            val selectedColor = viewModel.currentThemeColor
            if (darkTheme) {
                when(selectedColor) {
                    ThemeColor.Blue -> BlueDark
                    ThemeColor.Green -> GreenDark
                    ThemeColor.Red -> RedDark
                    ThemeColor.Purple -> PurpleDark
                    ThemeColor.Orange -> OrangeDark
                }
            } else {
                when(selectedColor) {
                    ThemeColor.Blue -> BlueLight
                    ThemeColor.Green -> GreenLight
                    ThemeColor.Red -> RedLight
                    ThemeColor.Purple -> PurpleLight
                    ThemeColor.Orange -> OrangeLight
                }
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
