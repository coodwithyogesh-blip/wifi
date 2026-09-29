package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatchData
import com.example.model.ConnectionStatus
import com.example.model.DiscoveredDevice
import com.example.model.ImportProgress
import com.example.model.ImportStatus
import com.example.model.PasswordEntry
import com.example.model.StoredListMetadata
import com.example.model.WifiNetwork
import com.example.ui.components.InfoBadge
import com.example.ui.components.LocalDevicesCard
import com.example.ui.components.SuccessResultCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.viewmodel.WifiVaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    network: WifiNetwork,
    uiState: WifiVaultUiState,
    connectionStatus: ConnectionStatus,
    storedMetadata: StoredListMetadata?,
    importProgress: ImportProgress,
    currentBatchData: BatchData?,
    isLoadingBatch: Boolean,
    discoveredDevices: List<DiscoveredDevice> = emptyList(),
    isRefreshingDevices: Boolean = false,
    onBackClick: () -> Unit,
    onStartTestWorkflow: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onManualPasswordChange: (String) -> Unit,
    onToggleManualPasswordVisibility: () -> Unit,
    onToggleResultPasswordVisibility: () -> Unit,
    onToggleSaveToVault: (Boolean) -> Unit,
    onSelectPasswordEntry: (PasswordEntry) -> Unit,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    onResetConnection: () -> Unit,
    onRefreshDevices: () -> Unit,
    onImportFile: (Uri) -> Unit,
    onGenerateTestFile: (Long, Long?, String?) -> Unit,
    onCancelImport: () -> Unit,
    onNextBatch: () -> Unit,
    onPrevBatch: () -> Unit,
    onShowJumpDialog: (Boolean) -> Unit,
    onJumpBatchInputChange: (String) -> Unit,
    onConfirmJumpBatch: () -> Unit,
    onSaveResultToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFile(uri)
        }
    }

    var showTestPresetsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = network.displaySsid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${network.securityType.label} • ${network.band}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Details Status Header Card
            item {
                NetworkStatusHeaderCard(
                    network = network,
                    connectionStatus = connectionStatus
                )
            }

            // Primary "Start Test" Action Banner
            item {
                Button(
                    onClick = onStartTestWorkflow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_test_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Test — Safe Authorized Workflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // If connection succeeded, show the Successful Connection Result Card FIRST!
            if (connectionStatus is ConnectionStatus.Connected) {
                item {
                    SuccessResultCard(
                        result = connectionStatus.result,
                        isPasswordVisible = uiState.isResultPasswordVisible,
                        onTogglePasswordVisibility = onToggleResultPasswordVisibility,
                        onSaveToVault = onSaveResultToVault,
                        onResetConnection = onResetConnection
                    )
                }

                // Legitimate Local Subnet Devices Discovery
                item {
                    LocalDevicesCard(
                        devices = discoveredDevices,
                        isRefreshing = isRefreshingDevices,
                        onRefreshClick = onRefreshDevices
                    )
                }
            }

            // If connection is in progress, show active connecting banner
            if (connectionStatus is ConnectionStatus.Connecting) {
                item {
                    ConnectingCard(
                        status = connectionStatus,
                        onStopClick = onStopConnection
                    )
                }
            }

            // If connection failed, show failure banner
            if (connectionStatus is ConnectionStatus.Failed) {
                item {
                    ConnectionFailedCard(
                        reason = connectionStatus.reason,
                        onRetryClick = onStartConnection,
                        onDismissClick = onResetConnection
                    )
                }
            }

            // Android System Wi-Fi Approval Guidance Notice
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Official Wi-Fi API: If prompted by Android, tap 'Connect' in the system approval dialog.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Selection: Option A — Enter Password / Option B — Import List
            item {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        text = {
                            Text(
                                "Option A — Enter Password",
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        text = {
                            Text(
                                "Option B — Import List",
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            if (uiState.selectedTab == 0) {
                // OPTION A: MANUAL ENTER
                item {
                    ManualPasswordCard(
                        password = uiState.manualPassword,
                        isPasswordVisible = uiState.isManualPasswordVisible,
                        saveToVaultChecked = uiState.saveToVaultChecked,
                        isConnecting = connectionStatus is ConnectionStatus.Connecting,
                        onPasswordChange = onManualPasswordChange,
                        onToggleVisibility = onToggleManualPasswordVisibility,
                        onToggleSaveToVault = onToggleSaveToVault,
                        onConnectClick = onStartConnection
                    )
                }
            } else {
                // OPTION B: IMPORT LIST
                // 1. If currently indexing, show progress
                if (importProgress.status == ImportStatus.INDEXING) {
                    item {
                        IndexingProgressCard(
                            progress = importProgress,
                            onCancelClick = onCancelImport
                        )
                    }
                }

                // 2. If no file imported or list is empty
                if (storedMetadata == null && importProgress.status != ImportStatus.INDEXING) {
                    item {
                        EmptyImportCard(
                            onStartTestClick = onStartTestWorkflow,
                            onImportClick = {
                                filePickerLauncher.launch(arrayOf("text/plain"))
                            },
                            onQuickTestClick = {
                                showTestPresetsDialog = true
                            }
                        )
                    }
                }

                // 3. If file is stored but unavailable (e.g. deleted or permission revoked)
                if (storedMetadata != null && !storedMetadata.isAvailable) {
                    item {
                        UnavailableFileCard(
                            onReimportClick = {
                                filePickerLauncher.launch(arrayOf("text/plain"))
                            }
                        )
                    }
                }

                // 4. File metadata header if imported and available
                if (storedMetadata != null && storedMetadata.isAvailable) {
                    item {
                        ImportedMetadataHeader(
                            metadata = storedMetadata,
                            onImportAnotherClick = {
                                filePickerLauncher.launch(arrayOf("text/plain"))
                            },
                            onQuickTestClick = {
                                showTestPresetsDialog = true
                            }
                        )
                    }

                    // 5. Batch Navigation Controls
                    item {
                        BatchNavigationControls(
                            batchData = currentBatchData,
                            isLoading = isLoadingBatch,
                            onPrevClick = onPrevBatch,
                            onNextClick = onNextBatch,
                            onJumpClick = { onShowJumpDialog(true) }
                        )
                    }

                    // 6. Selected credential banner & Connect button
                    if (uiState.selectedEntry != null) {
                        item {
                            SelectedCredentialCard(
                                entry = uiState.selectedEntry,
                                totalBatches = storedMetadata.totalBatches,
                                totalEntries = storedMetadata.totalEntries,
                                isConnecting = connectionStatus is ConnectionStatus.Connecting,
                                onConnectClick = onStartConnection
                            )
                        }
                    }

                    // 7. Batch entries list (LazyColumn items)
                    if (isLoadingBatch) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else if (currentBatchData != null) {
                        items(currentBatchData.entries, key = { it.globalLineNumber }) { entry ->
                            PasswordEntryRow(
                                entry = entry,
                                isSelected = uiState.selectedEntry?.globalLineNumber == entry.globalLineNumber,
                                onSelect = { onSelectPasswordEntry(entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Jump to Batch Dialog
    if (uiState.showJumpBatchDialog) {
        val maxBatch = storedMetadata?.totalBatches ?: 1L
        AlertDialog(
            onDismissRequest = { onShowJumpDialog(false) },
            title = { Text("Jump to Batch") },
            text = {
                Column {
                    Text(
                        text = "Enter batch number (1 to %,d):".format(maxBatch),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.jumpBatchInput,
                        onValueChange = onJumpBatchInputChange,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = onConfirmJumpBatch) {
                    Text("Jump")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowJumpDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Test Presets Dialog
    if (showTestPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showTestPresetsDialog = false },
            title = { Text("Select Test Dataset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Instantly generate and index bounded test files to verify batching and line numbering:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            showTestPresetsDialog = false
                            // 2000 lines with Aarti7756 at Line 11 & Line 3
                            onGenerateTestFile(2000L, 11L, "Aarti7756")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("2,000 Lines (Test Case Line 11: Aarti7756)")
                    }

                    OutlinedButton(
                        onClick = {
                            showTestPresetsDialog = false
                            onGenerateTestFile(500L, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("500 Lines (Exact Single Batch 1/1)")
                    }

                    OutlinedButton(
                        onClick = {
                            showTestPresetsDialog = false
                            onGenerateTestFile(501L, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("501 Lines (2 Batches: 500 + 1)")
                    }

                    OutlinedButton(
                        onClick = {
                            showTestPresetsDialog = false
                            onGenerateTestFile(10000L, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("10,000 Lines (20 Batches)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTestPresetsDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun NetworkStatusHeaderCard(
    network: WifiNetwork,
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = network.displaySsid,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Security: ${network.securityType.label}  •  ${network.band}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val statusText = when (connectionStatus) {
                    is ConnectionStatus.Connected -> "Connected"
                    is ConnectionStatus.Connecting -> "Connecting..."
                    is ConnectionStatus.Failed -> "Failed"
                    else -> "Not Connected"
                }

                val statusColor = when (connectionStatus) {
                    is ConnectionStatus.Connected -> EmeraldSuccess
                    is ConnectionStatus.Connecting -> MaterialTheme.colorScheme.primary
                    is ConnectionStatus.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Status: $statusText",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun ManualPasswordCard(
    password: String,
    isPasswordVisible: Boolean,
    saveToVaultChecked: Boolean,
    isConnecting: Boolean,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleSaveToVault: (Boolean) -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Enter Password Manually",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Wi-Fi Password") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.testTag("toggle_manual_password_visibility")
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSaveToVault(!saveToVaultChecked) }
            ) {
                Checkbox(
                    checked = saveToVaultChecked,
                    onCheckedChange = onToggleSaveToVault,
                    modifier = Modifier.testTag("save_to_vault_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Save to Authorized Credentials Vault",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onConnectClick,
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manual_connect_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isConnecting) "Connecting..." else "Connect",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun IndexingProgressCard(
    progress: ImportProgress,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Building Index...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "%.1f%%".format(progress.percent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (progress.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Processed: %,d / %,d".format(progress.processedCount, progress.totalEstimate),
                    style = MaterialTheme.typography.bodyMedium
                )
                val remaining = (progress.totalEstimate - progress.processedCount).coerceAtLeast(0L)
                Text(
                    text = "Remaining: %,d".format(remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (progress.speedEntriesPerSec > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speed: %,d entries/sec".format(progress.speedEntriesPerSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ETA: %d min %02d sec".format(progress.etaSeconds / 60, progress.etaSeconds % 60),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun EmptyImportCard(
    onStartTestClick: () -> Unit,
    onImportClick: () -> Unit,
    onQuickTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Import Password List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select a TXT file (one credential per line, up to 10,000,000 lines). Memory-efficient streaming guarantees zero OOM.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartTestClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_card_start_test_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Test (Load 2000-Line Batch)")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("import_file_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select TXT File from Storage")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onQuickTestClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("quick_test_presets_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Dataset, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Test Dataset Preset")
            }
        }
    }
}

@Composable
fun UnavailableFileCard(
    onReimportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "File No Longer Available",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Password list is no longer available. Please import it again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onReimportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Re-import Password List")
            }
        }
    }
}

@Composable
fun ImportedMetadataHeader(
    metadata: StoredListMetadata,
    onImportAnotherClick: () -> Unit,
    onQuickTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import Complete ✓",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                Row {
                    IconButton(
                        onClick = onImportAnotherClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import another file",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onQuickTestClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dataset,
                            contentDescription = "Test Presets",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "File: ${metadata.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Total Entries: %,d".format(metadata.totalEntries),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Batches: %,d".format(metadata.totalBatches),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Batch Size: ${metadata.batchSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BatchNavigationControls(
    batchData: BatchData?,
    isLoading: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onJumpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (batchData == null) return

    val isFirstBatch = batchData.batchNumber <= 1L
    val isLastBatch = batchData.batchNumber >= batchData.totalBatches

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Password List",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Entries: %,d".format(batchData.totalEntries),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onJumpClick() }
                ) {
                    Text(
                        text = "Batch %,d / %,d".format(batchData.batchNumber, batchData.totalBatches),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Showing: %,d–%,d".format(batchData.startLine, batchData.endLine),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevClick,
                    enabled = !isFirstBatch && !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_batch_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous")
                }

                OutlinedButton(
                    onClick = onJumpClick,
                    modifier = Modifier.testTag("jump_batch_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Jump")
                }

                OutlinedButton(
                    onClick = onNextClick,
                    enabled = !isLastBatch && !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("next_batch_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

/**
 * Section 4 & 5 Exact Card Format:
 *
 * Selected Credential
 * Line: 11 / 2000
 * Batch: 1 / 4
 * Position: 11 / 500
 *
 * [Connect with Selected Credential]
 */
@Composable
fun SelectedCredentialCard(
    entry: PasswordEntry,
    totalBatches: Long,
    totalEntries: Long,
    isConnecting: Boolean,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_credential_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Selected Credential",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Line: %,d / %,d".format(entry.globalLineNumber, totalEntries),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Batch: %,d / %,d".format(entry.batchNumber, totalBatches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Position: %,d / 500".format(entry.positionInBatch),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConnectClick,
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("connect_selected_credential_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isConnecting) "Connecting..." else "Connect with Selected Credential",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PasswordEntryRow(
    entry: PasswordEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("password_entry_${entry.globalLineNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Line %,d:".format(entry.globalLineNumber),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.password,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Pos ${entry.positionInBatch}/500",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Section 7 Exact Connecting Format:
 *
 * Connecting...
 * Wi-Fi: <SSID>
 * Credential: ••••••••
 * Line: 11 / 2000
 */
@Composable
fun ConnectingCard(
    status: ConnectionStatus.Connecting,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connecting_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Wi-Fi: ${status.ssid}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Credential: ${status.maskedCredential}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (status.lineDisplay != null) {
                Text(
                    text = status.lineDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stop_connecting_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Connection Attempt")
            }
        }
    }
}

/**
 * Section 9 Exact Failure Handling:
 *
 * Connection Failed
 * Keep the credential selected and allow user to choose another credential.
 * Never automatically advance to the next credential.
 */
@Composable
fun ConnectionFailedCard(
    reason: String,
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_failed_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connection Failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The credential remains selected. You can retry or choose another credential from the list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRetryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Retry Connection with Selected Credential")
            }
        }
    }
}
