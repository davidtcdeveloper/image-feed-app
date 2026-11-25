package com.example.imagefeed.model.cache

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NavigationPreviewCacheTest {
    @Test
    fun storesAndRetrievesPhotoCollectionAndUserByKey() {
        val cache = NavigationPreviewCache()
        val photo = testPhoto("p-1")
        val collection = testCollection("c-1")
        val user = testUser("alice")

        cache.seedPhoto(photo)
        cache.seedCollection(collection)
        cache.seedUser(user)

        assertEquals(photo, cache.getPhoto("p-1"))
        assertEquals(collection, cache.getCollection("c-1"))
        assertEquals(user, cache.getUser("alice"))
        assertNull(cache.getPhoto("missing"))
        assertNull(cache.getCollection("missing"))
        assertNull(cache.getUser("missing"))
    }

    @Test
    fun evictsLeastRecentlyUsedPhotoAtDefaultSixteenEntryLimit() {
        val cache = NavigationPreviewCache()
        for (i in 1..16) {
            cache.seedPhoto(testPhoto("p-$i"))
        }

        // Insert 17th entry; p-1 is eldest and must be evicted
        cache.seedPhoto(testPhoto("p-17"))

        assertNull(cache.getPhoto("p-1"))
        for (i in 2..17) {
            assertEquals("p-$i", cache.getPhoto("p-$i")?.id)
        }
    }

    @Test
    fun readingEntryPromotesItBeforeLruEviction() {
        val cache = NavigationPreviewCache()
        for (i in 1..16) {
            cache.seedPhoto(testPhoto("p-$i"))
        }

        // Promote p-1 so p-2 becomes the eldest entry
        assertEquals("p-1", cache.getPhoto("p-1")?.id)
        cache.seedPhoto(testPhoto("p-17"))

        assertEquals("p-1", cache.getPhoto("p-1")?.id)
        assertNull(cache.getPhoto("p-2"))
    }

    @Test
    fun reseedingExistingKeyUpdatesValueWithoutEvictingOtherEntries() {
        val cache = NavigationPreviewCache()
        for (i in 1..16) {
            cache.seedPhoto(testPhoto("p-$i"))
        }

        val updated = testPhoto("p-1").copy(description = "updated")
        cache.seedPhoto(updated)

        assertEquals("updated", cache.getPhoto("p-1")?.description)
        assertEquals("p-2", cache.getPhoto("p-2")?.id)
    }

    @Test
    fun enforcesIndependentCapacityForCollectionsAndUsersAndClearsAllStores() {
        val cache = NavigationPreviewCache()
        for (i in 1..17) {
            cache.seedCollection(testCollection("c-$i"))
            cache.seedUser(testUser("user-$i"))
        }

        assertNull(cache.getCollection("c-1"))
        assertEquals("c-2", cache.getCollection("c-2")?.id)
        assertNull(cache.getUser("user-1"))
        assertEquals("user-2", cache.getUser("user-2")?.username)

        cache.clear()
        assertNull(cache.getCollection("c-17"))
        assertNull(cache.getUser("user-17"))
    }

    @Test
    fun rejectsNonPositiveMaxEntries() {
        assertFailsWith<IllegalArgumentException> { NavigationPreviewCache(0) }
        assertFailsWith<IllegalArgumentException> { NavigationPreviewCache(-3) }
    }

    private fun testUser(username: String) =
        User(
            id = "id-$username",
            username = username,
            name = "Name $username",
            profileImage = ProfileImage("small", "medium", "large"),
            links = UserLinks("self", "html", "photos"),
        )

    private fun testPhoto(id: String) =
        Photo(
            id = id,
            width = 100,
            height = 100,
            urls = PhotoUrls("raw", "full", "regular", "small", "thumb"),
            user = testUser("u-$id"),
            links = PhotoLinks("self", "html", "download", "download-$id"),
        )

    private fun testCollection(id: String) =
        PhotoCollection(
            id = id,
            title = "Collection $id",
            totalPhotos = 10,
            user = testUser("owner-$id"),
        )
}
