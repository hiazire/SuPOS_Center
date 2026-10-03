package com.example.supos.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object SamsungOptimizationHelper {

    /**
     * 檢測是否為 Samsung 品牌設備 (例如 Galaxy Tab S7 SM-T870/T875)
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    /**
     * 檢測當前應用程式是否已取得原生 Android 電池最佳化豁免 (Doze Mode 白名單)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 取得原生 Android 請求忽略電池最佳化 Intent
     */
    fun getRequestBatteryOptimizationIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
    }

    /**
     * 取得 Samsung One UI 專屬裝置維護 (Device Care) 或背景休眠管理 Intent。
     * 若為 Samsung 設備，嘗試開啟 One UI 深度休眠/永不休眠管理；否則返回標準應用程式設定。
     */
    fun getSamsungNeverSleepingAppsIntent(context: Context): Intent {
        if (!isSamsungDevice()) {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }

        // Samsung 裝置維護 (Device Care) 常見套件與活動列表
        val samsungIntents = listOf(
            // One UI 3.x - 5.x 背景用量限制 (Background usage limits / Never sleeping apps)
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.ui.battery.AppSleepListActivity"
                )
            ),
            Intent("com.samsung.android.sm.ACTION_BATTERY")
        )

        val packageManager = context.packageManager
        for (intent in samsungIntents) {
            if (intent.resolveActivity(packageManager) != null) {
                return intent
            }
        }

        // 若特定的 Samsung Component 未命中，降級至電池最佳化總覽或 App 詳細設定
        val fallbackBatterySettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (fallbackBatterySettings.resolveActivity(packageManager) != null) {
            return fallbackBatterySettings
        }

        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
