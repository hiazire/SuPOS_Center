package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_drafts",
    indices = [
        Index(value = ["draftType"])
    ]
)
data class OrderDraftEntity(
    @PrimaryKey val draftId: String,
    val draftType: String, // "ACTIVE_CART", "TEMP_SAVED"
    val timestamp: Long,
    val tableNumber: String = "",
    val transactionType: String = "外帶",
    val createdAtText: String = "",
    val itemsJson: String = "[]"
)
