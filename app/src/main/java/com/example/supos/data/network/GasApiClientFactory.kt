package com.example.supos.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GasApiClientFactory {

    const val DEFAULT_BASE_URL = "https://script.google.com/"
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    fun createOkHttpClient(
        tracingInterceptor: NetworkTracingInterceptor = NetworkTracingInterceptor(),
        connectTimeoutSeconds: Long = CONNECT_TIMEOUT_SECONDS,
        readTimeoutSeconds: Long = READ_TIMEOUT_SECONDS,
        writeTimeoutSeconds: Long = WRITE_TIMEOUT_SECONDS
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(tracingInterceptor)
            .build()
    }

    fun createGasApiService(
        baseUrl: String = DEFAULT_BASE_URL,
        okHttpClient: OkHttpClient = createOkHttpClient()
    ): GasApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(GasApiService::class.java)
    }
}
