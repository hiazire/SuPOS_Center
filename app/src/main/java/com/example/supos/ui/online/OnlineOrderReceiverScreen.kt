package com.example.supos.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.supos.data.qburger.QBurgerOrderDetail
import com.example.supos.data.qburger.QBurgerOrderItem
import com.example.supos.data.qburger.QBurgerOrderState
import com.example.supos.data.qburger.QBurgerOrderSummary

// Platform Brand Colors
val ColorUberEats = Color(0xFF06C167)
val ColorFoodpanda = Color(0xFFD70F64)
val ColorQBApp = Color(0xFFFF7A00)
val ColorSelfOrder = Color(0xFF2D74FF)
val ColorDarkHeader = Color(0xFF1E222B)
val ColorCardBg = Color(0xFFF8F9FA)
val ColorSelectedCardBg = Color(0xFFE8F0FE)

@Composable
fun OnlineOrderReceiverScreen(
    onBack: () -> Unit,
    viewModel: OnlineOrderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ReceiverTopBar(
                isStoreOpen = uiState.isStoreOpen,
                selectedFilter = uiState.selectedSourceFilter,
                onFilterSelected = { viewModel.setSourceFilter(it) },
                onToggleStoreStatus = { showPinDialog = true }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE9ECEF))
        ) {
            // Left Column (35%): Order Queue Card List
            Box(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                OrderQueueColumn(
                    orders = uiState.orders.filter {
                        uiState.selectedSourceFilter == "ALL" || it.sourceCode.equals(uiState.selectedSourceFilter, ignoreCase = true)
                    },
                    selectedOrder = uiState.selectedOrder,
                    isLoading = uiState.isLoading,
                    onSelectOrder = { viewModel.selectOrder(it) }
                )
            }

            // Center Column (45%): Order Item Detail Panel
            Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
                OrderDetailColumn(
                    orderSummary = uiState.selectedOrder,
                    orderDetail = uiState.selectedOrderDetail,
                    isLoading = uiState.isDetailLoading
                )
            }

            // Right Column (20%): 1:1 Action Function Keys
            Box(modifier = Modifier.weight(0.20f).fillMaxHeight()) {
                FunctionActionColumn(
                    onRefresh = { viewModel.refreshOrders() },
                    onEnterOrder = { viewModel.markOrderEntered() },
                    onPrintKitchen = { viewModel.printCurrentOrderToKitchen() },
                    onReprint = { viewModel.reprintTicket() },
                    onCancelClick = { showCancelDialog = true },
                    onStoreSwitchClick = { showPinDialog = true },
                    onExit = onBack
                )
            }
        }
    }

    // Cancel Order Dialog
    if (showCancelDialog) {
        CancelReasonDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                viewModel.cancelOrder(reason)
                showCancelDialog = false
            }
        )
    }

    // PIN Safety Dialog
    if (showPinDialog) {
        StoreStatusPinDialog(
            isCurrentlyOpen = uiState.isStoreOpen,
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                if (viewModel.toggleStoreStatus(pin)) {
                    showPinDialog = false
                }
            }
        )
    }
}

