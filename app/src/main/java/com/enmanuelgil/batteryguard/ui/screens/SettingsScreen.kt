package com.enmanuelgil.batteryguard.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.batteryguard.core.HealthEstimate
import com.enmanuelgil.batteryguard.ui.theme.*
import com.enmanuelgil.batteryguard.viewmodel.MainViewModel
import com.enmanuelgil.batteryguard.viewmodel.ScanState
import com.enmanuelgil.batteryguard.viewmodel.SettingsState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(s: SettingsState, scan: ScanState, health: HealthEstimate, vm: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val version = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Exception) { "" } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        SectionLabel("Monitor en segundo plano")
        SettingsCard {
            SwitchRow("Vigilar la batería", "Muestra nivel y temperatura en las notificaciones y hace posibles los avisos. Solo reacciona cuando la batería cambia: gasta muy poco.", s.monitor, vm::setMonitor)
        }

        SectionLabel("Avisos")
        SettingsCard {
            SwitchRow("Límite de carga", "Te avisa al llegar al ${s.chargeLimit} % mientras cargas. Desenchufar ahí alarga la vida de la batería.", s.chargeAlarm, vm::setChargeAlarm)
            if (s.chargeAlarm) StepSlider(s.chargeLimit, 50, 100, 5, "%", vm::setChargeLimit)
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            SwitchRow("Batería caliente", "Aviso si la batería llega a ${s.tempLimit} °C (como mucho uno cada 10 minutos).", s.tempAlert, vm::setTempAlert)
            if (s.tempAlert) StepSlider(s.tempLimit, 35, 50, 1, "°C", vm::setTempLimit)
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            SwitchRow("Batería baja", "Aviso al bajar del ${s.lowLimit} % para que la cargues antes de que se agote.", s.lowAlert, vm::setLowAlert)
            if (s.lowAlert) StepSlider(s.lowLimit, 5, 50, 5, "%", vm::setLowLimit)
        }

        SectionLabel("Búsqueda de redes en segundo plano (avanzado)")
        SettingsCard {
            if (!scan.hasPermission) {
                Text("Android sigue buscando redes Wi‑Fi y dispositivos Bluetooth para la ubicación aunque los tengas apagados. " +
                    "Para poder desactivarlo desde aquí, conecta el móvil al PC y ejecuta una sola vez:", fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                val cmd = "adb shell pm grant ${ctx.packageName} android.permission.WRITE_SECURE_SETTINGS"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cmd, fontSize = 11.sp, color = BatteryYellow, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(cmd)) }) { Icon(Icons.Default.ContentCopy, "Copiar", tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                }
                Text("También puedes hacerlo tú en Ajustes → Ubicación → Servicios de ubicación.", fontSize = 11.sp, color = TextSecondary)
            } else {
                scan.items.forEachIndexed { i, it ->
                    if (i > 0) HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(it.title, fontSize = 14.sp, color = TextPrimary)
                            Text(it.desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                            Text(when (it.on) { true -> "Activa"; false -> "Desactivada"; null -> "No disponible en este móvil" },
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (it.on == false) BatteryGreen else BatteryYellow)
                        }
                        when {
                            it.on == true -> TextButton(onClick = { vm.scanOff(it.key) }) { Text("Desactivar", color = BatteryGreen) }
                            it.hasBackup -> TextButton(onClick = { vm.scanRestore(it.key) }) { Text("Restaurar", color = TextSecondary) }
                        }
                    }
                }
                Text("Antes de cambiar nada se guarda el valor original; «Restaurar» lo deja como estaba.", fontSize = 11.sp, color = TextSecondary)
            }
        }

        SectionLabel("Salud de la batería")
        SettingsCard {
            Text("La salud se calcula con tus propias cargas y se guarda solo en este móvil. Si cambiaste la batería o los datos no cuadran, bórralas para empezar de cero.",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
            Text("Cargas medidas: ${health.learnedSessions}", fontSize = 12.sp, color = TextPrimary)
            OutlinedButton(onClick = vm::resetHealth) { Text("Borrar mediciones") }
        }

        SectionLabel("Consejos que sí funcionan")
        SettingsCard {
            TipRow("⚡", "Carga entre 20 % y 80 %: es lo que más alarga la vida de la batería")
            TipRow("🌡", "Evita el calor: no la uses para juegos pesados mientras carga ni la dejes al sol")
            TipRow("🔋", "El ahorro de batería de Android reduce el consumo de verdad")
            TipRow("📱", "La pantalla es lo que más gasta: brillo automático y bloqueo rápido")
            TipRow("🌙", "Modo oscuro en pantallas OLED ahorra algo con brillo alto")
            TipRow("🚫", "Las apps que \"matan procesos\" no ahorran batería en Android moderno: el sistema vuelve a abrirlos")
        }

        SectionLabel("Apoya el proyecto")
        Card(Modifier.fillMaxWidth().border(1.dp, BatteryYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gratis, sin anuncios y de código abierto. Si te es útil, puedes apoyar su desarrollo (toca para copiar):",
                    fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                DonationRow("Binance Pay ID", "1165745950") { clipboard.setText(AnnotatedString("1165745950")) }
                DonationRow("USDT (BSC · BEP-20)", "0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08") { clipboard.setText(AnnotatedString("0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08")) }
            }
        }

        SectionLabel("Acerca de")
        SettingsCard {
            InfoRow("Versión", version ?: "")
            InfoRow("Desarrollado por", "Enmanuel Gil · OptiSuite")
            InfoRow("Compatibilidad", "Android 8.0 o superior")
            Text("No recoge datos, no tiene anuncios y no usa Internet: todo se calcula en tu móvil.", fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = BatteryGreen))
    }
}

@Composable
private fun StepSlider(value: Int, min: Int, max: Int, step: Int, unit: String, onChange: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(value = v, onValueChange = { v = it }, onValueChangeFinished = { onChange((v / step).roundToInt() * step) },
            valueRange = min.toFloat()..max.toFloat(), steps = (max - min) / step - 1, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = BatteryGreen, activeTrackColor = BatteryGreen))
        Text("${((v / step).roundToInt() * step)} $unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BatteryGreen, modifier = Modifier.width(56.dp))
    }
}

@Composable
fun SectionLabel(text: String) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary) }

@Composable
fun TipRow(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(emoji, fontSize = 14.sp)
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
fun DonationRow(label: String, value: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BatteryYellow)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ContentCopy, "Copiar", tint = TextSecondary, modifier = Modifier.size(18.dp)) }
    }
}
