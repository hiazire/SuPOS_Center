package com.example.supos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.supos.MainActivity
import com.example.supos.R
import com.example.supos.SuPOSApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OrderReceiverService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var alertJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID_MONITOR = "supos_order_monitor_channel"
        const val CHANNEL_ID_ALERT = "supos_new_order_alert_channel"
        const val NOTIFICATION_ID_MONITOR = 1001
        const val NOTIFICATION_ID_ALERT_BASE = 2000

        fun startService(context: Context) {
            val intent = Intent(context, OrderReceiverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OrderReceiverService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SuPOS:OrderReceiverWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8.0+ 前台服務必須在 5 秒內呼叫 startForeground
        val ongoingNotification = buildOngoingNotification()
        startForeground(NOTIFICATION_ID_MONITOR, ongoingNotification)

        val app = application as? SuPOSApplication
        if (app != null) {
            val receiverManager = app.orderReceiverManager
            receiverManager.start()

            alertJob?.cancel()
            alertJob = serviceScope.launch {
                receiverManager.newOrderEventFlow.collect { event ->
                    handleNewOrderAlert(event.newOrdersCount, event.newOrderIds)
                }
            }
        }

        return START_STICKY
    }

    private fun handleNewOrderAlert(orderCount: Int, orderIds: List<String>) {
        // 申請 5 秒安全 WakeLock，確保休眠時完整喚醒 CPU 發出響鈴
        try {
            wakeLock?.acquire(5000L)
        } catch (_: Exception) { }

        val alertNotification = buildNewOrderNotification(orderCount, orderIds)
        notificationManager.notify(NOTIFICATION_ID_ALERT_BASE, alertNotification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: 常駐狀態監聽 (無聲、低干擾)
            val monitorChannel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                "SuPOS 接單監聽常駐",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "維持 SuPOS 背景接單服務運作，防止系統誤殺"
                setShowBadge(false)
            }

            // Channel 2: 新單抵達強提醒 (高優先級、響鈴、震動)
            val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "SuPOS 線上新單提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "線上訂單即時進件提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(alertSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_MONITOR)
            .setContentTitle("SuPOS 接單服務運作中")
            .setContentText("正在即時監聽線上外送與外帶訂單")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun buildNewOrderNotification(orderCount: Int, orderIds: List<String>): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "ORDERS")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        return NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setContentTitle("🔔 收到 $orderCount 筆線上新訂單！")
            .setContentText("訂單編號: ${orderIds.joinToString(", ")}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        alertJob?.cancel()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) { }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
