package com.example.supos.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.model.CartItem
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

  sealed class CheckoutUiState {
      object Idle : CheckoutUiState()
      object Submitting : CheckoutUiState()
      data class Success(val orderId: String, val isSynced: Boolean) : CheckoutUiState()
      data class Error(val message: String) : CheckoutUiState()
  }

  sealed class TurnoverUiState {
      object Idle : TurnoverUiState()
      object Loading : TurnoverUiState()
      data class Success(val amount: Double, val fetchedAt: String) : TurnoverUiState()
      data class Error(val message: String, val lastKnownAmount: Double? = null) : TurnoverUiState()
  }

  sealed class MenuSyncUiState {
      object Idle : MenuSyncUiState()
      object Syncing : MenuSyncUiState()
      data class Success(val lastSyncedAt: Long) : MenuSyncUiState()
      data class Error(val message: String) : MenuSyncUiState()
  }

  // ---- 分類與翻頁狀態 ----
  val selectedCategory = MutableStateFlow<String?>(null)
  val categoryPage = MutableStateFlow(0)
  val itemPage = MutableStateFlow(0)

  // ---- 購物車狀態 ----
  private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
  val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

  val uiState: StateFlow<MainScreenUiState> =
    combine(
      menuRepository.getMenuItems(),
      _cartItems
    ) { menuItems, cartItems ->
      MainScreenUiState.Success(menuItems, cartItems) as MainScreenUiState
    }
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  private val _selectedCartItemId = MutableStateFlow<String?>(null)
  val selectedCartItemId: StateFlow<String?> = _selectedCartItemId

  private val _selectedOptionName = MutableStateFlow<String?>(null)
  val selectedOptionName: StateFlow<String?> = _selectedOptionName

  private val _transactionType = MutableStateFlow("外帶")
  val transactionType: StateFlow<String> = _transactionType

  private val _tableNumber = MutableStateFlow("")
  val tableNumber: StateFlow<String> = _tableNumber

  private val _invoiceNumber = MutableStateFlow("")
  val invoiceNumber: StateFlow<String> = _invoiceNumber

  private val _ubn = MutableStateFlow("")
  val ubn: StateFlow<String> = _ubn

  private val _carrier = MutableStateFlow("")
  val carrier: StateFlow<String> = _carrier

  private val _createdAt = MutableStateFlow("尚未建立")
  val createdAt: StateFlow<String> = _createdAt

  private val _allOptions = MutableStateFlow<List<OptionItemEntity>>(emptyList())
  val allOptions: StateFlow<List<OptionItemEntity>> = _allOptions

  private val _unmappedNotice = MutableStateFlow<String?>(null)
  val unmappedNotice: StateFlow<String?> = _unmappedNotice

  fun clearUnmappedNotice() {
      _unmappedNotice.value = null
  }

  private var activeCheckoutDraft: com.example.supos.data.model.CheckoutDraft? = null

  // ---- 暫存區與日營業額狀態 ----
  private val _tempSavedOrders = MutableStateFlow<List<com.example.supos.data.model.TempOrder>>(emptyList())
  val tempSavedOrders: StateFlow<List<com.example.supos.data.model.TempOrder>> = _tempSavedOrders

  private val _currentPOSViewMode = MutableStateFlow(POSViewMode.MANUAL_ORDERING)
  val currentPOSViewMode: StateFlow<POSViewMode> = _currentPOSViewMode

  private val _todayTurnover = MutableStateFlow(0.0)
  val todayTurnover: StateFlow<Double> = _todayTurnover

  private val _isFetchingTurnover = MutableStateFlow(false)
  val isFetchingTurnover: StateFlow<Boolean> = _isFetchingTurnover

  val unsyncedOrdersCount: StateFlow<Int> = orderRepository.unsyncedCount
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  private val _outboxSyncNotice = MutableStateFlow<String?>(null)
  val outboxSyncNotice: StateFlow<String?> = _outboxSyncNotice.asStateFlow()

  fun clearOutboxSyncNotice() {
      _outboxSyncNotice.value = null
  }

  fun retrySyncOutboxOrders() {
      viewModelScope.launch {
          val result = orderRepository.syncPendingOutboxOrders()
          if (result.isSuccess) {
              val count = result.getOrNull() ?: 0
              _outboxSyncNotice.value = if (count > 0) "已成功同步 $count 筆離線訂單至雲端" else "目前所有訂單已完成同步"
          } else {
              val msg = result.exceptionOrNull()?.message ?: "同步失敗"
              _outboxSyncNotice.value = "離線同步失敗：$msg"
          }
      }
  }

  private val _selectedPaymentMethod = MutableStateFlow(com.example.supos.data.model.PaymentMethod.CASH)
  val selectedPaymentMethod: StateFlow<com.example.supos.data.model.PaymentMethod> = _selectedPaymentMethod.asStateFlow()

  private val checkoutMutex = kotlinx.coroutines.sync.Mutex()
  private val _checkoutUiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
  val checkoutUiState: StateFlow<CheckoutUiState> = _checkoutUiState.asStateFlow()

  fun resetCheckoutUiState() {
      _checkoutUiState.value = CheckoutUiState.Idle
  }

  private val _turnoverUiState = MutableStateFlow<TurnoverUiState>(TurnoverUiState.Idle)
  val turnoverUiState: StateFlow<TurnoverUiState> = _turnoverUiState.asStateFlow()

  private val _menuSyncUiState = MutableStateFlow<MenuSyncUiState>(MenuSyncUiState.Idle)
  val menuSyncUiState: StateFlow<MenuSyncUiState> = _menuSyncUiState.asStateFlow()

  init {
      viewModelScope.launch {
          menuRepository.refreshMenuFromNetwork()
      }
      // 當菜單載入後自動選取第一個分類
      viewModelScope.launch {
          menuRepository.getMenuItems().collect { items ->
              val cats = items.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
              if (selectedCategory.value == null && cats.isNotEmpty()) {
                  selectedCategory.value = cats.first()
              }
          }
      }
      // 載入所有加料/客製化選項
      viewModelScope.launch {
          menuRepository.getOptions().collect { options ->
              _allOptions.value = options
          }
      }
      // 開機自動還原持久化之購物車草稿與暫存掛單
      viewModelScope.launch {
          val draft = orderRepository.getActiveCartDraft()
          if (draft != null && _cartItems.value.isEmpty()) {
              try {
                  val listType = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
                  val items: List<CartItem> = com.google.gson.Gson().fromJson(draft.itemsJson, listType)
                  if (items.isNotEmpty()) {
                      _cartItems.value = items
                      _selectedCartItemId.value = items.lastOrNull()?.id
                      _tableNumber.value = draft.tableNumber
                      _transactionType.value = draft.transactionType
                      _createdAt.value = draft.createdAtText
                  }
              } catch (_: Throwable) {}
          }
      }
      viewModelScope.launch {
          val tempDrafts = orderRepository.getTempOrderDrafts()
          if (tempDrafts.isNotEmpty()) {
              _tempSavedOrders.value = tempDrafts
          }
      }
      // 購物車變更自動保存草稿
      viewModelScope.launch {
          kotlinx.coroutines.flow.combine(_cartItems, _tableNumber, _transactionType, _createdAt) { cart, table, type, created ->
              orderRepository.saveActiveCartDraft(table, type, created, cart)
          }.collectLatest {}
      }
      // 監聽線上訂單帶入結帳草稿 (CheckoutDraft) - 單一 Owner 獨佔消費
      viewModelScope.launch {
          orderRepository.checkoutDraft.collect { draft ->
              if (draft != null) {
                  activeCheckoutDraft = draft
                  _cartItems.value = draft.cartItems
                  _selectedCartItemId.value = draft.cartItems.lastOrNull()?.id
                  _selectedOptionName.value = null
                  _createdAt.value = draft.timestamp
                  _transactionType.value = draft.transactionType
                  _currentPOSViewMode.value = POSViewMode.MANUAL_ORDERING

                  if (draft.unmappedItems.isNotEmpty()) {
                      _unmappedNotice.value = "已帶入結帳！注意：部分品項（如：${draft.unmappedItems.joinToString()}）已下架，未加入購物車"
                  } else {
                      _unmappedNotice.value = null
                  }

                  // 單次消費交接草稿
                  orderRepository.consumeCheckoutDraft()
              }
          }
      }
  }

  fun selectCategory(category: String) {
      selectedCategory.value = category
      itemPage.value = 0  // 換分類時重設品項頁碼
  }

  fun selectCartItem(id: String?) {
      _selectedCartItemId.value = id
      _selectedOptionName.value = null // 選取新商品時，重設加料選取
  }

  fun selectOption(optName: String?) {
      _selectedOptionName.value = optName
  }

  fun addToCart(menuItem: MenuItemEntity) {
      val currentCart = _cartItems.value.toMutableList()
      if (currentCart.isEmpty()) {
          val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
          _createdAt.value = formatter.format(java.util.Date())
      }
      // Note: Tap item always creates a new cart item so they can customize its options independently
      val newItem = CartItem(menuItem = menuItem)
      currentCart.add(newItem)
      _cartItems.value = currentCart
      _selectedCartItemId.value = newItem.id
      _selectedOptionName.value = null
  }

  fun addOptionToSelected(option: OptionItemEntity) {
      val id = _selectedCartItemId.value ?: return
      val currentCart = _cartItems.value.toMutableList()
      val idx = currentCart.indexOfFirst { it.id == id }
      if (idx == -1) return

      val item = currentCart[idx]
      val updatedOptions = item.options.toMutableList()

      if (option.optionType.lowercase() == "single") {
          // Remove options from the same group
          updatedOptions.removeAll { it.groupName == option.groupName }
          updatedOptions.add(option)
          _selectedOptionName.value = null
      } else {
          // Multi option: directly append another copy
          updatedOptions.add(option)
          _selectedOptionName.value = option.name
      }

      currentCart[idx] = item.copy(options = updatedOptions)
      _cartItems.value = currentCart
  }

  fun adjustSelectedQuantity(adjustment: Int) {
      val id = _selectedCartItemId.value ?: return
      val currentCart = _cartItems.value.toMutableList()
      val idx = currentCart.indexOfFirst { it.id == id }
      if (idx == -1) return

      val item = currentCart[idx]
      val optName = _selectedOptionName.value

      if (optName != null) {
          // Adjust options quantity
          val updatedOptions = item.options.toMutableList()
          if (adjustment > 0) {
              val optToDuplicate = updatedOptions.firstOrNull { it.name == optName }
              if (optToDuplicate != null) {
                  updatedOptions.add(optToDuplicate)
              }
          } else if (adjustment < 0) {
              val lastIdx = updatedOptions.indexOfLast { it.name == optName }
              if (lastIdx != -1) {
                  updatedOptions.removeAt(lastIdx)
              }
              if (updatedOptions.none { it.name == optName }) {
                  _selectedOptionName.value = null
              }
          }
          currentCart[idx] = item.copy(options = updatedOptions)
      } else {
          // Adjust main item quantity
          val newQty = item.quantity + adjustment
          if (newQty < 1) {
              currentCart.removeAt(idx)
              val lastItem = currentCart.lastOrNull()
              _selectedCartItemId.value = lastItem?.id
          } else {
              currentCart[idx] = item.copy(quantity = newQty)
          }
      }
      _cartItems.value = currentCart
  }

  fun deleteSelectedCartItem() {
      val id = _selectedCartItemId.value ?: return
      val currentCart = _cartItems.value.toMutableList()
      val idx = currentCart.indexOfFirst { it.id == id }
      if (idx == -1) return

      val optName = _selectedOptionName.value
      if (optName != null) {
          // Delete option
          val item = currentCart[idx]
          val updatedOptions = item.options.filterNot { it.name == optName }
          currentCart[idx] = item.copy(options = updatedOptions)
          _selectedOptionName.value = null
      } else {
          // Delete main item
          currentCart.removeAt(idx)
          val lastItem = currentCart.lastOrNull()
          _selectedCartItemId.value = lastItem?.id
          _selectedOptionName.value = null
      }
      _cartItems.value = currentCart
  }

  fun clearOptionsOfSelected() {
      val id = _selectedCartItemId.value ?: return
      val currentCart = _cartItems.value.toMutableList()
      val idx = currentCart.indexOfFirst { it.id == id }
      if (idx != -1) {
          currentCart[idx] = currentCart[idx].copy(options = emptyList())
          _selectedOptionName.value = null
          _cartItems.value = currentCart
      }
  }

  fun applyComplimentary() {
      val id = _selectedCartItemId.value
      val currentCart = _cartItems.value.toMutableList()
      if (id != null) {
          val idx = currentCart.indexOfFirst { it.id == id }
          if (idx != -1) {
              currentCart[idx] = currentCart[idx].copy(isComplimentary = !currentCart[idx].isComplimentary)
          }
      } else if (currentCart.isNotEmpty()) {
          for (i in currentCart.indices) {
              currentCart[i] = currentCart[i].copy(isComplimentary = true)
          }
      }
      _cartItems.value = currentCart
  }

  fun changePriceOfSelected(newPrice: Int?) {
      val id = _selectedCartItemId.value ?: return
      val currentCart = _cartItems.value.toMutableList()
      val idx = currentCart.indexOfFirst { it.id == id }
      if (idx != -1) {
          currentCart[idx] = currentCart[idx].copy(customPrice = newPrice)
          _cartItems.value = currentCart
      }
  }

  fun toggleTransactionType() {
      val types = listOf("外帶", "內用", "Line自取", "電話點餐")
      val currentType = _transactionType.value
      val nextIdx = (types.indexOf(currentType) + 1) % types.size
      _transactionType.value = types[nextIdx]
  }

  fun setTableNumber(table: String) {
      _tableNumber.value = table
  }

  fun cancelEntireTransaction() {
      _cartItems.value = emptyList()
      _selectedCartItemId.value = null
      _tableNumber.value = ""
      _transactionType.value = "外帶"
      _invoiceNumber.value = ""
      _ubn.value = ""
      _carrier.value = ""
      _createdAt.value = "尚未建立"
      activeCheckoutDraft = null
      viewModelScope.launch {
          orderRepository.clearActiveCartDraft()
      }
  }

  fun clearCart() {
      cancelEntireTransaction()
  }

  fun selectPaymentMethod(pm: com.example.supos.data.model.PaymentMethod) {
      _selectedPaymentMethod.value = pm
  }

  fun setInvoiceInfo(invoice: String = "", ubn: String = "", carrier: String = "") {
      _invoiceNumber.value = invoice
      _ubn.value = ubn
      _carrier.value = carrier
  }

  fun checkout() {
      val cartItems = _cartItems.value
      if (cartItems.isEmpty()) return

      val currentUbn = _ubn.value.trim()
      if (currentUbn.isNotEmpty() && !com.example.supos.data.util.InvoiceValidator.validateUbn(currentUbn)) {
          _checkoutUiState.value = CheckoutUiState.Error("統一編號格式錯誤，請輸入有效之 8 碼統編")
          return
      }

      val currentCarrier = _carrier.value.trim()
      if (currentCarrier.isNotEmpty() && !com.example.supos.data.util.InvoiceValidator.validateCarrier(currentCarrier)) {
          _checkoutUiState.value = CheckoutUiState.Error("手機載具格式錯誤（需以 / 開頭加 7 碼英數符號）")
          return
      }

      if (!checkoutMutex.tryLock()) return // 防連點雙發重入鎖

      val totalAmount = cartItems.sumOf { it.subtotal }
      val table = _tableNumber.value
      val type = if (table.isNotEmpty() && _transactionType.value == "內用") {
          "內用 (桌號: $table)"
      } else {
          _transactionType.value
      }

      val draft = activeCheckoutDraft
      val sourceType = draft?.sourceType ?: "OFFLINE_POS"
      val platformCode = draft?.platformCode ?: "POS"
      val sourceOrderId = draft?.sourceOrderId
      val paymentMethod = _selectedPaymentMethod.value.displayName

      viewModelScope.launch {
          try {
              _checkoutUiState.value = CheckoutUiState.Submitting

              val result = orderRepository.checkoutOrder(
                  cartItems = cartItems,
                  totalAmount = totalAmount,
                  orderType = type,
                  paymentMethod = paymentMethod,
                  sourceType = sourceType,
                  platformCode = platformCode,
                  sourceOrderId = sourceOrderId,
                  invoiceNumber = _invoiceNumber.value,
                  ubn = currentUbn,
                  carrier = currentCarrier
              )

              if (result.isSuccess) {
                  val checkoutResult = result.getOrThrow()
                  cancelEntireTransaction() // Room 本地保存成功才清空購物車
                  _checkoutUiState.value = CheckoutUiState.Success(
                      orderId = checkoutResult.orderId,
                      isSynced = checkoutResult.isSynced
                  )
              } else {
                  val errorMsg = result.exceptionOrNull()?.message ?: "保存失敗"
                  // Room 本地寫入失敗，購物車不清空！
                  _checkoutUiState.value = CheckoutUiState.Error(errorMsg)
              }
          } catch (e: Exception) {
              _checkoutUiState.value = CheckoutUiState.Error(e.message ?: "結帳時發生錯誤")
          } finally {
              checkoutMutex.unlock()
          }
      }
  }

  fun changeViewMode(mode: POSViewMode) {
      _currentPOSViewMode.value = mode
      if (mode == POSViewMode.DAILY_TURNOVER) {
          fetchTodayTurnover()
      }
  }

  fun saveCurrentOrderToTemp() {
      val currentCart = _cartItems.value
      if (currentCart.isEmpty()) return

      val newTemp = com.example.supos.data.model.TempOrder(
          items = currentCart,
          transactionType = _transactionType.value,
          tableNumber = _tableNumber.value,
          createdAt = _createdAt.value,
          invoiceNumber = _invoiceNumber.value,
          ubn = _ubn.value,
          carrier = _carrier.value
      )
      val updatedList = _tempSavedOrders.value.toMutableList()
      updatedList.add(0, newTemp)
      if (updatedList.size > 5) {
          val removed = updatedList.removeAt(updatedList.size - 1)
          viewModelScope.launch {
              orderRepository.deleteTempOrderDraft(removed.id)
          }
      }
      _tempSavedOrders.value = updatedList
      viewModelScope.launch {
          orderRepository.saveTempOrderDraft(newTemp)
      }
      cancelEntireTransaction()
  }

  fun restoreOrderFromTemp(tempOrder: com.example.supos.data.model.TempOrder) {
      _cartItems.value = tempOrder.items
      _transactionType.value = tempOrder.transactionType
      _tableNumber.value = tempOrder.tableNumber
      _createdAt.value = tempOrder.createdAt
      _invoiceNumber.value = tempOrder.invoiceNumber
      _ubn.value = tempOrder.ubn
      _carrier.value = tempOrder.carrier
      
      val updatedList = _tempSavedOrders.value.toMutableList()
      updatedList.removeAll { it.id == tempOrder.id }
      _tempSavedOrders.value = updatedList
      viewModelScope.launch {
          orderRepository.deleteTempOrderDraft(tempOrder.id)
      }
      _currentPOSViewMode.value = POSViewMode.MANUAL_ORDERING
  }

  fun manualRefreshMenu() {
      viewModelScope.launch {
          _menuSyncUiState.value = MenuSyncUiState.Syncing
          val result = menuRepository.refreshMenuFromNetwork()
          if (result.isSuccess) {
              _menuSyncUiState.value = MenuSyncUiState.Success(System.currentTimeMillis())
          } else {
              _menuSyncUiState.value = MenuSyncUiState.Error(result.exceptionOrNull()?.message ?: "菜單更新失敗")
          }
      }
  }

  fun fetchTodayTurnover() {
      viewModelScope.launch {
          _isFetchingTurnover.value = true
          _turnoverUiState.value = TurnoverUiState.Loading
          val result = orderRepository.fetchTodayTurnover()
          _isFetchingTurnover.value = false
          if (result.isSuccess) {
              val amount = result.getOrNull() ?: 0.0
              _todayTurnover.value = amount
              val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
              _turnoverUiState.value = TurnoverUiState.Success(amount, timeFormat)
          } else {
              val errMsg = result.exceptionOrNull()?.message ?: "連線逾時或讀取失敗"
              _turnoverUiState.value = TurnoverUiState.Error(errMsg, _todayTurnover.value.takeIf { it > 0 })
          }
      }
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState

  data class Success(val items: List<MenuItemEntity>, val cartItems: List<CartItem>) : MainScreenUiState
}
