package com.enmanuelgil.batteryguard.core

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.enmanuelgil.batteryguard.model.AppDrainInfo
import java.util.concurrent.TimeUnit

object AppDrainAnalyzer {

    fun getTopDrainApps(context: Context, topN: Int = 10): List<AppDrainInfo> {
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val endTime   = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.HOURS.toMillis(24)

        val usageStats = try {
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        } catch (e: Exception) { return emptyList() }

        if (usageStats.isNullOrEmpty()) return emptyList()

        // Calcular tiempo total para obtener porcentajes relativos
        val totalFgMs = usageStats.sumOf { it.totalTimeInForeground }.coerceAtLeast(1L)

        return usageStats
            .filter { it.totalTimeInForeground > 0 || it.totalTimeForegroundServiceUsed > 0 }
            .filter { it.packageName != context.packageName }  // Excluir la propia app del ranking
            .mapNotNull { stats ->
                val appName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(stats.packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) { return@mapNotNull null }

                val isSystem = try {
                    (pm.getApplicationInfo(stats.packageName, 0).flags
                            and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) { false }

                val fgMin  = TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeInForeground)
                val bgMin  = TimeUnit.MILLISECONDS.toMinutes(
                    stats.totalTimeForegroundServiceUsed.coerceAtLeast(0L)
                )
                val drainPct = (stats.totalTimeInForeground.toFloat() / totalFgMs * 100f)
                    .coerceIn(0f, 100f)

                AppDrainInfo(
                    packageName            = stats.packageName,
                    appName                = appName,
                    drainPercent           = drainPct,
                    backgroundTimeMinutes  = bgMin,
                    foregroundTimeMinutes  = fgMin,
                    isSystemApp            = isSystem
                )
            }
            .sortedByDescending { it.drainPercent }
            .take(topN)
    }

    fun hasUsagePermission(context: Context): Boolean {
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val end   = System.currentTimeMillis()
        val start = end - TimeUnit.MINUTES.toMillis(5)
        val stats = try {
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        } catch (e: Exception) { return false }
        return !stats.isNullOrEmpty()
    }
}
