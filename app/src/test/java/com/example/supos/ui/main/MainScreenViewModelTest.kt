package com.example.supos.ui.main

import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.model.CartItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun cartItem_subtotalCalculation_regular() {
        val item = MenuItemEntity(id = 1, category = "主餐", name = "經典漢堡", price = 150, imageUrl = "", optionsGroup = null)
        val option = OptionItemEntity(name = "加起司", groupName = "配料", price = 20, optionType = "SINGLE")
        val cartItem = CartItem(
            menuItem = item,
            quantity = 2,
            options = listOf(option)
        )
        // (150 + 20) * 2 = 340
        assertEquals(340, cartItem.subtotal)
    }

    @Test
    fun cartItem_subtotalCalculation_customPrice() {
        val item = MenuItemEntity(id = 2, category = "主餐", name = "雙層牛肉堡", price = 220, imageUrl = "", optionsGroup = null)
        val cartItem = CartItem(
            menuItem = item,
            quantity = 3,
            customPrice = 200
        )
        // 200 * 3 = 600
        assertEquals(600, cartItem.subtotal)
    }

    @Test
    fun cartItem_subtotalCalculation_complimentary() {
        val item = MenuItemEntity(id = 3, category = "飲料", name = "紅茶", price = 40, imageUrl = "", optionsGroup = null)
        val cartItem = CartItem(
            menuItem = item,
            quantity = 5,
            isComplimentary = true
        )
        // Complimentary item should be 0
        assertEquals(0, cartItem.subtotal)
    }
}
