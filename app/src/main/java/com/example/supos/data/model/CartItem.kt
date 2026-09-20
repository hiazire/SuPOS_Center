package com.example.supos.data.model

import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity

data class CartItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val menuItem: MenuItemEntity,
    var quantity: Int = 1,
    val options: List<OptionItemEntity> = emptyList(),
    val isComplimentary: Boolean = false,
    val customPrice: Int? = null
) {
    val subtotal: Int
        get() {
            if (isComplimentary) return 0
            val basePrice = customPrice ?: menuItem.price
            val optionsTotal = options.sumOf { it.price }
            return (basePrice + optionsTotal) * quantity
        }
}
