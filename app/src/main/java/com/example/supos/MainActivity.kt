package com.example.supos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.supos.theme.SuPOSTheme

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 隱藏頂部狀態列 (Status Bar) 與底部導覽列 (Navigation Bar)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

    enableEdgeToEdge()
    setContent {
      SuPOSTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation()
        }
      }
    }
  }
}
