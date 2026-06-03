package com.example.imagefeed.android.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

object BlurHashDecoder {
    private val charMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"
        .mapIndexed { index, c -> c to index }.toMap()

    fun decode(blurHash: String?, width: Int, height: Int, punch: Float = 1.0f): Bitmap? {
        if (blurHash == null || blurHash.length < 6) return null

        try {
            val numComp = decode83(blurHash, 0, 1)
            val yComponents = numComp / 9 + 1
            val xComponents = numComp % 9 + 1

            if (blurHash.length != 4 + 2 * xComponents * yComponents) return null

            val maxAc = (decode83(blurHash, 1, 2) + 1) / 166f * punch
            val colors = Array(xComponents * yComponents) { i ->
                if (i == 0) {
                    val color = decode83(blurHash, 2, 6)
                    decodeDc(color)
                } else {
                    val color = decode83(blurHash, 6 + (i - 1) * 2, 6 + i * 2)
                    decodeAc(color, maxAc)
                }
            }

            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var r = 0f
                    var g = 0f
                    var b = 0f
                    for (j in 0 until yComponents) {
                        for (i in 0 until xComponents) {
                            val basis = (cos(Math.PI * x * i / width) * cos(Math.PI * y * j / height)).toFloat()
                            val color = colors[i + j * xComponents]
                            r += color[0] * basis
                            g += color[1] * basis
                            b += color[2] * basis
                        }
                    }
                    val rInt = clamp((linearToSrgb(r) * 255).toInt())
                    val gInt = clamp((linearToSrgb(g) * 255).toInt())
                    val bInt = clamp((linearToSrgb(b) * 255).toInt())
                    pixels[x + y * width] = Color.rgb(rInt, gInt, bInt)
                }
            }

            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun decode83(str: String, start: Int, end: Int): Int {
        var value = 0
        for (i in start until end) {
            val c = str[i]
            val digit = charMap[c] ?: 0
            value = value * 83 + digit
        }
        return value
    }

    private fun decodeDc(value: Int): FloatArray {
        val r = value shr 16
        val g = (value shr 8) and 255
        val b = value and 255
        return floatArrayOf(srgbToLinear(r / 255f), srgbToLinear(g / 255f), srgbToLinear(b / 255f))
    }

    private fun decodeAc(value: Int, maxAc: Float): FloatArray {
        val r = value / (19 * 19)
        val g = (value / 19) % 19
        val b = value % 19
        return floatArrayOf(
            signedPower3((r - 9) / 9f) * maxAc,
            signedPower3((g - 9) / 9f) * maxAc,
            signedPower3((b - 9) / 9f) * maxAc
        )
    }

    private fun srgbToLinear(value: Float): Float {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(value: Float): Float {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
    }

    private fun signedPower3(value: Float): Float {
        return value.pow(3f).withSign(value)
    }

    private fun clamp(value: Int): Int {
        return value.coerceIn(0, 255)
    }
}
