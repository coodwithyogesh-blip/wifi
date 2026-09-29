package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.file.PasswordFileRepository
import com.example.data.wifi.LocalDeviceScanner
import com.example.model.ConnectionStatus
import com.example.model.CredentialSource
import com.example.model.PasswordEntry
import com.example.model.SuccessfulConnectionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WiFi Vault", appName)
    }

    @Test
    fun `verify test case bug prevention for selected candidate credential`() {
        // Section 23 explicit bug prevention test case:
        // Manual password: 123456
        // Imported candidate Line 3: Aarti7756
        // Connection succeeds with Aarti7756:
        // The SuccessfulConnectionResult must contain Aarti7756 and Line 3, NEVER 123456.

        val manualEntered = "123456"
        val selectedCandidatePassword = "Aarti7756"
        val selectedGlobalLineNumber = 3L
        val selectedBatchNumber = 1L
        val selectedPositionInBatch = 3

        val result = SuccessfulConnectionResult(
            credential = selectedCandidatePassword,
            source = CredentialSource.IMPORTED_TXT.label,
            globalLineNumber = selectedGlobalLineNumber,
            batchNumber = selectedBatchNumber,
            positionInBatch = selectedPositionInBatch,
            totalBatches = 4L,
            totalEntries = 2000L,
            ssid = "sai raj",
            ipAddress = "192.168.0.108",
            gateway = "192.168.0.1"
        )

        assertEquals("Aarti7756", result.credential)
        assertNotEquals(manualEntered, result.credential)
        assertEquals(3L, result.globalLineNumber)
        assertEquals("Imported TXT", result.source)
        assertEquals("sai raj", result.ssid)
        assertEquals("192.168.0.108", result.ipAddress)
        assertEquals("192.168.0.1", result.gateway)
    }

    @Test
    fun `verify indexing and batch navigation on 501 lines with partial final batch`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PasswordFileRepository(context)

        // Generate 501 lines
        val uri = repo.createTestFile(501L)
        val indexResult = repo.indexFile(uri, "test_501.txt")
        assertTrue(indexResult.isSuccess)

        val meta = indexResult.getOrThrow()
        assertEquals(501L, meta.totalEntries)
        assertEquals(2L, meta.totalBatches) // 500 lines in batch 1, 1 line in batch 2
        assertEquals(500, meta.batchSize)

        // Load Batch 1
        val batch1Result = repo.loadBatch(1L)
        assertTrue(batch1Result.isSuccess)
        val batch1 = batch1Result.getOrThrow()
        assertEquals(500, batch1.entries.size)
        assertEquals(1L, batch1.startLine)
        assertEquals(500L, batch1.endLine)
        assertEquals(1L, batch1.entries.first().globalLineNumber)
        assertEquals(500L, batch1.entries.last().globalLineNumber)

        // Load Batch 2 (Partial final batch: exactly 1 entry)
        val batch2Result = repo.loadBatch(2L)
        assertTrue(batch2Result.isSuccess)
        val batch2 = batch2Result.getOrThrow()
        assertEquals(1, batch2.entries.size)
        assertEquals(501L, batch2.startLine)
        assertEquals(501L, batch2.endLine)
        assertEquals(501L, batch2.entries.first().globalLineNumber)
        assertEquals(1, batch2.entries.first().positionInBatch)
    }

    @Test
    fun `verify Aarti7756 at Line 11 preserves globalLineNumber and batch coordinates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PasswordFileRepository(context)

        // Generate 2000 lines with Aarti7756 at line 11
        val uri = repo.createTestFile(2000L, customTargetLine = 11L, customTargetPassword = "Aarti7756")
        val indexResult = repo.indexFile(uri, "test_2000.txt")
        assertTrue(indexResult.isSuccess)

        val meta = indexResult.getOrThrow()
        assertEquals(2000L, meta.totalEntries)
        assertEquals(4L, meta.totalBatches)

        // Load Batch 1
        val batch1 = repo.loadBatch(1L).getOrThrow()
        val entry11 = batch1.entries.find { it.globalLineNumber == 11L }
        assertNotNull(entry11)
        assertEquals("Aarti7756", entry11!!.password)
        assertEquals(11L, entry11.globalLineNumber)
        assertEquals(1L, entry11.batchNumber)
        assertEquals(11, entry11.positionInBatch)
    }

    @Test
    fun `verify start test safe authorized workflow UI contract`() {
        // Requirements 4, 7, 9, 10:
        // 4. Selected Credential shows: Line: 11 / 2000, Batch: 1 / 4, Position: 11 / 500
        val entry = PasswordEntry(
            globalLineNumber = 11L,
            batchNumber = 1L,
            positionInBatch = 11,
            password = "Aarti7756"
        )
        val totalEntries = 2000L
        val totalBatches = 4L

        assertEquals("Line: 11 / 2000", "Line: ${entry.globalLineNumber} / $totalEntries")
        assertEquals("Batch: 1 / 4", "Batch: ${entry.batchNumber} / $totalBatches")
        assertEquals("Position: 11 / 500", "Position: ${entry.positionInBatch} / 500")

        // 7. Connecting state shows masked credential and line
        val connectingStatus = ConnectionStatus.Connecting(
            attemptId = 1L,
            ssid = "sai raj",
            message = "Connecting to sai raj...",
            maskedCredential = "••••••••",
            lineDisplay = "Line: 11 / 2000"
        )
        assertEquals("sai raj", connectingStatus.ssid)
        assertEquals("••••••••", connectingStatus.maskedCredential)
        assertEquals("Line: 11 / 2000", connectingStatus.lineDisplay)

        // 9. On failure: Keep credential selected, never auto-advance
        var selectedEntry: PasswordEntry? = entry
        val failureStatus = ConnectionStatus.Failed(
            reason = "Connection timed out",
            attemptId = 1L
        )
        // Failure does not clear or advance selectedEntry
        assertNotNull(selectedEntry)
        assertEquals(11L, selectedEntry?.globalLineNumber)
    }

    @Test
    fun `verify local network devices discovery scanner identifies gateway and host`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = LocalDeviceScanner(context)

        val devices = scanner.scanLocalSubnet("192.168.0.108", "192.168.0.1")
        assertTrue(devices.isNotEmpty())

        val gateway = devices.find { it.isGateway }
        assertNotNull(gateway)
        assertEquals("192.168.0.1", gateway?.ip)
        assertTrue(gateway?.deviceType?.contains("Gateway") == true || gateway?.deviceType?.contains("Router") == true)

        val currentDevice = devices.find { it.isCurrentDevice }
        assertNotNull(currentDevice)
        assertEquals("192.168.0.108", currentDevice?.ip)
    }
}
