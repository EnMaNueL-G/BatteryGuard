package com.enmanuelgil.batteryguard

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.enmanuelgil.batteryguard.service.BatteryMonitorService
import com.enmanuelgil.batteryguard.ui.screens.*
import com.enmanuelgil.batteryguard.ui.theme.*
import com.enmanuelgil.batteryguard.viewmodel.MainViewModel

class BatteryGuardApp : Application() {
    override fun onCreate() { super.onCreate() }
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BatteryMonitorService.start(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent { BatteryGuardTheme { BatteryGuardApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.checkPermissions()
    }
}

@Composable
fun BatteryGuardApp(viewModel: MainViewModel) {
    val batteryInfo   by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val drainApps     by viewModel.drainApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val isOptimizing  by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val lastResult    by viewModel.lastResult.collectAsStateWithLifecycle()
    val hasAdvanced   by viewModel.hasAdvanced.collectAsStateWithLifecycle()
    val hasUsagePerm  by viewModel.hasUsagePerm.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    val windowInfo = currentWindowAdaptiveInfo()
    val isTablet   = windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val tabs = listOf(
        NavTab("Panel",   Icons.Default.Battery5Bar),
        NavTab("Apps",    Icons.Default.Apps),
        NavTab("Ajustes", Icons.Default.Settings)
    )

    val content: @Composable (PaddingValues) -> Unit = { pv ->
        Box(Modifier.fillMaxSize().background(BackgroundDark).padding(pv)) {
            when (currentTab) {
                0 -> DashboardScreen(
                    info            = batteryInfo,
                    isOptimizing    = isOptimizing,
                    lastResult      = lastResult,
                    onOptimize      = viewModel::optimize,
                    onDismissResult = { viewModel.dismissResult() }
                )
                1 -> AppsScreen(drainApps, isLoadingApps, hasUsagePerm, viewModel::loadDrainApps)
                2 -> SettingsScreen(hasAdvanced)
            }
        }
    }

    if (isTablet) {
        BatteryGuardTheme {
            Row(Modifier.fillMaxSize().background(BackgroundDark)) {
                NavigationRail(containerColor = SurfaceDark) {
                    Spacer(Modifier.height(16.dp))
                    tabs.forEachIndexed { i, tab ->
                        NavigationRailItem(
                            selected = currentTab == i, onClick = { currentTab = i },
                            icon = { Icon(tab.icon, tab.label, tint = if (currentTab == i) BatteryGreen else TextSecondary) },
                            label = { Text(tab.label, color = if (currentTab == i) BatteryGreen else TextSecondary, fontWeight = if (currentTab == i) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = BatteryGreen.copy(alpha = 0.15f))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Box(Modifier.fillMaxSize()) { content(PaddingValues(0.dp)) }
            }
        }
    } else {
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = currentTab == i, onClick = { currentTab = i },
                            icon = { Icon(tab.icon, tab.label, tint = if (currentTab == i) BatteryGreen else TextSecondary) },
                            label = { Text(tab.label, color = if (currentTab == i) BatteryGreen else TextSecondary) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = BatteryGreen.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        ) { pv -> content(pv) }
    }
}

data class NavTab(val label: String, val icon: ImageVector)
