package com.enmanuelgil.batteryguard.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.enmanuelgil.batteryguard.MainActivity
import com.enmanuelgil.batteryguard.core.BatteryMonitor
import kotlinx.coroutines.*

class BatteryMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Monitoreando batería…"))
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val info = BatteryMonitor.read(this@BatteryMonitorService)
                val msg = "${info.levelPercent}% — ${info.temperatureCelsius}°C — ${if (info.isCharging) "Cargando" else "Descargando"}"
                updateNotification(msg)

                if (info.temperatureCelsius > 45f) {
                    showAlert("Temperatura elevada: ${info.temperatureCelsius}°C")
                }
                delay(15_000)
            }
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun showAlert(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("¡Temperatura de batería elevada!")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(ALERT_ID, notif)
    }

    private fun buildNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setContentTitle("BatteryGuard")
            .setContentText(text)
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Monitor Batería", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(ALERT_CHANNEL, "Alertas Batería", NotificationManager.IMPORTANCE_HIGH))
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val CHANNEL_ID  = "battery_monitor"
        private const val ALERT_CHANNEL= "battery_alert"
        private const val NOTIF_ID    = 200
        private const val ALERT_ID    = 201

        fun start(ctx: Context) = ctx.startForegroundService(Intent(ctx, BatteryMonitorService::class.java))
        fun stop(ctx: Context)  = ctx.stopService(Intent(ctx, BatteryMonitorService::class.java))
    }
}
