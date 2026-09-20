package com.example.supos.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.supos.data.health.SubsystemStatus
import com.example.supos.data.health.SystemHealthReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthDiagnosticsDialog(
    report: SystemHealthReport,
    onDismiss: () -> Unit
) {
    val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131E29),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "系統健康診斷",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = report.summary,
                            color = Color(0xFFA9BBC9),
                            fontSize = 13.sp
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(report.badgeColorHex).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(report.badgeColorHex).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = report.badgeText,
                            color = Color(report.badgeColorHex),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF223545), thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                // Subsystems Section
                Text(
                    text = "子系統狀態",
                    color = Color(0xFF7F94A5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    report.diagnostics.forEach { diag ->
                        SubsystemDiagnosticRow(diag, timeFormat)
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF223545), thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                // Recommendations Section
                Text(
                    text = "營運與排障處置建議",
                    color = Color(0xFF7F94A5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.recommendations.forEach { rec ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                color = Color(0xFF26C6A2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = rec,
                                color = Color(0xFFD3E0EA),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF26C6A2))
                    ) {
                        Text("關閉診斷", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsystemDiagnosticRow(
    diag: com.example.supos.data.health.SubsystemDiagnostic,
    timeFormat: SimpleDateFormat
) {
    val statusColor = when (diag.status) {
        SubsystemStatus.OK -> Color(0xFF65D6AD)
        SubsystemStatus.WARNING -> Color(0xFFFFB74D)
        SubsystemStatus.ERROR -> Color(0xFFE57373)
        SubsystemStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B38)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = diag.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = diag.detail,
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp
                    )
                }
            }

            if (diag.timestamp != null && diag.timestamp > 0) {
                Text(
                    text = timeFormat.format(Date(diag.timestamp)),
                    color = Color(0xFF627888),
                    fontSize = 11.sp
                )
            }
        }
    }
}
