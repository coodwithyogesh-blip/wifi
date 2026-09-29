package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.file.PasswordFileRepository
import com.example.data.local.AppDatabase
import com.example.data.local.AuthorizedVaultEntity
import com.example.data.wifi.WifiRepository
import com.example.model.BatchData
import com.example.model.ConnectionStatus
import com.example.model.CredentialSource
import com.example.model.ImportProgress
import com.example.model.ImportStatus
import com.example.model.PasswordEntry
import com.example.model.StoredListMetadata
import com.example.model.SuccessfulConnectionResult
import com.example.model.WifiNetwork
import com.example.service.FileIndexingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CONNECT,
    VAULT,
    SETTINGS
}

data class WifiVaultUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedNetwork: WifiNetwork? = null,
    val selectedTab: Int = 0, // 0 = Option A, 1 = Option B
    val manualPassword: String = "",
    val isManualPasswordVisible: Boolean = false,
    val saveToVaultChecked: Boolean = true,
    val selectedEntry: PasswordEntry? = null,
    val timeoutSeconds: Int = 15,
    val isResultPasswordVisible: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val showJumpBatchDialog: Boolean = false,
    val jumpBatchInput: String = ""
)

class WifiVaultViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WifiVaultVM"
    }

    val fileRepository = PasswordFileRepository(application)
    val wifiRepository = WifiRepository(application, viewModelScope)
    private val vaultDao = AppDatabase.getInstance(application).vaultDao()

    private val _uiState = MutableStateFlow(WifiVaultUiState())
    val uiState: StateFlow<WifiVaultUiState> = _uiState.asStateFlow()

    // Expose flows from repositories
    val scannedNetworks: StateFlow<List<WifiNetwork>> = wifiRepository.scannedNetworks
    val isScanning: StateFlow<Boolean> = wifiRepository.isScanning
    val connectionStatus: StateFlow<ConnectionStatus> = wifiRepository.connectionStatus
    val currentlyConnected: StateFlow<WifiNetwork?> = wifiRepository.currentlyConnectedNetwork

    val storedMetadata: StateFlow<StoredListMetadata?> = fileRepository.storedMetadata
    val importProgress: StateFlow<ImportProgress> = fileRepository.importProgress
    val currentBatchData: StateFlow<BatchData?> = fileRepository.currentBatchData
    val isLoadingBatch: StateFlow<Boolean> = fileRepository.isLoadingBatch

    val vaultEntries: StateFlow<List<AuthorizedVaultEntity>> = vaultDao.getAllVaultEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<List<com.example.model.DiscoveredDevice>> = wifiRepository.discoveredDevices
    val isRefreshingDevices: StateFlow<Boolean> = wifiRepository.isRefreshingDevices

    private var indexingJob: Job? = null

    init {
        scanWifi()
        // If metadata already exists, load batch 1
        if (storedMetadata.value != null && currentBatchData.value == null) {
            loadBatch(1L)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun selectNetwork(network: WifiNetwork) {
        _uiState.value = _uiState.value.copy(
            selectedNetwork = network,
            currentScreen = AppScreen.CONNECT
        )
    }

    fun scanWifi() {
        wifiRepository.startScan()
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setManualPassword(password: String) {
        _uiState.value = _uiState.value.copy(manualPassword = password)
    }

    fun toggleManualPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isManualPasswordVisible = !_uiState.value.isManualPasswordVisible
        )
    }

    fun toggleResultPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isResultPasswordVisible = !_uiState.value.isResultPasswordVisible
        )
    }

    fun toggleSaveToVault(save: Boolean) {
        _uiState.value = _uiState.value.copy(saveToVaultChecked = save)
    }

    fun selectPasswordEntry(entry: PasswordEntry) {
        _uiState.value = _uiState.value.copy(selectedEntry = entry)
    }

    fun setTimeoutSeconds(seconds: Int) {
        _uiState.value = _uiState.value.copy(timeoutSeconds = seconds)
    }

    fun showClearConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = show)
    }

    fun showJumpBatchDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showJumpBatchDialog = show,
            jumpBatchInput = currentBatchData.value?.batchNumber?.toString() ?: "1"
        )
    }

    fun setJumpBatchInput(input: String) {
        _uiState.value = _uiState.value.copy(jumpBatchInput = input)
    }

    fun confirmJumpBatch() {
        val target = _uiState.value.jumpBatchInput.toLongOrNull()
        val total = storedMetadata.value?.totalBatches ?: 1L
        if (target != null && target in 1L..total) {
            loadBatch(target)
            _uiState.value = _uiState.value.copy(showJumpBatchDialog = false)
        }
    }

    /**
     * START TEST BUTTON — SAFE AUTHORIZED WORKFLOW
     * 1. Opens the imported 500-entry batch.
     * 2. If no file is imported yet, loads the 2,000-line test batch (with Aarti7756 at Line 11).
     * 3. Sets selected tab to Option B (Import List) so all 500 entries appear in the LazyColumn.
     */
    fun startTestWorkflow() {
        _uiState.value = _uiState.value.copy(
            selectedTab = 1, // Option B — Import List
            currentScreen = AppScreen.CONNECT
        )
        if (storedMetadata.value != null) {
            // Already imported, ensure batch 1 is loaded
            loadBatch(1L)
        } else {
            // Generate standard 2,000-line test file with Aarti7756 at Line 11
            generateAndIndexTestFile(2000L, 11L, "Aarti7756")
        }
    }

    fun refreshDevices() {
        val status = connectionStatus.value
        val (ip, gateway) = if (status is ConnectionStatus.Connected) {
            status.result.ipAddress to status.result.gateway
        } else {
            "192.168.0.108" to "192.168.0.1"
        }
        viewModelScope.launch {
            wifiRepository.refreshLocalDevices(ip, gateway)
        }
    }

    /**
     * Start authorized Wi-Fi connection based on selected tab and options.
     */
    fun startConnection() {
        val network = _uiState.value.selectedNetwork ?: return
        val isManual = _uiState.value.selectedTab == 0

        if (isManual) {
            val pass = _uiState.value.manualPassword
            Log.d(TAG, "Starting manual authorized connection attempt")
            wifiRepository.connectAuthorized(
                ssid = network.ssid,
                credential = pass,
                source = CredentialSource.MANUAL,
                timeoutSeconds = _uiState.value.timeoutSeconds
            )
        } else {
            val entry = _uiState.value.selectedEntry
            if (entry == null) {
                Log.w(TAG, "No credential selected from batch")
                return
            }
            val meta = storedMetadata.value
            Log.d(TAG, "Starting imported credential connection attempt (Line ${entry.globalLineNumber})")
            wifiRepository.connectAuthorized(
                ssid = network.ssid,
                credential = entry.password,
                source = CredentialSource.IMPORTED_TXT,
                globalLineNumber = entry.globalLineNumber,
                batchNumber = entry.batchNumber,
                positionInBatch = entry.positionInBatch,
                totalBatches = meta?.totalBatches,
                totalEntries = meta?.totalEntries,
                timeoutSeconds = _uiState.value.timeoutSeconds
            )
        }
    }

    fun stopConnection() {
        wifiRepository.stopActiveConnection("Cancelled by user")
    }

    fun resetConnection() {
        wifiRepository.resetConnectionState()
    }

    /**
     * Import a TXT file from SAF Uri.
     */
    fun importFile(uri: Uri, customName: String? = null) {
        indexingJob?.cancel()
        val app = getApplication<Application>()
        FileIndexingService.startService(app, customName ?: "password.txt")

        indexingJob = viewModelScope.launch {
            val result = fileRepository.indexFile(uri, customName) { progress ->
                FileIndexingService.updateProgress(
                    app,
                    progress.processedCount,
                    progress.totalEstimate,
                    progress.percent
                )
            }
            FileIndexingService.stopService(app)
            if (result.isSuccess) {
                // Clear any previously selected entry
                _uiState.value = _uiState.value.copy(selectedEntry = null)
            }
        }
    }

    /**
     * Helper to generate a test file (e.g. 500, 501, 2000 lines with the prompt's test case)
     * and immediately index it.
     */
    fun generateAndIndexTestFile(
        lines: Long,
        customTargetLine: Long? = null,
        customTargetPassword: String? = null
    ) {
        viewModelScope.launch {
            val uri = fileRepository.createTestFile(lines, customTargetLine, customTargetPassword)
            val name = "test_${lines}_entries.txt"
            importFile(uri, name)
        }
    }

    fun cancelImport() {
        indexingJob?.cancel()
        indexingJob = null
        FileIndexingService.stopService(getApplication())
    }

    fun loadBatch(batchNumber: Long) {
        viewModelScope.launch {
            fileRepository.loadBatch(batchNumber)
        }
    }

    fun nextBatch() {
        val current = currentBatchData.value ?: return
        if (current.batchNumber < current.totalBatches) {
            loadBatch(current.batchNumber + 1L)
        }
    }

    fun prevBatch() {
        val current = currentBatchData.value ?: return
        if (current.batchNumber > 1L) {
            loadBatch(current.batchNumber - 1L)
        }
    }

    fun clearStoredList() {
        fileRepository.clearStoredList()
        _uiState.value = _uiState.value.copy(
            selectedEntry = null,
            showClearConfirmDialog = false
        )
    }

    fun saveResultToVault(result: SuccessfulConnectionResult) {
        viewModelScope.launch {
            vaultDao.insertEntry(
                AuthorizedVaultEntity(
                    ssid = result.ssid,
                    credential = result.credential,
                    securityType = "WPA2/WPA3",
                    notes = "Source: ${result.source}" +
                            if (result.globalLineNumber != null) " (Line ${result.globalLineNumber})" else ""
                )
            )
        }
    }

    fun deleteVaultEntry(id: Long) {
        viewModelScope.launch {
            vaultDao.deleteById(id)
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            vaultDao.clearVault()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wifiRepository.unregisterScanReceiver()
        wifiRepository.stopActiveConnection("ViewModel cleared")
        FileIndexingService.stopService(getApplication())
    }
}
