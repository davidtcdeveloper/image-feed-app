package com.example.imagefeed.model.color

import kotlin.math.abs
import kotlin.math.roundToInt

data class HexRgb(
    val r: Int,
    val g: Int,
    val b: Int,
    val argb: Int,
)

data class PhotoTonalSwatch(
    val hex: String,
    val rgb: HexRgb,
)

object PhotoColorEngine {
    private const val MIN_ALPHA = 16
    private const val HEX_SHORT_LENGTH = 3
    private const val HEX_RGB_LENGTH = 6
    private const val HEX_ARGB_LENGTH = 8
    private const val HEX_RADIX = 16
    private const val OPAQUE_ALPHA_MASK = 0xFF000000L
    private const val ALPHA_BYTE = 0xFF
    private const val BYTE_MASK = 0xFF
    private const val SHIFT_ALPHA = 24
    private const val SHIFT_RED = 16
    private const val SHIFT_GREEN = 8
    private const val MAX_CHANNEL_FLOAT = 255f
    private const val ACHROMATIC_SATURATION_THRESHOLD = 0.05f
    private const val MIN_SWATCH_SATURATION = 0.40f
    private const val DARK_ACCENT_MIN_SATURATION = 0.55f
    private const val DARK_ACCENT_LUMINANCE = 0.70f
    private const val LIGHT_ACCENT_MIN_SATURATION = 0.60f
    private const val LIGHT_ACCENT_LUMINANCE = 0.35f
    private const val FULL_CIRCLE_DEGREES = 360f
    private const val HUE_SECTOR_DEGREES = 60f
    private const val HALF_LUMINANCE = 0.5f
    private const val GREEN_HUE_OFFSET = 2f
    private const val BLUE_HUE_OFFSET = 4f
    private const val HUE_MOD_PERIOD = 6f
    private const val SECONDARY_MOD_PERIOD = 2f
    private const val HEX_PAD_LENGTH = 2
    private const val HUE_SECTOR_0 = 0
    private const val HUE_SECTOR_1 = 1
    private const val HUE_SECTOR_2 = 2
    private const val HUE_SECTOR_3 = 3
    private const val HUE_SECTOR_4 = 4
    private const val TONAL_LUM_1 = 0.35f
    private const val TONAL_LUM_2 = 0.50f
    private const val TONAL_LUM_3 = 0.65f
    private const val TONAL_LUM_4 = 0.75f
    private const val TONAL_LUM_5 = 0.85f

    private val tonalLuminanceSteps = listOf(TONAL_LUM_1, TONAL_LUM_2, TONAL_LUM_3, TONAL_LUM_4, TONAL_LUM_5)

    fun parseHexRgb(seedHex: String?): HexRgb? {
        val hex = seedHex?.trim()?.removePrefix("#")?.trim()
        val isValidHex =
            !hex.isNullOrBlank() &&
                hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        val argb =
            if (!isValidHex) {
                null
            } else {
                when (hex.length) {
                    HEX_SHORT_LENGTH -> {
                        val expanded = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                        (OPAQUE_ALPHA_MASK or expanded.toLong(HEX_RADIX)).toInt()
                    }
                    HEX_RGB_LENGTH -> (OPAQUE_ALPHA_MASK or hex.toLong(HEX_RADIX)).toInt()
                    HEX_ARGB_LENGTH -> {
                        val parsed = hex.toLong(HEX_RADIX).toInt()
                        val alpha = (parsed ushr SHIFT_ALPHA) and BYTE_MASK
                        if (alpha < MIN_ALPHA) null else parsed
                    }
                    else -> null
                }
            }
        return argb?.let { value ->
            HexRgb(
                r = (value ushr SHIFT_RED) and BYTE_MASK,
                g = (value ushr SHIFT_GREEN) and BYTE_MASK,
                b = value and BYTE_MASK,
                argb = value,
            )
        }
    }

    fun deriveTonalSwatches(seedHex: String?): List<PhotoTonalSwatch> {
        val rgb = parseHexRgb(seedHex) ?: return emptyList()
        val hsl =
            rgbToHsl(
                r = rgb.r / MAX_CHANNEL_FLOAT,
                g = rgb.g / MAX_CHANNEL_FLOAT,
                b = rgb.b / MAX_CHANNEL_FLOAT,
            )
        val hue = hsl[0]
        val saturation = hsl[1]

        // Neutral seeds stay achromatic (s = 0) to avoid arbitrary red tinting
        val effectiveSaturation =
            if (saturation < ACHROMATIC_SATURATION_THRESHOLD) {
                0f
            } else {
                saturation.coerceAtLeast(MIN_SWATCH_SATURATION)
            }

        return tonalLuminanceSteps.map { lum ->
            val swatchRgb = hslToRgb(hue, effectiveSaturation, lum)
            PhotoTonalSwatch(
                hex = rgbToHex(swatchRgb.r, swatchRgb.g, swatchRgb.b),
                rgb = swatchRgb,
            )
        }
    }

