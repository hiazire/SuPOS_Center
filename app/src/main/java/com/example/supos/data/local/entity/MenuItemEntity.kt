package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey
    val id: Int,
    val category: String,
    val name: String,
    val price: Int,
    val imageUrl: String,
    val optionsGroup: String?
)
