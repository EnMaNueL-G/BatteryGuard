package com.enmanuelgil.batteryguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Al encender el móvil (o tras actualizar la app) vuelve a arrancar el monitor, si el usuario lo tiene activado. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            BatteryMonitorService.start(context)
        }
    }
}
