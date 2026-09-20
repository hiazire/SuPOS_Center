package com.example.supos.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class WindowSizeClassInfo(
    val widthType: WindowWidthSize,
    val widthDp: Dp,
    val heightDp: Dp,
    val fontScale: Float
) {
    val isCompact: Boolean get() = widthType == WindowWidthSize.COMPACT
    val isMedium: Boolean get() = widthType == WindowWidthSize.MEDIUM
    val isExpanded: Boolean get() = widthType == WindowWidthSize.EXPANDED
    val isLargeFont: Boolean get() = fontScale >= 1.3f
}

object AdaptiveBreakpoints {
    val COMPACT_MAX_WIDTH = 600.dp
    val MEDIUM_MAX_WIDTH = 840.dp

    fun calculateWidthType(widthDp: Dp): WindowWidthSize = when {
        widthDp < COMPACT_MAX_WIDTH -> WindowWidthSize.COMPACT
        widthDp < MEDIUM_MAX_WIDTH -> WindowWidthSize.MEDIUM
        else -> WindowWidthSize.EXPANDED
    }
}

@Composable
fun rememberWindowSizeClass(overrideWidthDp: Dp? = null, overrideHeightDp: Dp? = null): WindowSizeClassInfo {
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val currentWidthDp = overrideWidthDp ?: config.screenWidthDp.dp
    val currentHeightDp = overrideHeightDp ?: config.screenHeightDp.dp
    val fontScale = density.fontScale

    val widthType = AdaptiveBreakpoints.calculateWidthType(currentWidthDp)

    return WindowSizeClassInfo(
        widthType = widthType,
        widthDp = currentWidthDp,
        heightDp = currentHeightDp,
        fontScale = fontScale
    )
}
