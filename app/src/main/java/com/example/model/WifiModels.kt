package com.example.model

/**
 * Supported Wi-Fi security protocols.
 */
enum class WifiSecurityType(val label: String) {
    OPEN("Open (No Security)"),
    WEP("WEP"),
    WPA_PSK("WPA-PSK"),
    WPA2_PSK("WPA2"),
    WPA3_SAE("WPA3-SAE"),
    EAP("WPA-Enterprise"),
    UNKNOWN("Unknown")
}

/**
 * Scanned or detected Wi-Fi network.
 */
data class WifiNetwork(
    val ssid: String,
    val bssid: String = "",
    val securityType: WifiSecurityType = WifiSecurityType.WPA2_PSK,
    val frequencyMhz: Int = 2412,
    val band: String = "2.4 GHz",
    val level: Int = 3, // 0 to 4 signal level
    val rssi: Int = -65, // in dBm
    val isConnected: Boolean = false,
    val capabilities: String = ""
) {
    val displaySsid: String
        get() = if (ssid.isBlank()) "<Hidden Network>" else ssid

    val signalDescription: String
        get() = when (level) {
            4 -> "Excellent"
            3 -> "Strong"
            2 -> "Fair"
            1 -> "Weak"
            else -> "Very Weak"
        }
}

/**
 * Origin of the credential used for connection.
 */
enum class CredentialSource(val label: String) {
    MANUAL("Manually Entered"),
    IMPORTED_TXT("Imported TXT")
}

/**
 * Immutable connection result capturing the exact credential and metadata
 * of a successful authorized connection attempt.
 *
 * Prevents reading passwords from stale Compose UI or previous states.
 */
data class SuccessfulConnectionResult(
    val credential: String,
    val source: String, // "Imported TXT" or "Manually Entered"
    val globalLineNumber: Long?,
    val batchNumber: Long?,
    val positionInBatch: Int?,
    val totalBatches: Long?,
    val totalEntries: Long?,
    val ssid: String,
    val ipAddress: String,
    val gateway: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * State machine for active authorized Wi-Fi connection.
 */
sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    data class Connecting(
        val attemptId: Long,
        val ssid: String,
        val message: String,
        val maskedCredential: String = "••••••••",
        val lineDisplay: String? = null
    ) : ConnectionStatus()
    data class Connected(val result: SuccessfulConnectionResult) : ConnectionStatus()
    data class Failed(val reason: String, val attemptId: Long) : ConnectionStatus()
    object Cancelled : ConnectionStatus()
}

/**
 * A single password entry in a 500-item batch with 1-based original line coordinates.
 */
data class PasswordEntry(
    val globalLineNumber: Long,
    val batchNumber: Long,
    val positionInBatch: Int,
    val password: String
)

/**
 * Current 500-entry batch in memory.
 */
data class BatchData(
    val batchNumber: Long,
    val totalBatches: Long,
    val startLine: Long,
    val endLine: Long,
    val totalEntries: Long,
    val entries: List<PasswordEntry>
)

/**
 * Live status during file import and indexing.
 */
enum class ImportStatus {
    IDLE,
    INDEXING,
    COMPLETED,
    CANCELLED,
    ERROR
}

/**
 * Progress details during large file indexing.
 */
data class ImportProgress(
    val status: ImportStatus = ImportStatus.IDLE,
    val fileName: String = "",
    val processedCount: Long = 0L,
    val totalEstimate: Long = 0L,
    val percent: Float = 0f,
    val speedEntriesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Persistent metadata for an imported password list.
 */
data class StoredListMetadata(
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val totalEntries: Long,
    val totalBatches: Long,
    val batchSize: Int = 500,
    val indexFilePath: String,
    val importTimestamp: Long,
    val isAvailable: Boolean = true
)

/**
 * Legitimate discovered device on the authorized local network.
 */
data class DiscoveredDevice(
    val ip: String,
    val hostname: String? = null,
    val isGateway: Boolean = false,
    val isCurrentDevice: Boolean = false,
    val deviceType: String = "Network Host",
    val isReachable: Boolean = true
)
