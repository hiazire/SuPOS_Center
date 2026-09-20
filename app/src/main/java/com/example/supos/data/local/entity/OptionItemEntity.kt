package com.example.supos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "option_items")
data class OptionItemEntity(
    @PrimaryKey
    val name: String,
    val groupName: String, // group is a reserved keyword in sqlite
    val price: Int,
    val optionType: String
)
