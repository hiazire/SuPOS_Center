package com.example.supos.ui.main

import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.repository.OrderRepository
import com.example.supos.test.FakeGasApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutDraftImportTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() {
        orderRepository = OrderRepository(FakeOrderDao(), FakeGasApiService(), testDispatcher)
    }

    @Test
    fun createCheckoutDraftFromOrder_matchesItemsAndOptionsCorrectly() = runTest(testDispatcher) {
        val order = HistoryOrderPayload(
            orderId = "WEB-888",
            timestamp = "2026-08-15 10:00:00 GMT+0800",
            details = """[{"name":"雙層起司堡","qty":2,"addons":[{"name":"加蛋","qty":1}]}]""",
            state = "PENDING",
            totalAmount = 300.0,
            paymentStatus = "UNPAID"
        )
        val menuItems = listOf(
            MenuItemEntity(id = 1, category = "主餐", name = "雙層起司堡", price = 130, imageUrl = "", optionsGroup = null)
        )
        val optionItems = listOf(
            OptionItemEntity(name = "加蛋", groupName = "配料", price = 20, optionType = "SINGLE")
        )

        val draft = orderRepository.createCheckoutDraftFromOrder(order, menuItems, optionItems)

        assertEquals("WEB-888", draft.sourceOrderId)
        assertEquals(1, draft.cartItems.size)
        val cartItem = draft.cartItems.first()
        assertEquals("雙層起司堡", cartItem.menuItem.name)
        assertEquals(2, cartItem.quantity)
        assertEquals(1, cartItem.options.size)
        assertEquals("加蛋", cartItem.options.first().name)
        assertTrue(draft.unmappedItems.isEmpty())
    }

    @Test
    fun createCheckoutDraftFromOrder_recordsUnmappedItemsWhenMissing() = runTest(testDispatcher) {
        val order = HistoryOrderPayload(
            orderId = "WEB-889",
            timestamp = "2026-08-15 11:00:00 GMT+0800",
            details = """[{"name":"下架冰淇淋","qty":1},{"name":"雙層起司堡","qty":1}]""",
            state = "PENDING",
            totalAmount = 200.0,
            paymentStatus = "UNPAID"
        )
        val menuItems = listOf(
            MenuItemEntity(id = 1, category = "主餐", name = "雙層起司堡", price = 130, imageUrl = "", optionsGroup = null)
        )
        val optionItems = emptyList<OptionItemEntity>()

        val draft = orderRepository.createCheckoutDraftFromOrder(order, menuItems, optionItems)

        assertEquals(1, draft.cartItems.size)
        assertEquals("雙層起司堡", draft.cartItems.first().menuItem.name)
        assertEquals(1, draft.unmappedItems.size)
        assertEquals("下架冰淇淋", draft.unmappedItems.first())
    }

    @Test
    fun consumeCheckoutDraft_clearsSignalAfterSingleRead() = runTest(testDispatcher) {
        val order = HistoryOrderPayload(
            orderId = "WEB-890",
            timestamp = "2026-08-15 12:00:00",
            details = "[]",
            state = "PENDING",
            totalAmount = 0.0,
            paymentStatus = "UNPAID"
        )
        orderRepository.createCheckoutDraftFromOrder(order, emptyList(), emptyList())

        val firstRead = orderRepository.consumeCheckoutDraft()
        assertNotNull(firstRead)
        assertEquals("WEB-890", firstRead?.sourceOrderId)

        val secondRead = orderRepository.consumeCheckoutDraft()
        assertNull(secondRead)
    }

    private class FakeOrderDao : OrderDao {
        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {}
        override fun getAllOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity> = emptyList()
        override suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity> = emptyList()
        override suspend fun markOrderAsSynced(orderId: String) {}
        override fun getUnsyncedOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getUnsyncedOrdersList(): List<OrderEntity> = emptyList()
        override fun getUnsyncedCount(): Flow<Int> = flowOf(0)
    }
}
