package com.example.supos.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.supos.SuPOSApplication
import com.example.supos.data.health.SystemHealthAggregator
import com.example.supos.data.health.NetworkHealth
import com.example.supos.data.health.ApiHealth
import com.example.supos.data.health.MenuHealth
import com.example.supos.data.health.OutboxHealth
import com.example.supos.data.health.LiveOrderHealth
import com.example.supos.data.health.DatabaseHealth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private data class WorkspaceCardStyle(
    val eyebrow: String,
    val title: String,
    val description: String,
    val accent: Color
)

@Composable
fun DashboardScreen(
    onOpenOnlineOrders: () -> Unit,
    onOpenManualOrder: () -> Unit,
    onOpenPerformance: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? SuPOSApplication
    val vm: DashboardViewModel? = viewModel ?: app?.let {
        viewModel(factory = DashboardViewModel.provideFactory(it.systemHealthRepository))
    }

    val defaultReport = remember {
        SystemHealthAggregator.aggregate(
            network = NetworkHealth(),
            api = ApiHealth(),
            menu = MenuHealth(),
            outbox = OutboxHealth(),
            liveOrder = LiveOrderHealth(),
            database = DatabaseHealth()
        )
    }

    val healthReport = vm?.healthReport?.collectAsStateWithLifecycle()?.value ?: defaultReport
    val isDiagnosticsOpen = vm?.isDiagnosticsDialogOpen?.collectAsStateWithLifecycle()?.value ?: false

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    val windowSize = com.example.supos.ui.adaptive.rememberWindowSizeClass()
    val hPadding = if (windowSize.isCompact) 20.dp else 40.dp
    val vPadding = if (windowSize.isCompact) 16.dp else 30.dp

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF12202D), Color(0xFF071018))
                )
            )
            .padding(horizontal = hPadding, vertical = vPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SuPOS 中控臺",
                    color = Color.White,
                    fontSize = if (windowSize.isCompact) 26.sp else 34.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "選擇工作區開始營運",
                    color = Color(0xFFA9BBC9),
                    fontSize = if (windowSize.isCompact) 14.sp else 16.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    color = Color.White,
                    fontSize = if (windowSize.isCompact) 24.sp else 34.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault()).format(now),
                    color = Color(0xFFA9BBC9),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(if (windowSize.isCompact) 18.dp else 34.dp))

        if (windowSize.isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "LIVE ORDERS",
                        title = "線上訂單",
                        description = "接收外送與線上訂單、確認狀態，並帶入櫃臺結帳。",
                        accent = Color(0xFFFF9F43)
                    ),
                    onClick = onOpenOnlineOrders,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "NEW ORDER",
                        title = "現場開單",
                        description = "快速選品、加入客製選項、暫存訂單並完成現場結帳。",
                        accent = Color(0xFF26C6A2)
                    ),
                    onClick = onOpenManualOrder,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "PERFORMANCE",
                        title = "業績戰報",
                        description = "檢視營收、已付款單數、平均客單與每日趨勢。",
                        accent = Color(0xFF5B8DEF)
                    ),
                    onClick = onOpenPerformance,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "LIVE ORDERS",
                        title = "線上訂單",
                        description = "接收外送與線上訂單、確認狀態，並帶入櫃臺結帳。",
                        accent = Color(0xFFFF9F43)
                    ),
                    onClick = onOpenOnlineOrders,
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "NEW ORDER",
                        title = "現場開單",
                        description = "快速選品、加入客製選項、暫存訂單並完成現場結帳。",
                        accent = Color(0xFF26C6A2)
                    ),
                    onClick = onOpenManualOrder,
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
                WorkspaceCard(
                    style = WorkspaceCardStyle(
                        eyebrow = "PERFORMANCE",
                        title = "業績戰報",
                        description = "檢視營收、已付款單數、平均客單與每日趨勢。",
                        accent = Color(0xFF5B8DEF)
                    ),
                    onClick = onOpenPerformance,
                    modifier = Modifier.weight(1f).fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("門市營運工作區", color = Color(0xFF7F94A5), fontSize = 13.sp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(healthReport.badgeColorHex).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(healthReport.badgeColorHex).copy(alpha = 0.5f)),
                modifier = Modifier.clickable { vm?.openDiagnosticsDialog() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = healthReport.badgeText,
                        color = Color(healthReport.badgeColorHex),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "診斷",
                        color = Color(healthReport.badgeColorHex).copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (isDiagnosticsOpen) {
        HealthDiagnosticsDialog(
            report = healthReport,
            onDismiss = { vm?.closeDiagnosticsDialog() }
        )
    }
}

@Composable
private fun WorkspaceCard(
    style: WorkspaceCardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182A38)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = style.eyebrow,
                    color = style.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = style.title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = style.description,
                    color = Color(0xFFB8C7D2),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 25.sp
                )
            }
            Text(
                text = "進入工作區  →",
                color = style.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
