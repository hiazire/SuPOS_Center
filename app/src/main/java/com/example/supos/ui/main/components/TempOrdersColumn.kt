package com.example.supos.ui.main.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supos.data.model.TempOrder
import com.example.supos.ui.main.PosDesignTokens

@Composable
fun TempOrdersColumn(
    tempSavedOrders: List<TempOrder>,
    onRestoreOrder: (TempOrder) -> Unit,
    onBackToOrdering: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosDesignTokens.PanelBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "暫存調閱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PosDesignTokens.TextPrimary
                )
                Text(
                    text = "目前：${tempSavedOrders.size} 筆",
                    color = PosDesignTokens.TextSecondary,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = PosDesignTokens.Divider)
        }

        // List of temp orders
        if (tempSavedOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "無暫存的交易",
                    color = PosDesignTokens.TextSecondary,
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tempSavedOrders) { order ->
                    TempOrderRow(
                        order = order,
                        onClick = { onRestoreOrder(order) }
                    )
                }
            }
        }

        // Footer Back button
        Button(
            onClick = onBackToOrdering,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(PosDesignTokens.ControlCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = PosDesignTokens.Function
            )
        ) {
            Text(
                text = "返回點餐",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TempOrderRow(
    order: TempOrder,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PosDesignTokens.ControlCorner))
            .background(PosDesignTokens.PanelBackgroundRaised)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.timeString,
                    color = PosDesignTokens.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val description = buildString {
                    append(order.transactionType)
                    if (order.tableNumber.isNotEmpty()) {
                        append(" (桌號: ${order.tableNumber})")
                    }
                    append(" - ${order.items.size} 品項")
                }
                Text(
                    text = description,
                    color = PosDesignTokens.TextSecondary,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "▶",
                color = PosDesignTokens.TextSecondary,
                fontSize = 18.sp
            )
        }
    }
}
