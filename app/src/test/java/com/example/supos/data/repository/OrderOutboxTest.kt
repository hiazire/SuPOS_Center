package com.example.supos.data.repository

import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.test.FakeGasApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderOutboxTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: MemoryOrderDao
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = MemoryOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun outbox_syncsAllPendingOrdersWithSameIdempotencyKey() = runTest(testDispatcher) {
        val order1 = OrderEntity(
            orderId = "WEB:ORD-101",
            totalAmount = 250,
            orderType = "線上訂單",
            paymentMethod = "現金",
            isSynced = false,
            sourceType = "ONLINE_IMPORT",
            platformCode = "WEB",
            sourceOrderId = "ORD-101",
            idempotencyKey = "WEB:ORD-101"
        )
        val order2 = OrderEntity(
            orderId = "POS:UUID-999",
            totalAmount = 120,
            orderType = "外帶",
            paymentMethod = "現金",
            isSynced = false,
            sourceType = "OFFLINE_POS",
            platformCode = "POS",
            sourceOrderId = null,
            idempotencyKey = "POS:UUID-999"
        )

        fakeOrderDao.ordersStore.add(order1)
        fakeOrderDao.ordersStore.add(order2)

        fakeOrderDao.itemsStore.add(
            OrderItemEntity(id = 1, orderId = "WEB:ORD-101", menuItemId = 1, menuItemName = "經典堡", quantity = 1, subtotal = 250, optionsString = "")
        )
        fakeOrderDao.itemsStore.add(
            OrderItemEntity(id = 2, orderId = "POS:UUID-999", menuItemId = 2, menuItemName = "紅茶", quantity = 2, subtotal = 120, optionsString = "")
        )

        // Act: Run Outbox sync
        val result = orderRepository.syncPendingOutboxOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Both orders synced with same idempotency key
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        assertEquals(2, fakeGasApiService.syncedOrders.size)
        val synced1 = fakeGasApiService.syncedOrders[0]
        val synced2 = fakeGasApiService.syncedOrders[1]

        assertEquals("WEB:ORD-101", synced1.orderId)
        assertEquals("WEB:ORD-101", synced1.idempotencyKey)
        assertEquals("WEB", synced1.platformCode)

        assertEquals("POS:UUID-999", synced2.orderId)
        assertEquals("POS:UUID-999", synced2.idempotencyKey)
        assertEquals("POS", synced2.platformCode)

        // Assert: Outbox is now empty (isSynced = true for all)
        assertTrue(fakeOrderDao.ordersStore.all { it.isSynced })
    }

    @Test
    fun outbox_networkFailure_retainsPendingStateForNextRetry() = runTest(testDispatcher) {
        val order = OrderEntity(
            orderId = "POS:UUID-888",
            totalAmount = 100,
            orderType = "內用",
            paymentMethod = "現金",
            isSynced = false,
            sourceType = "OFFLINE_POS",
            platformCode = "POS",
            sourceOrderId = null,
            idempotencyKey = "POS:UUID-888"
        )
        fakeOrderDao.ordersStore.add(order)
        fakeGasApiService.shouldReturnError = true

        // Act: Sync fails due to network
        val result = orderRepository.syncPendingOutboxOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Failure returned and order remains unsynced in Outbox
        assertFalse(result.isSuccess)
        assertFalse(fakeOrderDao.ordersStore.first().isSynced)
        assertEquals(1, fakeOrderDao.getUnsyncedOrdersList().size)
    }

    private class MemoryOrderDao : OrderDao {
        val ordersStore = mutableListOf<OrderEntity>()
        val itemsStore = mutableListOf<OrderItemEntity>()
        val optionsStore = mutableListOf<OrderOptionEntity>()

        override suspend fun insertOrder(order: OrderEntity) {
            ordersStore.removeAll { it.orderId == order.orderId }
            ordersStore.add(order)
        }

        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> {
            itemsStore.addAll(items)
            return items.map { it.id.toLong() }
        }

        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {
            optionsStore.addAll(options)
        }

        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {
            insertOrder(order)
        }

        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {
            insertOrder(order)
        }

        override fun getAllOrders(): Flow<List<OrderEntity>> = flowOf(ordersStore.toList())

        override suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity> {
            return itemsStore.filter { it.orderId == orderId }
        }

        override suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity> {
            return optionsStore.filter { it.orderItemId == orderItemId }
        }

        override suspend fun markOrderAsSynced(orderId: String) {
            val idx = ordersStore.indexOfFirst { it.orderId == orderId }
            if (idx >= 0) {
                ordersStore[idx] = ordersStore[idx].copy(isSynced = true)
            }
        }

        override fun getUnsyncedOrders(): Flow<List<OrderEntity>> {
            return flowOf(ordersStore.filter { !it.isSynced })
        }

        override suspend fun getUnsyncedOrdersList(): List<OrderEntity> {
            return ordersStore.filter { !it.isSynced }
        }

        override fun getUnsyncedCount(): Flow<Int> {
            return flowOf(ordersStore.count { !it.isSynced })
        }
    }
}
