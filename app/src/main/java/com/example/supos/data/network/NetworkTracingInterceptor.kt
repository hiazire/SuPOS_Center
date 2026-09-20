package com.example.supos.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class NetworkTracingInterceptor(
    private val clientVersion: String = "1.0.0",
    private val deviceId: String = "ANDROID_POS_TABLET",
    private val apiKeyProvider: () -> String? = { null }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .header("X-Request-Id", UUID.randomUUID().toString())
            .header("X-Client-Version", clientVersion)
            .header("X-Device-Id", deviceId)

        val apiKey = apiKeyProvider()
        if (!apiKey.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        return chain.proceed(builder.build())
    }
}
