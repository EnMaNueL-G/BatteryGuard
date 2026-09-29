package com.enmanuelgil.batteryguard.core

import android.content.Context
import org.json.JSONArray
import kotlin.math.roundToInt

/**
 * Cálculos puros (sin Android) para poder probarlos con tests.
 */
object CapacityMath {
    const val MIN_SESSION_DELTA = 20      // una carga debe subir al menos 20 puntos para contar
    const val MAX_ESTIMATES = 12

    /** Capacidad total (mAh) deducida de una carga: mAh que entraron / puntos que subió × 100. */
    fun estimateFromSession(startLevel: Int, endLevel: Int, startMah: Int, endMah: Int, designMah: Int?): Int? {
        val dl = endLevel - startLevel
        val dm = endMah - startMah
        if (dl < MIN_SESSION_DELTA || dm <= 0) return null
        val est = (dm * 100.0 / dl).roundToInt()
        return est.takeIf { plausible(it, designMah) }
    }

    /** Descarta lecturas absurdas (medidores que se reinician, datos en otra unidad…). */
    fun plausible(mah: Int, designMah: Int?): Boolean {
        if (mah !in 800..25_000) return false
        if (designMah != null && (mah < designMah * 0.45 || mah > designMah * 1.25)) return false
        return true
    }

    fun median(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else ((s[s.size / 2 - 1] + s[s.size / 2]) / 2.0).roundToInt()
    }

    /** Salud (%) = capacidad actual / de fábrica. Se limita a 100 (una batería nueva a veces supera su valor nominal). */
    fun healthPercent(fullMah: Int?, designMah: Int?): Int? {
        if (fullMah == null || designMah == null || designMah <= 0) return null
        return (fullMah * 100.0 / designMah).roundToInt().coerceIn(1, 100)
    }
}

/** Resultado para la interfaz. */
data class HealthEstimate(
    val healthPercent: Int?,       // null = sin datos suficientes
    val fullMah: Int?,             // capacidad actual estimada
    val designMah: Int?,
    val learnedSessions: Int,      // cargas completas medidas
    val method: String,            // explicación corta de cómo se calculó
    val sessionActive: Boolean,
    val sessionFromLevel: Int?,
)

/**
 * Aprende la capacidad real de la batería observando las cargas (lo hace el monitor en segundo plano).
 * Todo se guarda solo en este móvil.
 */
object HealthTracker {
    private fun prefs(c: Context) = c.getSharedPreferences("health", Context.MODE_PRIVATE)

    /** Se llama en cada cambio de batería (nivel, carga, enchufe). */
    @Synchronized
    fun onSample(c: Context, level: Int, chargeMah: Int?, charging: Boolean, designMah: Int?) {
        val p = prefs(c)
        val e = p.edit()
        // 1) lectura instantánea del medidor (carga / nivel) → para la "estimación rápida"
        if (chargeMah != null && level >= 50) {   // a nivel alto el medidor es más fiable
            val inst = (chargeMah * 100.0 / level).roundToInt()
            if (CapacityMath.plausible(inst, designMah)) {
                val arr = readList(p.getString("inst", null))
                if (arr.isEmpty() || level != p.getInt("instLastLevel", -1)) {
                    arr.add(inst); while (arr.size > 40) arr.removeAt(0)
                    e.putString("inst", JSONArray(arr).toString()).putInt("instLastLevel", level)
                }
            }
        }
        // 2) sesiones de carga
        val active = p.getBoolean("sActive", false)
        if (charging && chargeMah != null) {
            if (!active) {
                e.putBoolean("sActive", true).putInt("sStartL", level).putInt("sStartM", chargeMah)
            }
            e.putInt("sLastL", level).putInt("sLastM", chargeMah)
        } else if (!charging && active) {
            val est = CapacityMath.estimateFromSession(p.getInt("sStartL", 0), p.getInt("sLastL", 0), p.getInt("sStartM", 0), p.getInt("sLastM", 0), designMah)
            if (est != null) {
                val arr = readList(p.getString("learned", null))
                arr.add(est); while (arr.size > CapacityMath.MAX_ESTIMATES) arr.removeAt(0)
                e.putString("learned", JSONArray(arr).toString())
            }
            e.putBoolean("sActive", false)
        }
        e.apply()
    }

    fun estimate(c: Context, designMah: Int?): HealthEstimate {
        val p = prefs(c)
        val learned = readList(p.getString("learned", null))
        val inst = readList(p.getString("inst", null))
        val active = p.getBoolean("sActive", false)
        val fromL = if (active) p.getInt("sStartL", 0) else null
        return when {
            learned.isNotEmpty() -> {
                val full = CapacityMath.median(learned)
                HealthEstimate(CapacityMath.healthPercent(full, designMah), full, designMah, learned.size,
                    "Medida en ${learned.size} carga(s) real(es)", active, fromL)
            }
            inst.isNotEmpty() -> {
                val full = CapacityMath.median(inst)
                HealthEstimate(CapacityMath.healthPercent(full, designMah), full, designMah, 0,
                    "Estimación rápida con el medidor de la batería · se afina tras cargar de ≤ 60 % a ≥ 80 %", active, fromL)
            }
            else -> HealthEstimate(null, null, designMah, 0,
                "Recogiendo datos: deja el monitor activo y carga el móvil (de ≤ 60 % a ≥ 80 %)", active, fromL)
        }
    }

    fun reset(c: Context) { prefs(c).edit().clear().apply() }

    private fun readList(json: String?): MutableList<Int> {
        if (json.isNullOrBlank()) return mutableListOf()
        return try { val a = JSONArray(json); MutableList(a.length()) { a.getInt(it) } } catch (_: Exception) { mutableListOf() }
    }
}
