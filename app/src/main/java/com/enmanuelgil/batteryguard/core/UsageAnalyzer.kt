package com.enmanuelgil.batteryguard.core

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import com.enmanuelgil.batteryguard.model.AppUsageInfo
import java.util.concurrent.TimeUnit

/**
 * Qué apps se usaron más en las últimas 24 h. Android NO permite a una app medir la batería que gastan
 * las demás (solo el sistema lo sabe: Ajustes → Batería), así que aquí se muestra TIEMPO, que es la
 * mejor pista honesta: lo que más tiempo está en pantalla o trabajando, más gasta.
 */
object UsageAnalyzer {
    private const val BASE_PKG = "com.enmanuelgil.batteryguard"

    fun topApps(context: Context, topN: Int = 15): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val pm = context.packageManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.HOURS.toMillis(24)
        // queryAndAggregateUsageStats: UNA entrada por app (la 1.0.0 podía repetir apps)
        val stats = try { usm.queryAndAggregateUsageStats(start, end) } catch (_: Exception) { return emptyList() }
        val rows = stats.values
            // fuera la propia app y sus variantes (p. ej. una versión .debug instalada)
            .filter { !it.packageName.startsWith(BASE_PKG) }
            .map { s ->
                // Android agrupa por días completos: sin este límite salían "26 h" en una ventana de 24 h
                val cap = end - start
                val svc = if (Build.VERSION.SDK_INT >= 29) s.totalTimeForegroundServiceUsed.coerceAtMost(cap) else 0L
                Triple(s.packageName, s.totalTimeInForeground.coerceAtMost(cap), svc)
            }
            .filter { it.second >= 60_000 || it.third >= 60_000 }
        val totalFg = rows.sumOf { it.second }.coerceAtLeast(1L)
        return rows.mapNotNull { (pkg, fg, svc) ->
            val ai = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { return@mapNotNull null }
            val launcher = pm.getLaunchIntentForPackage(pkg) != null
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !launcher) return@mapNotNull null        // quita procesos internos del sistema
            AppUsageInfo(
                packageName = pkg,
                appName = pm.getApplicationLabel(ai).toString(),
                foregroundMinutes = TimeUnit.MILLISECONDS.toMinutes(fg),
                serviceMinutes = TimeUnit.MILLISECONDS.toMinutes(svc),
                sharePercent = (fg.toFloat() / totalFg * 100f).coerceIn(0f, 100f),
                isSystemApp = isSystem,
            )
        }.sortedWith(compareByDescending<AppUsageInfo> { it.foregroundMinutes }.thenByDescending { it.serviceMinutes }).take(topN)
    }

    /** Permiso de uso concedido (sin hacer consultas pesadas). */
    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
