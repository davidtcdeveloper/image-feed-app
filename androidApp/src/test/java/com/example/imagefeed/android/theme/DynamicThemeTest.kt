@file:Suppress("MagicNumber")

package com.example.imagefeed.android.theme

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DynamicThemeTest {
    @Test
    fun generatePhotoColorSchemeWithValidSeedInDarkThemeDerivesExpectedTokens() {
        val scheme = generatePhotoColorScheme("#EAA83B", isDark = true)

        assertNotEquals(DarkColorScheme.primary, scheme.primary)
        assertNotEquals(DarkColorScheme.tertiary, scheme.tertiary)
        assertNotEquals(DarkColorScheme.surfaceContainer, scheme.surfaceContainer)

        // Verify primary luminance is around 0.70 for dark theme contrast
        val primaryHsl = rgbToHsl(scheme.primary.red, scheme.primary.green, scheme.primary.blue)
        assertTrue(abs(primaryHsl[2] - 0.70f) < 0.05f, "Primary lightness should be ~0.70, was ${primaryHsl[2]}")

        // Verify primaryContainer luminance is around 0.20
        val containerColor = scheme.primaryContainer
        val containerHsl = rgbToHsl(containerColor.red, containerColor.green, containerColor.blue)
        assertTrue(
            abs(containerHsl[2] - 0.20f) < 0.05f,
            "Primary container lightness should be ~0.20, was ${containerHsl[2]}",
        )

        // Verify tertiary hue is shifted by ~60 degrees
        val tertiaryHsl = rgbToHsl(scheme.tertiary.red, scheme.tertiary.green, scheme.tertiary.blue)
        val expectedTertiaryHue = (primaryHsl[0] + 60f) % 360f
        assertTrue(
            abs(tertiaryHsl[0] - expectedTertiaryHue) < 5f,
            "Tertiary hue should be ~$expectedTertiaryHue, was ${tertiaryHsl[0]}",
        )
    }

    @Test
    fun generatePhotoColorSchemeWithValidSeedInLightThemeDerivesExpectedTokens() {
        val scheme = generatePhotoColorScheme("#EAA83B", isDark = false)

        assertNotEquals(LightColorScheme.primary, scheme.primary)
        assertNotEquals(LightColorScheme.tertiary, scheme.tertiary)

        // Verify primary luminance is around 0.35 for high WCAG contrast on light surfaces
        val primaryHsl = rgbToHsl(scheme.primary.red, scheme.primary.green, scheme.primary.blue)
        assertTrue(abs(primaryHsl[2] - 0.35f) < 0.05f, "Light primary lightness should be ~0.35, was ${primaryHsl[2]}")
    }

    @Test
    fun generatePhotoColorSchemeWithNullOrEmptySeedFallsBackToDefaults() {
        assertEquals(DarkColorScheme, generatePhotoColorScheme(null, isDark = true))
        assertEquals(LightColorScheme, generatePhotoColorScheme(null, isDark = false))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("", isDark = true))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("   ", isDark = true))
    }

    @Test
    fun generatePhotoColorSchemeWithInvalidSeedFallsBackSafely() {
        assertEquals(DarkColorScheme, generatePhotoColorScheme("not-a-color", isDark = true))
        assertEquals(LightColorScheme, generatePhotoColorScheme("#12", isDark = false))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("GGGGGG", isDark = true))
    }

    @Test
    fun generatePhotoColorSchemeHandlesPrefixVariations() {
        val withPrefix = generatePhotoColorScheme("#4A90E2", isDark = true)
        val withoutPrefix = generatePhotoColorScheme("4A90E2", isDark = true)

        assertEquals(withPrefix.primary, withoutPrefix.primary)
        assertEquals(withPrefix.tertiary, withoutPrefix.tertiary)
        assertEquals(withPrefix.surfaceContainer, withoutPrefix.surfaceContainer)
    }

    @Test
    fun deriveTonalSwatchesGeneratesFiveStepsWithIncreasingLuminance() {
        val swatches = deriveTonalSwatches("#EAA83B")
        assertEquals(5, swatches.size)

        val expectedLuminance = listOf(0.35f, 0.50f, 0.65f, 0.75f, 0.85f)
        for (i in swatches.indices) {
            val swatch = swatches[i]
            assertTrue(swatch.hex.startsWith("#"), "Swatch hex should start with #: ${swatch.hex}")
            assertEquals(7, swatch.hex.length, "Swatch hex should be 7 characters (#RRGGBB): ${swatch.hex}")

            val hsl = rgbToHsl(swatch.color.red, swatch.color.green, swatch.color.blue)
            assertTrue(
                abs(hsl[2] - expectedLuminance[i]) < 0.05f,
                "Step $i expected luminance ~${expectedLuminance[i]} but got ${hsl[2]}",
            )
        }
    }

    @Test
    fun deriveTonalSwatchesReturnsEmptyOnInvalidInput() {
        assertTrue(deriveTonalSwatches(null).isEmpty())
        assertTrue(deriveTonalSwatches("").isEmpty())
        assertTrue(deriveTonalSwatches("invalid").isEmpty())
    }

    @Test
    fun hslRoundTripConversionPreservesPrimaryHues() {
        val redHsl = rgbToHsl(1f, 0f, 0f)
        assertEquals(0f, redHsl[0], 0.1f)
        assertEquals(1f, redHsl[1], 0.01f)
        assertEquals(0.5f, redHsl[2], 0.01f)

        val redColor = hslToColor(0f, 1f, 0.5f)
        assertEquals(1f, redColor.red, 0.01f)
        assertEquals(0f, redColor.green, 0.01f)
        assertEquals(0f, redColor.blue, 0.01f)

        val greenHsl = rgbToHsl(0f, 1f, 0f)
        assertEquals(120f, greenHsl[0], 0.1f)

        val blueHsl = rgbToHsl(0f, 0f, 1f)
        assertEquals(240f, blueHsl[0], 0.1f)
    }

    @Test
    fun generatePhotoColorSchemeWithAchromaticSeedFallsBackToDefaults() {
        assertEquals(DarkColorScheme, generatePhotoColorScheme("#000000", isDark = true))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("#FFFFFF", isDark = true))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("#808080", isDark = true))
        assertEquals(LightColorScheme, generatePhotoColorScheme("#000000", isDark = false))
        assertEquals(LightColorScheme, generatePhotoColorScheme("#888888", isDark = false))
    }

    @Test
    fun deriveTonalSwatchesWithAchromaticSeedDerivesGrayscaleRampWithoutRedTint() {
        val swatches = deriveTonalSwatches("#000000")
        assertEquals(5, swatches.size)

        for (swatch in swatches) {
            val hsl = rgbToHsl(swatch.color.red, swatch.color.green, swatch.color.blue)
            assertEquals(0f, hsl[1], 0.01f, "Achromatic swatch should have 0 saturation, was ${hsl[1]}")
            assertEquals(swatch.color.red, swatch.color.green, 0.01f)
            assertEquals(swatch.color.green, swatch.color.blue, 0.01f)
        }
    }

    @Test
    fun generatePhotoColorSchemeRejectsSignedHexAndTransparentColors() {
        assertEquals(DarkColorScheme, generatePhotoColorScheme("-12345", isDark = true))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("+12345", isDark = true))
        assertEquals(DarkColorScheme, generatePhotoColorScheme("#00FFFFFF", isDark = true))
    }

    @Test
    fun generatePhotoColorSchemeSupportsThreeDigitHex() {
        val threeDigit = generatePhotoColorScheme("#EAB", isDark = true)
        val sixDigit = generatePhotoColorScheme("#EEAABB", isDark = true)
        assertEquals(sixDigit.primary, threeDigit.primary)
        assertEquals(sixDigit.surface, threeDigit.surface)
    }

    @Test
    fun generatePhotoColorSchemeSetsBackgroundMatchingSurface() {
        val darkScheme = generatePhotoColorScheme("#EAA83B", isDark = true)
        assertEquals(darkScheme.surface, darkScheme.background)

        val lightScheme = generatePhotoColorScheme("#EAA83B", isDark = false)
        assertEquals(lightScheme.surface, lightScheme.background)
    }
}
