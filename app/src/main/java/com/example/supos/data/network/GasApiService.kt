package com.example.supos.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GasApiService {
    
    @GET("macros/s/AKfycbw0bjJpGXFNh9TvxOdh_gFRvttou-DkSpRtzu_nFLIyr30KUHpwFr3Bn8LGWBGE5SpJcA/exec")
    suspend fun fetchMenu(): MenuResponsePayload

    @POST("macros/s/AKfycbw0bjJpGXFNh9TvxOdh_gFRvttou-DkSpRtzu_nFLIyr30KUHpwFr3Bn8LGWBGE5SpJcA/exec")
    suspend fun syncOrder(@Body order: OrderPayload): SyncResponse

    @POST("macros/s/AKfycbw0bjJpGXFNh9TvxOdh_gFRvttou-DkSpRtzu_nFLIyr30KUHpwFr3Bn8LGWBGE5SpJcA/exec")
    suspend fun getHistoryOrders(@Body request: HistoryRequest): HistoryResponsePayload

    @POST("macros/s/AKfycbw0bjJpGXFNh9TvxOdh_gFRvttou-DkSpRtzu_nFLIyr30KUHpwFr3Bn8LGWBGE5SpJcA/exec")
    suspend fun updateOrderState(@Body request: Map<String, String>): SyncResponse
}
