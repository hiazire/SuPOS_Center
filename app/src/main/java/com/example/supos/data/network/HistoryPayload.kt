package com.example.supos.data.network

import com.google.gson.annotations.SerializedName

data class HistoryRequest(
    @SerializedName("action") val action: String = "getHistoryOrders",
    @SerializedName("days") val days: Int = 1
)

data class HistoryResponsePayload(
    @SerializedName("success") val success: Boolean,
    @SerializedName("orders") val orders: List<HistoryOrderPayload>?
)

data class HistoryOrderPayload(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("details") val details: String,
    @SerializedName("state") val state: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("paymentStatus") val paymentStatus: String,
    @SerializedName("cancelReason") val cancelReason: String? = null,
    @SerializedName("platformCode") val platformCode: String? = null,
    @SerializedName("sourceType") val sourceType: String? = null,
    @SerializedName("acceptanceStatus") val acceptanceStatus: String? = null,
    @SerializedName("fulfillmentStatus") val fulfillmentStatus: String? = null,
    @SerializedName("printStatus") val printStatus: String? = null
)
