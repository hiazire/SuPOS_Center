package com.example.supos.data.model

import com.example.supos.data.local.entity.MenuItemEntity

/** Menu!J currently uses the legacy typo "TURE" for available items. */
fun MenuItemEntity.isOrderable(): Boolean = when (isAvailable?.trim()?.uppercase()) {
    null -> true // Preserved local menu rows from before the availability column existed.
    "TRUE", "TURE", "1", "YES" -> true
    else -> false
}
