package com.enmanuelgil.batteryguard.model

/**
 * Lectura instantánea de la batería. Todo lo "estimado" se marca como tal: si Android no da un dato,
 * el valor queda en null (la interfaz muestra "—"), nunca un número inventado.
 */
data class BatteryInfo(
    val levelPercent: Int = 0,
    val temperatureCelsius: Float = 0f,
    val voltageMillivolts: Int = 0,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val chargePlug: ChargePlug = ChargePlug.NONE,
    val healthStatus: BatteryHealth = BatteryHealth.UNKNOWN,
    val cycleCount: Int? = null,            // Android 14+: dato del sistema
    val currentMilliAmps: Int? = null,      // + cargando / − descargando (ya normalizado)
    val powerWatts: Float? = null,          // potencia instantánea = V × I
    val chargeNowMah: Int? = null,          // carga que queda ahora (charge counter)
    val fullNowMah: Int? = null,            // capacidad total estimada con la lectura de este momento
    val designMah: Int? = null,             // capacidad de fábrica (perfil de energía del fabricante)
    val remainingMinutes: Int? = null,      // al ritmo actual de consumo
    val chargingMinutes: Int? = null,       // hasta el 100 % al ritmo actual de carga
    val powerSaveMode: Boolean = false,
    val technology: String = "",
    val rawCurrentNow: Int = 0,             // valor bruto de Android (para "Datos técnicos")
    val rawChargeCounter: Int = 0,
)

enum class ChargePlug {
    NONE, USB, AC, WIRELESS, DOCK;
    fun label(): String = when (this) {
        USB -> "USB"; AC -> "cargador"; WIRELESS -> "inalámbrica"; DOCK -> "base"; NONE -> "desconectado"
    }
}

enum class BatteryHealth {
    UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNSPECIFIED_FAILURE;

    fun label(): String = when (this) {
        GOOD -> "Buena"
        OVERHEAT -> "Sobrecalentada"
        DEAD -> "Agotada"
        OVER_VOLTAGE -> "Sobrevoltaje"
        COLD -> "Muy fría"
        UNSPECIFIED_FAILURE -> "Fallo"
        else -> "Sin dato"
    }
}

/** Uso de una app en las últimas 24 h (tiempo, no batería: Android no deja medir la batería de otras apps). */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val foregroundMinutes: Long,     // en pantalla
    val serviceMinutes: Long,        // trabajando con notificación visible (música, navegación, descargas…)
    val sharePercent: Float,         // % del tiempo total en pantalla
    val isSystemApp: Boolean = false,
)
