package com.enmanuelgil.batteryguard.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.model.AppDrainInfo
import com.enmanuelgil.batteryguard.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AppsScreen(
    apps: List<AppDrainInfo>,
    isLoading: Boolean,
    hasUsagePerm: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Consumo por App", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = BatteryGreen)
            }
        }

        if (!hasUsagePerm) {
            // Solicitar permiso
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BatteryYellow.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BatteryYellow)
                        Text("Permiso necesario", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text(
                        "Para analizar qué apps drenan tu batería, BatteryGuard necesita acceso al historial de uso.\nToca el botón para activarlo en Ajustes.",
                        fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
                    )
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = BatteryYellow)
                    ) {
                        Text("Activar en Ajustes", color = CardDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BatteryGreen)
            }
        } else if (apps.isEmpty() && hasUsagePerm) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay datos de uso disponibles.\nUsa el teléfono un poco y vuelve a actualizar.",
                    fontSize = 14.sp, color = TextSecondary)
            }
        } else {
            Text("Últimas 24 horas — ordenadas por consumo", fontSize = 12.sp, color = TextSecondary)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps) { app ->
                    AppDrainCard(app)
                }
            }
        }
    }
}

@Composable
fun AppDrainCard(app: AppDrainInfo) {
    val barColor = when {
        app.drainPercent > 20f -> BatteryRed
        app.drainPercent > 10f -> BatteryYellow
        else                   -> BatteryGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(app.packageName, fontSize = 10.sp, color = TextSecondary)
                }
                Text("${app.drainPercent.roundToInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = barColor)
            }
            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CardDark.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((app.drainPercent / 100f).coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (app.foregroundTimeMinutes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Text("${app.foregroundTimeMinutes}m primer plano", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (app.backgroundTimeMinutes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Text("${app.backgroundTimeMinutes}m background", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
