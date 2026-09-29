package com.enmanuelgil.batteryguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.enmanuelgil.batteryguard.MainActivity
import com.enmanuelgil.batteryguard.R
import com.enmanuelgil.batteryguard.core.BatteryMonitor
import com.enmanuelgil.batteryguard.core.HealthTracker
import com.enmanuelgil.batteryguard.core.Prefs
import com.enmanuelgil.batteryguard.model.BatteryInfo
import kotlin.math.roundToInt

/**
 * Monitor ligero: NO hace sondeos. Escucha el aviso de Android "la batería cambió" (llega al cambiar
 * nivel, temperatura o enchufe) y con eso:
 *   · actualiza la notificación (nivel · temperatura · estado),
 *   · avisa al llegar al límite de carga (80 % por defecto), por calor o por batería baja,
 *   · aprende la capacidad real de la batería con cada carga.
 * Tipo "specialUse": Android 15+ no deja arrancar los de tipo "dataSync" al encender el móvil y los
 * corta a las 6 h (la 1.0.0 usaba dataSync y un bucle cada 15 s).
 */
class BatteryMonitorService : Service() {

    private var receiver: BroadcastReceiver? = null
    private var lastText = ""
    private var chargeAlarmFired = false
    private var lowAlarmFired = false
    private var lastTempAlert = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildOngoing("Vigilando la batería…"), type)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { onBattery(BatteryMonitor.read(c, i)) }
        }
        // ACTION_BATTERY_CHANGED es "sticky": al registrarse llega el estado actual al momento
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun onBattery(info: BatteryInfo) {
        HealthTracker.onSample(this, info.levelPercent, info.chargeNowMah, info.isCharging && !info.isFull, info.designMah)

        val state = when {
            info.isFull && info.isCharging -> "Carga completa"
            info.isCharging -> "Cargando" + (info.powerWatts?.let { " · %.1f W".format(it) } ?: "")
            else -> "En uso"
        }
        val text = "${info.levelPercent} % · ${info.temperatureCelsius.roundToInt()} °C · $state"
        if (text != lastText) {
            lastText = text
            notify(NOTIF_ID, buildOngoing(text))
        }

        // Límite de carga: una vez por cada vez que se enchufa
        if (!info.isCharging) chargeAlarmFired = false
        val limit = Prefs.chargeLimit(this)
        if (Prefs.chargeAlarmEnabled(this) && info.isCharging && !chargeAlarmFired && info.levelPercent >= limit) {
            chargeAlarmFired = true
            alert(ALERT_CHARGE, "Batería al ${info.levelPercent} %: desenchufa",
                "Llegó a tu límite del $limit %. Parar aquí alarga la vida de la batería.")
        }
        // Batería baja: una vez hasta que vuelva a cargarse por encima del límite
        val low = Prefs.lowLimit(this)
        if (info.isCharging || info.levelPercent > low + 5) lowAlarmFired = false
        if (Prefs.lowAlertEnabled(this) && !info.isCharging && !lowAlarmFired && info.levelPercent <= low) {
            lowAlarmFired = true
            alert(ALERT_LOW, "Batería baja: ${info.levelPercent} %", "Conecta el cargador. Descargarla del todo a menudo la desgasta.")
        }
        // Temperatura: como mucho un aviso cada 10 minutos
        val tLimit = Prefs.tempLimit(this)
        if (Prefs.tempAlertEnabled(this) && info.temperatureCelsius >= tLimit && System.currentTimeMillis() - lastTempAlert > 10 * 60_000) {
            lastTempAlert = System.currentTimeMillis()
            alert(ALERT_TEMP, "Batería caliente: ${info.temperatureCelsius.roundToInt()} °C",
                if (info.isCharging) "Si puedes, deja de usarlo mientras carga o quita la funda." else "Cierra juegos o apps pesadas y deja que se enfríe.")
        }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    private fun buildOngoing(text: String): Notification =
        NotificationCompat.Builder(this, CH_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle("BatteryGuard")
            .setContentText(text)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun alert(id: Int, title: String, text: String) {
        notify(id, NotificationCompat.Builder(this, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build())
    }

    private fun notify(id: Int, n: Notification) {
        try { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        try { receiver?.let { unregisterReceiver(it) } } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        private const val CH_MONITOR = "battery_monitor"
        private const val CH_ALERTS = "battery_alerts"
        private const val NOTIF_ID = 200
        private const val ALERT_CHARGE = 201
        private const val ALERT_TEMP = 202
        private const val ALERT_LOW = 203

        fun createChannels(c: Context) {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CH_MONITOR, "Monitor de batería", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Nivel y temperatura en la barra de notificaciones"; setShowBadge(false)
            })
            nm.createNotificationChannel(NotificationChannel(CH_ALERTS, "Avisos de batería", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Límite de carga, calor y batería baja"
            })
        }

        fun start(ctx: Context) {
            if (!Prefs.monitorEnabled(ctx)) return
            try { ctx.startForegroundService(Intent(ctx, BatteryMonitorService::class.java)) } catch (_: Exception) {}
        }
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, BatteryMonitorService::class.java))
    }
}
