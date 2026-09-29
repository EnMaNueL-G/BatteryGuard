package com.enmanuelgil.batteryguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.batteryguard.core.BatteryMonitor
import com.enmanuelgil.batteryguard.core.HealthEstimate
import com.enmanuelgil.batteryguard.core.HealthTracker
import com.enmanuelgil.batteryguard.core.Prefs
import com.enmanuelgil.batteryguard.core.ScanSettings
import com.enmanuelgil.batteryguard.core.UsageAnalyzer
import com.enmanuelgil.batteryguard.model.AppUsageInfo
import com.enmanuelgil.batteryguard.model.BatteryInfo
import com.enmanuelgil.batteryguard.service.BatteryMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsState(
    val monitor: Boolean = true,
    val chargeAlarm: Boolean = true, val chargeLimit: Int = 80,
    val tempAlert: Boolean = true, val tempLimit: Int = 42,
    val lowAlert: Boolean = false, val lowLimit: Int = 20,
)

data class ScanState(val hasPermission: Boolean = false, val items: List<ScanItemState> = emptyList())
data class ScanItemState(val key: String, val title: String, val desc: String, val on: Boolean?, val hasBackup: Boolean)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()

    private val _battery = MutableStateFlow(BatteryInfo())
    val battery: StateFlow<BatteryInfo> = _battery
    private val _health = MutableStateFlow(HealthEstimate(null, null, null, 0, "", false, null))
    val health: StateFlow<HealthEstimate> = _health
    private val _apps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val apps: StateFlow<List<AppUsageInfo>> = _apps
    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps
    private val _hasUsagePerm = MutableStateFlow(false)
    val hasUsagePerm: StateFlow<Boolean> = _hasUsagePerm
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings
    private val _scan = MutableStateFlow(ScanState())
    val scan: StateFlow<ScanState> = _scan
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        loadSettings()
        viewModelScope.launch {
            while (true) {
                val b = withContext(Dispatchers.Default) { BatteryMonitor.read(ctx) }
                _battery.value = b
                // Si el monitor está apagado, la app también aprende mientras está abierta
                withContext(Dispatchers.IO) { HealthTracker.onSample(ctx, b.levelPercent, b.chargeNowMah, b.isCharging && !b.isFull, b.designMah) }
                _health.value = withContext(Dispatchers.IO) { HealthTracker.estimate(ctx, b.designMah) }
                delay(3_000)
            }
        }
    }

    fun onResume() {
        _hasUsagePerm.value = UsageAnalyzer.hasPermission(ctx)
        refreshScan()
        if (_hasUsagePerm.value && _apps.value.isEmpty()) loadApps()
    }

    fun loadApps() {
        if (!UsageAnalyzer.hasPermission(ctx)) { _hasUsagePerm.value = false; return }
        _hasUsagePerm.value = true
        viewModelScope.launch {
            _loadingApps.value = true
            _apps.value = withContext(Dispatchers.IO) { UsageAnalyzer.topApps(ctx) }   // fuera del hilo principal
            _loadingApps.value = false
        }
    }

    private fun loadSettings() {
        _settings.value = SettingsState(
            Prefs.monitorEnabled(ctx), Prefs.chargeAlarmEnabled(ctx), Prefs.chargeLimit(ctx),
            Prefs.tempAlertEnabled(ctx), Prefs.tempLimit(ctx), Prefs.lowAlertEnabled(ctx), Prefs.lowLimit(ctx))
    }

    fun setMonitor(v: Boolean) {
        Prefs.setMonitorEnabled(ctx, v)
        if (v) BatteryMonitorService.start(ctx) else BatteryMonitorService.stop(ctx)
        loadSettings()
    }
    fun setChargeAlarm(v: Boolean) { Prefs.setChargeAlarmEnabled(ctx, v); loadSettings(); ensureMonitorFor(v) }
    fun setChargeLimit(v: Int) { Prefs.setChargeLimit(ctx, v); loadSettings() }
    fun setTempAlert(v: Boolean) { Prefs.setTempAlertEnabled(ctx, v); loadSettings(); ensureMonitorFor(v) }
    fun setTempLimit(v: Int) { Prefs.setTempLimit(ctx, v); loadSettings() }
    fun setLowAlert(v: Boolean) { Prefs.setLowAlertEnabled(ctx, v); loadSettings(); ensureMonitorFor(v) }
    fun setLowLimit(v: Int) { Prefs.setLowLimit(ctx, v); loadSettings() }
    private fun ensureMonitorFor(alertOn: Boolean) {
        if (alertOn && !Prefs.monitorEnabled(ctx)) { _message.value = "Los avisos necesitan el monitor en segundo plano: actívalo arriba." }
    }

    fun refreshScan() {
        _scan.value = ScanState(ScanSettings.hasPermission(ctx), ScanSettings.items.map {
            ScanItemState(it.key, it.title, it.desc, ScanSettings.isOn(ctx, it.key), ScanSettings.hasBackup(ctx, it.key))
        })
    }
    fun scanOff(key: String) { val ok = ScanSettings.turnOff(ctx, key); _message.value = if (ok) "Desactivado. Puedes restaurarlo cuando quieras." else "No se pudo cambiar (falta el permiso por ADB)."; refreshScan() }
    fun scanRestore(key: String) { val ok = ScanSettings.restore(ctx, key); _message.value = if (ok) "Restaurado a como estaba." else "No se pudo restaurar."; refreshScan() }

    fun resetHealth() { HealthTracker.reset(ctx); _health.value = HealthTracker.estimate(ctx, _battery.value.designMah); _message.value = "Mediciones borradas: la salud se volverá a calcular." }
    fun consumeMessage() { _message.value = null }
}
