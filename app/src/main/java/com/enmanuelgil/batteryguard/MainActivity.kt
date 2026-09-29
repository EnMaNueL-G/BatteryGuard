package com.enmanuelgil.batteryguard

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    override fun onCreate() {
        super.onCreate()
        BatteryMonitorService.createChannels(this)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    // Android 13+: sin este permiso no se ve ni el monitor ni los avisos (la 1.0.0 nunca lo pedía)
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { BatteryMonitorService.start(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        else BatteryMonitorService.start(this)
        setContent { BatteryGuardTheme { BatteryGuardRoot(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}

@Composable
fun BatteryGuardRoot(vm: MainViewModel) {
    val battery by vm.battery.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loadingApps by vm.loadingApps.collectAsStateWithLifecycle()
    val hasUsagePerm by vm.hasUsagePerm.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val isTablet = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.consumeMessage() } }

    val tabs = listOf(NavTab("Batería", Icons.Default.Battery5Bar), NavTab("Uso", Icons.Default.Apps), NavTab("Ajustes", Icons.Default.Settings))
    val content: @Composable (PaddingValues) -> Unit = { pv ->
        Box(Modifier.fillMaxSize().background(BackgroundDark).padding(pv)) {
            when (currentTab) {
                0 -> DashboardScreen(battery, health, settings, onGoSettings = { currentTab = 2 })
                1 -> AppsScreen(apps, loadingApps, hasUsagePerm, vm::loadApps)
                2 -> SettingsScreen(settings, scan, health, vm)
            }
        }
    }

    if (isTablet) {
        Row(Modifier.fillMaxSize().background(BackgroundDark)) {
            NavigationRail(containerColor = SurfaceDark) {
                Spacer(Modifier.height(16.dp))
                tabs.forEachIndexed { i, tab ->
                    NavigationRailItem(
                        selected = currentTab == i, onClick = { currentTab = i },
                        icon = { Icon(tab.icon, tab.label, tint = if (currentTab == i) BatteryGreen else TextSecondary) },
                        label = { Text(tab.label, color = if (currentTab == i) BatteryGreen else TextSecondary, fontWeight = if (currentTab == i) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = NavigationRailItemDefaults.colors(indicatorColor = BatteryGreen.copy(alpha = 0.15f)))
                    Spacer(Modifier.height(4.dp))
                }
            }
            Scaffold(containerColor = BackgroundDark, snackbarHost = { SnackbarHost(snack) }) { pv -> content(pv) }
        }
    } else {
        Scaffold(
            containerColor = BackgroundDark,
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = currentTab == i, onClick = { currentTab = i },
                            icon = { Icon(tab.icon, tab.label, tint = if (currentTab == i) BatteryGreen else TextSecondary) },
                            label = { Text(tab.label, color = if (currentTab == i) BatteryGreen else TextSecondary) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = BatteryGreen.copy(alpha = 0.15f)))
                    }
                }
            }
        ) { pv -> content(pv) }
    }
}

data class NavTab(val label: String, val icon: ImageVector)
