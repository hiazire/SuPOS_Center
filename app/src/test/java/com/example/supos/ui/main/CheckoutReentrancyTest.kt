package com.example.supos.ui.main

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.model.CartItem
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutReentrancyTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: CountingOrderDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = CountingOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(FakeMenuDao(), fakeGasApiService, testDispatcher)
        viewModel = MainScreenViewModel(menuRepository, orderRepository)

        val item = MenuItemEntity(id = 1, category = "主餐", name = "經典漢堡", price = 150, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkout_rapidClicks_createsOnlyOneOrder() = runTest(testDispatcher) {
        // Trigger checkout 5 times rapidly
        repeat(5) {
            viewModel.checkout()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeOrderDao.createOrderCallCount)
        assertTrue(viewModel.checkoutUiState.value is MainScreenViewModel.CheckoutUiState.Success)
    }

    @Test
    fun checkout_roomFailure_preservesCart() = runTest(testDispatcher) {
        fakeOrderDao.shouldFail = true

        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Room DB write failed -> Cart must NOT be cleared!
        assertEquals(1, viewModel.cartItems.value.size)
        assertTrue(viewModel.checkoutUiState.value is MainScreenViewModel.CheckoutUiState.Error)
    }

    @Test
    fun checkout_networkFailure_clearsCartAndNotifiesPendingSync() = runTest(testDispatcher) {
        fakeGasApiService.shouldReturnError = true

        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Room DB succeeded -> Cart is cleared, but isSynced is false!
        assertEquals(0, viewModel.cartItems.value.size)
        val state = viewModel.checkoutUiState.value
        assertTrue(state is MainScreenViewModel.CheckoutUiState.Success)
        assertFalse((state as MainScreenViewModel.CheckoutUiState.Success).isSynced)
    }

    private class CountingOrderDao : OrderDao {
        var createOrderCallCount = 0
        var shouldFail = false

        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {
            createOrderCallCount++
            if (shouldFail) {
                throw RuntimeException("Database write exception")
            }
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
