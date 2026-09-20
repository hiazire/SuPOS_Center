package com.example.supos.ui.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PerformanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as SuPOSApplication
    val viewModel: PerformanceViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PerformanceViewModel(application.orderRepository) as T
        }
    )
    val range by viewModel.selectedRange.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PerformanceContent(
        selectedRange = range,
        state = state,
        onSelectRange = viewModel::selectRange,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PerformanceContent(
    selectedRange: ReportRange,
    state: PerformanceUiState,
    onSelectRange: (ReportRange) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(Color(0xFFF3F6F8)).padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("業績戰報", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF17232D))
                    Text("付款完成訂單的營運摘要", color = Color(0xFF6D7E8C))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.entries.forEach { item ->
                    Button(
                        onClick = { onSelectRange(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRange == item) Color(0xFF2457C5) else Color.White,
                            contentColor = if (selectedRange == item) Color.White else Color(0xFF33434F)
                        )
                    ) { Text(item.label) }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        when (state) {
            PerformanceUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is PerformanceUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("戰報載入失敗", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB42318))
                    Text(state.message, color = Color(0xFF6D7E8C), modifier = Modifier.padding(12.dp))
                    Button(onClick = onRefresh) { Text("重新載入") }
                }
            }
            is PerformanceUiState.Success -> ReportBody(state.report)
        }
    }
}

@Composable
private fun ReportBody(report: SalesReport) {
    val windowSize = com.example.supos.ui.adaptive.rememberWindowSizeClass()

    Column(Modifier.fillMaxSize()) {
        if (windowSize.isExpanded && !windowSize.isLargeFont) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MetricCard("營業額", money(report.revenue), Color(0xFF2457C5), Modifier.weight(1f), isLargeFont = false)
                MetricCard("已付款單數", "${report.paidOrderCount} 單", Color(0xFF14866D), Modifier.weight(1f), isLargeFont = false)
                MetricCard("平均客單", money(report.averageTicket), Color(0xFF9A5A00), Modifier.weight(1f), isLargeFont = false)
                MetricCard("待處理", "${report.pendingOrderCount} 單", Color(0xFFB42318), Modifier.weight(1f), isLargeFont = false)
            }
        } else {
            // Compact, Medium 或大字體倍率: 2x2 矩陣排版避免文字擠壓溢出
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("營業額", money(report.revenue), Color(0xFF2457C5), Modifier.weight(1f), isLargeFont = windowSize.isLargeFont)
                    MetricCard("已付款單數", "${report.paidOrderCount} 單", Color(0xFF14866D), Modifier.weight(1f), isLargeFont = windowSize.isLargeFont)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("平均客單", money(report.averageTicket), Color(0xFF9A5A00), Modifier.weight(1f), isLargeFont = windowSize.isLargeFont)
                    MetricCard("待處理", "${report.pendingOrderCount} 單", Color(0xFFB42318), Modifier.weight(1f), isLargeFont = windowSize.isLargeFont)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Text("每日營收", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17232D))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                if (report.dailySales.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("此區間尚無已付款訂單", color = Color(0xFF788995))
                    }
                } else {
                    LazyColumn {
                        items(report.dailySales) { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(day.dateLabel, fontWeight = FontWeight.Bold, color = Color(0xFF33434F))
                                Text("${day.paidOrderCount} 單", color = Color(0xFF788995))
                                Text(money(day.revenue), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2457C5))
                            }
                            HorizontalDivider(color = Color(0xFFE8EDF0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier, isLargeFont: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(if (isLargeFont) 14.dp else 20.dp)) {
            Text(label, color = Color(0xFF6D7E8C), fontSize = if (isLargeFont) 12.sp else 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = accent, fontSize = if (isLargeFont) 21.sp else 27.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun money(amount: Double): String =
    "NT$ ${NumberFormat.getIntegerInstance(Locale.TAIWAN).format(amount)}"
