package com.example.supos.ui.main

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.dao.OrderDraftDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderDraftEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.model.CartItem
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import com.example.supos.test.FakeGasApiService
import com.google.gson.Gson
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartDraftPersistenceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var fakeDraftDao: FakeOrderDraftDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        fakeMenuDao = FakeMenuDao()
        fakeDraftDao = FakeOrderDraftDao()
        orderRepository = OrderRepository(
            orderDao = fakeOrderDao,
            gasApiService = fakeGasApiService,
            ioDispatcher = testDispatcher,
            orderDraftDao = fakeDraftDao
        )
        menuRepository = MenuRepository(fakeMenuDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cartChange_autoPersistsActiveDraft() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = MenuItemEntity(id = 1, category = "主餐", name = "牛肉漢堡", price = 120, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedDraft = fakeDraftDao.getDraftById("ACTIVE_CART_DEFAULT")
        assertNotNull(savedDraft)
        assertEquals("ACTIVE_CART", savedDraft?.draftType)
        assertTrue(savedDraft!!.itemsJson.contains("牛肉漢堡"))
    }

    @Test
    fun viewModelInit_restoresActiveCartFromDb() = runTest(testDispatcher) {
        val initialItems = listOf(
            CartItem(
                id = "item-123",
                menuItem = MenuItemEntity(id = 2, category = "飲品", name = "珍珠奶茶", price = 60, imageUrl = "", optionsGroup = null),
                quantity = 2,
                options = emptyList(),
                customPrice = null,
                isComplimentary = false
            )
        )
        fakeDraftDao.insertDraft(
            OrderDraftEntity(
                draftId = "ACTIVE_CART_DEFAULT",
                draftType = "ACTIVE_CART",
                timestamp = System.currentTimeMillis(),
                tableNumber = "A-5",
                transactionType = "內用",
                createdAtText = "2026-08-16 10:00:00",
                itemsJson = Gson().toJson(initialItems)
            )
        )

        // Initialize ViewModel (simulating app restart)
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.cartItems.value.size)
        assertEquals("珍珠奶茶", viewModel.cartItems.value.first().menuItem.name)
        assertEquals(2, viewModel.cartItems.value.first().quantity)
        assertEquals("A-5", viewModel.tableNumber.value)
        assertEquals("內用", viewModel.transactionType.value)
    }

    @Test
    fun cancelEntireTransaction_clearsActiveDraft() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = MenuItemEntity(id = 1, category = "主餐", name = "牛肉漢堡", price = 120, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(fakeDraftDao.getDraftById("ACTIVE_CART_DEFAULT"))

        viewModel.cancelEntireTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeDraftDao.getDraftById("ACTIVE_CART_DEFAULT"))
    }

    @Test
    fun tempOrder_persistsAndRestoresAcrossRestart() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = MenuItemEntity(id = 1, category = "主餐", name = "雞腿堡", price = 110, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        viewModel.setTableNumber("B-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveCurrentOrderToTemp()
        testDispatcher.scheduler.advanceUntilIdle()

        // Draft saved in DB
        val tempDrafts = fakeDraftDao.getDraftsListByType("TEMP_SAVED")
        assertEquals(1, tempDrafts.size)
        assertEquals("B-1", tempDrafts.first().tableNumber)

        // New ViewModel session loads temp draft
        val newViewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, newViewModel.tempSavedOrders.value.size)
        assertEquals("B-1", newViewModel.tempSavedOrders.value.first().tableNumber)
    }

    private class FakeOrderDraftDao : OrderDraftDao {
        val drafts = mutableListOf<OrderDraftEntity>()

        override suspend fun insertDraft(draft: OrderDraftEntity) {
            drafts.removeAll { it.draftId == draft.draftId }
            drafts.add(draft)
        }

        override fun getDraftsByType(type: String): Flow<List<OrderDraftEntity>> {
            return flowOf(drafts.filter { it.draftType == type })
        }

        override suspend fun getDraftsListByType(type: String): List<OrderDraftEntity> {
            return drafts.filter { it.draftType == type }
        }

        override suspend fun getDraftById(draftId: String): OrderDraftEntity? {
            return drafts.firstOrNull { it.draftId == draftId }
        }

        override suspend fun deleteDraft(draftId: String) {
            drafts.removeAll { it.draftId == draftId }
        }

        override suspend fun clearDraftsByType(type: String) {
            drafts.removeAll { it.draftType == type }
        }
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
