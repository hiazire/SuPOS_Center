package com.example.supos.ui.online

import com.example.supos.data.qburger.QBurgerOrderState
import com.example.supos.data.qburger.QBurgerOrderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineOrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFilterSourceUpdatesState() {
        val vm = OnlineOrderViewModel()
        vm.setSourceFilter("UberEats")
        assertEquals("UberEats", vm.uiState.value.selectedSourceFilter)

        vm.setSourceFilter("ALL")
        assertEquals("ALL", vm.uiState.value.selectedSourceFilter)
    }

    @Test
    fun testToggleStoreStatusWithPin() {
        val vm = OnlineOrderViewModel()
        assertTrue(vm.uiState.value.isStoreOpen)

        // Wrong PIN fails
        val wrongResult = vm.toggleStoreStatus("1234")
        assertFalse(wrongResult)
        assertTrue(vm.uiState.value.isStoreOpen)

        // Correct PIN 8991 succeeds and toggles to closed
        val correctResult = vm.toggleStoreStatus("8991")
        assertTrue(correctResult)
        assertFalse(vm.uiState.value.isStoreOpen)

        // Toggle back to open
        val reopenResult = vm.toggleStoreStatus("8991")
        assertTrue(reopenResult)
        assertTrue(vm.uiState.value.isStoreOpen)
    }
}
