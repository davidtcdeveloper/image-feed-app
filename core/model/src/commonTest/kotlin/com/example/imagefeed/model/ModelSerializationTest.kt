package com.example.imagefeed.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }

    @Test
    fun photoSerializationRoundtrip() {
        val photo =
            Photo(
                id = "photo_123",
                createdAt = "2026-01-01T00:00:00Z",
                width = 1920,
                height = 1080,
                color = "#000000",
                blurHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj",
                description = "A beautiful scenery",
                altDescription = "Scenic view",
                urls =
                    PhotoUrls(
                        raw = "https://example.com/raw.jpg",
                        full = "https://example.com/full.jpg",
                        regular = "https://example.com/regular.jpg",
                        small = "https://example.com/small.jpg",
                        thumb = "https://example.com/thumb.jpg",
                    ),
                user =
                    User(
                        id = "user_1",
                        username = "johndoe",
                        name = "John Doe",
                        profileImage =
                            ProfileImage(
                                small = "https://example.com/s.jpg",
                                medium = "https://example.com/m.jpg",
                                large = "https://example.com/l.jpg",
                            ),
                        links =
                            UserLinks(
                                self = "https://api.unsplash.com/users/johndoe",
                                html = "https://unsplash.com/@johndoe",
                                photos = "https://api.unsplash.com/users/johndoe/photos",
                            ),
                    ),
                links =
                    PhotoLinks(
                        self = "https://api.unsplash.com/photos/photo_123",
                        html = "https://unsplash.com/photos/photo_123",
                        download = "https://unsplash.com/photos/photo_123/download",
                        downloadLocation = "https://api.unsplash.com/photos/photo_123/download?ixid=123",
                    ),
                exif =
                    Exif(
                        make = "Canon",
                        model = "EOS R5",
                        exposureTime = "1/200",
                        aperture = "2.8",
                        focalLength = "50.0",
                        iso = 100,
                    ),
                location =
                    Location(
                        name = "Lisbon, Portugal",
                        city = "Lisbon",
                        country = "Portugal",
                        position = Position(latitude = 38.7223, longitude = -9.1393),
                    ),
                tags = listOf(Tag(title = "nature")),
            )

        val encoded = json.encodeToString(photo)
        val decoded = json.decodeFromString<Photo>(encoded)

        assertEquals(photo.id, decoded.id)
        assertEquals(photo.width, decoded.width)
        assertEquals(photo.height, decoded.height)
        assertEquals(photo.urls.regular, decoded.urls.regular)
        assertEquals(photo.user.username, decoded.user.username)
        assertEquals("Canon", decoded.exif?.make)
        assertEquals("Lisbon", decoded.location?.city)
        assertEquals(540, decoded.getAspectRatioHeight(960))
    }

    @Test
    fun topicSerializationRoundtrip() {
        val topic =
            Topic(
                id = "topic_1",
                slug = "nature",
                title = "Nature",
                description = "Into the wild",
                totalPhotos = 42,
            )

        val encoded = json.encodeToString(topic)
        val decoded = json.decodeFromString<Topic>(encoded)

        assertEquals("topic_1", decoded.id)
        assertEquals("nature", decoded.slug)
        assertEquals(42, decoded.totalPhotos)
    }

    @Test
    fun collectionSerializationRoundtrip() {
        val collection =
            PhotoCollection(
                id = "col_1",
                title = "Favorites",
                totalPhotos = 10,
                user =
                    User(
                        id = "user_1",
                        username = "curator",
                        name = "Curator",
                        profileImage =
                            ProfileImage(
                                small = "https://example.com/s.jpg",
                                medium = "https://example.com/m.jpg",
                                large = "https://example.com/l.jpg",
                            ),
                        links =
                            UserLinks(
                                self = "https://api.unsplash.com/users/curator",
                                html = "https://unsplash.com/@curator",
                                photos = "https://api.unsplash.com/users/curator/photos",
                            ),
                    ),
            )

        val encoded = json.encodeToString(collection)
        val decoded = json.decodeFromString<PhotoCollection>(encoded)

        assertEquals("col_1", decoded.id)
        assertEquals("Favorites", decoded.title)
        assertEquals("curator", decoded.user.username)
    }

    @Test
    fun searchResponseDeserialization() {
        val searchJson =
            """
            {
                "total": 100,
                "total_pages": 10,
                "results": [
                    {
                        "id": "col_summary_1",
                        "title": "Minimal",
                        "total_photos": 5,
                        "user": {
                            "id": "u1",
                            "username": "min",
                            "name": "Minimalist",
                            "profile_image": {
                                "small": "s",
                                "medium": "m",
                                "large": "l"
                            },
                            "links": {
                                "self": "s",
                                "html": "h",
                                "photos": "p"
                            }
                        }
                    }
                ]
            }
            """.trimIndent()

        val response = json.decodeFromString<SearchResponse<CollectionSummary>>(searchJson)
        assertEquals(100, response.total)
        assertEquals(10, response.totalPages)
        assertEquals(1, response.results.size)
        assertEquals("col_summary_1", response.results[0].id)
    }

    @Test
    fun photoUrlsGetOptimalUrlAppendsWidthAndQuality() {
        val urls =
            PhotoUrls(
                raw = "https://images.unsplash.com/photo-123",
                full = "https://example.com/full.jpg",
                regular = "https://example.com/regular.jpg",
                small = "https://example.com/small.jpg",
                thumb = "https://example.com/thumb.jpg",
            )
        val optimal = urls.getOptimalUrl(400, 75)
        assertEquals("https://images.unsplash.com/photo-123?w=400&q=75&auto=format", optimal)
        assertEquals("https://images.unsplash.com/photo-123?w=400&q=80&auto=format", urls.getOptimalUrl(400))
    }

    @Test
    fun photoUrlsGetOptimalUrlPreservesIxidAndStripsExistingDimensions() {
        val urls =
            PhotoUrls(
                raw = "https://images.unsplash.com/photo-123?ixid=M3wxMjA3fDB8MHxwaG90by1wYWdl&w=1080&q=80&auto=format",
                full = "https://example.com/full.jpg",
                regular = "https://example.com/regular.jpg",
                small = "https://example.com/small.jpg",
                thumb = "https://example.com/thumb.jpg",
            )
        val optimal = urls.getOptimalUrl(600, 85)
        assertEquals("https://images.unsplash.com/photo-123?ixid=M3wxMjA3fDB8MHxwaG90by1wYWdl&w=600&q=85&auto=format", optimal)
    }

    @Test
    fun photoUrlsGetOptimalUrlClampsWidthAndQuality() {
        val urls =
            PhotoUrls(
                raw = "https://images.unsplash.com/photo-123",
                full = "https://example.com/full.jpg",
                regular = "https://example.com/regular.jpg",
                small = "https://example.com/small.jpg",
                thumb = "https://example.com/thumb.jpg",
            )
        assertEquals("https://images.unsplash.com/photo-123?w=64&q=10&auto=format", urls.getOptimalUrl(10, 5))
        assertEquals("https://images.unsplash.com/photo-123?w=2400&q=100&auto=format", urls.getOptimalUrl(5000, 150))
    }
}
