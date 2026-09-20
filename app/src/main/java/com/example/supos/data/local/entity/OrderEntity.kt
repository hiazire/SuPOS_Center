package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["platformCode", "sourceOrderId"], unique = true)
    ]
)
data class OrderEntity(
    @PrimaryKey val orderId: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Int,
    val orderType: String, // e.g., "Dine-in", "Takeout"
    val paymentMethod: String, // e.g., "Cash", "Card"
    val isSynced: Boolean = false,
    val sourceType: String = "OFFLINE_POS",
    val platformCode: String = "POS",
    val sourceOrderId: String? = null,
    val idempotencyKey: String = "",
    val acceptanceStatus: String = "ACCEPTED",
    val paymentStatus: String = "UNPAID",
    val fulfillmentStatus: String = "PENDING",
    val printStatus: String = "NOT_PRINTED",
    val invoiceNumber: String = "",
    val ubn: String = "",
    val carrier: String = ""
)
