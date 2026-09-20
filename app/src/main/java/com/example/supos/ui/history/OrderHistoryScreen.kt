package com.example.supos.ui.history

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.supos.SuPOSApplication
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.ui.main.MainScreenViewModel
import com.example.supos.ui.main.MainScreenUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as SuPOSApplication
    val viewModel: OrderHistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrderHistoryViewModel(application.orderRepository, application.menuRepository) as T
            }
        }
    )

    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val selectedOrder by viewModel.selectedOrder.collectAsStateWithLifecycle()
    val selectedOrderItems by viewModel.selectedOrderItems.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val customStartDate by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEndDate by viewModel.customEndDate.collectAsStateWithLifecycle()
    val orderMutationStates by viewModel.orderMutationStates.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var orderToCancel by remember { mutableStateOf<HistoryOrderPayload?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.startAutoPolling()
        onDispose {
            viewModel.stopAutoPolling()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.newOrderAlertFlow.collect { event ->
            Toast.makeText(context, "🔔 收到 ${event.newOrdersCount} 筆新線上訂單！", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("線上訂單中心", fontWeight = FontWeight.Bold)
                        
                        // Connection State Indicator
                        when (connectionState) {
                            ConnectionState.CONNECTED -> {
                                Text(
                                    text = "🟢 即時連線",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            ConnectionState.SYNCING -> {
                                Text(
                                    text = "🔄 同步中...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            ConnectionState.DISCONNECTED -> {
                                Text(
                                    text = "🔴 離線 (保留本機快照)",
                                    fontSize = 13.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (lastSyncTimestamp > 0L) {
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTimestamp))
                            Text(
                                text = "最後同步 $timeStr",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("返回", color = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.fetchHistoryFromCloud() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("重新整理", color = Color.White)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Date Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                OrderDateFilter.values().forEach { filter ->
                    val isSelected = selectedDateFilter == filter
                    Button(
                        onClick = {
                            if (filter == OrderDateFilter.CUSTOM) {
                                showCustomDateDialog = true
                            } else {
                                viewModel.setDateFilter(filter)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedDateFilter == OrderDateFilter.CUSTOM) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val rangeText = "${sdf.format(Date(customStartDate))} - ${sdf.format(Date(customEndDate))}"
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE3F2FD))
                            .clickable { showCustomDateDialog = true }
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 $rangeText",
                            color = Color(0xFF1976D2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider()

            val windowSize = com.example.supos.ui.adaptive.rememberWindowSizeClass()

            @Composable
            fun RenderOrderList() {
                when (ordersState) {
                    is OrderHistoryUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1976D2))
                        }
                    }
                    is OrderHistoryUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("載入訂單失敗: ${(ordersState as OrderHistoryUiState.Error).throwable.message}", color = Color.Red)
                        }
                    }
                    is OrderHistoryUiState.Success -> {
                        val orders = (ordersState as OrderHistoryUiState.Success).orders
                        if (orders.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${selectedDateFilter.label}尚無任何紀錄", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF5F5F5))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(orders.reversed()) { order ->
                                    OrderHistoryRow(
                                        order = order,
                                        isSelected = selectedOrder?.orderId == order.orderId,
                                        onTap = { viewModel.selectOrder(order) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (windowSize.isCompact) {
                // Compact (分割畫面 / 手機直向): Master-Detail 單欄流
                if (selectedOrder == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        RenderOrderList()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE3F2FD))
                                .clickable { viewModel.clearSelectedOrder() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("← 返回訂單列表", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        OrderDetailsColumn(
                            order = selectedOrder,
                            items = selectedOrderItems,
                            mutationState = selectedOrder?.let { orderMutationStates[it.orderId] } ?: OrderMutationState.Idle,
                            onUpdateState = { orderId, state -> viewModel.updateWebOrderState(orderId, state) },
                            onRetryMutation = { orderId -> viewModel.retryFailedMutation(orderId) },
                            onRequestCancelOrder = { order -> orderToCancel = order },
                            onCheckout = { order ->
                                viewModel.prepareWebOrderForCheckout(order) {
                                    Toast.makeText(context, "已帶入結帳", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Medium & Expanded: 雙欄並排
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                        RenderOrderList()
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OrderDetailsColumn(
                        order = selectedOrder,
                        items = selectedOrderItems,
                        mutationState = selectedOrder?.let { orderMutationStates[it.orderId] } ?: OrderMutationState.Idle,
                        onUpdateState = { orderId, state -> viewModel.updateWebOrderState(orderId, state) },
                        onRetryMutation = { orderId -> viewModel.retryFailedMutation(orderId) },
                        onRequestCancelOrder = { order -> orderToCancel = order },
                        onCheckout = { order ->
                            viewModel.prepareWebOrderForCheckout(order) {
                                Toast.makeText(context, "已帶入結帳", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }

    // Cancel Order Confirm Dialog
    orderToCancel?.let { targetOrder ->
        CancelConfirmDialog(
            order = targetOrder,
            onDismiss = { orderToCancel = null },
            onConfirm = { reason ->
                viewModel.updateWebOrderState(targetOrder.orderId, "CANCELED", reason)
                orderToCancel = null
            },
            isSubmitting = orderMutationStates[targetOrder.orderId] is OrderMutationState.Submitting
        )
    }

    // Custom Date Range Selector Dialog
    if (showCustomDateDialog) {
        var tempStart by remember { mutableStateOf(customStartDate) }
        var tempEnd by remember { mutableStateOf(customEndDate) }
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("選擇自訂日期範圍", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("開始日期:", fontWeight = FontWeight.Medium)
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = tempStart
                                DatePickerDialog(context, { _, year, month, day ->
                                    val selected = Calendar.getInstance()
                                    selected.set(year, month, day)
                                    tempStart = selected.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                        ) {
                            Text(sdf.format(Date(tempStart)), color = Color.Black)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("結束日期:", fontWeight = FontWeight.Medium)
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = tempEnd
                                DatePickerDialog(context, { _, year, month, day ->
                                    val selected = Calendar.getInstance()
                                    selected.set(year, month, day)
                                    tempEnd = selected.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                        ) {
                            Text(sdf.format(Date(tempEnd)), color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomDateRange(tempStart, tempEnd)
                        showCustomDateDialog = false
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCustomDateDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun OrderHistoryRow(
    order: HistoryOrderPayload,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val cleanTime = order.timestamp.take(24)
    val stateColor = when (order.state) {
        "PENDING" -> Color(0xFFFF9800)
        "READY" -> Color(0xFF2196F3)
        "PRINTED" -> Color(0xFF4CAF50)
        "CANCELED" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    val stateText = when (order.state) {
        "PENDING" -> "等待入機"
        "READY" -> "已入機"
        "PRINTED" -> "已列印"
        "CANCELED" -> "已取消"
        else -> order.state
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0x1A1976D2) else Color.White)
            .clickable(onClick = onTap)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "單號：${order.orderId}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cleanTime,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = stateText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(stateColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("▶", color = Color.LightGray)
        }
    }
}

@Composable
fun OrderDetailsColumn(
    order: HistoryOrderPayload?,
    items: List<ParsedOrderItem>,
    mutationState: OrderMutationState,
    onUpdateState: (String, String) -> Unit,
    onRetryMutation: (String) -> Unit = {},
    onRequestCancelOrder: (HistoryOrderPayload) -> Unit,
    onCheckout: (HistoryOrderPayload) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 16.dp)
    ) {
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("選擇歷史訂單以查看明細", color = Color.Gray)
            }
        } else {
            // Header Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "單號：${order.orderId}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = "時間：${order.timestamp}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                val stateColor = when (order.state) {
                    "PENDING" -> Color(0xFFFFA000)
                    "READY" -> Color(0xFF4CAF50)
                    "CANCELED" -> Color(0xFFD32F2F)
                    else -> Color.Gray
                }
                Text(
                    text = if (order.state == "PENDING") "等待入機" else if (order.state == "READY") "已入機" else if (order.state == "CANCELED") "已取消" else order.state,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stateColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Items List Grouped by Category
            val grouped = items.groupBy { it.category ?: "其他" }
            LazyColumn(modifier = Modifier.weight(1f)) {
                grouped.keys.sorted().forEach { category ->
                    item {
                        Text(
                            text = "【$category】",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(grouped[category] ?: emptyList()) { item ->
                        val effectivePrice = if (item.isComplimentary) {
                            0
                        } else if (item.customPrice != null) {
                            item.customPrice
                        } else if (item.unitPrice > 0) {
                            item.unitPrice
                        } else {
                            0
                        }

                        val priceDisplayText = if (item.isComplimentary) {
                            "招待 ($0)"
                        } else if (item.customPrice != null && item.unitPrice > 0) {
                            "改價 $${effectivePrice} (原價 $${item.unitPrice})"
                        } else {
                            "$${effectivePrice}"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(text = priceDisplayText, fontSize = 13.sp, color = if (item.isComplimentary) Color(0xFF4CAF50) else Color.Gray)
                                }
                                Text(
                                    text = "x ${item.qty}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            item.addons?.forEach { addon ->
                                val addonPrice = addon.price
                                val qtyText = if ((addon.qty ?: 1) > 1) " x${addon.qty}" else ""
                                val priceText = if (addonPrice > 0) " (+${addonPrice}元)" else ""

                                Text(
                                    text = "  - ${addon.name}$qtyText$priceText",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Failure Banner with Retry
            if (mutationState is OrderMutationState.Failed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "❌ 狀態更新失敗: ${mutationState.message}",
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (mutationState.attempt != null) {
                            Button(
                                onClick = { onRetryMutation(order.orderId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp).padding(start = 8.dp)
                            ) {
                                Text("重試", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Buttons
            val isTerminal = com.example.supos.data.model.OrderStateMachine.isTerminal(order.state)
            val canMarkReady = com.example.supos.data.model.OrderStateMachine.canTransition(
                order.state,
                com.example.supos.data.model.OrderStateMachine.STATE_READY
            )
            val canCancel = com.example.supos.data.model.OrderStateMachine.canTransition(
                order.state,
                com.example.supos.data.model.OrderStateMachine.STATE_CANCELED
            )

            if (isTerminal) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (order.state == "CANCELED") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (order.state == "CANCELED") {
                            "🚫 此訂單已取消" + (if (!order.cancelReason.isNullOrBlank()) " (${order.cancelReason})" else "")
                        } else {
                            "✅ 此訂單已完成出餐"
                        },
                        color = if (order.state == "CANCELED") Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (mutationState is OrderMutationState.Submitting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1976D2))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (order.paymentStatus != "PAID") {
                        Button(
                            onClick = { onCheckout(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text("帶入結帳", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (canMarkReady) {
                        Button(
                            onClick = { onUpdateState(order.orderId, com.example.supos.data.model.OrderStateMachine.STATE_READY) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text("確認入機", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (canCancel) {
                        Button(
                            onClick = { onRequestCancelOrder(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text("取消訂單", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CancelConfirmDialog(
    order: HistoryOrderPayload,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
    isSubmitting: Boolean
) {
    val presets = listOf("顧客主動取消", "餐點已售罄", "現場重複點單", "其他原因")
    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var customText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("確認取消訂單 #${order.orderId}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (order.paymentStatus == "PAID") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ 警示：此訂單已完成付款，取消將紀錄退款審計日誌！",
                            color = Color(0xFFE65100),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text("請選擇取消原因：", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = presets.chunked(2)
                    chunked.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = selectedPreset == preset
                                Button(
                                    onClick = { selectedPreset = preset },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFFEBEE) else Color(0xFFF5F5F5)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = preset,
                                        color = if (isSelected) Color(0xFFD32F2F) else Color.DarkGray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                androidx.compose.material3.OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("備註說明（可選）") },
                    placeholder = { Text("補充詳細取消細節...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSubmitting
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (customText.isNotBlank()) {
                        "$selectedPreset ($customText)"
                    } else {
                        selectedPreset
                    }
                    onConfirm(finalReason)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("確認取消", color = Color.White)
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("放棄", color = Color.White)
            }
        }
    )
}
