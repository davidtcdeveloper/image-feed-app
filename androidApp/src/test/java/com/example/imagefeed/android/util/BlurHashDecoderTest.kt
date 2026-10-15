package com.example.imagefeed.android.util

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class BlurHashDecoderTest {
    private val validHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj"
    private var creationCount = 0

    @BeforeTest
    fun setUp() {
        BlurHashDecoder.clearCache()
        creationCount = 0

        BlurHashDecoder.bitmapCreator = { _, _, _ ->
            creationCount++
            createTestBitmap()
        }
    }

    private fun createTestBitmap(): Bitmap {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstance.invoke(unsafe, Bitmap::class.java) as Bitmap
    }

    @AfterTest
    fun tearDown() {
        BlurHashDecoder.clearCache()
        BlurHashDecoder.resizeCache(256)
        BlurHashDecoder.resetBitmapCreator()
    }

    @Test
    fun safeHandlingOfNullEmptyOrCorruptedBlurHash() {
        assertNull(BlurHashDecoder.decode(null, 32, 20))
        assertNull(BlurHashDecoder.decode("", 32, 20))
        assertNull(BlurHashDecoder.decode("abc", 32, 20))
        assertNull(BlurHashDecoder.decode("invalid_length_blurhash", 32, 20))
        assertNull(BlurHashDecoder.decode("      ", 32, 20))
        assertNull(BlurHashDecoder.decode("!!!!!!", 32, 20))
        assertNull(BlurHashDecoder.decode("LEHV6nWB2yk8pyo!adR*.7kCMdnj", 32, 20))

        assertNull(BlurHashDecoder.getCached(null, 32, 20))
        assertNull(BlurHashDecoder.getCached("", 32, 20))
        assertNull(BlurHashDecoder.getCached("abc", 32, 20))
        assertNull(BlurHashDecoder.getCached("      ", 32, 20))
        assertEquals(0, creationCount)
    }

    @Test
    fun boundaryDimensionHandlingReturnsNullSafely() {
        assertNull(BlurHashDecoder.decode(validHash, 0, 20))
        assertNull(BlurHashDecoder.decode(validHash, 32, 0))
        assertNull(BlurHashDecoder.decode(validHash, -10, 20))
        assertNull(BlurHashDecoder.decode(validHash, 32, -5))
        assertNull(BlurHashDecoder.decode(validHash, -1, -1))
        assertNull(BlurHashDecoder.decode(validHash, 513, 20))
        assertNull(BlurHashDecoder.decode(validHash, 32, 513))

        assertNull(BlurHashDecoder.getCached(validHash, 0, 20))
        assertNull(BlurHashDecoder.getCached(validHash, 32, 0))
        assertNull(BlurHashDecoder.getCached(validHash, -10, 20))
        assertNull(BlurHashDecoder.getCached(validHash, 32, -5))
        assertNull(BlurHashDecoder.getCached(validHash, 513, 20))
        assertNull(BlurHashDecoder.getCached(validHash, 32, 513))
        assertEquals(0, creationCount)
    }

    @Test
    fun synchronousCacheHitsOnRepeatedCallsWithIdenticalKeys() {
        assertNull(BlurHashDecoder.getCached(validHash, 32, 20))

        val firstBitmap = BlurHashDecoder.decode(validHash, 32, 20)
        assertNotNull(firstBitmap)
        assertEquals(1, creationCount)

        // Immediate synchronous getCached hit
        val cachedBitmap = BlurHashDecoder.getCached(validHash, 32, 20)
        assertNotNull(cachedBitmap)
        assertSame(firstBitmap, cachedBitmap)
        assertEquals(1, creationCount)

        // Subsequent decode call hits cache without invoking creator
        val secondBitmap = BlurHashDecoder.decode(validHash, 32, 20)
        assertNotNull(secondBitmap)
        assertSame(firstBitmap, secondBitmap)
        assertEquals(1, creationCount)
    }

    @Test
    fun keyIsolationAcrossDifferingAspectRatiosAndResolutions() {
        val bitmap32x20 = BlurHashDecoder.decode(validHash, 32, 20)
        assertNotNull(bitmap32x20)
        assertEquals(1, creationCount)

        // Same hash, different height (different aspect ratio)
        assertNull(BlurHashDecoder.getCached(validHash, 32, 18))
        val bitmap32x18 = BlurHashDecoder.decode(validHash, 32, 18)
        assertNotNull(bitmap32x18)
        assertEquals(2, creationCount)

        // Same hash, different resolution (e.g. 16x12)
        assertNull(BlurHashDecoder.getCached(validHash, 16, 12))
        val bitmap16x12 = BlurHashDecoder.decode(validHash, 16, 12)
        assertNotNull(bitmap16x12)
        assertEquals(3, creationCount)

        // Different punch level
        assertNull(BlurHashDecoder.getCached(validHash, 32, 20, punch = 1.5f))
        val bitmapWithPunch = BlurHashDecoder.decode(validHash, 32, 20, punch = 1.5f)
        assertNotNull(bitmapWithPunch)
        assertEquals(4, creationCount)

        // Original 32x20 remains intact
        assertSame(bitmap32x20, BlurHashDecoder.getCached(validHash, 32, 20))
    }

    @Test
    fun lruEvictionBehaviorUnderSimulatedCachePressure() {
        BlurHashDecoder.resizeCache(3)

        // Insert 3 entries with different dimensions
        val bmp1 = BlurHashDecoder.decode(validHash, 10, 10)
        val bmp2 = BlurHashDecoder.decode(validHash, 20, 20)
        val bmp3 = BlurHashDecoder.decode(validHash, 30, 30)
        assertNotNull(bmp2)
        assertEquals(3, BlurHashDecoder.cacheSize())

        // Access bmp1 so that access order becomes: bmp2 (least recently used), bmp3, bmp1
        assertSame(bmp1, BlurHashDecoder.getCached(validHash, 10, 10))

        // Insert 4th entry -> bmp2 should be evicted!
        val bmp4 = BlurHashDecoder.decode(validHash, 40, 40)
        assertEquals(3, BlurHashDecoder.cacheSize())

        assertSame(bmp1, BlurHashDecoder.getCached(validHash, 10, 10))
        assertNull(BlurHashDecoder.getCached(validHash, 20, 20)) // evicted!
        assertSame(bmp3, BlurHashDecoder.getCached(validHash, 30, 30))
        assertSame(bmp4, BlurHashDecoder.getCached(validHash, 40, 40))
    }

    @Test
    fun asyncDecodingOffloadsAndPopulatesCache() =
        runTest {
            assertNull(BlurHashDecoder.getCached(validHash, 32, 20))

            val bitmap = BlurHashDecoder.decodeAsync(validHash, 32, 20)
            assertNotNull(bitmap)
            assertEquals(1, creationCount)

            // Cache is populated
            val cached = BlurHashDecoder.getCached(validHash, 32, 20)
            assertSame(bitmap, cached)
        }

    @Test
    fun punchNormalizationAcrossZeroNegativeAndNonFiniteValues() {
        val bmpDefault = BlurHashDecoder.decode(validHash, 32, 20, punch = 1.0f)
        assertNotNull(bmpDefault)
        assertEquals(1, creationCount)

        // 0.0f, -1.0f, and NaN should normalize to 1.0f and hit cache
        val bmpZero = BlurHashDecoder.decode(validHash, 32, 20, punch = 0.0f)
        assertSame(bmpDefault, bmpZero)

        val bmpNegative = BlurHashDecoder.decode(validHash, 32, 20, punch = -2.5f)
        assertSame(bmpDefault, bmpNegative)

        val bmpNaN = BlurHashDecoder.decode(validHash, 32, 20, punch = Float.NaN)
        assertSame(bmpDefault, bmpNaN)

        assertEquals(1, creationCount)
    }

    @Test
    fun concurrentDecodingIsThreadSafe() =
        runTest {
            val deferreds =
                (1..30).map {
                    async(Dispatchers.Default) {
                        BlurHashDecoder.decodeAsync(validHash, 32, 20)
                    }
                }
            val results = deferreds.awaitAll()
            results.forEach { assertNotNull(it) }
            val first = results.first()
            results.forEach { assertSame(first, it) }
        }
}
