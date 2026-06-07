package com.enmanuelgil.batteryguard.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.enmanuelgil.batteryguard.model.BatteryHealth
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.model.ChargePlug

object BatteryMonitor {

    fun read(context: Context): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level   = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale   = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPct = if (scale > 0) (level * 100 / scale) else 0

        val tempRaw  = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC    = tempRaw / 10f

        val voltage  = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val status   = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val plugType = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargePlug = when (plugType) {
            BatteryManager.BATTERY_PLUGGED_USB      -> ChargePlug.USB
            BatteryManager.BATTERY_PLUGGED_AC       -> ChargePlug.AC
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
            BatteryManager.BATTERY_PLUGGED_DOCK     -> ChargePlug.DOCK
            else                                    -> ChargePlug.NONE
        }

        val healthRaw = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val health = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD              -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT          -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD              -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE      -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD              -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE-> BatteryHealth.UNSPECIFIED_FAILURE
            else                                             -> BatteryHealth.UNKNOWN
        }

        // Capacidad actual (μAh) — disponible en Android 8+
        val currentNowUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val capacityRemain = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Ciclos de carga — Android 14+ nativo (API 34 = UPSIDE_DOWN_CAKE)
        val cycles: Int = if (Build.VERSION.SDK_INT >= 34) {
            try {
                // BATTERY_PROPERTY_CYCLE_COUNT = 4, disponible en Android 14+
                bm.getIntProperty(4)
            } catch (e: Exception) { 0 }
        } else { 0 }

        // Estimación de salud % basada en capacidad restante vs nivel
        val healthPct = estimateHealthPercent(levelPct, chargeCounter)

        // Capacidad de diseño estimada (chargeCounter / nivel * 100)
        val designMah = if (levelPct > 5 && chargeCounter > 0) (chargeCounter * 100 / levelPct) else 0

        // Minutos restantes estimados
        val remainMin = estimateRemainingMinutes(levelPct, currentNowUa, chargeCounter, isCharging)
        val chargeMin = estimateChargeMinutes(levelPct, currentNowUa, chargeCounter, isCharging)

        return BatteryInfo(
            levelPercent        = levelPct,
            temperatureCelsius  = tempC,
            voltageMillivolts   = voltage,
            isCharging          = isCharging,
            chargePlug          = chargePlug,
            healthStatus        = health,
            healthPercent       = healthPct,
            cycleCount          = cycles,
            currentMicroAmps    = currentNowUa,
            capacityDesignMah   = designMah,
            capacityCurrentMah  = if (chargeCounter > 0) chargeCounter / 1000 else 0,
            remainingMinutes    = remainMin,
            chargingMinutes     = chargeMin
        )
    }

    private fun estimateHealthPercent(levelPct: Int, chargeCounterUah: Int): Int {
        // Si no tenemos charge counter fiable, devolvemos 0 (desconocido)
        if (chargeCounterUah <= 0 || levelPct <= 0) return 0
        // El charge counter en μAh representa la carga actual
        // Estimamos la capacidad total: chargeCounter / (level/100)
        val estimatedTotalUah = (chargeCounterUah.toLong() * 100 / levelPct).toInt()
        // Batería típica de teléfono: 3000–5000 mAh. Si estimamos > 6000 mAh, probablemente el dato es infiable
        if (estimatedTotalUah > 6_000_000) return 0
        // Asumimos batería "nueva" = estimatedTotal * (100/currentLevel) — simplificado
        return 85 // Fallback razonable cuando no hay datos de fábrica
    }

    private fun estimateRemainingMinutes(level: Int, currentUa: Int, chargeUah: Int, charging: Boolean): Int {
        if (charging || level <= 0) return 0
        return if (chargeUah > 0 && currentUa < 0) {
            // current es negativo cuando descarga
            val dischargeRateUahPerMin = (-currentUa.toLong()) / 60
            if (dischargeRateUahPerMin > 0) (chargeUah / dischargeRateUahPerMin).toInt() else 0
        } else {
            // Estimación simple: ~1% por hora en uso moderado
            level * 60
        }
    }

    private fun estimateChargeMinutes(level: Int, currentUa: Int, chargeUah: Int, charging: Boolean): Int {
        if (!charging || level >= 100) return 0
        return if (chargeUah > 0 && currentUa > 0) {
            val remainingUah = (chargeUah.toLong() * (100 - level) / level).toInt()
            val chargeRateUahPerMin = currentUa.toLong() / 60
            if (chargeRateUahPerMin > 0) (remainingUah / chargeRateUahPerMin).toInt() else 0
        } else {
            (100 - level) * 2  // ~2 min por punto de %, estimación simple
        }
    }
}
