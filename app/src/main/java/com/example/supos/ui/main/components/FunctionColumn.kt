package com.example.supos.ui.main.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supos.ui.main.PosDesignTokens

@Composable
fun FunctionColumn(
    transactionType: String,
    tableNumber: String,
    selectedCartItemId: String?,
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
    onSaveOrderToTemp: () -> Unit,
    onShowTempOrders: () -> Unit,
    onShowDailyTurnover: () -> Unit,
    onShowManualOrdering: () -> Unit,
    onBackToDashboard: () -> Unit,
    isSubmitting: Boolean = false,
    unsyncedOrdersCount: Int = 0,
    onRetrySyncOutbox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPriceDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var customPriceInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosDesignTokens.PanelBackground)
            .padding(PosDesignTokens.ControlGap),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(PosDesignTokens.ControlGap),
            verticalArrangement = Arrangement.spacedBy(PosDesignTokens.ControlGap),
            modifier = Modifier.weight(1f)
        ) {
            item {
                FunctionSquareButton(
                    label = "+1",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = { onAdjustQuantity(1) }
                )
            }
            item {
                FunctionSquareButton(
                    label = "-1",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = { onAdjustQuantity(-1) }
                )
            }
            item {
                FunctionSquareButton(
                    label = "交易取消",
                    subLabel = "",
                    backgroundColor = PosDesignTokens.Destructive,
                    labelColor = Color.White,
                    onClick = onClearCart
                )
            }
            item {
                FunctionSquareButton(
                    label = if (tableNumber.isEmpty()) "桌號輸入" else "桌號 $tableNumber",
                    subLabel = "",
                    onClick = { showTableDialog = true }
                )
            }
            item {
                FunctionSquareButton(
                    label = "刪除商品",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onDeleteItem
                )
            }
            item {
                FunctionSquareButton(
                    label = "內用/\n外帶",
                    subLabel = transactionType,
                    backgroundColor = PosDesignTokens.Selection,
                    labelColor = Color.White,
                    onClick = onToggleTransactionType
                )
            }
            item {
                FunctionSquareButton(
                    label = "交易暫存",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onSaveOrderToTemp
                )
            }
            item {
                FunctionSquareButton(
                    label = "暫存調閱",
                    subLabel = "",
                    onClick = onShowTempOrders
                )
            }
            item {
                FunctionSquareButton(
                    label = "交易紀錄\n查詢",
                    subLabel = "",
                    onClick = onNavigateToHistory
                )
            }
            item {
                FunctionSquareButton(
                    label = "前筆交易\n查詢",
                    subLabel = "",
                    onClick = onNavigateToHistory
                )
            }
            item {
                FunctionSquareButton(
                    label = "線上點餐\n查詢",
                    subLabel = "",
                    backgroundColor = PosDesignTokens.Category,
                    labelColor = Color.White,
                    onClick = onNavigateToOnlineOrder
                )
            }
            item {
                FunctionSquareButton(
                    label = "日營業額",
                    subLabel = "",
                    onClick = onShowDailyTurnover
                )
            }
            item {
                FunctionSquareButton(
                    label = "招待",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onApplyComplimentary
                )
            }
            item {
                FunctionSquareButton(
                    label = "變價",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = {
                        customPriceInput = ""
                        showPriceDialog = true
                    }
                )
            }
            item {
                FunctionSquareButton(
                    label = "清除加料",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onClearOptions
                )
            }
            item {
                FunctionSquareButton(
                    label = "返回點餐",
                    subLabel = "",
                    onClick = onShowManualOrdering
                )
            }
            item {
                FunctionSquareButton(
                    label = if (unsyncedOrdersCount > 0) "待同步 $unsyncedOrdersCount" else "同步狀態",
                    subLabel = if (unsyncedOrdersCount > 0) "點擊重試" else "全部完成",
                    backgroundColor = if (unsyncedOrdersCount > 0) PosDesignTokens.Warning else PosDesignTokens.PanelBackgroundRaised,
                    labelColor = if (unsyncedOrdersCount > 0) Color.White else PosDesignTokens.TextSecondary,
                    onClick = onRetrySyncOutbox
                )
            }
            item {
                FunctionSquareButton(
                    label = "返回",
                    subLabel = "中控首頁",
                    backgroundColor = PosDesignTokens.PanelBackgroundRaised,
                    labelColor = Color.White,
                    onClick = onBackToDashboard
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom part: Checkout (結帳)
        Button(
            onClick = onCheckout,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(PosDesignTokens.ControlCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = PosDesignTokens.Confirm,
                disabledContainerColor = PosDesignTokens.PanelBackgroundRaised
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("結帳處理中...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text("結帳", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // Change Price Dialog
    if (showPriceDialog) {
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("變更商品售價") },
            text = {
                Column {
                    Text("請輸入新的售價 (輸入空白或 0 回復原價):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPriceInput,
                        onValueChange = { customPriceInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("例如: 50") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = customPriceInput.toIntOrNull()
                        onChangePrice(if (newPrice != null && newPrice > 0) newPrice else null)
                        showPriceDialog = false
                    }
                ) {
                    Text("確認")
                }
            },
            dismissButton = {
                Button(onClick = { showPriceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Table Number Dialog
    if (showTableDialog) {
        TableSelectionDialog(
            currentTableNumber = tableNumber,
            onSelectTable = { selectedTable ->
                onSetTableNumber(selectedTable)
            },
            onDismiss = {
                showTableDialog = false
            }
        )
    }
}

@Composable
private fun FunctionSquareButton(
    label: String,
    subLabel: String,
    backgroundColor: Color = PosDesignTokens.Function,
    labelColor: Color = PosDesignTokens.TextPrimary,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PosDesignTokens.MinimumTouchHeight)
            .clip(RoundedCornerShape(PosDesignTokens.ControlCorner))
            .clickable(enabled = isEnabled, onClick = onClick),
        color = if (isEnabled) backgroundColor else PosDesignTokens.PanelBackgroundRaised,
        shape = RoundedCornerShape(PosDesignTokens.ControlCorner)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) labelColor else PosDesignTokens.TextDisabled,
                textAlign = TextAlign.Center
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = if (isEnabled) PosDesignTokens.TextSecondary else PosDesignTokens.TextDisabled,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
