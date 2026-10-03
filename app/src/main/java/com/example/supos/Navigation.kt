package com.example.supos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.supos.ui.dashboard.DashboardScreen
import com.example.supos.ui.main.MainScreen
import com.example.supos.ui.performance.PerformanceScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Dashboard)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Dashboard> {
          DashboardScreen(
            onOpenOnlineOrders = { backStack.add(OnlineOrderReceiver) },
            onOpenManualOrder = { backStack.add(Main) },
            onOpenPerformance = { backStack.add(Performance) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            onBackToDashboard = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<OnlineOrderReceiver> {
          com.example.supos.ui.online.OnlineOrderReceiverScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<OrderHistory> {
          com.example.supos.ui.history.OrderHistoryScreen(
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
        entry<Performance> {
          PerformanceScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
