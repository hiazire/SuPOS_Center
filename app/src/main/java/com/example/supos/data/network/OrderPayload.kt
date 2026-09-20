package com.example.supos.data.network

import com.google.gson.annotations.SerializedName

data class OrderPayload(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("totalAmount") val totalAmount: Int,
    @SerializedName("orderType") val orderType: String,
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("items") val items: List<OrderItemPayload>,
    @SerializedName("sourceType") val sourceType: String? = "OFFLINE_POS",
    @SerializedName("platformCode") val platformCode: String? = "POS",
    @SerializedName("sourceOrderId") val sourceOrderId: String? = null,
    @SerializedName("idempotencyKey") val idempotencyKey: String? = null,
    @SerializedName("acceptanceStatus") val acceptanceStatus: String? = "ACCEPTED",
    @SerializedName("paymentStatus") val paymentStatus: String? = "UNPAID",
    @SerializedName("fulfillmentStatus") val fulfillmentStatus: String? = "PENDING",
    @SerializedName("printStatus") val printStatus: String? = "NOT_PRINTED",
    @SerializedName("invoiceNumber") val invoiceNumber: String? = "",
    @SerializedName("ubn") val ubn: String? = "",
    @SerializedName("carrier") val carrier: String? = ""
)

data class OrderItemPayload(
    @SerializedName("menuItemName") val menuItemName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("subtotal") val subtotal: Int,
    @SerializedName("options") val options: String,
    @SerializedName("unitPrice") val unitPrice: Int = 0,
    @SerializedName("customPrice") val customPrice: Int? = null,
    @SerializedName("isComplimentary") val isComplimentary: Boolean = false,
    @SerializedName("optionsJson") val optionsJson: String = "[]"
)

data class SyncResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)
