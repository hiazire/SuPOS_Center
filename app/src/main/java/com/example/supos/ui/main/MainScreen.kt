package com.example.supos.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.supos.SuPOSApplication
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.theme.SuPOSTheme
import com.example.supos.ui.adaptive.TabletLayoutProportions
import com.example.supos.ui.adaptive.rememberWindowSizeClass
import com.example.supos.ui.main.components.CartColumn
import com.example.supos.ui.main.components.DailyTurnoverColumn
import com.example.supos.ui.main.components.FunctionColumn
import com.example.supos.ui.main.components.MenuColumn
import com.example.supos.ui.main.components.TempOrdersColumn


@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  onBackToDashboard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val application = LocalContext.current.applicationContext as SuPOSApplication
  val viewModel: MainScreenViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainScreenViewModel(application.menuRepository, application.orderRepository) as T
      }
    }
  )

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val categoryPage by viewModel.categoryPage.collectAsStateWithLifecycle()
  val itemPage by viewModel.itemPage.collectAsStateWithLifecycle()
  val transactionType by viewModel.transactionType.collectAsStateWithLifecycle()
  val tableNumber by viewModel.tableNumber.collectAsStateWithLifecycle()
  val selectedCartItemId by viewModel.selectedCartItemId.collectAsStateWithLifecycle()
  val selectedOptionName by viewModel.selectedOptionName.collectAsStateWithLifecycle()
  val allOptions by viewModel.allOptions.collectAsStateWithLifecycle()
  val currentPOSViewMode by viewModel.currentPOSViewMode.collectAsStateWithLifecycle()
  val tempSavedOrders by viewModel.tempSavedOrders.collectAsStateWithLifecycle()
  val todayTurnover by viewModel.todayTurnover.collectAsStateWithLifecycle()
  val isFetchingTurnover by viewModel.isFetchingTurnover.collectAsStateWithLifecycle()
  val turnoverUiState by viewModel.turnoverUiState.collectAsStateWithLifecycle()
  val menuSyncUiState by viewModel.menuSyncUiState.collectAsStateWithLifecycle()
  val createdAt by viewModel.createdAt.collectAsStateWithLifecycle()
  val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
  val ubn by viewModel.ubn.collectAsStateWithLifecycle()
  val carrier by viewModel.carrier.collectAsStateWithLifecycle()
  val unmappedNotice by viewModel.unmappedNotice.collectAsStateWithLifecycle()
  val checkoutUiState by viewModel.checkoutUiState.collectAsStateWithLifecycle()
  val unsyncedOrdersCount by viewModel.unsyncedOrdersCount.collectAsStateWithLifecycle()
  val outboxSyncNotice by viewModel.outboxSyncNotice.collectAsStateWithLifecycle()

  val context = androidx.compose.ui.platform.LocalContext.current
  androidx.compose.runtime.LaunchedEffect(unmappedNotice) {
    unmappedNotice?.let { notice ->
      android.widget.Toast.makeText(context, notice, android.widget.Toast.LENGTH_LONG).show()
      viewModel.clearUnmappedNotice()
    }
  }

  androidx.compose.runtime.LaunchedEffect(outboxSyncNotice) {
    outboxSyncNotice?.let { notice ->
      android.widget.Toast.makeText(context, notice, android.widget.Toast.LENGTH_SHORT).show()
      viewModel.clearOutboxSyncNotice()
    }
  }

  androidx.compose.runtime.LaunchedEffect(checkoutUiState) {
    when (val s = checkoutUiState) {
      is MainScreenViewModel.CheckoutUiState.Success -> {
        val msg = if (s.isSynced) {
          "結帳成功！單號：${s.orderId.take(8)}"
        } else {
          "訂單已保存至本地（單號：${s.orderId.take(8)}，等待背景自動同步）"
        }
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        viewModel.resetCheckoutUiState()
      }
      is MainScreenViewModel.CheckoutUiState.Error -> {
        android.widget.Toast.makeText(context, "結帳失敗：${s.message}（購物車已保留）", android.widget.Toast.LENGTH_LONG).show()
        viewModel.resetCheckoutUiState()
      }
      else -> {}
    }
  }

  when (state) {
    MainScreenUiState.Loading -> {
      // Blank while loading
    }
    is MainScreenUiState.Success -> {
      val successState = state as MainScreenUiState.Success
      val selectedCartItem = successState.cartItems.find { it.id == selectedCartItemId }

      MainScreenContent(
          items = successState.items,
          cartItems = successState.cartItems,
          selectedCategory = selectedCategory,
          categoryPage = categoryPage,
          itemPage = itemPage,
          transactionType = transactionType,
          tableNumber = tableNumber,
          selectedCartItemId = selectedCartItemId,
          selectedOptionName = selectedOptionName,
          selectedCartItem = selectedCartItem,
          allOptions = allOptions,
          currentPOSViewMode = currentPOSViewMode,
          tempSavedOrders = tempSavedOrders,
          todayTurnover = todayTurnover,
          isFetchingTurnover = isFetchingTurnover,
          turnoverUiState = turnoverUiState,
          onRetryTurnover = { viewModel.fetchTodayTurnover() },
          createdAt = createdAt,
          invoiceNumber = invoiceNumber,
          ubn = ubn,
          carrier = carrier,
          onCategorySelect = { viewModel.selectCategory(it) },
          onCategoryPageChange = { viewModel.categoryPage.value = it },
          onItemPageChange = { viewModel.itemPage.value = it },
          onAddToCart = { viewModel.addToCart(it) },
          onClearCart = { viewModel.clearCart() },
          onCheckout = { viewModel.checkout() },
          onNavigateToHistory = { onItemClick(com.example.supos.OrderHistory) },
          onNavigateToOnlineOrder = { onItemClick(com.example.supos.OnlineOrderReceiver) },
          onAdjustQuantity = { viewModel.adjustSelectedQuantity(it) },
          onDeleteItem = { viewModel.deleteSelectedCartItem() },
          onClearOptions = { viewModel.clearOptionsOfSelected() },
          onApplyComplimentary = { viewModel.applyComplimentary() },
          onChangePrice = { viewModel.changePriceOfSelected(it) },
          onToggleTransactionType = { viewModel.toggleTransactionType() },
          onSetTableNumber = { viewModel.setTableNumber(it) },
          onSelectCartItem = { viewModel.selectCartItem(it) },
          onOptionSelect = { viewModel.selectOption(it) },
          onOptionClick = { viewModel.addOptionToSelected(it) },
          onSaveOrderToTemp = { viewModel.saveCurrentOrderToTemp() },
          onShowTempOrders = { viewModel.changeViewMode(POSViewMode.TEMP_ORDERS) },
          onShowDailyTurnover = { viewModel.changeViewMode(POSViewMode.DAILY_TURNOVER) },
          onShowManualOrdering = { viewModel.changeViewMode(POSViewMode.MANUAL_ORDERING) },
          onBackToDashboard = onBackToDashboard,
          onRestoreOrder = { viewModel.restoreOrderFromTemp(it) },
          isSubmitting = checkoutUiState is MainScreenViewModel.CheckoutUiState.Submitting,
          unsyncedOrdersCount = unsyncedOrdersCount,
          onRetrySyncOutbox = { viewModel.retrySyncOutboxOrders() },
          modifier = modifier
      )
    }
    is MainScreenUiState.Error -> {
      Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
    }
  }
}

