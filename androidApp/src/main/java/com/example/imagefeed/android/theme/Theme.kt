package com.example.imagefeed.android.theme

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class PhotoSwatch(
    val color: Color,
    val hex: String,
)

@SuppressLint("NewApi")
fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    context: Context? = null,
    sdkInt: Int = Build.VERSION.SDK_INT,
): ColorScheme =
    when {
        dynamicColor && sdkInt >= Build.VERSION_CODES.S && context != null -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

@Suppress("MagicNumber")
private fun parseHexArgb(seedHex: String?): Int? {
    val hex = seedHex?.removePrefix("#")?.trim()
    if (hex.isNullOrBlank() || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        return null
    }
    return try {
        when (hex.length) {
            3 -> {
                val expanded = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                (0xFF000000L or expanded.toLong(16)).toInt()
            }
            6 -> (0xFF000000L or hex.toLong(16)).toInt()
            8 -> {
                val argb = hex.toLong(16).toInt()
                val alpha = (argb ushr 24) and 0xFF
                if (alpha < 16) null else argb
            }
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}

@Suppress("MagicNumber")
fun generatePhotoColorScheme(
    seedHex: String?,
    isDark: Boolean,
): ColorScheme {
    val baseScheme = if (isDark) DarkColorScheme else LightColorScheme
    val argb = parseHexArgb(seedHex)

    val hsl =
        argb?.let {
            val r = ((it shr 16) and 0xFF) / 255f
            val g = ((it shr 8) and 0xFF) / 255f
            val b = (it and 0xFF) / 255f
            rgbToHsl(r, g, b)
        }

    // Achromatic seeds (black/white/neutral gray) lack chromatic identity; fall back to base scheme
    if (hsl == null || hsl[1] < 0.05f) {
        return baseScheme
    }

    val hue = hsl[0]
    val saturation = hsl[1]
    val tertiaryHue = (hue + 60f) % 360f

    return if (isDark) {
        val darkSurface = hslToColor(hue, 0.10f, 0.08f)
        baseScheme.copy(
            primary = hslToColor(hue, saturation.coerceAtLeast(0.55f), 0.70f),
            onPrimary = Color(0xFF14141A),
            primaryContainer = hslToColor(hue, saturation.coerceIn(0.20f, 0.45f), 0.20f),
            onPrimaryContainer = hslToColor(hue, saturation.coerceAtLeast(0.50f), 0.85f),
            surface = darkSurface,
            surfaceContainerLowest = hslToColor(hue, 0.10f, 0.05f),
            surfaceContainerLow = hslToColor(hue, 0.12f, 0.10f),
            surfaceContainer = hslToColor(hue, 0.12f, 0.13f),
            surfaceContainerHigh = hslToColor(hue, 0.12f, 0.17f),
            surfaceContainerHighest = hslToColor(hue, 0.12f, 0.22f),
            surfaceVariant = hslToColor(hue, 0.12f, 0.22f),
            background = darkSurface,
            tertiary = hslToColor(tertiaryHue, saturation.coerceAtLeast(0.50f), 0.65f),
            onTertiary = Color(0xFF281500),
            tertiaryContainer = hslToColor(tertiaryHue, 0.35f, 0.25f),
            onTertiaryContainer = hslToColor(tertiaryHue, 0.50f, 0.85f),
            outline = hslToColor(hue, 0.15f, 0.35f),
            outlineVariant = hslToColor(hue, 0.12f, 0.24f),
        )
    } else {
        val lightSurface = hslToColor(hue, 0.06f, 0.98f)
        baseScheme.copy(
            primary = hslToColor(hue, saturation.coerceAtLeast(0.60f), 0.35f),
            onPrimary = Color.White,
            primaryContainer = hslToColor(hue, saturation.coerceIn(0.20f, 0.40f), 0.90f),
            onPrimaryContainer = hslToColor(hue, saturation.coerceAtLeast(0.60f), 0.15f),
            surface = lightSurface,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslToColor(hue, 0.08f, 0.96f),
            surfaceContainer = hslToColor(hue, 0.08f, 0.93f),
            surfaceContainerHigh = hslToColor(hue, 0.08f, 0.90f),
            surfaceContainerHighest = hslToColor(hue, 0.08f, 0.86f),
            surfaceVariant = hslToColor(hue, 0.08f, 0.88f),
            background = lightSurface,
            tertiary = hslToColor(tertiaryHue, saturation.coerceAtLeast(0.60f), 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslToColor(tertiaryHue, 0.30f, 0.90f),
            onTertiaryContainer = hslToColor(tertiaryHue, 0.60f, 0.15f),
            outline = hslToColor(hue, 0.10f, 0.50f),
            outlineVariant = hslToColor(hue, 0.08f, 0.80f),
        )
    }
}

@Suppress("MagicNumber")
fun deriveTonalSwatches(seedHex: String?): List<PhotoSwatch> {
    val argb = parseHexArgb(seedHex) ?: return emptyList()

    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val hsl = rgbToHsl(r, g, b)
    val hue = hsl[0]
    val saturation = hsl[1]

    // Neutral seeds stay achromatic (s = 0) to avoid arbitrary red tinting
    val effectiveSaturation = if (saturation < 0.05f) 0f else saturation.coerceAtLeast(0.40f)

    val luminanceSteps = listOf(0.35f, 0.50f, 0.65f, 0.75f, 0.85f)
    return luminanceSteps.map { lum ->
        val swatchColor = hslToColor(hue, effectiveSaturation, lum)
        PhotoSwatch(
            color = swatchColor,
            hex = colorToHex(swatchColor),
        )
    }
}

@Suppress("MagicNumber")
internal fun rgbToHsl(
    r: Float,
    g: Float,
    b: Float,
): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val l = (max + min) / 2f
    val s =
        if (delta == 0f) {
            0f
        } else if (l <= 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }
    val h =
        when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta % 6f) * 60f
            max == g -> ((b - r) / delta + 2f) * 60f
            else -> ((r - g) / delta + 4f) * 60f
        }.let { if (it < 0f) it + 360f else it }
    return floatArrayOf(h, s, l)
}

@Suppress("MagicNumber")
internal fun hslToColor(
    h: Float,
    s: Float,
    l: Float,
    alpha: Float = 1f,
): Color {
    val normalizedH = ((h % 360f) + 360f) % 360f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((normalizedH / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (rPrime, gPrime, bPrime) =
        when ((normalizedH / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    return Color(
        red = (rPrime + m).coerceIn(0f, 1f),
        green = (gPrime + m).coerceIn(0f, 1f),
        blue = (bPrime + m).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

@Suppress("MagicNumber")
internal fun colorToHex(color: Color): String {
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

@Composable
fun ImageFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocalContext.current else null
    val colorScheme =
        resolveColorScheme(
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
            context = context,
        )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
