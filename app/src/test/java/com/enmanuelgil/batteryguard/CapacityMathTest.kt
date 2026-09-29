package com.enmanuelgil.batteryguard

import com.enmanuelgil.batteryguard.core.BatteryMonitor
import com.enmanuelgil.batteryguard.core.CapacityMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapacityMathTest {

    @Test fun cargaReal_S25_30a80_da_su_capacidad() {
        // 5000 mAh de fábrica, entran 2450 mAh al subir 50 puntos → 4900 mAh (98 %)
        val est = CapacityMath.estimateFromSession(30, 80, 1470, 3920, 5000)
        assertEquals(4900, est)
        assertEquals(98, CapacityMath.healthPercent(est, 5000))
    }

    @Test fun cargaCorta_no_cuenta() {
        assertNull(CapacityMath.estimateFromSession(70, 85, 3000, 3750, 5000))
    }

    @Test fun medidorReiniciado_o_absurdo_se_descarta() {
        assertNull(CapacityMath.estimateFromSession(20, 80, 4000, 3000, 5000))      // bajó: medidor reiniciado
        assertNull(CapacityMath.estimateFromSession(20, 80, 1000, 9000, 5000))      // 13 333 mAh en un móvil de 5000
        assertFalse(CapacityMath.plausible(500, null))
        assertTrue(CapacityMath.plausible(4800, 5000))
    }

    @Test fun mediana_y_salud_limitada_a_100() {
        assertEquals(4900, CapacityMath.median(listOf(4700, 5100, 4900)))
        assertEquals(4800, CapacityMath.median(listOf(4700, 4900)))
        assertEquals(100, CapacityMath.healthPercent(5200, 5000))
        assertNull(CapacityMath.healthPercent(4800, null))
    }

    @Test fun corriente_en_microamperios_y_en_miliamperios() {
        // Android dice µA; muchos fabricantes dan mA o el signo al revés
        assertEquals(-850, BatteryMonitor.normalizeCurrentMa(-850_000, charging = false))   // µA estándar
        assertEquals(-850, BatteryMonitor.normalizeCurrentMa(850, charging = false))        // mA con signo invertido
        assertEquals(2300, BatteryMonitor.normalizeCurrentMa(-2_300_000, charging = true))  // cargando pero con signo −
        assertNull(BatteryMonitor.normalizeCurrentMa(0, charging = false))
    }

    @Test fun contador_de_carga_uah_y_mah() {
        assertEquals(4737, BatteryMonitor.normalizeChargeMah(4_737_850))   // S25 (µAh)
        assertEquals(3110, BatteryMonitor.normalizeChargeMah(3110))         // algunos dan mAh
        assertNull(BatteryMonitor.normalizeChargeMah(0))
    }
}
