package com.enmanuelgil.batteryguard.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.model.AppUsageInfo
import com.enmanuelgil.batteryguard.ui.theme.*

@Composable
fun AppsScreen(apps: List<AppUsageInfo>, loading: Boolean, hasPermission: Boolean, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Apps que más usas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                if (hasPermission) IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = BatteryGreen) }
            }
            Text("Últimas 24 horas", fontSize = 12.sp, color = TextSecondary)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = BatteryBlue.copy(alpha = 0.08f)), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Android no deja que una app mida la batería que gastan las demás: solo el sistema lo sabe. " +
                        "Aquí ves el TIEMPO de uso, que es la mejor pista: lo que más tiempo pasa en pantalla o trabajando, más gasta.",
                        fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    OutlinedButton(onClick = {
                        try { ctx.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) } catch (_: Exception) {
                            try { ctx.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) } catch (_: Exception) {}
                        }
                    }) { Icon(Icons.Default.BatteryChargingFull, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ver el consumo real en Ajustes de Android", fontSize = 12.sp) }
                }
            }
        }
        if (!hasPermission) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Falta un permiso", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Para ver el tiempo de uso, busca BatteryGuard en la lista y activa «Permitir acceso de uso». No sale nada de tu móvil.",
                            fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                        Button(onClick = {
                            val i = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = Uri.parse("package:${ctx.packageName}") }
                            try { ctx.startActivity(i) } catch (_: Exception) { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        }, colors = ButtonDefaults.buttonColors(containerColor = BatteryGreen)) { Text("Dar permiso") }
                    }
                }
            }
        } else if (loading) {
            item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BatteryGreen) } }
        } else if (apps.isEmpty()) {
            item { Text("Sin datos de uso en las últimas 24 horas.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
        } else {
            item { Text("Toca una app para ver su información y restringir su batería en Android.", fontSize = 12.sp, color = TextSecondary) }
            items(apps, key = { it.packageName }) { app -> UsageRow(app) }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun UsageRow(app: AppUsageInfo) {
    val ctx = LocalContext.current
    val color = when { app.sharePercent >= 25f -> BatteryRed; app.sharePercent >= 10f -> BatteryYellow; else -> BatteryGreen }
    Card(Modifier.fillMaxWidth().clickable {
        try { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) } catch (_: Exception) {}
    }, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(
                        if (app.foregroundMinutes > 0) "${fmt(app.foregroundMinutes)} en pantalla" else null,
                        if (app.serviceMinutes > 0) "${fmt(app.serviceMinutes)} trabajando en segundo plano" else null,
                        if (app.isSystemApp) "del sistema" else null,
                    ).joinToString(" · "), fontSize = 11.sp, color = TextSecondary)
                }
                Text("${app.sharePercent.toInt()} %", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            }
            LinearProgressIndicator(progress = { (app.sharePercent / 100f).coerceIn(0.01f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = color, trackColor = BackgroundDark)
        }
    }
}

private fun fmt(min: Long): String = if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
