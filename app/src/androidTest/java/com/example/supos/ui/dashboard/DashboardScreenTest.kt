package com.example.supos.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dashboard_showsThreeCoreWorkspaces() {
        composeTestRule.setContent {
            DashboardScreen(
                onOpenOnlineOrders = {},
                onOpenManualOrder = {},
                onOpenPerformance = {}
            )
        }

        composeTestRule.onNodeWithText("線上訂單").assertExists()
        composeTestRule.onNodeWithText("現場開單").assertExists()
        composeTestRule.onNodeWithText("業績戰報").assertExists()
    }
}
