package com.example.supos.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun breakpointCalculation_compactSizes() {
        assertEquals(WindowWidthSize.COMPACT, AdaptiveBreakpoints.calculateWidthType(360.dp))
        assertEquals(WindowWidthSize.COMPACT, AdaptiveBreakpoints.calculateWidthType(480.dp))
        assertEquals(WindowWidthSize.COMPACT, AdaptiveBreakpoints.calculateWidthType(599.dp))
    }

    @Test
    fun breakpointCalculation_mediumSizes() {
        // 7 吋平板或半螢幕分割
        assertEquals(WindowWidthSize.MEDIUM, AdaptiveBreakpoints.calculateWidthType(600.dp))
        assertEquals(WindowWidthSize.MEDIUM, AdaptiveBreakpoints.calculateWidthType(720.dp))
        assertEquals(WindowWidthSize.MEDIUM, AdaptiveBreakpoints.calculateWidthType(839.dp))
    }

    @Test
    fun breakpointCalculation_expandedSizes() {
        // 10 吋與 12 吋平板橫向
        assertEquals(WindowWidthSize.EXPANDED, AdaptiveBreakpoints.calculateWidthType(840.dp))
        assertEquals(WindowWidthSize.EXPANDED, AdaptiveBreakpoints.calculateWidthType(1024.dp))
        assertEquals(WindowWidthSize.EXPANDED, AdaptiveBreakpoints.calculateWidthType(1280.dp))
    }

    @Test
    fun fontScale_largeFontDetection() {
        val normalInfo = WindowSizeClassInfo(
            widthType = WindowWidthSize.EXPANDED,
            widthDp = 1024.dp,
            heightDp = 768.dp,
            fontScale = 1.0f
        )
        assertFalse(normalInfo.isLargeFont)

        val font13 = WindowSizeClassInfo(
            widthType = WindowWidthSize.EXPANDED,
            widthDp = 1024.dp,
            heightDp = 768.dp,
            fontScale = 1.3f
        )
        assertTrue(font13.isLargeFont)

        val font20 = WindowSizeClassInfo(
            widthType = WindowWidthSize.COMPACT,
            widthDp = 400.dp,
            heightDp = 800.dp,
            fontScale = 2.0f
        )
        assertTrue(font20.isLargeFont)
    }

    @Test
    fun windowSizeClassInfo_flagsConsistency() {
        val compact = WindowSizeClassInfo(
            widthType = WindowWidthSize.COMPACT,
            widthDp = 400.dp,
            heightDp = 800.dp,
            fontScale = 1.0f
        )
        assertTrue(compact.isCompact)
        assertFalse(compact.isMedium)
        assertFalse(compact.isExpanded)

        val medium = WindowSizeClassInfo(
            widthType = WindowWidthSize.MEDIUM,
            widthDp = 700.dp,
            heightDp = 900.dp,
            fontScale = 1.0f
        )
        assertFalse(medium.isCompact)
        assertTrue(medium.isMedium)
        assertFalse(medium.isExpanded)

        val expanded = WindowSizeClassInfo(
            widthType = WindowWidthSize.EXPANDED,
            widthDp = 1200.dp,
            heightDp = 800.dp,
            fontScale = 1.0f
        )
        assertFalse(expanded.isCompact)
        assertFalse(expanded.isMedium)
        assertTrue(expanded.isExpanded)
    }
}
