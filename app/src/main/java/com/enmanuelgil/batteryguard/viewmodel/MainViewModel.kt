package com.enmanuelgil.batteryguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.batteryguard.core.AppDrainAnalyzer
import com.enmanuelgil.batteryguard.core.BatteryMonitor
import com.enmanuelgil.batteryguard.core.BatteryOptimizer
import com.enmanuelgil.batteryguard.core.OptimizeResult
import com.enmanuelgil.batteryguard.model.AppDrainInfo
import com.enmanuelgil.batteryguard.model.BatteryInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    private val _batteryInfo   = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo

    private val _drainApps     = MutableStateFlow<List<AppDrainInfo>>(emptyList())
    val drainApps: StateFlow<List<AppDrainInfo>> = _drainApps

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps

    private val _isOptimizing  = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing

    private val _lastResult    = MutableStateFlow<OptimizeResult?>(null)
    val lastResult: StateFlow<OptimizeResult?> = _lastResult

    private val _hasAdvanced   = MutableStateFlow(false)
    val hasAdvanced: StateFlow<Boolean> = _hasAdvanced

    private val _hasUsagePerm  = MutableStateFlow(false)
    val hasUsagePerm: StateFlow<Boolean> = _hasUsagePerm

    init {
        startPolling()
        checkPermissions()
        loadDrainApps()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                _batteryInfo.value = BatteryMonitor.read(ctx)
                delay(5_000)
            }
        }
    }

    fun checkPermissions() {
        _hasAdvanced.value  = BatteryOptimizer.hasAdvancedPermission(ctx.contentResolver)
        _hasUsagePerm.value = AppDrainAnalyzer.hasUsagePermission(ctx)
    }

    fun loadDrainApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _drainApps.value = AppDrainAnalyzer.getTopDrainApps(ctx)
            _isLoadingApps.value = false
        }
    }

    fun optimize() {
        if (_isOptimizing.value) return
        viewModelScope.launch {
            _isOptimizing.value = true
            _lastResult.value = null
            val result = BatteryOptimizer.optimize(ctx)
            _lastResult.value = result
            _batteryInfo.value = BatteryMonitor.read(ctx)
            _isOptimizing.value = false
        }
    }
}
