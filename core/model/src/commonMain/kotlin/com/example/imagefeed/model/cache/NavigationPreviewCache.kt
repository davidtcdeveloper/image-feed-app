package com.example.imagefeed.model.cache

import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.User

/**
 * Ephemeral in-memory LRU cache storing metadata DTOs (`Photo`, `PhotoCollection`, `User`)
 * strictly for Frame-0 navigation seeding without persisting image binaries or disk files.
 */
class NavigationPreviewCache(
    maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive, got $maxEntries" }
    }

    private val lock: CacheLock = createCacheLock()
    private val photos = LruStore<Photo>(maxEntries)
    private val collections = LruStore<PhotoCollection>(maxEntries)
    private val users = LruStore<User>(maxEntries)

    fun seedPhoto(photo: Photo) {
        lock.withLock {
            photos.put(photo.id, photo)
        }
    }

    fun getPhoto(photoId: String): Photo? =
        lock.withLock {
            photos.get(photoId)
        }

    fun seedCollection(collection: PhotoCollection) {
        lock.withLock {
            collections.put(collection.id, collection)
        }
    }

    fun getCollection(collectionId: String): PhotoCollection? =
        lock.withLock {
            collections.get(collectionId)
        }

    fun seedUser(user: User) {
        lock.withLock {
            users.put(user.username, user)
        }
    }

    fun getUser(username: String): User? =
        lock.withLock {
            users.get(username)
        }

    fun clear() {
        lock.withLock {
            photos.clear()
            collections.clear()
            users.clear()
        }
    }

    private class LruStore<V>(
        private val maxEntries: Int,
    ) {
        private val map = LinkedHashMap<String, V>()

        fun put(
            key: String,
            value: V,
        ) {
            map.remove(key)
            map[key] = value
            while (map.size > maxEntries) {
                val eldestKey = map.keys.firstOrNull() ?: break
                map.remove(eldestKey)
            }
        }

        fun get(key: String): V? {
            val value = map.remove(key) ?: return null
            map[key] = value
            return value
        }

        fun clear() {
            map.clear()
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 16
    }
}