@Composable
internal fun MainScreenContent(
    items: List<MenuItemEntity>,
    cartItems: List<com.example.supos.data.model.CartItem>,
    selectedCategory: String?,
    categoryPage: Int,
    itemPage: Int,
    transactionType: String,
    tableNumber: String,
    selectedCartItemId: String?,
    selectedOptionName: String?,
    selectedCartItem: com.example.supos.data.model.CartItem?,
    allOptions: List<com.example.supos.data.local.entity.OptionItemEntity>,
    currentPOSViewMode: POSViewMode,
    tempSavedOrders: List<com.example.supos.data.model.TempOrder>,
    todayTurnover: Double,
    isFetchingTurnover: Boolean,
    turnoverUiState: MainScreenViewModel.TurnoverUiState = MainScreenViewModel.TurnoverUiState.Idle,
    onRetryTurnover: () -> Unit = {},
    createdAt: String,
    invoiceNumber: String,
    ubn: String,
    carrier: String,
    onCategorySelect: (String) -> Unit,
    onCategoryPageChange: (Int) -> Unit,
    onItemPageChange: (Int) -> Unit,
    onAddToCart: (MenuItemEntity) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOnlineOrder: () -> Unit = {},
    onAdjustQuantity: (Int) -> Unit,
    onDeleteItem: () -> Unit,
    onClearOptions: () -> Unit,
    onApplyComplimentary: () -> Unit,
    onChangePrice: (Int?) -> Unit,
    onToggleTransactionType: () -> Unit,
    onSetTableNumber: (String) -> Unit,
    onSelectCartItem: (String) -> Unit,
    onOptionSelect: (String?) -> Unit,
    onOptionClick: (com.example.supos.data.local.entity.OptionItemEntity) -> Unit,
    onSaveOrderToTemp: () -> Unit,
    onShowTempOrders: () -> Unit,
    onShowDailyTurnover: () -> Unit,
    onShowManualOrdering: () -> Unit,
    onBackToDashboard: () -> Unit,
    onRestoreOrder: (com.example.supos.data.model.TempOrder) -> Unit,
    isSubmitting: Boolean = false,
    unsyncedOrdersCount: Int = 0,
    onRetrySyncOutbox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val windowSize = rememberWindowSizeClass(overrideWidthDp = maxWidth, overrideHeightDp = maxHeight)

    @Composable
    fun RenderRightWorkspace(mod: Modifier) {
      Box(modifier = mod.fillMaxHeight()) {
        when (currentPOSViewMode) {
          POSViewMode.MANUAL_ORDERING -> {
            MenuColumn(
                allItems = items,
                selectedCategory = selectedCategory,
                categoryPage = categoryPage,
                itemPage = itemPage,
                selectedCartItem = selectedCartItem,
                allOptions = allOptions,
                onCategorySelect = onCategorySelect,
                onCategoryPageChange = onCategoryPageChange,
                onItemPageChange = onItemPageChange,
                onItemClick = onAddToCart,
                onOptionClick = onOptionClick,
                modifier = Modifier.fillMaxSize()
            )
          }
          POSViewMode.TEMP_ORDERS -> {
            TempOrdersColumn(
                tempSavedOrders = tempSavedOrders,
                onRestoreOrder = onRestoreOrder,
                onBackToOrdering = onShowManualOrdering,
                modifier = Modifier.fillMaxSize()
            )
          }
          POSViewMode.DAILY_TURNOVER -> {
            DailyTurnoverColumn(
                todayTurnover = todayTurnover,
                isFetching = isFetchingTurnover,
                turnoverUiState = turnoverUiState,
                onRetryTurnover = onRetryTurnover,
                onBackToOrdering = onShowManualOrdering,
                modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }

    if (windowSize.isExpanded) {
      val proportions = TabletLayoutProportions.getProportions(windowSize.isLargeTabletLandscape)
      // 10-12 吋平板橫向：三欄填滿螢幕；Tab S7 採 34% / 13% / 53%。
      Row(
          modifier = Modifier
              .fillMaxSize()
              .background(PosDesignTokens.AppBackground),
          horizontalArrangement = Arrangement.spacedBy(PosDesignTokens.PanelGap)
      ) {
        CartColumn(
            cartItems = cartItems,
            selectedCartItemId = selectedCartItemId,
            selectedOptionName = selectedOptionName,
            createdAt = createdAt,
            transactionType = transactionType,
            tableNumber = tableNumber,
            invoiceNumber = invoiceNumber,
            ubn = ubn,
            carrier = carrier,
            onItemSelect = onSelectCartItem,
            onOptionSelect = onOptionSelect,
            modifier = Modifier.weight(proportions.cartWeight)
        )

        FunctionColumn(
            transactionType = transactionType,
            tableNumber = tableNumber,
            selectedCartItemId = selectedCartItemId,
            onClearCart = onClearCart,
            onCheckout = onCheckout,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToOnlineOrder = onNavigateToOnlineOrder,
            onAdjustQuantity = onAdjustQuantity,
            onDeleteItem = onDeleteItem,
            onClearOptions = onClearOptions,
            onApplyComplimentary = onApplyComplimentary,
            onChangePrice = onChangePrice,
            onToggleTransactionType = onToggleTransactionType,
            onSetTableNumber = onSetTableNumber,
            onSaveOrderToTemp = onSaveOrderToTemp,
            onShowTempOrders = onShowTempOrders,
            onShowDailyTurnover = onShowDailyTurnover,
            onShowManualOrdering = onShowManualOrdering,
            onBackToDashboard = onBackToDashboard,
            isSubmitting = isSubmitting,
            unsyncedOrdersCount = unsyncedOrdersCount,
            onRetrySyncOutbox = onRetrySyncOutbox,
            modifier = Modifier.weight(proportions.functionWeight)
        )

        RenderRightWorkspace(mod = Modifier.weight(proportions.menuWeight))
      }
    } else if (windowSize.isMedium) {
      // 7 吋平板或 1/2 分割畫面: 雙欄自適應 (左側 Tab 切換購物車與功能 42%, 右側菜單 58%)
      var mediumLeftTab by remember { mutableStateOf(0) }
      Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
          TabRow(selectedTabIndex = mediumLeftTab, containerColor = Color(0xFFE0E0E0)) {
            Tab(
                selected = mediumLeftTab == 0,
                onClick = { mediumLeftTab = 0 },
                text = { Text("購物車 (${cartItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = mediumLeftTab == 1,
                onClick = { mediumLeftTab = 1 },
                text = { Text("門市功能", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
          }
          if (mediumLeftTab == 0) {
            CartColumn(
                cartItems = cartItems,
                selectedCartItemId = selectedCartItemId,
                selectedOptionName = selectedOptionName,
                createdAt = createdAt,
                transactionType = transactionType,
                tableNumber = tableNumber,
                invoiceNumber = invoiceNumber,
                ubn = ubn,
                carrier = carrier,
                onItemSelect = onSelectCartItem,
                onOptionSelect = onOptionSelect,
                modifier = Modifier.fillMaxSize()
            )
          } else {
            FunctionColumn(
                transactionType = transactionType,
                tableNumber = tableNumber,
                selectedCartItemId = selectedCartItemId,
                onClearCart = onClearCart,
                onCheckout = onCheckout,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToOnlineOrder = onNavigateToOnlineOrder,
                onAdjustQuantity = onAdjustQuantity,
                onDeleteItem = onDeleteItem,
                onClearOptions = onClearOptions,
                onApplyComplimentary = onApplyComplimentary,
                onChangePrice = onChangePrice,
                onToggleTransactionType = onToggleTransactionType,
                onSetTableNumber = onSetTableNumber,
                onSaveOrderToTemp = onSaveOrderToTemp,
                onShowTempOrders = onShowTempOrders,
                onShowDailyTurnover = onShowDailyTurnover,
                onShowManualOrdering = onShowManualOrdering,
                onBackToDashboard = onBackToDashboard,
                isSubmitting = isSubmitting,
                unsyncedOrdersCount = unsyncedOrdersCount,
                onRetrySyncOutbox = onRetrySyncOutbox,
                modifier = Modifier.fillMaxSize()
            )
          }
        }

        RenderRightWorkspace(mod = Modifier.weight(0.58f))
      }
    } else {
      // Compact (< 600dp, 分割畫面或直向小螢幕): 三分頁自適應切換，並具備購物車快速結帳列
      var compactTab by remember { mutableStateOf(0) }
      Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = compactTab,
            containerColor = Color(0xFF1B2B38),
            contentColor = Color.White
        ) {
          Tab(
              selected = compactTab == 0,
              onClick = { compactTab = 0 },
              text = { Text("點餐菜單", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
          Tab(
              selected = compactTab == 1,
              onClick = { compactTab = 1 },
              text = { Text("購物車 (${cartItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
          Tab(
              selected = compactTab == 2,
              onClick = { compactTab = 2 },
              text = { Text("門市功能", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (compactTab) {
            0 -> RenderRightWorkspace(mod = Modifier.fillMaxSize())
            1 -> {
              CartColumn(
                  cartItems = cartItems,
                  selectedCartItemId = selectedCartItemId,
                  selectedOptionName = selectedOptionName,
                  createdAt = createdAt,
                  transactionType = transactionType,
                  tableNumber = tableNumber,
                  invoiceNumber = invoiceNumber,
                  ubn = ubn,
                  carrier = carrier,
                  onItemSelect = onSelectCartItem,
                  onOptionSelect = onOptionSelect,
                  modifier = Modifier.fillMaxSize()
              )
            }
            2 -> {
              FunctionColumn(
                  transactionType = transactionType,
                  tableNumber = tableNumber,
                  selectedCartItemId = selectedCartItemId,
                  onClearCart = onClearCart,
                  onCheckout = onCheckout,
                  onNavigateToHistory = onNavigateToHistory,
                  onNavigateToOnlineOrder = onNavigateToOnlineOrder,
                  onAdjustQuantity = onAdjustQuantity,
                  onDeleteItem = onDeleteItem,
                  onClearOptions = onClearOptions,
                  onApplyComplimentary = onApplyComplimentary,
                  onChangePrice = onChangePrice,
                  onToggleTransactionType = onToggleTransactionType,
                  onSetTableNumber = onSetTableNumber,
                  onSaveOrderToTemp = onSaveOrderToTemp,
                  onShowTempOrders = onShowTempOrders,
                  onShowDailyTurnover = onShowDailyTurnover,
                  onShowManualOrdering = onShowManualOrdering,
                  onBackToDashboard = onBackToDashboard,
                  isSubmitting = isSubmitting,
                  unsyncedOrdersCount = unsyncedOrdersCount,
                  onRetrySyncOutbox = onRetrySyncOutbox,
                  modifier = Modifier.fillMaxSize()
              )
            }
          }
        }

        // 點餐中且購物車有品項時，顯示底部快速結帳條
        if (compactTab == 0 && cartItems.isNotEmpty()) {
          val total = cartItems.sumOf { it.subtotal }
          Surface(
              color = Color(0xFF26C6A2),
              modifier = Modifier
                  .fillMaxWidth()
                  .clickable { compactTab = 1 }
          ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Text("🛒 購物車 ${cartItems.size} 項 ($total 元)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("查看購物車 / 結帳 →", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  SuPOSTheme {
    MainScreenContent(
        items = emptyList(),
        cartItems = emptyList(),
        selectedCategory = null,
        categoryPage = 0,
        itemPage = 0,
        transactionType = "外帶",
        tableNumber = "",
        selectedCartItemId = null,
        selectedOptionName = null,
        selectedCartItem = null,
        allOptions = emptyList(),
        currentPOSViewMode = POSViewMode.MANUAL_ORDERING,
        tempSavedOrders = emptyList(),
        todayTurnover = 0.0,
        isFetchingTurnover = false,
        createdAt = "尚未建立",
        invoiceNumber = "",
        ubn = "",
        carrier = "",
        onCategorySelect = {},
        onCategoryPageChange = {},
        onItemPageChange = {},
        onAddToCart = {},
        onClearCart = {},
        onCheckout = {},
        onNavigateToHistory = {},
        onAdjustQuantity = {},
        onDeleteItem = {},
        onClearOptions = {},
        onApplyComplimentary = {},
        onChangePrice = {},
        onToggleTransactionType = {},
        onSetTableNumber = {},
        onSelectCartItem = {},
        onOptionSelect = {},
        onOptionClick = {},
        onSaveOrderToTemp = {},
        onShowTempOrders = {},
        onShowDailyTurnover = {},
        onShowManualOrdering = {},
        onBackToDashboard = {},
        onRestoreOrder = {}
    )
  }
}
