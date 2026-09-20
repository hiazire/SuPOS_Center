package com.example.supos.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val HtcSenseFontFamily = FontFamily.SansSerif

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = HtcSenseFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = HtcSenseFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = HtcSenseFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = HtcSenseFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = HtcSenseFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = HtcSenseFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = HtcSenseFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = HtcSenseFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = HtcSenseFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = HtcSenseFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = HtcSenseFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = HtcSenseFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = HtcSenseFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = HtcSenseFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = HtcSenseFontFamily)
)
