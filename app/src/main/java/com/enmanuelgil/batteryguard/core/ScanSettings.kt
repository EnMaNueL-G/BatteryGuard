package com.enmanuelgil.batteryguard.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Búsqueda de redes Wi‑Fi y dispositivos Bluetooth "siempre activa": Android sigue escaneando para mejorar
 * la ubicación aunque tengas el Wi‑Fi/Bluetooth apagados. Desactivarla ahorra algo de batería.
 * Necesita el permiso WRITE_SECURE_SETTINGS (se concede una vez por ADB). Antes de cambiar nada se
 * guarda el valor original; "Restaurar" lo devuelve tal cual.
 * (La 1.0.0 cambiaba además las animaciones sin avisar y sin poder deshacerlo.)
 */
object ScanSettings {
    data class Item(val key: String, val title: String, val desc: String)

    val items = listOf(
        Item("wifi_scan_always_enabled", "Búsqueda de Wi‑Fi en segundo plano",
            "Android busca redes Wi‑Fi para la ubicación aunque el Wi‑Fi esté apagado."),
        Item("ble_scan_always_enabled", "Búsqueda de Bluetooth en segundo plano",
            "Android busca dispositivos Bluetooth para la ubicación aunque el Bluetooth esté apagado."),
    )

    private fun backup(c: Context) = c.getSharedPreferences("scan_backup", Context.MODE_PRIVATE)

    fun hasPermission(c: Context) =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** true = activa (Android escanea). null = no se pudo leer. */
    fun isOn(c: Context, key: String): Boolean? = try {
        Settings.Global.getInt(c.contentResolver, key) == 1
    } catch (_: Settings.SettingNotFoundException) { null } catch (_: Exception) { null }

    fun hasBackup(c: Context, key: String) = backup(c).contains(key)

    /** Apaga el escaneo (guardando el valor original la primera vez). Devuelve si quedó apagado de verdad. */
    fun turnOff(c: Context, key: String): Boolean {
        if (!hasPermission(c)) return false
        val cur = try { Settings.Global.getInt(c.contentResolver, key) } catch (_: Exception) { -1 }
        if (!hasBackup(c, key)) backup(c).edit().putInt(key, cur).apply()
        return try { Settings.Global.putInt(c.contentResolver, key, 0); isOn(c, key) == false } catch (_: Exception) { false }
    }

    /** Devuelve el valor original guardado. */
    fun restore(c: Context, key: String): Boolean {
        if (!hasPermission(c)) return false
        val b = backup(c)
        val orig = b.getInt(key, 1)
        return try {
            Settings.Global.putInt(c.contentResolver, key, if (orig == -1) 1 else orig)
            b.edit().remove(key).apply()
            true
        } catch (_: Exception) { false }
    }
}
