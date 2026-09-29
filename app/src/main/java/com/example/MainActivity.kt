package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WifiNetwork
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.WifiVaultViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WifiVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: WifiVaultViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scannedNetworks by viewModel.scannedNetworks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val currentlyConnected by viewModel.currentlyConnected.collectAsStateWithLifecycle()

    val storedMetadata by viewModel.storedMetadata.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val currentBatchData by viewModel.currentBatchData.collectAsStateWithLifecycle()
    val isLoadingBatch by viewModel.isLoadingBatch.collectAsStateWithLifecycle()
    val vaultEntries by viewModel.vaultEntries.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isRefreshingDevices by viewModel.isRefreshingDevices.collectAsStateWithLifecycle()

    // Handle back button on sub-screens
    if (uiState.currentScreen != AppScreen.HOME) {
        BackHandler {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp(4f)
            ) {
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.HOME,
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    icon = { Icon(Icons.Default.WifiFind, contentDescription = "Scanner") },
                    label = { Text("Networks") },
                    modifier = Modifier.testTag("nav_item_networks")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.CONNECT,
                    onClick = {
                        if (uiState.selectedNetwork != null) {
                            viewModel.navigateTo(AppScreen.CONNECT)
                        } else if (scannedNetworks.isNotEmpty()) {
                            viewModel.selectNetwork(scannedNetworks.first())
                        }
                    },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = "Connect") },
                    label = { Text("Connect") },
                    modifier = Modifier.testTag("nav_item_connect")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.VAULT,
                    onClick = { viewModel.navigateTo(AppScreen.VAULT) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    modifier = Modifier.testTag("nav_item_vault")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        scannedNetworks = scannedNetworks,
                        isScanning = isScanning,
                        currentlyConnected = currentlyConnected,
                        onScanClick = { viewModel.scanWifi() },
                        onNetworkSelected = { network -> viewModel.selectNetwork(network) }
                    )
                }

                AppScreen.CONNECT -> {
                    val network = uiState.selectedNetwork
                        ?: scannedNetworks.firstOrNull()
                        ?: WifiNetwork(ssid = "sai raj")

                    ConnectScreen(
                        network = network,
                        uiState = uiState,
                        connectionStatus = connectionStatus,
                        storedMetadata = storedMetadata,
                        importProgress = importProgress,
                        currentBatchData = currentBatchData,
                        isLoadingBatch = isLoadingBatch,
                        discoveredDevices = discoveredDevices,
                        isRefreshingDevices = isRefreshingDevices,
                        onBackClick = { viewModel.navigateTo(AppScreen.HOME) },
                        onStartTestWorkflow = { viewModel.startTestWorkflow() },
                        onSelectTab = { tab -> viewModel.setSelectedTab(tab) },
                        onManualPasswordChange = { pass -> viewModel.setManualPassword(pass) },
                        onToggleManualPasswordVisibility = { viewModel.toggleManualPasswordVisibility() },
                        onToggleResultPasswordVisibility = { viewModel.toggleResultPasswordVisibility() },
                        onToggleSaveToVault = { save -> viewModel.toggleSaveToVault(save) },
                        onSelectPasswordEntry = { entry -> viewModel.selectPasswordEntry(entry) },
                        onStartConnection = { viewModel.startConnection() },
                        onStopConnection = { viewModel.stopConnection() },
                        onResetConnection = { viewModel.resetConnection() },
                        onRefreshDevices = { viewModel.refreshDevices() },
                        onImportFile = { uri -> viewModel.importFile(uri) },
                        onGenerateTestFile = { lines, customLine, customPass ->
                            viewModel.generateAndIndexTestFile(lines, customLine, customPass)
                        },
                        onCancelImport = { viewModel.cancelImport() },
                        onNextBatch = { viewModel.nextBatch() },
                        onPrevBatch = { viewModel.prevBatch() },
                        onShowJumpDialog = { show -> viewModel.showJumpBatchDialog(show) },
                        onJumpBatchInputChange = { input -> viewModel.setJumpBatchInput(input) },
                        onConfirmJumpBatch = { viewModel.confirmJumpBatch() },
                        onSaveResultToVault = {
                            val status = connectionStatus
                            if (status is com.example.model.ConnectionStatus.Connected) {
                                viewModel.saveResultToVault(status.result)
                            }
                        }
                    )
                }

                AppScreen.VAULT -> {
                    VaultScreen(
                        vaultEntries = vaultEntries,
                        onDeleteEntry = { id -> viewModel.deleteVaultEntry(id) },
                        onSelectForConnect = { network, credential ->
                            viewModel.selectNetwork(network)
                            viewModel.setManualPassword(credential)
                            viewModel.setSelectedTab(0)
                        }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        storedMetadata = storedMetadata,
                        timeoutSeconds = uiState.timeoutSeconds,
                        showClearConfirmDialog = uiState.showClearConfirmDialog,
                        onTimeoutChange = { seconds -> viewModel.setTimeoutSeconds(seconds) },
                        onShowClearConfirmDialog = { show -> viewModel.showClearConfirmDialog(show) },
                        onConfirmClearStorage = { viewModel.clearStoredList() }
                    )
                }
            }
        }
    }
}
