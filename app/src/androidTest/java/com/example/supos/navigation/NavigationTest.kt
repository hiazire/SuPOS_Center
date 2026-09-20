package com.example.supos.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.supos.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigateToOrderHistory_displaysOnlineOrderCenterTopBar() {
        // Find "歷史訂單" or "訂單歷史" button on main screen and click it
        composeTestRule.onNodeWithText("歷史訂單").performClick()

        // Verify top bar title "線上訂單中心" is displayed
        composeTestRule.onNodeWithText("線上訂單中心").assertIsDisplayed()

        // Click "返回" to return to main screen
        composeTestRule.onNodeWithText("返回").performClick()

        // Verify main screen is active
        composeTestRule.onNodeWithText("歷史訂單").assertIsDisplayed()
    }
}
