package com.example.supos.ui.main.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.supos.ui.main.MainScreenViewModel.TurnoverUiState

@Composable
fun DailyTurnoverColumn(
    todayTurnover: Double,
    isFetching: Boolean,
    turnoverUiState: TurnoverUiState = TurnoverUiState.Idle,
    onRetryTurnover: () -> Unit = {},
    onBackToOrdering: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(30.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日營業額統計",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                if (turnoverUiState is TurnoverUiState.Success) {
                    Text(
                        text = "更新於 ${turnoverUiState.fetchedAt}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(25.dp))

            when (turnoverUiState) {
                is TurnoverUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1976D2))
                            Text(
                                text = "正在計算今日所有門市與外送營收...",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                is TurnoverUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(25.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ 營業額讀取失敗",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = turnoverUiState.message,
                            fontSize = 15.sp,
                            color = Color(0xFFD32F2F)
                        )
                        if (turnoverUiState.lastKnownAmount != null) {
                            Text(
                                text = "上次有效紀錄：$${turnoverUiState.lastKnownAmount.toInt()} (可能非即時)",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                        Button(
                            onClick = onRetryTurnover,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("重新整理", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    val displayAmount = if (turnoverUiState is TurnoverUiState.Success) {
                        turnoverUiState.amount
                    } else {
                        todayTurnover
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(25.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "今日營業額：", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$${displayAmount.toInt()}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1976D2)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "現金：", fontSize = 20.sp, color = Color.Gray)
                            Text(text = "-", fontSize = 20.sp, color = Color.LightGray)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Uber Eats：", fontSize = 20.sp, color = Color.Gray)
                            Text(text = "-", fontSize = 20.sp, color = Color.LightGray)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "FoodPanda：", fontSize = 20.sp, color = Color.Gray)
                            Text(text = "-", fontSize = 20.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Footer Back button
        Button(
            onClick = onBackToOrdering,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(1.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray
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
