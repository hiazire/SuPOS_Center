package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: String,
    val menuItemId: Int,
    val menuItemName: String,
    val quantity: Int,
    val subtotal: Int,
    val optionsString: String,
    val unitPrice: Int = 0,
    val customPrice: Int? = null,
    val isComplimentary: Boolean = false
)
