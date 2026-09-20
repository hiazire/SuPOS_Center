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

@Composable
fun FunctionColumn(
    transactionType: String,
    tableNumber: String,
    selectedCartItemId: String?,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToHistory: () -> Unit,
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
    var tableNumberInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper part: Function grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 1. + Quantity
            item {
                FunctionSquareButton(
                    label = "＋",
                    subLabel = "數量加一",
                    isEnabled = selectedCartItemId != null,
                    onClick = { onAdjustQuantity(1) }
                )
            }

            // 2. - Quantity
            item {
                FunctionSquareButton(
                    label = "－",
                    subLabel = "數量減一",
                    isEnabled = selectedCartItemId != null,
                    onClick = { onAdjustQuantity(-1) }
                )
            }

            // 3. Delete Selected
            item {
                FunctionSquareButton(
                    label = "刪除商品",
                    subLabel = "",
                    backgroundColor = Color(0xFFE57373),
                    labelColor = Color.White,
                    isEnabled = selectedCartItemId != null,
                    onClick = onDeleteItem
                )
            }

            // 4. Clear options
            item {
                FunctionSquareButton(
                    label = "清除加料",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onClearOptions
                )
            }

            // 5. Complimentary (招待)
            item {
                FunctionSquareButton(
                    label = "招待",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onApplyComplimentary
                )
            }

            // 6. Change price (變價)
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

            // 7. Transaction type (型態)
            item {
                FunctionSquareButton(
                    label = transactionType,
                    subLabel = "切換型態",
                    backgroundColor = Color(0xFF64B5F6),
                    labelColor = Color.White,
                    onClick = onToggleTransactionType
                )
            }

            // 8. Table number (桌號)
            item {
                FunctionSquareButton(
                    label = if (tableNumber.isEmpty()) "桌號" else "桌號: $tableNumber",
                    subLabel = "點擊設定",
                    onClick = {
                        tableNumberInput = tableNumber
                        showTableDialog = true
                    }
                )
            }

            // 9. Cancel entire transaction (取消交易)
            item {
                FunctionSquareButton(
                    label = "取消交易",
                    subLabel = "",
                    backgroundColor = Color(0xFFEF5350),
                    labelColor = Color.White,
                    onClick = onClearCart
                )
            }

            // 10. Order History (歷史訂單)
            item {
                FunctionSquareButton(
                    label = "歷史訂單",
                    subLabel = "",
                    onClick = onNavigateToHistory
                )
            }

            // --- Temporary Orders & Daily Turnover Functions ---
            item {
                FunctionSquareButton(
                    label = "訂單暫存",
                    subLabel = "",
                    isEnabled = selectedCartItemId != null,
                    onClick = onSaveOrderToTemp
                )
            }
            item {
                FunctionSquareButton(
                    label = "暫存區",
                    subLabel = "",
                    onClick = onShowTempOrders
                )
            }
            item {
                FunctionSquareButton(
                    label = "手動點餐",
                    subLabel = "",
                    onClick = onShowManualOrdering
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
                    label = "交易紀錄",
                    subLabel = "",
                    onClick = onNavigateToHistory
                )
            }
            item {
                FunctionSquareButton(
                    label = if (unsyncedOrdersCount > 0) "⚠️ 待同步 ($unsyncedOrdersCount)" else "離線同步",
                    subLabel = if (unsyncedOrdersCount > 0) "點擊重試" else "全部已同步",
                    backgroundColor = if (unsyncedOrdersCount > 0) Color(0xFFFFA000) else Color(0xFFCFD8DC),
                    labelColor = if (unsyncedOrdersCount > 0) Color.White else Color(0xFF37474F),
                    onClick = onRetrySyncOutbox
                )
            }
            item {
                FunctionSquareButton(
                    label = "中控首頁",
                    subLabel = "切換工作區",
                    backgroundColor = Color(0xFF455A64),
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
                .height(64.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                disabledContainerColor = Color.Gray
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
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("設定桌號") },
            text = {
                Column {
                    Text("請輸入內用桌號 (留空則清除):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tableNumberInput,
                        onValueChange = { tableNumberInput = it },
                        singleLine = true,
                        placeholder = { Text("例如: A1") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetTableNumber(tableNumberInput.trim())
                        showTableDialog = false
                    }
                ) {
                    Text("確認")
                }
            },
            dismissButton = {
                Button(onClick = { showTableDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FunctionSquareButton(
    label: String,
    subLabel: String,
    backgroundColor: Color = Color.White,
    labelColor: Color = Color(0xFF333333),
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val opacity = if (isEnabled) 1.0f else 0.4f
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(if (isEnabled) 1.dp else 0.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled, onClick = onClick),
        color = backgroundColor.copy(alpha = opacity),
        shape = RoundedCornerShape(8.dp)
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
                color = labelColor,
                textAlign = TextAlign.Center
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = if (labelColor == Color.White) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
