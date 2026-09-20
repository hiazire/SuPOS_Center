package com.example.supos.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class GasApiClientTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun request_includesTracingHeaders() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val interceptor = NetworkTracingInterceptor(
            clientVersion = "2.1.0",
            deviceId = "TABLET_001"
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        val recordedRequest = mockWebServer.takeRequest()
        val requestId = recordedRequest.getHeader("X-Request-Id")
        val clientVersion = recordedRequest.getHeader("X-Client-Version")
        val deviceId = recordedRequest.getHeader("X-Device-Id")

        assertNotNull(requestId)
        // Verify requestId is a valid UUID
        assertNotNull(UUID.fromString(requestId))
        assertEquals("2.1.0", clientVersion)
        assertEquals("TABLET_001", deviceId)
    }

    @Test
    fun apiKey_injectedWhenConfigured() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val interceptor = NetworkTracingInterceptor(
            apiKeyProvider = { "my-secret-gas-token" }
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/menu"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertEquals("Bearer my-secret-gas-token", authHeader)
    }

    @Test
    fun domainException_classificationAndRetryability() {
        val timeoutError = GasDomainException.TimeoutError()
        assertTrue(timeoutError.isRetryable)

        val networkError = GasDomainException.NetworkError()
        assertTrue(networkError.isRetryable)

        val authError = GasDomainException.AuthError()
        assertFalse(authError.isRetryable)

        val server500 = GasDomainException.ServerError(500, "Internal Server Error")
        assertTrue(server500.isRetryable)

        val client400 = GasDomainException.ServerError(400, "Bad Request")
        assertFalse(client400.isRetryable)
    }

    @Test
    fun clientFactory_createsConfiguredClient() {
        val client = GasApiClientFactory.createOkHttpClient(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 20,
            writeTimeoutSeconds = 20
        )
        assertEquals(10000, client.connectTimeoutMillis)
        assertEquals(20000, client.readTimeoutMillis)
        assertEquals(20000, client.writeTimeoutMillis)
    }
}
