package com.example.imagefeed.model.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoColorEngineTest {
    @Test
    fun parseHexRgbParsesValidThreeSixAndEightDigitHexes() {
        val sixDigit = PhotoColorEngine.parseHexRgb("  #2e6be6 ")
        assertNotNull(sixDigit)
        assertEquals(0x2E, sixDigit.r)
        assertEquals(0x6B, sixDigit.g)
        assertEquals(0xE6, sixDigit.b)
        assertEquals(0xFF2E6BE6.toInt(), sixDigit.argb)

        val bareSixDigit = PhotoColorEngine.parseHexRgb("1a2b3c")
        assertNotNull(bareSixDigit)
        assertEquals(0x1A, bareSixDigit.r)
        assertEquals(0x2B, bareSixDigit.g)
        assertEquals(0x3C, bareSixDigit.b)
        assertEquals(0xFF1A2B3C.toInt(), bareSixDigit.argb)

        val threeDigit = PhotoColorEngine.parseHexRgb("#F0a")
        assertNotNull(threeDigit)
        assertEquals(0xFF, threeDigit.r)
        assertEquals(0x00, threeDigit.g)
        assertEquals(0xAA, threeDigit.b)
        assertEquals(0xFFFF00AA.toInt(), threeDigit.argb)

        val eightDigitOpaque = PhotoColorEngine.parseHexRgb("#10336699")
        assertNotNull(eightDigitOpaque)
        assertEquals(0x33, eightDigitOpaque.r)
        assertEquals(0x66, eightDigitOpaque.g)
        assertEquals(0x99, eightDigitOpaque.b)
        assertEquals(0x10336699, eightDigitOpaque.argb)
    }

    @Test
    fun parseHexRgbRejectsInvalidAndLowAlphaInputs() {
        assertNull(PhotoColorEngine.parseHexRgb(null))
        assertNull(PhotoColorEngine.parseHexRgb(""))
        assertNull(PhotoColorEngine.parseHexRgb("   "))
        assertNull(PhotoColorEngine.parseHexRgb("#"))
        assertNull(PhotoColorEngine.parseHexRgb("#12345"))
        assertNull(PhotoColorEngine.parseHexRgb("#1234567"))
        assertNull(PhotoColorEngine.parseHexRgb("#ZZZZZZ"))
        assertNull(PhotoColorEngine.parseHexRgb("#0F336699"))
        assertNull(PhotoColorEngine.parseHexRgb("#00FFFFFF"))
    }

    @Test
    fun deriveTonalSwatchesProducesFiveAscendingLuminanceSwatchesForChromaticSeed() {
        val swatches = PhotoColorEngine.deriveTonalSwatches("#2E6BE6")

        assertEquals(5, swatches.size)
        val luminances =
            swatches.map { swatch ->
                assertEquals(
                    PhotoColorEngine.rgbToHex(swatch.rgb.r, swatch.rgb.g, swatch.rgb.b),
                    swatch.hex,
                )
                PhotoColorEngine.rgbToHsl(
                    swatch.rgb.r / 255f,
                    swatch.rgb.g / 255f,
                    swatch.rgb.b / 255f,
                )[2]
            }
        assertTrue(luminances.zipWithNext().all { (lower, higher) -> lower < higher })
    }

    @Test
    fun deriveTonalSwatchesPreservesNeutralGrayForAchromaticSeeds() {
        val neutralSeeds = listOf("#000000", "#FFFFFF", "#808080", "#7F8081")
        neutralSeeds.forEach { seed ->
            val swatches = PhotoColorEngine.deriveTonalSwatches(seed)
            assertEquals(5, swatches.size, "Expected 5 swatches for $seed")
            swatches.forEach { swatch ->
                assertEquals(swatch.rgb.r, swatch.rgb.g, "Expected r == g for neutral $seed")
                assertEquals(swatch.rgb.g, swatch.rgb.b, "Expected g == b for neutral $seed")
            }
        }
    }

    @Test
    fun deriveTonalSwatchesReturnsEmptyListForInvalidInputs() {
        assertTrue(PhotoColorEngine.deriveTonalSwatches(null).isEmpty())
        assertTrue(PhotoColorEngine.deriveTonalSwatches("not-hex").isEmpty())
        assertTrue(PhotoColorEngine.deriveTonalSwatches("#08FF0000").isEmpty())
    }

    @Test
    fun deriveAccentHexHandlesChromaticAndAchromaticSeedsAcrossLightAndDarkModes() {
        val lightAccent = PhotoColorEngine.deriveAccentHex("#2E6BE6", isDark = false)
        val darkAccent = PhotoColorEngine.deriveAccentHex("#2E6BE6", isDark = true)

        assertNotNull(lightAccent)
        assertNotNull(darkAccent)
        assertTrue(lightAccent.startsWith("#") && lightAccent.length == 7)
        assertTrue(darkAccent.startsWith("#") && darkAccent.length == 7)

        val lightRgb = PhotoColorEngine.parseHexRgb(lightAccent)
        val darkRgb = PhotoColorEngine.parseHexRgb(darkAccent)
        assertNotNull(lightRgb)
        assertNotNull(darkRgb)

        val lightLum = PhotoColorEngine.rgbToHsl(lightRgb.r / 255f, lightRgb.g / 255f, lightRgb.b / 255f)[2]
        val darkLum = PhotoColorEngine.rgbToHsl(darkRgb.r / 255f, darkRgb.g / 255f, darkRgb.b / 255f)[2]
        assertTrue(darkLum > lightLum, "Dark mode accent should be lighter than light mode accent")

        assertNull(PhotoColorEngine.deriveAccentHex("#000000", isDark = false))
        assertNull(PhotoColorEngine.deriveAccentHex("#FFFFFF", isDark = true))
        assertNull(PhotoColorEngine.deriveAccentHex("#808080", isDark = false))
        assertNull(PhotoColorEngine.deriveAccentHex("#052E6BE6", isDark = true))
        assertNull(PhotoColorEngine.deriveAccentHex(null, isDark = false))
    }

    @Test
    fun rgbAndHslConversionsRoundTripPrimaryColors() {
        val primaryRedHsl = PhotoColorEngine.rgbToHsl(1f, 0f, 0f)
        assertEquals(0f, primaryRedHsl[0], 0.01f)
        assertEquals(1f, primaryRedHsl[1], 0.01f)
        assertEquals(0.5f, primaryRedHsl[2], 0.01f)

        val roundTrip = PhotoColorEngine.hslToRgb(primaryRedHsl[0], primaryRedHsl[1], primaryRedHsl[2])
        assertEquals("#FF0000", PhotoColorEngine.rgbToHex(roundTrip.r, roundTrip.g, roundTrip.b))

        val negativeHueWrap = PhotoColorEngine.hslToRgb(-60f, 1f, 0.5f)
        assertEquals("#FF00FF", PhotoColorEngine.rgbToHex(negativeHueWrap.r, negativeHueWrap.g, negativeHueWrap.b))
    }
}
