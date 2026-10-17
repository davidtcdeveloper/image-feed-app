package com.example.imagefeed.core.network

import com.example.imagefeed.model.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class AppErrorMapperTest {
    @Test
    fun testTimeoutExceptions() {
        assertEquals(AppError.Timeout, SocketTimeoutException("timeout").toAppError())
        assertEquals(AppError.Timeout, HttpRequestTimeoutException("url", 1000L).toAppError())
        assertEquals(AppError.Timeout, ConnectTimeoutException("connect timeout").toAppError())
    }

    @Test
    fun testParsedUnsplashExceptions() {
        val notFound = Exception("Unsplash API Error (Status 404): Resource not found")
        assertEquals(AppError.NotFound, notFound.toAppError())

        val server500 = Exception("Unsplash API Error (Status 500): Internal server error")
        assertEquals(AppError.Server(500), server500.toAppError())

        val server503 = Exception("Unsplash API Error (Status 503): Service Unavailable")
        assertEquals(AppError.Server(503), server503.toAppError())
    }

    @Test
    fun testFallbackToNetwork() {
        val generic = RuntimeException("Something failed")
        assertEquals(AppError.Network, generic.toAppError())
    }
}
