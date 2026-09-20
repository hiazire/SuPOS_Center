package com.example.supos.ui.main.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supos.data.model.CartItem

@Composable
fun CartColumn(
    cartItems: List<CartItem>,
    selectedCartItemId: String?,
    selectedOptionName: String?,
    createdAt: String,
    transactionType: String,
    tableNumber: String,
    invoiceNumber: String,
    ubn: String,
    carrier: String,
    onItemSelect: (String) -> Unit,
    onOptionSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = cartItems.sumOf { it.subtotal }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Metadata Info Box (mirrors iPad OrderDetailsColumn top header)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.20f)
                .background(Color(0xFFEEEEEE))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "訂單總覽",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LabeledInfoRow(title = "建立時間", value = createdAt)
                LabeledInfoRow(title = "交易型態", value = transactionType)
                LabeledInfoRow(title = "桌號", value = if (tableNumber.isEmpty()) "-" else tableNumber)
                LabeledInfoRow(title = "發票號碼", value = if (invoiceNumber.isEmpty()) "-" else invoiceNumber)
                LabeledInfoRow(title = "統一編號", value = if (ubn.isEmpty()) "-" else ubn)
                LabeledInfoRow(title = "發票載具", value = if (carrier.isEmpty()) "-" else carrier)
            }
            Text(
                text = "Ver: SuPOS_Android_Antigravity",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
        
        HorizontalDivider()

        // Cart Items
        LazyColumn(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxWidth()
        ) {
            items(cartItems) { item ->
                val isSelected = item.id == selectedCartItemId
                CartItemRow(
                    item = item,
                    isSelected = isSelected,
                    selectedOptionName = if (isSelected) selectedOptionName else null,
                    onClick = {
                        onItemSelect(item.id)
                    },
                    onOptionClick = { optName ->
                        onItemSelect(item.id)
                        onOptionSelect(optName)
                    }
                )
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }

        HorizontalDivider()

        // Total
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.05f)
                .background(Color(0xFFEEEEEE))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "總計金額", 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$$totalAmount", 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    selectedOptionName: String?,
    onClick: () -> Unit,
    onOptionClick: (String?) -> Unit
) {
    val backgroundColor = if (isSelected && selectedOptionName == null) Color(0x1A1976D2) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.menuItem.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1976D2) else Color.Black
                    )
                    if (item.isComplimentary) {
                        Text(
                            text = "(招待)",
                            fontSize = 14.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Quantity shown next to price if > 1
            if (item.quantity > 1) {
                Text(
                    text = "x${item.quantity}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Total price
            val priceColor = when {
                item.isComplimentary -> Color.Red
                item.customPrice != null -> Color(0xFF1976D2)
                else -> Color.Black
            }
            Text(
                text = if (item.isComplimentary) "$0" else "$${item.subtotal}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = priceColor
            )
        }

        // Render selected options grouped
        if (item.options.isNotEmpty()) {
            val groupedOptions = item.options.groupBy { it.name }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedOptions.keys.sorted().forEach { optName ->
                    val opts = groupedOptions[optName] ?: emptyList()
                    val optCount = opts.size
                    val isSingle = opts.firstOrNull()?.optionType?.lowercase() == "single"

                    if (isSingle) {
                        // Single option: Orange background, clicking clears option selection (selects main item)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFF9800)) // Light Orange
                                .clickable {
                                    onOptionClick(null)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "・$optName",
                                fontSize = 14.sp,
                                color = Color(0xFFFF9800), // Orange
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Multi option: Blue background, clickable to adjust/select
                        val isSelectedOption = selectedOptionName == optName
                        val optBg = if (isSelectedOption) Color(0xFF1976D2) else Color(0x1A1976D2)
                        val optColor = if (isSelectedOption) Color.White else Color(0xFF1976D2)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(optBg)
                                .clickable {
                                    onOptionClick(optName)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "・$optName" + if (optCount > 1) " x$optCount" else "",
                                fontSize = 14.sp,
                                color = optColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}
