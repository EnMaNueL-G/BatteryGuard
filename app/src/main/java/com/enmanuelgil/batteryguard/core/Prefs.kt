package com.enmanuelgil.batteryguard.core

import android.content.Context

/** Ajustes del usuario (solo en este móvil). */
object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun monitorEnabled(c: Context) = p(c).getBoolean("monitor", true)
    fun setMonitorEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("monitor", v).apply()

    fun chargeAlarmEnabled(c: Context) = p(c).getBoolean("chargeAlarm", true)
    fun setChargeAlarmEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("chargeAlarm", v).apply()

    fun chargeLimit(c: Context) = p(c).getInt("chargeLimit", 80)
    fun setChargeLimit(c: Context, v: Int) = p(c).edit().putInt("chargeLimit", v.coerceIn(50, 100)).apply()

    fun tempAlertEnabled(c: Context) = p(c).getBoolean("tempAlert", true)
    fun setTempAlertEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("tempAlert", v).apply()

    fun tempLimit(c: Context) = p(c).getInt("tempLimit", 42)
    fun setTempLimit(c: Context, v: Int) = p(c).edit().putInt("tempLimit", v.coerceIn(35, 50)).apply()

    fun lowAlertEnabled(c: Context) = p(c).getBoolean("lowAlert", false)
    fun setLowAlertEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("lowAlert", v).apply()

    fun lowLimit(c: Context) = p(c).getInt("lowLimit", 20)
    fun setLowLimit(c: Context, v: Int) = p(c).edit().putInt("lowLimit", v.coerceIn(5, 50)).apply()
}
