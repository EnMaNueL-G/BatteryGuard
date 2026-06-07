package com.enmanuelgil.batteryguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.model.ChargePlug
import com.enmanuelgil.batteryguard.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    info: BatteryInfo,
    isOptimizing: Boolean = false,
    onOptimize: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("BatteryGuard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Indicador principal de batería
        BatteryArcIndicator(info)

        // Métricas secundarias
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(Modifier.weight(1f), "Temperatura", "${info.temperatureCelsius.roundToInt()}°C",
                tempColor(info.temperatureCelsius), Icons.Default.Thermostat)
            MetricCard(Modifier.weight(1f), "Voltaje",
                if (info.voltageMillivolts > 0) "${info.voltageMillivolts} mV" else "—",
                BatteryBlue, Icons.Default.ElectricBolt)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(Modifier.weight(1f),
                if (info.isCharging) "Carga completa en" else "Tiempo restante",
                formatMinutes(if (info.isCharging) info.chargingMinutes else info.remainingMinutes),
                BatteryGreenLight, Icons.Default.Schedule)
            MetricCard(Modifier.weight(1f), "Estado",
                info.healthStatus.label(), BatteryGreen, Icons.Default.FavoriteBorder)
        }

        // Información de carga
        if (info.isCharging) {
            ChargingCard(info)
        }

        // Botón optimizar
        QuickOptimizeButton(isOptimizing, onOptimize)

        // Salud de la batería
        HealthCard(info)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun BatteryArcIndicator(info: BatteryInfo) {
    val color = batteryColor(info.levelPercent)
    val animatedProgress by animateFloatAsState(
        targetValue = info.levelPercent / 100f,
        animationSpec = tween(800), label = "battery"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val stroke = 16.dp.toPx()
                    val inset  = stroke / 2
                    drawArc(
                        color      = CardDark.copy(alpha = 0.3f),
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter  = false,
                        style      = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                        size       = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    )
                    drawArc(
                        color      = color,
                        startAngle = 135f, sweepAngle = 270f * animatedProgress,
                        useCenter  = false,
                        style      = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                        size       = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${info.levelPercent}%", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = color)
                    if (info.isCharging) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = BatteryYellow, modifier = Modifier.size(16.dp))
                            Text(info.chargePlug.name, fontSize = 12.sp, color = BatteryYellow)
                        }
                    } else {
                        Text("Descargando", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, title: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(title, fontSize = 11.sp, color = TextSecondary)
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ChargingCard(info: BatteryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = BatteryYellow, modifier = Modifier.size(28.dp))
                Column {
                    Text("Cargando via ${info.chargePlug.name}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (info.chargingMinutes > 0)
                        Text("Completo en ${formatMinutes(info.chargingMinutes)}", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Text("${info.voltageMillivolts} mV", fontSize = 13.sp, color = BatteryBlue)
        }
    }
}

@Composable
fun HealthCard(info: BatteryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(20.dp))
                Text("Salud de la Batería", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            InfoRow("Estado", info.healthStatus.label())
            if (info.cycleCount > 0) InfoRow("Ciclos de carga", info.cycleCount.toString())
            if (info.capacityCurrentMah > 0) InfoRow("Capacidad actual", "${info.capacityCurrentMah} mAh")
            if (info.capacityDesignMah > 0) InfoRow("Capacidad estimada total", "${info.capacityDesignMah} mAh")
            Text(
                "• Mantén entre 20–80% para maximizar la vida útil\n• Evita cargar a 100% todas las noches\n• Temperaturas >45°C dañan la batería permanentemente",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun QuickOptimizeButton(isOptimizing: Boolean, onOptimize: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "scale"
    )
    Card(
        onClick = { if (!isOptimizing) onOptimize() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOptimizing) BatteryYellow.copy(alpha = 0.15f) else BatteryGreen.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isOptimizing) Icons.Default.HourglassTop else Icons.Default.BatteryFull,
                contentDescription = null,
                tint = if (isOptimizing) BatteryYellow else BatteryGreen,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (isOptimizing) "Optimizando…" else "Optimizar Batería",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (isOptimizing) BatteryYellow else BatteryGreen
                )
                Text(
                    if (isOptimizing) "Liberando recursos del sistema" else "Matar background, reducir consumo",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

private fun formatMinutes(min: Int): String {
    if (min <= 0) return "—"
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
