package com.enmanuelgil.batteryguard.core

import android.annotation.SuppressLint
import android.content.Context

/**
 * Capacidad de FÁBRICA de la batería (mAh), leída del perfil de energía que el fabricante mete en
 * Android (com.android.internal.os.PowerProfile). No es una API pública: si el móvil no la expone o da
 * un valor absurdo, se devuelve null y la app lo dice en vez de inventar.
 */
object DesignCapacity {
    @Volatile private var cached: Int? = null
    @Volatile private var tried = false

    @SuppressLint("PrivateApi")
    fun mah(context: Context): Int? {
        if (tried) return cached
        tried = true
        cached = try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val profile = cls.getConstructor(Context::class.java).newInstance(context)
            val v = cls.getMethod("getBatteryCapacity").invoke(profile) as Double
            v.toInt().takeIf { it in 1000..20_000 }
        } catch (_: Throwable) { null }
        return cached
    }
}
