package com.example.supos.ui.main

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdempotencyOrderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: CapturingOrderDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = CapturingOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(FakeMenuDao(), fakeGasApiService, testDispatcher)
        viewModel = MainScreenViewModel(menuRepository, orderRepository)

        val item = MenuItemEntity(id = 10, category = "主餐", name = "經典牛肉堡", price = 200, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkout_importedOnlineOrder_usesStableIdempotencyKey() = runTest(testDispatcher) {
        val onlineOrder = HistoryOrderPayload(
            orderId = "ORD-888",
            timestamp = "2026-08-15 12:00:00",
            details = "[{\"name\":\"經典牛肉堡\",\"qty\":1}]",
            state = "PENDING",
            totalAmount = 200.0,
            paymentStatus = "UNPAID",
            platformCode = "WEB",
            sourceType = "ONLINE_IMPORT"
        )

        // 1. Create draft from imported online order
        val draft = orderRepository.createCheckoutDraftFromOrder(
            order = onlineOrder,
            menuItems = listOf(MenuItemEntity(id = 10, category = "主餐", name = "經典牛肉堡", price = 200, imageUrl = "", optionsGroup = null)),
            optionItems = emptyList()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Perform checkout
        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Verify captured OrderEntity idempotency fields
        val savedEntity = fakeOrderDao.lastCreatedOrder
        requireNotNull(savedEntity)
        assertEquals("WEB:ORD-888", savedEntity.orderId)
        assertEquals("WEB:ORD-888", savedEntity.idempotencyKey)
        assertEquals("WEB", savedEntity.platformCode)
        assertEquals("ORD-888", savedEntity.sourceOrderId)
        assertEquals("ONLINE_IMPORT", savedEntity.sourceType)

        // 4. Verify captured OrderPayload sent to GAS
        val lastSyncedPayload = fakeGasApiService.lastSyncedOrderPayload
        requireNotNull(lastSyncedPayload)
        assertEquals("WEB:ORD-888", lastSyncedPayload.orderId)
        assertEquals("WEB:ORD-888", lastSyncedPayload.idempotencyKey)
        assertEquals("WEB", lastSyncedPayload.platformCode)
        assertEquals("ORD-888", lastSyncedPayload.sourceOrderId)
        assertEquals("ONLINE_IMPORT", lastSyncedPayload.sourceType)
    }

    @Test
    fun checkout_offlinePosOrder_usesGeneratedUuidAndPosDefaults() = runTest(testDispatcher) {
        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        val savedEntity = fakeOrderDao.lastCreatedOrder
        requireNotNull(savedEntity)
        assertEquals("POS", savedEntity.platformCode)
        assertNull(savedEntity.sourceOrderId)
        assertEquals("OFFLINE_POS", savedEntity.sourceType)
        assertTrue(savedEntity.idempotencyKey.isNotEmpty())
    }

    private class CapturingOrderDao : OrderDao {
        var lastCreatedOrder: OrderEntity? = null

        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {
            lastCreatedOrder = order
        }
        override fun getAllOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity> = emptyList()
        override suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity> = emptyList()
        override suspend fun markOrderAsSynced(orderId: String) {}
        override fun getUnsyncedOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getUnsyncedOrdersList(): List<OrderEntity> = emptyList()
        override fun getUnsyncedCount(): Flow<Int> = flowOf(0)
    }

    private class FakeMenuDao : MenuDao {
        override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(emptyList())
        override fun getAllOptions(): Flow<List<OptionItemEntity>> = flowOf(emptyList())
        override suspend fun insertMenuItems(items: List<MenuItemEntity>) {}
        override suspend fun insertOptions(options: List<OptionItemEntity>) {}
        override suspend fun clearMenuItems() {}
        override suspend fun clearOptions() {}
    }
}
