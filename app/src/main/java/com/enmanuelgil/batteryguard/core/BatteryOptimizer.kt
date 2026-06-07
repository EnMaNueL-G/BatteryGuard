package com.enmanuelgil.batteryguard.core

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class OptimizeResult(
    val appsKilled: Int = 0,
    val ramFreedMb: Int = 0,
    val actionsApplied: Int = 0,
    val advancedApplied: Boolean = false,
    val errorMessage: String? = null
)

object BatteryOptimizer {

    suspend fun optimize(context: Context): OptimizeResult = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val cr = context.contentResolver
        var appsKilled = 0
        var actions = 0

        try {
            // 1. Forzar GC del sistema
            Runtime.getRuntime().gc()
            actions++
            delay(200)

            // 2. Matar procesos en background
            val ramBefore = getFreeRamMb(am)
            @Suppress("DEPRECATION")
            val services = am.getRunningServices(200)
            val killed = mutableSetOf<String>()
            for (svc in services) {
                val pkg = svc.process.split(":").first()
                if (pkg != context.packageName && !killed.contains(pkg)) {
                    try {
                        am.killBackgroundProcesses(pkg)
                        killed.add(pkg)
                    } catch (_: Exception) {}
                }
            }
            appsKilled = killed.size
            actions++
            delay(300)

            val ramAfter = getFreeRamMb(am)
            val ramFreed = (ramAfter - ramBefore).coerceAtLeast(0)

            // 3. Intento de optimizaciones avanzadas (WRITE_SECURE_SETTINGS)
            val advancedOk = tryAdvancedOptimizations(cr)
            if (advancedOk) actions += 3

            OptimizeResult(
                appsKilled      = appsKilled,
                ramFreedMb      = ramFreed,
                actionsApplied  = actions,
                advancedApplied = advancedOk
            )
        } catch (e: Exception) {
            OptimizeResult(errorMessage = e.message)
        }
    }

    private fun tryAdvancedOptimizations(cr: ContentResolver): Boolean {
        return try {
            // Reducir animaciones para ahorrar CPU/GPU → menos calor → menos drenaje
            Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
            Settings.Global.putFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 0.5f)
            Settings.Global.putFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE, 0.5f)
            // Desactivar WiFi scan pasivo cuando no se usa
            Settings.Global.putInt(cr, "wifi_scan_always_enabled", 0)
            true
        } catch (e: SecurityException) {
            Log.d("BatteryOptimizer", "WRITE_SECURE_SETTINGS no concedido: ${e.message}")
            false
        }
    }

    fun hasAdvancedPermission(cr: ContentResolver): Boolean {
        return try {
            Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE,
                Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
            true
        } catch (e: SecurityException) { false }
    }

    private fun getFreeRamMb(am: ActivityManager): Int {
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return (info.availMem / 1024 / 1024).toInt()
    }
}
