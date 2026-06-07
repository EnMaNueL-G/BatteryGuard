package com.enmanuelgil.batteryguard.model

data class BatteryInfo(
    val levelPercent: Int = 0,
    val temperatureCelsius: Float = 0f,
    val voltageMillivolts: Int = 0,
    val isCharging: Boolean = false,
    val chargePlug: ChargePlug = ChargePlug.NONE,
    val healthStatus: BatteryHealth = BatteryHealth.UNKNOWN,
    val healthPercent: Int = 0,          // 0-100, estimado
    val cycleCount: Int = 0,             // Android 14+ nativo; estimado en anteriores
    val currentMicroAmps: Int = 0,       // consumo instantáneo (μA)
    val capacityDesignMah: Int = 0,      // capacidad de diseño
    val capacityCurrentMah: Int = 0,     // capacidad actual estimada
    val remainingMinutes: Int = 0,       // minutos restantes estimados
    val chargingMinutes: Int = 0         // minutos para carga completa estimados
)

enum class ChargePlug { NONE, USB, AC, WIRELESS, DOCK }

enum class BatteryHealth {
    UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNSPECIFIED_FAILURE;

    fun label(): String = when(this) {
        GOOD -> "Buena"
        OVERHEAT -> "Sobrecalentada"
        DEAD -> "Muerta"
        OVER_VOLTAGE -> "Sobrevoltaje"
        COLD -> "Muy fría"
        UNSPECIFIED_FAILURE -> "Fallo"
        else -> "Desconocido"
    }

    fun labelEn(): String = when(this) {
        GOOD -> "Good"
        OVERHEAT -> "Overheated"
        DEAD -> "Dead"
        OVER_VOLTAGE -> "Over Voltage"
        COLD -> "Too Cold"
        UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }
}

data class AppDrainInfo(
    val packageName: String,
    val appName: String,
    val drainPercent: Float,         // % del total de batería consumido
    val backgroundTimeMinutes: Long, // minutos en background
    val foregroundTimeMinutes: Long,
    val isSystemApp: Boolean = false
)
