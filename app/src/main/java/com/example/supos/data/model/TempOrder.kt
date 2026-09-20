package com.example.supos.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TempOrder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem>,
    val transactionType: String,
    val tableNumber: String,
    val createdAt: String,
    val invoiceNumber: String,
    val ubn: String,
    val carrier: String
) {
    val timeString: String
        get() {
            val formatter = SimpleDateFormat("a hh:mm:ss", Locale.ENGLISH)
            return formatter.format(Date(timestamp))
        }
}
