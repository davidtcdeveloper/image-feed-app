package com.example.imagefeed.android.util

import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

const val BLURHASH_PLACEHOLDER_WIDTH = 32
const val BLURHASH_PLACEHOLDER_MIN_HEIGHT = 1
const val BLURHASH_PLACEHOLDER_MAX_HEIGHT = 128

object BlurHashDecoder {
    private const val DEFAULT_CACHE_SIZE = 256
    internal const val MAX_DIMENSION = 512

    private val cache = LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE)
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()

    internal var bitmapCreator: (pixels: IntArray, width: Int, height: Int) -> Bitmap = { pixels, width, height ->
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    internal fun resetBitmapCreator() {
        bitmapCreator = { pixels, width, height ->
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }

    fun clearCache() {
        inFlight.clear()
        cache.evictAll()
    }

    fun cacheSize(): Int = cache.size()

    fun resizeCache(maxSize: Int) {
        cache.resize(maxSize)
    }

    fun cacheKey(
        blurHash: String,
        width: Int,
        height: Int,
        punch: Float = 1.0f,
    ): String {
        val actualPunch = if (punch <= 0f || !punch.isFinite()) 1.0f else punch
        return "${blurHash}_${width}x${height}_$actualPunch"
    }

    fun getCached(
        blurHash: String?,
        width: Int,
        height: Int,
        punch: Float = 1.0f,
    ): Bitmap? {
        if (blurHash == null ||
            blurHash.length < 6 ||
            width <= 0 ||
            height <= 0 ||
            width > MAX_DIMENSION ||
            height > MAX_DIMENSION
        ) {
            return null
        }
        return cache.get(cacheKey(blurHash, width, height, punch))
    }

    fun decode(
        blurHash: String?,
        width: Int,
        height: Int,
        punch: Float = 1.0f,
    ): Bitmap? {
        if (blurHash == null ||
            blurHash.length < 6 ||
            width <= 0 ||
            height <= 0 ||
            width > MAX_DIMENSION ||
            height > MAX_DIMENSION
        ) {
            return null
        }

        for (i in blurHash.indices) {
            if (!charMap.containsKey(blurHash[i])) return null
        }

        val actualPunch = if (punch <= 0f || !punch.isFinite()) 1.0f else punch

        val cached = getCached(blurHash, width, height, actualPunch)
        if (cached != null) return cached

        return try {
            val numComp = decode83(blurHash, 0, 1)
            val yComponents = numComp / 9 + 1
            val xComponents = numComp % 9 + 1

            if (blurHash.length != 4 + 2 * xComponents * yComponents) return null

            val maxAc = (decode83(blurHash, 1, 2) + 1) / 166f * actualPunch
            val colors =
                Array(xComponents * yComponents) { i ->
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
                    pixels[x + y * width] = (0xFF shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                }
            }

            val bitmap = bitmapCreator(pixels, width, height)
            cache.put(cacheKey(blurHash, width, height, actualPunch), bitmap)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeAsync(
        blurHash: String?,
        width: Int,
        height: Int,
        punch: Float = 1.0f,
    ): Bitmap? {
        val actualPunch = if (punch <= 0f || !punch.isFinite()) 1.0f else punch
        val cached = getCached(blurHash, width, height, actualPunch)
        if (cached != null) return cached

        if (blurHash == null ||
            blurHash.length < 6 ||
            width <= 0 ||
            height <= 0 ||
            width > MAX_DIMENSION ||
            height > MAX_DIMENSION
        ) {
            return null
        }

        val key = cacheKey(blurHash, width, height, actualPunch)
        var isLeader = false
        val deferred =
            inFlight.computeIfAbsent(key) {
                isLeader = true
                CompletableDeferred()
            }

        return if (isLeader) {
            try {
                val result =
                    withContext(Dispatchers.Default) {
                        decode(blurHash, width, height, actualPunch)
                    }
                deferred.complete(result)
                result
            } catch (t: Throwable) {
                deferred.completeExceptionally(t)
                throw t
            } finally {
                inFlight.remove(key)
            }
        } else {
            try {
                deferred.await()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                decodeAsync(blurHash, width, height, actualPunch)
            }
        }
    }
}

private val charMap =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"
        .mapIndexed { index, c -> c to index }
        .toMap()

private fun decode83(
    str: String,
    start: Int,
    end: Int,
): Int {
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

private fun decodeAc(
    value: Int,
    maxAc: Float,
): FloatArray {
    val r = value / (19 * 19)
    val g = (value / 19) % 19
    val b = value % 19
    return floatArrayOf(
        signedPower3((r - 9) / 9f) * maxAc,
        signedPower3((g - 9) / 9f) * maxAc,
        signedPower3((b - 9) / 9f) * maxAc,
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

private fun signedPower3(value: Float): Float = value.pow(3f).withSign(value)

private fun clamp(value: Int): Int = value.coerceIn(0, 255)

@Composable
fun rememberBlurHashPlaceholder(
    blurHash: String?,
    width: Int,
    height: Int,
    punch: Float = 1.0f,
): Bitmap? {
    var placeholderBitmap by remember(blurHash, width, height, punch) {
        mutableStateOf(BlurHashDecoder.getCached(blurHash, width, height, punch))
    }

    LaunchedEffect(blurHash, width, height, punch) {
        if (placeholderBitmap == null &&
            blurHash != null &&
            blurHash.length >= 6 &&
            width in 1..BlurHashDecoder.MAX_DIMENSION &&
            height in 1..BlurHashDecoder.MAX_DIMENSION
        ) {
            placeholderBitmap = BlurHashDecoder.decodeAsync(blurHash, width, height, punch)
        }
    }

    return placeholderBitmap
}
