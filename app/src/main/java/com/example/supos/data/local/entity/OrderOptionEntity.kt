package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_options",
    foreignKeys = [
        ForeignKey(
            entity = OrderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderItemId")]
)
data class OrderOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderItemId: Int = 0,
    val optionId: Int,
    val optionName: String,
    val price: Int,
    val quantity: Int = 1
)
