package com.example.supos.data.local.entity

import androidx.room.ColumnInfo
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
    val optionsGroup: String?,
    @ColumnInfo(defaultValue = "2147483647")
    val sourceOrder: Int = Int.MAX_VALUE,
    @ColumnInfo(defaultValue = "''")
    val icon: String = "",
    @ColumnInfo(defaultValue = "''")
    val printer: String = "",
    @ColumnInfo(defaultValue = "''")
    val sizeTypes: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val isAvailable: String? = null
)
