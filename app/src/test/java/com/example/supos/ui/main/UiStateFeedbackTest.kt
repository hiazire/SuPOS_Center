package com.example.supos.ui.main

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UiStateFeedbackTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

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
    fun turnoverFailure_setsErrorStateWithoutZeroing() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate network failure
        fakeGasApiService.shouldReturnError = true

        viewModel.fetchTodayTurnover()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.turnoverUiState.value
        assertTrue("State should be Error when network fails", state is MainScreenViewModel.TurnoverUiState.Error)
        val errorState = state as MainScreenViewModel.TurnoverUiState.Error
        assertNotNull(errorState.message)
    }

    @Test
    fun turnoverSuccess_setsSuccessStateWithTimestamp() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGasApiService.shouldReturnError = false

        viewModel.fetchTodayTurnover()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.turnoverUiState.value
        assertTrue("State should be Success when network succeeds", state is MainScreenViewModel.TurnoverUiState.Success)
        val successState = state as MainScreenViewModel.TurnoverUiState.Success
        assertNotNull(successState.fetchedAt)
    }

    @Test
    fun menuSync_updatesSyncState() = runTest(testDispatcher) {
        fakeGasApiService.customMenuResponse = com.example.supos.data.network.MenuResponsePayload(
            success = true,
            menu = listOf(
                com.example.supos.data.network.MenuItemPayload(
                    id = "1",
                    category = "主餐",
                    name = "招牌堡",
                    price = "100",
                    imageUrl = "",
                    optionsGroup = null
                )
            ),
            options = emptyList()
        )

        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.manualRefreshMenu()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.menuSyncUiState.value
        assertTrue("Menu sync state should be Success", state is MainScreenViewModel.MenuSyncUiState.Success)
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
