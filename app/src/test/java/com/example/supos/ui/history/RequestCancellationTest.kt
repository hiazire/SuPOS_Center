package com.example.supos.ui.history

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.network.HistoryResponsePayload
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import com.example.supos.test.FakeGasApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RequestCancellationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

    private val todayFormatted: String
        get() = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US).format(Date())

    private val yesterdayFormatted: String
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            return SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US).format(cal.time)
        }

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
    fun rapidFilterSwitching_cancelsStaleRequestsAndPreservesLatestOnly() = runTest(testDispatcher) {
        val todayOrder = HistoryOrderPayload(
            orderId = "ORD-TODAY-999",
            timestamp = todayFormatted,
            totalAmount = 500.0,
            paymentStatus = "PAID",
            details = "[]",
            state = "READY"
        )
        val yesterdayOrder = HistoryOrderPayload(
            orderId = "ORD-YESTERDAY-888",
            timestamp = yesterdayFormatted,
            totalAmount = 250.0,
            paymentStatus = "PAID",
            details = "[]",
            state = "READY"
        )

        // Initial setup
        fakeGasApiService.historyOrdersStore.clear()
        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Configure delayed response for days=0 (TODAY) and fast response for days=1 (YESTERDAY)
        fakeGasApiService.onGetHistoryOrders = { request ->
            if (request.days == 0) {
                delay(2000) // Slow response
                HistoryResponsePayload(success = true, orders = listOf(todayOrder))
            } else {
                delay(50)   // Fast response
                HistoryResponsePayload(success = true, orders = listOf(yesterdayOrder))
            }
        }

        // Trigger slow request (TODAY)
        viewModel.setDateFilter(OrderDateFilter.TODAY)
        testDispatcher.scheduler.advanceTimeBy(100) // Let it begin execution

        // Rapidly switch to fast request (YESTERDAY)
        viewModel.setDateFilter(OrderDateFilter.YESTERDAY)

        // Advance time to allow both to complete
        testDispatcher.scheduler.advanceTimeBy(3000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the latest filter (YESTERDAY) is preserved and slow TODAY response was discarded
        val state = viewModel.ordersState.value
        assertTrue("State should be Success", state is OrderHistoryUiState.Success)
        val successState = state as OrderHistoryUiState.Success
        assertEquals(1, successState.orders.size)
        assertEquals("ORD-YESTERDAY-888", successState.orders[0].orderId)
    }

    @Test
    fun filterSwitch_immediatelySetsLoadingState() = runTest(testDispatcher) {
        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGasApiService.onGetHistoryOrders = {
            delay(500)
            HistoryResponsePayload(success = true, orders = emptyList())
        }

        viewModel.setDateFilter(OrderDateFilter.YESTERDAY)

        // Immediately after setDateFilter, UI state must be Loading
        assertEquals(OrderHistoryUiState.Loading, viewModel.ordersState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.ordersState.value is OrderHistoryUiState.Success)
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
