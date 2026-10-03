package com.example.supos.data.model

import com.example.supos.data.local.entity.MenuItemEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuAvailabilityTest {
    private fun item(availability: String?) = MenuItemEntity(
        id = 47,
        category = "鹹味吐司",
        name = "壽喜燒炸雞吐司",
        price = 85,
        imageUrl = "",
        optionsGroup = null,
        isAvailable = availability
    )

    @Test
    fun recognizesCurrentSheetValuesAndKeepsLegacyRowsVisible() {
        assertTrue(item("TURE").isOrderable())
        assertTrue(item("TRUE").isOrderable())
        assertFalse(item("").isOrderable())
        assertFalse(item("FALSE").isOrderable())
        assertFalse(item("unexpected").isOrderable())
        assertTrue(item(null).isOrderable())
    }
}
