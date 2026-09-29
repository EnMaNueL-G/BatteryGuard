package com.enmanuelgil.batteryguard.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.core.HealthEstimate
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.ui.theme.*
import com.enmanuelgil.batteryguard.viewmodel.SettingsState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(info: BatteryInfo, health: HealthEstimate, settings: SettingsState, onGoSettings: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("BatteryGuard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        item { BatteryArcIndicator(info) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Temperatura", "${info.temperatureCelsius.roundToInt()} °C", tempColor(info.temperatureCelsius), Icons.Default.Thermostat)
                MetricCard(Modifier.weight(1f), if (info.isCharging) "Entrando" else "Consumo",
                    info.currentMilliAmps?.let { "${abs(it)} mA" } ?: "—", BatteryBlue, Icons.Default.ElectricBolt,
                    info.powerWatts?.let { "%.1f W · %d mV".format(it, info.voltageMillivolts) } ?: if (info.voltageMillivolts > 0) "${info.voltageMillivolts} mV" else null)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val t = if (info.isCharging) info.chargingMinutes else info.remainingMinutes
                MetricCard(Modifier.weight(1f), if (info.isCharging) "Hasta el 100 %" else "Queda (a este ritmo)", formatMinutes(t),
                    BatteryGreenLight, Icons.Default.Schedule, if (t == null) "sin datos suficientes" else "cambia según el uso")
                MetricCard(Modifier.weight(1f), "Estado (Android)", info.healthStatus.label(),
                    if (info.healthStatus.name == "GOOD") BatteryGreen else BatteryYellow, Icons.Default.FavoriteBorder,
                    info.cycleCount?.let { "$it ciclos" } ?: info.technology.ifBlank { null })
            }
        }
        item { ChargeLimitCard(info, settings, onGoSettings) }
        item { HealthCard(info, health) }
        item {
            Card(Modifier.fillMaxWidth().clickable {
                try { ctx.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) } catch (_: Exception) {
                    try { ctx.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) } catch (_: Exception) {}
                }
            }, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.BatterySaver, null, tint = if (info.powerSaveMode) BatteryGreen else TextSecondary, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ahorro de batería de Android: ${if (info.powerSaveMode) "activado" else "desactivado"}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Es lo que de verdad reduce el consumo (limita procesos en segundo plano, brillo y efectos). Toca para abrirlo.", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                }
            }
        }
        item { TechCard(info) }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ChargeLimitCard(info: BatteryInfo, s: SettingsState, onGoSettings: () -> Unit) {
    val active = s.chargeAlarm && s.monitor
    val txt = when {
        !s.monitor -> "El monitor está apagado: no hay avisos. Actívalo en Ajustes."
        !s.chargeAlarm -> "Aviso de carga desactivado."
        info.isCharging && info.levelPercent >= s.chargeLimit -> "Ya pasaste tu límite del ${s.chargeLimit} %: puedes desenchufar."
        info.isCharging -> "Cargando: te aviso al llegar al ${s.chargeLimit} %."
        else -> "Cuando cargues, te aviso al llegar al ${s.chargeLimit} %."
    }
    Card(Modifier.fillMaxWidth().clickable { onGoSettings() }, colors = CardDefaults.cardColors(containerColor = if (active) BatteryGreen.copy(alpha = 0.10f) else CardDark), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.NotificationsActive, null, tint = if (active) BatteryGreen else TextSecondary, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text("Límite de carga: ${s.chargeLimit} %", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(txt, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
        }
    }
}

@Composable
fun BatteryArcIndicator(info: BatteryInfo) {
    val color = batteryColor(info.levelPercent)
    val p by animateFloatAsState(info.levelPercent / 100f, tween(800), label = "battery")
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(176.dp)) {
                Canvas(Modifier.size(176.dp)) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val stroke = 16.dp.toPx(); val inset = stroke / 2
                    val arc = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(Color.White.copy(alpha = 0.06f), 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = tl, size = arc)
                    val sweep = (270f * p).coerceAtLeast(if (p > 0f) 3f else 0f)
                    if (sweep > 0f) drawArc(color, 135f, sweep, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = tl, size = arc)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${info.levelPercent}%", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = color)
                    val sub = when {
                        info.isFull && info.isCharging -> "Carga completa"
                        info.isCharging -> "Cargando · ${info.chargePlug.label()}"
                        else -> "En uso"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (info.isCharging) Icon(Icons.Default.ElectricBolt, null, tint = BatteryYellow, modifier = Modifier.size(15.dp))
                        Text(sub, fontSize = 12.sp, color = if (info.isCharging) BatteryYellow else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, title: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, caption: String? = null) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(title, fontSize = 11.sp, color = TextSecondary)
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            caption?.let { Text(it, fontSize = 10.sp, color = TextSecondary) }
        }
    }
}

@Composable
fun HealthCard(info: BatteryInfo, h: HealthEstimate) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, null, tint = BatteryGreen, modifier = Modifier.size(20.dp))
                Text("Salud de la batería", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Text(h.healthPercent?.let { "$it %" } ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = when { h.healthPercent == null -> TextSecondary; h.healthPercent >= 85 -> BatteryGreen; h.healthPercent >= 70 -> BatteryYellow; else -> BatteryRed })
            }
            Text(h.method, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            if (h.sessionActive && h.sessionFromLevel != null)
                Text("Midiendo esta carga desde el ${h.sessionFromLevel} %: sigue hasta ≥ ${h.sessionFromLevel + 20} % para que cuente.", fontSize = 12.sp, color = BatteryYellow, lineHeight = 16.sp)
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            InfoRow("Capacidad actual", h.fullMah?.let { "$it mAh" } ?: "—")
            InfoRow("Capacidad de fábrica", h.designMah?.let { "$it mAh" } ?: "tu móvil no la informa")
            InfoRow("Ciclos de carga", info.cycleCount?.toString() ?: if (android.os.Build.VERSION.SDK_INT >= 34) "tu móvil no los informa" else "Android 14 o superior")
            InfoRow("Carga que queda ahora", info.chargeNowMah?.let { "$it mAh" } ?: "—")
            Text("• Cargar entre 20 % y 80 % y evitar el calor (> 40 °C) es lo que más alarga su vida.", fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun TechCard(info: BatteryInfo) {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { open = !open }, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Datos técnicos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary)
            }
            if (open) {
                TechRow("Corriente (valor bruto de Android)", info.rawCurrentNow.toString())
                TechRow("Contador de carga (bruto)", info.rawChargeCounter.toString())
                TechRow("Capacidad total con esta lectura", info.fullNowMah?.let { "$it mAh" } ?: "—")
                TechRow("Voltaje", "${info.voltageMillivolts} mV")
                TechRow("Tecnología", info.technology.ifBlank { "—" })
                Text("Cada fabricante informa estos datos a su manera; BatteryGuard los normaliza (µA/mA y signo).", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

fun formatMinutes(min: Int?): String {
    if (min == null || min <= 0) return "—"
    val h = min / 60; val m = min % 60
    return if (h > 0) "${h} h ${m} min" else "$m min"
}
