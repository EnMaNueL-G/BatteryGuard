package com.enmanuelgil.batteryguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.core.OptimizeResult
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.model.ChargePlug
import com.enmanuelgil.batteryguard.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    info: BatteryInfo,
    isOptimizing: Boolean      = false,
    lastResult: OptimizeResult? = null,
    onOptimize: () -> Unit     = {},
    onDismissResult: () -> Unit = {}
) {
    // LazyColumn — previene ArrayIndexOutOfBoundsException en recomposición
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("BatteryGuard", fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Indicador de batería principal
        item { BatteryArcIndicator(info) }

        // Métricas secundarias — fila 1
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Temperatura",
                    "${info.temperatureCelsius.roundToInt()}°C",
                    tempColor(info.temperatureCelsius), Icons.Default.Thermostat)
                MetricCard(Modifier.weight(1f), "Voltaje",
                    if (info.voltageMillivolts > 0) "${info.voltageMillivolts} mV" else "—",
                    BatteryBlue, Icons.Default.ElectricBolt)
            }
        }

        // Métricas secundarias — fila 2
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f),
                    if (info.isCharging) "Carga completa en" else "Tiempo restante",
                    formatMinutes(if (info.isCharging) info.chargingMinutes else info.remainingMinutes),
                    BatteryGreenLight, Icons.Default.Schedule)
                MetricCard(Modifier.weight(1f), "Estado",
                    info.healthStatus.label(), BatteryGreen, Icons.Default.FavoriteBorder)
            }
        }

        // Información de carga (solo si está cargando)
        if (info.isCharging) {
            item { ChargingCard(info) }
        }

        // Botón optimizar
        item { QuickOptimizeButton(isOptimizing, onOptimize) }

        // Confirmación de optimización — aparece 4s y se auto-cierra
        lastResult?.let { result ->
            item {
                LaunchedEffect(result) {
                    kotlinx.coroutines.delay(4000)
                    onDismissResult()
                }
                OptimizeResultCard(result, onDismissResult)
            }
        }

        // Salud de la batería
        item { HealthCard(info) }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Tarjeta de confirmación de optimización ────────────────────────────────────
@Composable
fun OptimizeResultCard(result: OptimizeResult, onDismiss: () -> Unit) {
    val success = result.appsKilled > 0 || result.actionsApplied > 0
    val (bg, fg, icon, title) = if (success) {
        arrayOf(
            BatteryGreen.copy(alpha = 0.12f),
            BatteryGreen,
            Icons.Default.CheckCircle,
            "✅ Optimización completada"
        )
    } else {
        arrayOf(
            BatteryBlue.copy(alpha = 0.10f),
            BatteryBlue,
            Icons.Default.Info,
            "ℹ️ Sistema analizado"
        )
    }

    val details = buildList {
        if (result.appsKilled > 0)    add("${result.appsKilled} apps en background detenidas")
        if (result.ramFreedMb > 0)    add("${result.ramFreedMb} MB de RAM liberados")
        if (result.advancedApplied)   add("Optimizaciones avanzadas aplicadas")
        if (result.actionsApplied > 0 && result.appsKilled == 0 && result.ramFreedMb == 0)
            add("${result.actionsApplied} ajustes del sistema aplicados")
        if (isEmpty())                add("El sistema ya estaba optimizado")
    }.joinToString(" · ")

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg as Color),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, (fg as Color).copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null,
                tint = fg, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text(title as String, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(details, fontSize = 12.sp, color = fg, lineHeight = 16.sp)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Indicador de arco de batería ──────────────────────────────────────────────
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
                    // Guardia de tamaño — previene crash cuando size=0 en primera medición
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val stroke = 16.dp.toPx()
                    val inset  = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(
                        color = CardDark.copy(alpha = 0.3f),
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = topLeft, size = arcSize
                    )
                    val sweep = (270f * animatedProgress).coerceAtLeast(if (animatedProgress > 0f) 3f else 0f)
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = 135f, sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = topLeft, size = arcSize
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${info.levelPercent}%", fontSize = 42.sp,
                        fontWeight = FontWeight.Bold, color = color)
                    if (info.isCharging) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, null, tint = BatteryYellow,
                                modifier = Modifier.size(16.dp))
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
fun MetricCard(modifier: Modifier, title: String, value: String, color: Color,
               icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.BatteryChargingFull, null, tint = BatteryYellow,
                    modifier = Modifier.size(28.dp))
                Column {
                    Text("Cargando via ${info.chargePlug.name}", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (info.chargingMinutes > 0)
                        Text("Completo en ${formatMinutes(info.chargingMinutes)}",
                            fontSize = 12.sp, color = TextSecondary)
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
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, null, tint = BatteryGreen, modifier = Modifier.size(20.dp))
                Text("Salud de la Batería", fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            InfoRow("Estado", info.healthStatus.label())
            if (info.cycleCount > 0) InfoRow("Ciclos de carga", info.cycleCount.toString())
            if (info.capacityCurrentMah > 0) InfoRow("Capacidad actual", "${info.capacityCurrentMah} mAh")
            if (info.capacityDesignMah > 0) InfoRow("Capacidad estimada", "${info.capacityDesignMah} mAh")
            Text(
                "• Mantén entre 20–80% para maximizar la vida útil\n" +
                "• Evita cargar a 100% todas las noches\n" +
                "• Temperaturas >45°C dañan la batería permanentemente",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun QuickOptimizeButton(isOptimizing: Boolean, onOptimize: () -> Unit) {
    Card(
        onClick = { if (!isOptimizing) onOptimize() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOptimizing) BatteryYellow.copy(alpha = 0.15f)
                             else BatteryGreen.copy(alpha = 0.12f)
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
                null,
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
                    if (isOptimizing) "Liberando recursos del sistema"
                    else "Matar background, reducir consumo",
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
