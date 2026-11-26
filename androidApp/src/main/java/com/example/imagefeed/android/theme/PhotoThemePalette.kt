package com.example.imagefeed.android.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.imagefeed.model.color.PhotoColorEngine

data class PhotoSwatch(
    val color: Color,
    val hex: String,
)

private const val MAX_RGB_CHANNEL = 255f
private const val ACHROMATIC_SATURATION_THRESHOLD = 0.05f
private const val DARK_AMBIENT_LUMINANCE = 0.14f
private const val LIGHT_AMBIENT_LUMINANCE = 0.92f
private const val DARK_AMBIENT_MIN_SATURATION = 0.18f
private const val DARK_AMBIENT_MAX_SATURATION = 0.42f
private const val LIGHT_AMBIENT_MIN_SATURATION = 0.14f
private const val LIGHT_AMBIENT_MAX_SATURATION = 0.34f

private fun hslColor(
    hue: Float,
    saturation: Float,
    luminance: Float,
): Color = Color(PhotoColorEngine.hslToRgb(hue, saturation, luminance).argb)

@Suppress("MagicNumber")
fun generatePhotoColorScheme(
    seedHex: String?,
    isDark: Boolean,
): ColorScheme {
    val baseScheme = if (isDark) DarkColorScheme else LightColorScheme
    val rgb = PhotoColorEngine.parseHexRgb(seedHex)
    val hsl =
        rgb?.let {
            PhotoColorEngine.rgbToHsl(
                r = it.r / MAX_RGB_CHANNEL,
                g = it.g / MAX_RGB_CHANNEL,
                b = it.b / MAX_RGB_CHANNEL,
            )
        }

    // Achromatic seeds (black/white/neutral gray) lack chromatic identity; fall back to base scheme
    if (hsl == null || hsl[1] < ACHROMATIC_SATURATION_THRESHOLD) {
        return baseScheme
    }

    val hue = hsl[0]
    val saturation = hsl[1]
    val tertiaryHue = (hue + 60f) % 360f

    return if (isDark) {
        val darkSurface = hslColor(hue, 0.10f, 0.08f)
        baseScheme.copy(
            primary = hslColor(hue, saturation.coerceAtLeast(0.55f), 0.70f),
            onPrimary = Color(0xFF14141A),
            primaryContainer = hslColor(hue, saturation.coerceIn(0.20f, 0.45f), 0.20f),
            onPrimaryContainer = hslColor(hue, saturation.coerceAtLeast(0.50f), 0.85f),
            surface = darkSurface,
            surfaceContainerLowest = hslColor(hue, 0.10f, 0.05f),
            surfaceContainerLow = hslColor(hue, 0.12f, 0.10f),
            surfaceContainer = hslColor(hue, 0.12f, 0.13f),
            surfaceContainerHigh = hslColor(hue, 0.12f, 0.17f),
            surfaceContainerHighest = hslColor(hue, 0.12f, 0.22f),
            surfaceVariant = hslColor(hue, 0.12f, 0.22f),
            background = darkSurface,
            tertiary = hslColor(tertiaryHue, saturation.coerceAtLeast(0.50f), 0.65f),
            onTertiary = Color(0xFF281500),
            tertiaryContainer = hslColor(tertiaryHue, 0.35f, 0.25f),
            onTertiaryContainer = hslColor(tertiaryHue, 0.50f, 0.85f),
            outline = hslColor(hue, 0.15f, 0.35f),
            outlineVariant = hslColor(hue, 0.12f, 0.24f),
        )
    } else {
        val lightSurface = hslColor(hue, 0.06f, 0.98f)
        baseScheme.copy(
            primary = hslColor(hue, saturation.coerceAtLeast(0.60f), 0.35f),
            onPrimary = Color.White,
            primaryContainer = hslColor(hue, saturation.coerceIn(0.20f, 0.40f), 0.90f),
            onPrimaryContainer = hslColor(hue, saturation.coerceAtLeast(0.60f), 0.15f),
            surface = lightSurface,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(hue, 0.08f, 0.96f),
            surfaceContainer = hslColor(hue, 0.08f, 0.93f),
            surfaceContainerHigh = hslColor(hue, 0.08f, 0.90f),
            surfaceContainerHighest = hslColor(hue, 0.08f, 0.86f),
            surfaceVariant = hslColor(hue, 0.08f, 0.88f),
            background = lightSurface,
            tertiary = hslColor(tertiaryHue, saturation.coerceAtLeast(0.60f), 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(tertiaryHue, 0.30f, 0.90f),
            onTertiaryContainer = hslColor(tertiaryHue, 0.60f, 0.15f),
            outline = hslColor(hue, 0.10f, 0.50f),
            outlineVariant = hslColor(hue, 0.08f, 0.80f),
        )
    }
}

/**
 * Derives a subtle ambient canvas wash tinted by [seedHex] for edge-to-edge photo detail surfaces,
 * returning `null` for invalid or achromatic seeds so neutral photos keep the default surface.
 */
fun derivePhotoAmbientTint(
    seedHex: String?,
    isDark: Boolean,
): Color? =
    PhotoColorEngine
        .parseHexRgb(seedHex)
        ?.let { rgb ->
            PhotoColorEngine.rgbToHsl(
                r = rgb.r / MAX_RGB_CHANNEL,
                g = rgb.g / MAX_RGB_CHANNEL,
                b = rgb.b / MAX_RGB_CHANNEL,
            )
        }?.takeIf { hsl -> hsl[1] >= ACHROMATIC_SATURATION_THRESHOLD }
        ?.let { hsl ->
            if (isDark) {
                hslColor(
                    hue = hsl[0],
                    saturation = hsl[1].coerceIn(DARK_AMBIENT_MIN_SATURATION, DARK_AMBIENT_MAX_SATURATION),
                    luminance = DARK_AMBIENT_LUMINANCE,
                )
            } else {
                hslColor(
                    hue = hsl[0],
                    saturation = hsl[1].coerceIn(LIGHT_AMBIENT_MIN_SATURATION, LIGHT_AMBIENT_MAX_SATURATION),
                    luminance = LIGHT_AMBIENT_LUMINANCE,
                )
            }
        }

fun deriveTonalSwatches(seedHex: String?): List<PhotoSwatch> =
    PhotoColorEngine.deriveTonalSwatches(seedHex).map { swatch ->
        PhotoSwatch(
            color = Color(swatch.rgb.argb),
            hex = swatch.hex,
        )
    }
