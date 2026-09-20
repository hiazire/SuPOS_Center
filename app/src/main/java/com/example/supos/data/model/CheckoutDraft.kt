package com.example.supos.data.model

data class CheckoutDraft(
    val sourceOrderId: String,
    val timestamp: String,
    val transactionType: String,
    val cartItems: List<CartItem>,
    val unmappedItems: List<String> = emptyList(),
    val sourceType: String = "ONLINE_IMPORT",
    val platformCode: String = "WEB"
)
