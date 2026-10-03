package com.example.supos.ui.main.components

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TablePreferencesHelper {
    private const val PREFS_NAME = "supos_table_settings"
    private const val KEY_TABLES = "table_list"
    val DEFAULT_TABLES = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09")

    fun loadTables(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TABLES, null) ?: return DEFAULT_TABLES
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(json, type) ?: DEFAULT_TABLES
        } catch (e: Exception) {
            DEFAULT_TABLES
        }
    }

    fun saveTables(context: Context, tables: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TABLES, Gson().toJson(tables)).apply()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableSelectionDialog(
    currentTableNumber: String,
    onSelectTable: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tables by remember { mutableStateOf(TablePreferencesHelper.loadTables(context)) }
    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTableInput by remember { mutableStateOf("") }

    // iOS style jiggle animation for edit mode
    val infiniteTransition = rememberInfiniteTransition(label = "ios_jiggle")
    val wobbleRotation by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 110, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    Dialog(
        onDismissRequest = {
            if (isEditMode) {
                isEditMode = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141F2B),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFF283A4D)),
            modifier = Modifier
                .widthIn(min = 440.dp, max = 560.dp)
                .heightIn(max = 700.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "設定內用桌號",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (currentTableNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF26C6A2).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF26C6A2))
                                ) {
                                    Text(
                                        text = "目前: $currentTableNumber",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF26C6A2),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEditMode) "長按模式中：點擊紅色 ✕ 移除桌號" else "點選快速帶入；長按任一鍵可移除桌號",
                            fontSize = 12.sp,
                            color = if (isEditMode) Color(0xFFFFB74D) else Color(0xFF8FA3B8),
                            fontWeight = if (isEditMode) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    if (isEditMode) {
                        Button(
                            onClick = { isEditMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26C6A2)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("完成", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    } else {
                        TextButton(
                            onClick = { isEditMode = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("編輯", fontSize = 13.sp, color = Color(0xFF8FA3B8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Table Grid (3 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(tables, key = { it }) { table ->
                        val isSelected = table == currentTableNumber

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationZ = if (isEditMode) wobbleRotation else 0f
                                }
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E3A34) else Color(0xFF1D2C3A)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF26C6A2) else Color(0xFF2D4255)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.25f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (!isEditMode) {
                                                onSelectTable(table)
                                                onDismiss()
                                            }
                                        },
                                        onLongClick = {
                                            isEditMode = true
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = table,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color(0xFF26C6A2) else Color.White
                                    )
                                    Text(
                                        text = "桌",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFF26C6A2).copy(alpha = 0.8f) else Color(0xFF8FA3B8)
                                    )
                                }
                            }

                            // Red Delete Badge (iOS Style)
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 5.dp, y = (-5).dp)
                                        .size(24.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(Color(0xFFE53935), CircleShape)
                                        .clickable {
                                            val updated = tables.filter { it != table }
                                            tables = updated
                                            TablePreferencesHelper.saveTables(context, updated)
                                            if (currentTableNumber == table) {
                                                onSelectTable("")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✕",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Add '+' Button Item
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162330)),
                            border = BorderStroke(1.5.dp, Color(0xFF32536B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.25f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    // Calculate auto suggestion: e.g. next sequential number
                                    val numbers = tables.mapNotNull { it.toIntOrNull() }
                                    val nextNum = if (numbers.isNotEmpty()) numbers.max() + 1 else tables.size + 1
                                    newTableInput = String.format("%02d", nextNum)
                                    showAddDialog = true
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "＋",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4DD0E1)
                                )
                                Text(
                                    text = "新增桌號",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8FA3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear Table Number Button
                    OutlinedButton(
                        onClick = {
                            onSelectTable("")
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("清除桌號", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Close / Cancel Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("關閉", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // Modal to Add New Table
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1A2634),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCFD8DC),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("新增桌號", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    Text("請輸入桌號名稱（如 10、11 或 A1）：", fontSize = 13.sp, color = Color(0xFFA9BBC9))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newTableInput,
                        onValueChange = { newTableInput = it },
                        singleLine = true,
                        placeholder = { Text("例如: 10", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26C6A2),
                            unfocusedBorderColor = Color(0xFF455A64)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTableInput.trim()
                        if (trimmed.isNotEmpty() && !tables.contains(trimmed)) {
                            val updated = tables + trimmed
                            tables = updated
                            TablePreferencesHelper.saveTables(context, updated)
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26C6A2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("新增", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消", color = Color.LightGray)
                }
            }
        )
    }
}
