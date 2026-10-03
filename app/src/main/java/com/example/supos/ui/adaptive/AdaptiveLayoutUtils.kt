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

    // 針對 Samsung Galaxy Tab S7 11 吋 (16:10 寬螢幕，橫向寬度 >= 1200dp) 判定
    val isLargeTabletLandscape: Boolean get() = widthDp >= 1200.dp && widthDp > heightDp
}

data class PosColumnWeights(
    val cartWeight: Float,
    val functionWeight: Float,
    val menuWeight: Float
)

object TabletLayoutProportions {
    // Samsung Galaxy Tab S7 16:10 橫向：沿用 Windows POS 的三欄心智模型，
    // 但由 Compose weight 填滿全部可用寬度，不保留 4:3 黑邊。
    val LARGE_WIDESCREEN_TABLET = PosColumnWeights(
        cartWeight = 0.34f,
        functionWeight = 0.13f,
        menuWeight = 0.53f
    )

    // 其他 expanded 平板仍保留可讀性較高的功能欄寬度。
    val STANDARD_EXPANDED = PosColumnWeights(
        cartWeight = 0.34f,
        functionWeight = 0.16f,
        menuWeight = 0.50f
    )

    fun getProportions(isLargeTabletLandscape: Boolean): PosColumnWeights {
        return if (isLargeTabletLandscape) LARGE_WIDESCREEN_TABLET else STANDARD_EXPANDED
    }
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