    fun deriveAccentHex(
        seedHex: String?,
        isDark: Boolean,
    ): String? {
        val rgb = parseHexRgb(seedHex)
        val hsl =
            rgb?.let {
                rgbToHsl(
                    r = it.r / MAX_CHANNEL_FLOAT,
                    g = it.g / MAX_CHANNEL_FLOAT,
                    b = it.b / MAX_CHANNEL_FLOAT,
                )
            }
        val saturation = hsl?.get(1) ?: 0f
        return if (hsl == null || saturation < ACHROMATIC_SATURATION_THRESHOLD) {
            null
        } else {
            val targetSaturation =
                if (isDark) {
                    saturation.coerceAtLeast(DARK_ACCENT_MIN_SATURATION)
                } else {
                    saturation.coerceAtLeast(LIGHT_ACCENT_MIN_SATURATION)
                }
            val targetLuminance = if (isDark) DARK_ACCENT_LUMINANCE else LIGHT_ACCENT_LUMINANCE
            val accentRgb = hslToRgb(hsl[0], targetSaturation, targetLuminance)
            rgbToHex(accentRgb.r, accentRgb.g, accentRgb.b)
        }
    }

    fun rgbToHsl(
        r: Float,
        g: Float,
        b: Float,
    ): FloatArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2f
        val s =
            when {
                delta == 0f -> 0f
                l <= HALF_LUMINANCE -> delta / (max + min)
                else -> delta / (2f - max - min)
            }
        val h =
            when {
                delta == 0f -> 0f
                max == r -> ((g - b) / delta % HUE_MOD_PERIOD) * HUE_SECTOR_DEGREES
                max == g -> ((b - r) / delta + GREEN_HUE_OFFSET) * HUE_SECTOR_DEGREES
                else -> ((r - g) / delta + BLUE_HUE_OFFSET) * HUE_SECTOR_DEGREES
            }.let { if (it < 0f) it + FULL_CIRCLE_DEGREES else it }
        return floatArrayOf(h, s, l)
    }

    fun hslToRgb(
        h: Float,
        s: Float,
        l: Float,
    ): HexRgb {
        val normalizedH = ((h % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((normalizedH / HUE_SECTOR_DEGREES) % SECONDARY_MOD_PERIOD - 1f))
        val m = l - c / 2f
        val (rPrime, gPrime, bPrime) =
            when ((normalizedH / HUE_SECTOR_DEGREES).toInt()) {
                HUE_SECTOR_0 -> Triple(c, x, 0f)
                HUE_SECTOR_1 -> Triple(x, c, 0f)
                HUE_SECTOR_2 -> Triple(0f, c, x)
                HUE_SECTOR_3 -> Triple(0f, x, c)
                HUE_SECTOR_4 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        val r = ((rPrime + m).coerceIn(0f, 1f) * MAX_CHANNEL_FLOAT).roundToInt().coerceIn(0, BYTE_MASK)
        val g = ((gPrime + m).coerceIn(0f, 1f) * MAX_CHANNEL_FLOAT).roundToInt().coerceIn(0, BYTE_MASK)
        val b = ((bPrime + m).coerceIn(0f, 1f) * MAX_CHANNEL_FLOAT).roundToInt().coerceIn(0, BYTE_MASK)
        val argb = (ALPHA_BYTE shl SHIFT_ALPHA) or (r shl SHIFT_RED) or (g shl SHIFT_GREEN) or b
        return HexRgb(r = r, g = g, b = b, argb = argb)
    }

    fun rgbToHex(
        r: Int,
        g: Int,
        b: Int,
    ): String {
        val hexR =
            r
                .coerceIn(0, BYTE_MASK)
                .toString(HEX_RADIX)
                .uppercase()
                .padStart(HEX_PAD_LENGTH, '0')
        val hexG =
            g
                .coerceIn(0, BYTE_MASK)
                .toString(HEX_RADIX)
                .uppercase()
                .padStart(HEX_PAD_LENGTH, '0')
        val hexB =
            b
                .coerceIn(0, BYTE_MASK)
                .toString(HEX_RADIX)
                .uppercase()
                .padStart(HEX_PAD_LENGTH, '0')
        return "#$hexR$hexG$hexB"
    }
}
