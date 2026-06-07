package com.enmanuelgil.batteryguard.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.ui.theme.*

@Composable
fun SettingsScreen(hasAdvanced: Boolean) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Estado permisos
        SectionLabel("Estado de Permisos")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusRow("Optimización avanzada (ADB)", hasAdvanced)
                if (!hasAdvanced) {
                    Text(
                        "Ejecuta desde PC:\nadb shell pm grant com.enmanuelgil.batteryguard android.permission.WRITE_SECURE_SETTINGS",
                        fontSize = 12.sp, color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Consejos
        SectionLabel("Consejos para maximizar la batería")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipRow("⚡", "Carga entre 20% y 80% para prolongar la vida útil")
                TipRow("🌡", "Evita usar el teléfono con temperaturas >40°C")
                TipRow("📶", "Desactiva WiFi, BT y GPS cuando no los uses")
                TipRow("🌙", "Activa el modo oscuro — ahorra hasta 15% en pantallas OLED")
                TipRow("🔋", "El modo de bajo consumo baja la frecuencia de CPU")
                TipRow("📱", "Reduce el brillo al 30% — es el mayor consumidor de energía")
            }
        }

        // Donaciones
        SectionLabel("Apoya el Proyecto")
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BatteryYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = BatteryYellow)
                    Text("¿Te fue útil BatteryGuard?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("100% gratuita, sin anuncios, código abierto. Si mejoró la vida de tu batería, considerá apoyar el desarrollo.",
                    fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                Text("Binance Pay ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BatteryYellow)
                DonationRow("Pay ID", "1140153333") { clipboard.setText(AnnotatedString("1140153333")) }
                Text("BSC BEP20 (Binance Smart Chain)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BatteryYellow)
                DonationRow("Dirección", "0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1") {
                    clipboard.setText(AnnotatedString("0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1"))
                }
            }
        }

        // Acerca de
        SectionLabel("Acerca de BatteryGuard")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Versión", "1.0.0")
                InfoRow("Desarrollado por", "Enmanuel Gil")
                InfoRow("Compatibilidad", "Android 8.0+ (API 26)")
                InfoRow("Dependencias externas", "Ninguna")
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                Text("No recopila datos personales. No requiere internet. No modifica archivos del usuario.",
                    fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

@Composable
fun StatusRow(label: String, active: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                if (active) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null, tint = if (active) BatteryGreen else BatteryRed,
                modifier = Modifier.size(18.dp)
            )
            Text(if (active) "Activo" else "Inactivo", fontSize = 13.sp,
                color = if (active) BatteryGreen else TextSecondary)
        }
    }
}

@Composable
fun TipRow(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(emoji, fontSize = 14.sp)
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
fun DonationRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BatteryYellow)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
