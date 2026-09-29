package com.enmanuelgil.batteryguard.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.enmanuelgil.batteryguard.model.BatteryHealth
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.model.ChargePlug
import kotlin.math.abs
import kotlin.math.roundToInt

object BatteryMonitor {

    /** Extra oficial de Android 14+ (BatteryManager.EXTRA_CYCLE_COUNT). */
    private const val EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT"

    fun read(context: Context, intentIn: Intent? = null): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = intentIn ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val tempC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugRaw = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugged = plugRaw != 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || (plugged && status == BatteryManager.BATTERY_STATUS_FULL)
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = when (plugRaw) {
            BatteryManager.BATTERY_PLUGGED_USB -> ChargePlug.USB
            BatteryManager.BATTERY_PLUGGED_AC -> ChargePlug.AC
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
            8 /* BATTERY_PLUGGED_DOCK (API 33) */ -> ChargePlug.DOCK
            else -> ChargePlug.NONE
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            else -> BatteryHealth.UNKNOWN
        }
        // Ciclos: la 1.0.0 leía la propiedad 4 (= % de carga) creyendo que eran ciclos
        val cycles = if (Build.VERSION.SDK_INT >= 34) intent?.getIntExtra(EXTRA_CYCLE_COUNT, -1)?.takeIf { it > 0 } else null

        val rawCurrent = safeProp(bm, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val rawAvg = safeProp(bm, BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val rawCounter = safeProp(bm, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val currentMa = normalizeCurrentMa(rawCurrent, isCharging && !isFull)
        val avgMa = normalizeCurrentMa(rawAvg, isCharging && !isFull)
        val useMa = avgMa?.takeIf { abs(it) >= 20 } ?: currentMa
        val chargeNow = normalizeChargeMah(rawCounter)
        val fullNow = if (chargeNow != null && levelPct >= 15) (chargeNow * 100.0 / levelPct).roundToInt() else null
        val design = DesignCapacity.mah(context)

        val power = if (currentMa != null && voltage > 0) abs(currentMa) * voltage / 1_000_000f else null
        val remaining = if (!isCharging && useMa != null && useMa < -20 && chargeNow != null) (chargeNow * 60.0 / -useMa).roundToInt() else null
        val toFull = if (isCharging && !isFull && useMa != null && useMa > 20 && fullNow != null && chargeNow != null)
            (((fullNow - chargeNow).coerceAtLeast(0)) * 60.0 / useMa * 1.15).roundToInt()   // +15 %: la carga se frena al final
        else null

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return BatteryInfo(
            levelPercent = levelPct,
            temperatureCelsius = tempC,
            voltageMillivolts = voltage,
            isCharging = isCharging,
            isFull = isFull,
            chargePlug = chargePlug,
            healthStatus = health,
            cycleCount = cycles,
            currentMilliAmps = currentMa,
            powerWatts = power,
            chargeNowMah = chargeNow,
            fullNowMah = fullNow,
            designMah = design,
            remainingMinutes = remaining?.takeIf { it in 1..(60 * 24 * 4) },
            chargingMinutes = toFull?.takeIf { it in 1..(60 * 24) },
            powerSaveMode = pm.isPowerSaveMode,
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "",
            rawCurrentNow = rawCurrent,
            rawChargeCounter = rawCounter,
        )
    }

    private fun safeProp(bm: BatteryManager, id: Int): Int = try {
        val v = bm.getIntProperty(id)
        if (v == Int.MIN_VALUE) 0 else v
    } catch (_: Exception) { 0 }

    /**
     * Android define CURRENT_NOW en µA, pero muchos fabricantes lo dan en mA y con el signo al revés.
     * Un móvil consume/carga entre ~20 mA y ~12 A: un valor absoluto < 15 000 solo tiene sentido en mA.
     * El signo se pone según el estado real (+ cargando, − descargando).
     */
    fun normalizeCurrentMa(raw: Int, charging: Boolean): Int? {
        if (raw == 0) return null
        val a = abs(raw.toLong())
        val ma = if (a < 15_000) a else a / 1000
        if (ma == 0L || ma > 15_000) return null
        return if (charging) ma.toInt() else -ma.toInt()
    }

    /** CHARGE_COUNTER debería ser µAh; algunos lo dan en mAh. Devuelve mAh o null si no es creíble. */
    fun normalizeChargeMah(raw: Int): Int? {
        if (raw <= 0) return null
        val mah = if (raw < 30_000) raw else raw / 1000
        return mah.takeIf { it in 50..30_000 }
    }
}
