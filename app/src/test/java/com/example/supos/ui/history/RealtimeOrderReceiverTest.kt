package com.example.supos.ui.history

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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeOrderReceiverTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

    private val nowFormatted: String
        get() = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US).format(Date())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        fakeMenuDao = FakeMenuDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(fakeMenuDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun polling_detectsNewIncomingOrdersAndTriggersAlert() = runTest(testDispatcher) {
        // Initial state with 1 order
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = "ORDER-101",
                timestamp = nowFormatted,
                totalAmount = 100.0,
                paymentStatus = "PAID",
                details = "[{\"name\":\"原味蛋餅\",\"qty\":1}]",
                state = "PENDING",
                platformCode = "LINE",
                sourceType = "ONLINE"
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val alertEvents = mutableListOf<NewOrderAlertEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.newOrderAlertFlow.collect { alertEvents.add(it) }
        }

        // New order arrives
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = "ORDER-102",
                timestamp = nowFormatted,
                totalAmount = 250.0,
                paymentStatus = "PAID",
                details = "[{\"name\":\"招牌漢堡\",\"qty\":2}]",
                state = "PENDING",
                platformCode = "UBER_EATS",
                sourceType = "ONLINE"
            )
        )

        // Trigger polling
        viewModel.fetchHistoryFromCloud(isBackgroundPoll = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, alertEvents.size)
        assertEquals(1, alertEvents[0].newOrdersCount)
        assertEquals(listOf("ORDER-102"), alertEvents[0].newOrderIds)

        job.cancel()
    }

    @Test
    fun polling_deduplicatesRepeatedOrdersWithoutSpammingAlerts() = runTest(testDispatcher) {
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = "ORDER-201",
                timestamp = nowFormatted,
                totalAmount = 120.0,
                paymentStatus = "PAID",
                details = "[]",
                state = "PENDING"
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val alertEvents = mutableListOf<NewOrderAlertEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.newOrderAlertFlow.collect { alertEvents.add(it) }
        }

        // Poll repeatedly with same order
        viewModel.fetchHistoryFromCloud(isBackgroundPoll = true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.fetchHistoryFromCloud(isBackgroundPoll = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, alertEvents.size)

        job.cancel()
    }

    @Test
    fun polling_networkError_setsDisconnectedStateWhilePreservingOrders() = runTest(testDispatcher) {
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = "ORDER-301",
                timestamp = nowFormatted,
                totalAmount = 150.0,
                paymentStatus = "PAID",
                details = "[]",
                state = "PENDING"
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.ordersState.value is OrderHistoryUiState.Success)
        val successState = viewModel.ordersState.value as OrderHistoryUiState.Success
        assertEquals(1, successState.orders.size)

        // Network drops
        fakeGasApiService.shouldReturnError = true

        viewModel.fetchHistoryFromCloud(isBackgroundPoll = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.connectionState.value)
        // Orders should remain visible (not replaced with error screen)
        assertTrue("Orders state must be preserved during network drops", viewModel.ordersState.value is OrderHistoryUiState.Success)
        val preservedState = viewModel.ordersState.value as OrderHistoryUiState.Success
        assertEquals(1, preservedState.orders.size)
    }

    @Test
    fun manualRefresh_updatesSyncTimeAndOrders() = runTest(testDispatcher) {
        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = "ORDER-401",
                timestamp = nowFormatted,
                totalAmount = 200.0,
                paymentStatus = "PAID",
                details = "[]",
                state = "PENDING"
            )
        )

        viewModel.fetchHistoryFromCloud()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ConnectionState.CONNECTED, viewModel.connectionState.value)
        assertTrue(viewModel.lastSyncTimestamp.value > 0L)
    }

    private class FakeOrderDao : OrderDao {
        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {}
        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {}
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
        override suspend fun replaceMenuAndOptions(items: List<MenuItemEntity>, options: List<OptionItemEntity>) {}
    }
}
