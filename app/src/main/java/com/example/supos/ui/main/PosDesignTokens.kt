package com.example.supos.ui.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Windows POS 的空間與操作語意，搭配 Android 平板觸控可讀性調整。
 * 顏色不逐像素複製 Windows，避免用單一顏色承擔狀態判斷。
 */
object PosDesignTokens {
    val AppBackground = Color(0xFF171B1F)
    val PanelBackground = Color(0xFF22272D)
    val PanelBackgroundRaised = Color(0xFF2B3138)
    val Divider = Color(0xFF49515A)

    val TextPrimary = Color(0xFFF4F6F8)
    val TextSecondary = Color(0xFFB7C0C9)
    val TextDisabled = Color(0xFF707A84)

    val Function = Color(0xFF2468A9)
    val FunctionPressed = Color(0xFF174A78)
    val Category = Color(0xFF187A3C)
    val CategorySelected = Color(0xFF22A653)
    val Product = Color(0xFF9E3037)
    val ProductPressed = Color(0xFF742329)
    val Confirm = Color(0xFF1F9D55)
    val Warning = Color(0xFFE69A28)
    val Destructive = Color(0xFFC83E46)
    val Selection = Color(0xFF2E86D1)

    val ControlCorner = 3.dp
    val PanelGap = 1.dp
    val ControlGap = 4.dp
    val MinimumTouchHeight = 56.dp
}