@Composable
fun ReceiverTopBar(
    isStoreOpen: Boolean,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onToggleStoreStatus: () -> Unit
) {
    Surface(
        color = ColorDarkHeader,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // Store Title
            Column {
                Text(
                    text = "Q Burger 八德介壽店 [Q23031]",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "頤瑜瀚堡工作坊 統編: 93016436",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Store Status Toggle Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isStoreOpen) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                modifier = Modifier.clickable { onToggleStoreStatus() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(if (isStoreOpen) Color(0xFF00E676) else Color(0xFFFF5252), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isStoreOpen) "接單中 (營業)" else "已打烊 (暫停)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Filter Tabs
            val filters = listOf(
                "ALL" to "全部",
                "UberEats" to "UberEats",
                "Foodpanda" to "foodpanda",
                "QBAPPv2" to "會員App",
                "SelfOrder" to "桌邊掃碼"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEach { (code, label) ->
                    val isSelected = selectedFilter == code
                    val badgeColor = when (code) {
                        "UberEats" -> ColorUberEats
                        "Foodpanda" -> ColorFoodpanda
                        "QBAPPv2" -> ColorQBApp
                        "SelfOrder" -> ColorSelfOrder
                        else -> Color(0xFF455A64)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) badgeColor else Color(0xFF2C3240),
                        modifier = Modifier.clickable { onFilterSelected(code) }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderQueueColumn(
    orders: List<QBurgerOrderSummary>,
    selectedOrder: QBurgerOrderSummary?,
    isLoading: Boolean,
    onSelectOrder: (QBurgerOrderSummary) -> Unit
) {
    Surface(
        color = Color(0xFFF0F2F5),
        modifier = Modifier.fillMaxSize().padding(end = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Queue Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDEE2E6))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "📥 線上訂單隊列 (${orders.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF212529)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                }
            }

            if (orders.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("目前無符合條件之線上訂單", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(orders, key = { it.tradeId }) { order ->
                        val isSelected = order.tradeId == selectedOrder?.tradeId
                        OrderCardItem(
                            order = order,
                            isSelected = isSelected,
                            onClick = { onSelectOrder(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCardItem(
    order: QBurgerOrderSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val platformColor = when (order.sourceCode) {
        "UberEats" -> ColorUberEats
        "Foodpanda" -> ColorFoodpanda
        "QBAPPv2" -> ColorQBApp
        "SelfOrder" -> ColorSelfOrder
        else -> Color.DarkGray
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ColorSelectedCardBg else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF1A73E8), RoundedCornerShape(8.dp))
                else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Platform Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = platformColor,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = order.sourceName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "#${order.tradeId.takeLast(4)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status Badge
                val (stateBg, stateFg) = when (order.state) {
                    QBurgerOrderState.NEW_ORDER -> Color(0xFFFF9800) to Color.White
                    QBurgerOrderState.ORDERED -> Color(0xFF2196F3) to Color.White
                    QBurgerOrderState.PRINTED -> Color(0xFF4CAF50) to Color.White
                    QBurgerOrderState.CHECKED_OUT -> Color(0xFF9E9E9E) to Color.White
                    QBurgerOrderState.CANCELLED -> Color(0xFFF44336) to Color.White
                    QBurgerOrderState.NON_PAY_CANCEL -> Color(0xFF795548) to Color.White
                }
                Surface(shape = RoundedCornerShape(4.dp), color = stateBg) {
                    Text(
                        text = order.state.displayName,
                        color = stateFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (order.preTimeInterval.isNotBlank()) "取餐：${order.preTimeInterval}" else "下單：${order.createTime.takeLast(8)}",
                    fontSize = 13.sp,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (order.buyerNick.isNotBlank()) {
                    Text(text = "顧客：${order.buyerNick}", fontSize = 13.sp, color = Color(0xFF616161))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$ ${order.saleAmount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFFB71C1C)
                )
            }

            if (order.preRemark.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "備註: ${order.preRemark}",
                    fontSize = 11.sp,
                    color = Color(0xFFE65100),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun OrderDetailColumn(
    orderSummary: QBurgerOrderSummary?,
    orderDetail: QBurgerOrderDetail?,
    isLoading: Boolean
) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
    ) {
        if (orderSummary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("請從左側點選訂單以查看明細", color = Color.Gray, fontSize = 15.sp)
            }
            return@Surface
        }

        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "單號：${orderSummary.tradeId}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E222B)
                    )
                    Text(
                        text = "下單時間：${orderSummary.createTime}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (orderSummary.preTimeInterval.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF3E0)) {
                        Text(
                            text = "指定取餐：${orderSummary.preTimeInterval}",
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Block
            Surface(
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "顧客: ${orderSummary.buyerNick.ifEmpty { "外送顧客" }}  ${orderSummary.mobile}",
                        fontSize = 13.sp,
                        color = Color(0xFF333333)
                    )
                    if (orderSummary.eInvoiceVehicle.isNotBlank()) {
                        Text(
                            text = "載具: ${orderSummary.eInvoiceVehicle}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
            }

            // Customer Remark Card
            if (orderSummary.preRemark.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "📢 顧客備註：${orderSummary.preRemark}",
                        fontSize = 13.sp,
                        color = Color(0xFFF57F17),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Items List
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (orderDetail != null && orderDetail.items.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(orderDetail.items) { item ->
                        OrderItemRow(item)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("無品項明細或載入失敗", color = Color.Gray, fontSize = 13.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Bottom Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    text = "付款：${orderDetail?.paymentName ?: "線上支付"} (PAID)",
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "總計：$ ${orderSummary.saleAmount}",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFFB71C1C)
                )
            }
        }
    }
}

@Composable
fun OrderItemRow(item: QBurgerOrderItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFFFAFAFA), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${item.quantity} x",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.width(36.dp)
            )
            Text(
                text = item.productName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$ ${item.price}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF424242)
            )
        }

        if (item.productNote.isNotBlank()) {
            Text(
                text = "【${item.productNote}】",
                fontSize = 12.sp,
                color = Color(0xFFD84315),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 36.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun FunctionActionColumn(
    onRefresh: () -> Unit,
    onEnterOrder: () -> Unit,
    onPrintKitchen: () -> Unit,
    onReprint: () -> Unit,
    onCancelClick: () -> Unit,
    onStoreSwitchClick: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        color = Color(0xFF2C3240),
        modifier = Modifier.fillMaxSize().padding(start = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚙️ 功能操作列",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )

            ActionButton(
                label = "🔄 重新整理",
                containerColor = Color(0xFF1565C0),
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "📥 訂單入機",
                containerColor = Color(0xFFE65100),
                onClick = onEnterOrder,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "🖨️ 廚房出單",
                containerColor = Color(0xFF2E7D32),
                onClick = onPrintKitchen,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "🧾 補印單據",
                containerColor = Color(0xFF00838F),
                onClick = onReprint,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "🚫 退單/取消",
                containerColor = Color(0xFFC62828),
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "🏪 平台開關",
                containerColor = Color(0xFF4527A0),
                onClick = onStoreSwitchClick,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                label = "🚪 離開返回",
                containerColor = Color(0xFF37474F),
                onClick = onExit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun CancelReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val reasons = listOf(
        "產品原物料不足",
        "產品已下架",
        "商品主檔錯誤",
        "客人致電取消",
        "現金付款客人未取"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇退單原因 (回報總部)") },
        text = {
            Column {
                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = (r == selectedReason), onClick = { selectedReason = r })
                        Text(text = r, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                onClick = { onConfirm(selectedReason) }
            ) {
                Text("確認退單")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun StoreStatusPinDialog(
    isCurrentlyOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCurrentlyOpen) "切換為【關閉接單/打烊】" else "切換為【開始接單/營業】") },
        text = {
            Column {
                Text("請輸入操作安全驗證碼 (預設: 8991):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it
                        isError = false
                    },
                    isError = isError,
                    singleLine = true,
                    placeholder = { Text("4 位數驗證碼") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pinText) }) {
                Text("確認變更")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
