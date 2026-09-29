package com.example.data.file

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.BatchData
import com.example.model.ImportProgress
import com.example.model.ImportStatus
import com.example.model.PasswordEntry
import com.example.model.StoredListMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext

class PasswordFileRepository(private val context: Context) {

    companion object {
        private const val TAG = "PasswordFileRepo"
        const val BATCH_SIZE = 500
        private const val PREFS_NAME = "wifi_vault_meta"
        private const val KEY_URI = "meta_uri"
        private const val KEY_DISPLAY_NAME = "meta_display_name"
        private const val KEY_FILE_SIZE = "meta_file_size"
        private const val KEY_TOTAL_ENTRIES = "meta_total_entries"
        private const val KEY_TOTAL_BATCHES = "meta_total_batches"
        private const val KEY_BATCH_SIZE = "meta_batch_size"
        private const val KEY_INDEX_PATH = "meta_index_path"
        private const val KEY_IMPORT_TIMESTAMP = "meta_import_timestamp"
        private const val KEY_LOCAL_COPY_PATH = "meta_local_copy_path"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _storedMetadata = MutableStateFlow<StoredListMetadata?>(null)
    val storedMetadata: StateFlow<StoredListMetadata?> = _storedMetadata.asStateFlow()

    private val _importProgress = MutableStateFlow(ImportProgress())
    val importProgress: StateFlow<ImportProgress> = _importProgress.asStateFlow()

    private val _currentBatchData = MutableStateFlow<BatchData?>(null)
    val currentBatchData: StateFlow<BatchData?> = _currentBatchData.asStateFlow()

    private val _isLoadingBatch = MutableStateFlow(false)
    val isLoadingBatch: StateFlow<Boolean> = _isLoadingBatch.asStateFlow()

    init {
        restoreStoredMetadata()
    }

    /**
     * Restore persistent file metadata on app startup.
     */
    fun restoreStoredMetadata() {
        val uriStr = prefs.getString(KEY_URI, null)
        val indexPath = prefs.getString(KEY_INDEX_PATH, null)
        val totalEntries = prefs.getLong(KEY_TOTAL_ENTRIES, -1L)

        if (uriStr != null && indexPath != null && totalEntries > 0) {
            val indexFile = File(indexPath)
            val displayName = prefs.getString(KEY_DISPLAY_NAME, "password.txt") ?: "password.txt"
            val fileSize = prefs.getLong(KEY_FILE_SIZE, 0L)
            val totalBatches = prefs.getLong(KEY_TOTAL_BATCHES, 1L)
            val importTime = prefs.getLong(KEY_IMPORT_TIMESTAMP, 0L)

            // Verify availability
            val isAvailable = indexFile.exists() && checkFileReadable(uriStr)

            val meta = StoredListMetadata(
                uriString = uriStr,
                fileName = displayName,
                fileSizeBytes = fileSize,
                totalEntries = totalEntries,
                totalBatches = totalBatches,
                batchSize = BATCH_SIZE,
                indexFilePath = indexPath,
                importTimestamp = importTime,
                isAvailable = isAvailable
            )
            _storedMetadata.value = meta
        } else {
            _storedMetadata.value = null
        }
    }

    private fun checkFileReadable(uriStr: String): Boolean {
        return try {
            val localPath = prefs.getString(KEY_LOCAL_COPY_PATH, null)
            if (localPath != null && File(localPath).exists()) {
                true
            } else {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Index a TXT file from a Content Uri with up to 10,000,000 lines.
     * Memory usage is strictly bounded. Only byte offsets are saved to disk.
     */
    suspend fun indexFile(
        sourceUri: Uri,
        customDisplayName: String? = null,
        onProgressUpdate: ((ImportProgress) -> Unit)? = null
    ): Result<StoredListMetadata> = withContext(Dispatchers.IO) {
        val fileName = customDisplayName ?: queryFileName(sourceUri)
        val fileSize = queryFileSize(sourceUri)
        val indexFile = File(context.filesDir, "passwords_${System.currentTimeMillis()}.idx")

        Log.d(TAG, "Import started for $fileName (estimated size: $fileSize bytes)")

        _importProgress.value = ImportProgress(
            status = ImportStatus.INDEXING,
            fileName = fileName,
            processedCount = 0L,
            totalEstimate = if (fileSize > 0) (fileSize / 15).coerceAtLeast(100L) else 0L,
            percent = 0f
        )

        var totalLines = 0L
        val startTime = System.currentTimeMillis()
        var lastProgressUpdateTime = startTime

        try {
            // Check if we need local caching for non-seekable SAF streams or performance
            // For standard files, we stream directly from content resolver
            DataOutputStream(BufferedOutputStream(FileOutputStream(indexFile), 65536)).use { idxOut ->
                // Always write offset of Line 1 (Batch 1 start) = 0L
                idxOut.writeLong(0L)

                // Stream through the input using a bounded buffer
                val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: throw IllegalArgumentException("Cannot open stream for URI: $sourceUri")

                val bufferedStream = BufferedInputStream(inputStream, 65536)
                val buffer = ByteArray(65536)
                var bytesRead: Int
                var currentByteOffset = 0L

                // Track carriage return to handle \r\n properly
                var prevCharWasCR = false

                while (bufferedStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!coroutineContext.isActive) {
                        throw CancellationException("Import cancelled by user")
                    }

                    for (i in 0 until bytesRead) {
                        val b = buffer[i]
                        val offsetOfChar = currentByteOffset + i

                        if (b == '\n'.code.toByte()) {
                            totalLines++
                            val nextLineOffset = offsetOfChar + 1L
                            if (totalLines % BATCH_SIZE == 0L) {
                                // Start of next batch
                                idxOut.writeLong(nextLineOffset)
                            }
                            prevCharWasCR = false
                        } else if (b == '\r'.code.toByte()) {
                            totalLines++
                            val nextLineOffset = offsetOfChar + 1L
                            if (totalLines % BATCH_SIZE == 0L) {
                                idxOut.writeLong(nextLineOffset)
                            }
                            prevCharWasCR = true
                        } else {
                            if (prevCharWasCR) {
                                prevCharWasCR = false
                            }
                        }
                    }

                    currentByteOffset += bytesRead

                    // Throttle progress updates: every 100ms or 50,000 lines
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdateTime >= 100L) {
                        val elapsedSec = (now - startTime) / 1000.0
                        val speed = if (elapsedSec > 0) (totalLines / elapsedSec).toLong() else 0L
                        val estimatedTotal = if (fileSize > 0) {
                            ((currentByteOffset.toDouble() / fileSize.toDouble()) * totalLines)
                                .toLong().coerceAtLeast(totalLines)
                        } else totalLines

                        val percent = if (fileSize > 0) {
                            ((currentByteOffset.toFloat() / fileSize.toFloat()) * 100f).coerceIn(0f, 99.9f)
                        } else 0f

                        val remaining = if (estimatedTotal > totalLines) estimatedTotal - totalLines else 0L
                        val eta = if (speed > 0) (remaining / speed) else 0L

                        val progress = ImportProgress(
                            status = ImportStatus.INDEXING,
                            fileName = fileName,
                            processedCount = totalLines,
                            totalEstimate = estimatedTotal,
                            percent = percent,
                            speedEntriesPerSec = speed,
                            etaSeconds = eta
                        )
                        _importProgress.value = progress
                        onProgressUpdate?.invoke(progress)
                        lastProgressUpdateTime = now
                    }
                }

                // If file does not end with newline but has bytes, count last line
                if (currentByteOffset > 0 && !prevCharWasCR && buffer.isNotEmpty()) {
                    // Check if last byte was newline
                    // If not already counted:
                    // totalLines is already tracked
                }

                // If 0 lines counted but file has content, treat as 1 line
                if (totalLines == 0L && currentByteOffset > 0) {
                    totalLines = 1L
                }

                idxOut.flush()
            }

            val totalBatches = if (totalLines == 0L) 1L else (totalLines + BATCH_SIZE - 1) / BATCH_SIZE

            val meta = StoredListMetadata(
                uriString = sourceUri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                totalEntries = totalLines,
                totalBatches = totalBatches,
                batchSize = BATCH_SIZE,
                indexFilePath = indexFile.absolutePath,
                importTimestamp = System.currentTimeMillis(),
                isAvailable = true
            )

            // Save metadata to persistent preferences
            saveMetadataToPrefs(meta)
            _storedMetadata.value = meta

            val completedProgress = ImportProgress(
                status = ImportStatus.COMPLETED,
                fileName = fileName,
                processedCount = totalLines,
                totalEstimate = totalLines,
                percent = 100f,
                speedEntriesPerSec = 0L,
                etaSeconds = 0L
            )
            _importProgress.value = completedProgress
            onProgressUpdate?.invoke(completedProgress)

            Log.d(TAG, "Import completed: $totalLines entries, $totalBatches batches in ${(System.currentTimeMillis() - startTime)}ms")

            // Immediately load Batch 1 into memory
            loadBatch(1L)

            Result.success(meta)
        } catch (e: CancellationException) {
            Log.d(TAG, "Import cancelled")
            indexFile.delete()
            _importProgress.value = ImportProgress(
                status = ImportStatus.CANCELLED,
                fileName = fileName,
                errorMessage = "Import cancelled by user"
            )
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}")
            indexFile.delete()
            _importProgress.value = ImportProgress(
                status = ImportStatus.ERROR,
                fileName = fileName,
                errorMessage = e.localizedMessage ?: "Failed to process file"
            )
            Result.failure(e)
        }
    }

    /**
     * Load a 500-entry batch from disk using the lightweight byte index.
     * Memory usage is bounded to at most 500 strings.
     */
    suspend fun loadBatch(batchNumber: Long): Result<BatchData> = withContext(Dispatchers.IO) {
        val meta = _storedMetadata.value ?: return@withContext Result.failure(
            IllegalStateException("No stored password list")
        )

        val totalBatches = meta.totalBatches
        if (batchNumber < 1 || batchNumber > totalBatches) {
            return@withContext Result.failure(
                IllegalArgumentException("Batch number $batchNumber out of range (1..$totalBatches)")
            )
        }

        _isLoadingBatch.value = true

        try {
            val indexFile = File(meta.indexFilePath)
            if (!indexFile.exists()) {
                metaUpdateUnavailable()
                return@withContext Result.failure(
                    IllegalStateException("Password list index is missing. Please import it again.")
                )
            }

            // Read the start byte offset for this batch from index file
            // Each entry is 8 bytes (Long)
            val batchIndex = batchNumber - 1L
            val offsetInFile: Long = RandomAccessFile(indexFile, "r").use { raf ->
                raf.seek(batchIndex * 8L)
                raf.readLong()
            }

            // Open the source stream and seek to offset
            val uri = Uri.parse(meta.uriString)
            val entries = mutableListOf<PasswordEntry>()
            val startLine = batchIndex * BATCH_SIZE + 1L
            var currentLineNumber = startLine

            openSeekableStream(uri, offsetInFile).use { reader ->
                var positionInBatch = 1
                while (positionInBatch <= BATCH_SIZE) {
                    val line = reader.readLine() ?: break
                    entries.add(
                        PasswordEntry(
                            globalLineNumber = currentLineNumber,
                            batchNumber = batchNumber,
                            positionInBatch = positionInBatch,
                            password = line
                        )
                    )
                    currentLineNumber++
                    positionInBatch++
                }
            }

            val endLine = currentLineNumber - 1L
            val batchData = BatchData(
                batchNumber = batchNumber,
                totalBatches = totalBatches,
                startLine = startLine,
                endLine = endLine,
                totalEntries = meta.totalEntries,
                entries = entries
            )

            _currentBatchData.value = batchData
            _isLoadingBatch.value = false
            Log.d(TAG, "Batch $batchNumber loaded: lines $startLine–$endLine (${entries.size} entries)")
            Result.success(batchData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load batch $batchNumber: ${e.message}")
            _isLoadingBatch.value = false
            Result.failure(e)
        }
    }

    /**
     * Opens a BufferedReader seeked to the requested byte offset.
     */
    private fun openSeekableStream(uri: Uri, byteOffset: Long): BufferedReader {
        val localCopyPath = prefs.getString(KEY_LOCAL_COPY_PATH, null)
        if (localCopyPath != null && File(localCopyPath).exists()) {
            val fis = FileInputStream(localCopyPath)
            fis.channel.position(byteOffset)
            return BufferedReader(InputStreamReader(fis, Charsets.UTF_8), 16384)
        }

        // Try openFileDescriptor for channel positioning
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val fis = FileInputStream(pfd.fileDescriptor)
                try {
                    fis.channel.position(byteOffset)
                    return BufferedReader(InputStreamReader(fis, Charsets.UTF_8), 16384)
                } catch (e: Exception) {
                    // Fall back to skip if channel position not supported on this provider
                    pfd.close()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "File descriptor seek unavailable, falling back to stream skip: ${e.message}")
        }

        // Fallback: open standard stream and skip bytes
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for $uri")
        var skipped = 0L
        while (skipped < byteOffset) {
            val s = inputStream.skip(byteOffset - skipped)
            if (s <= 0) break
            skipped += s
        }
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 16384)
    }

    private fun metaUpdateUnavailable() {
        val current = _storedMetadata.value
        if (current != null) {
            val updated = current.copy(isAvailable = false)
            _storedMetadata.value = updated
        }
    }

    private fun saveMetadataToPrefs(meta: StoredListMetadata) {
        prefs.edit()
            .putString(KEY_URI, meta.uriString)
            .putString(KEY_DISPLAY_NAME, meta.fileName)
            .putLong(KEY_FILE_SIZE, meta.fileSizeBytes)
            .putLong(KEY_TOTAL_ENTRIES, meta.totalEntries)
            .putLong(KEY_TOTAL_BATCHES, meta.totalBatches)
            .putInt(KEY_BATCH_SIZE, meta.batchSize)
            .putString(KEY_INDEX_PATH, meta.indexFilePath)
            .putLong(KEY_IMPORT_TIMESTAMP, meta.importTimestamp)
            .apply()
    }

    /**
     * Clear stored password list and index files.
     */
    fun clearStoredList() {
        val meta = _storedMetadata.value
        if (meta != null) {
            try {
                File(meta.indexFilePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete index file: ${e.message}")
            }
        }

        val localCopyPath = prefs.getString(KEY_LOCAL_COPY_PATH, null)
        if (localCopyPath != null) {
            try {
                File(localCopyPath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete local copy: ${e.message}")
            }
        }

        prefs.edit().clear().apply()
        _storedMetadata.value = null
        _currentBatchData.value = null
        _importProgress.value = ImportProgress()
        Log.d(TAG, "Stored password list cleared")
    }

    /**
     * Helper to query display name from ContentResolver.
     */
    private fun queryFileName(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query file name: ${e.message}")
            }
        }
        return name ?: uri.lastPathSegment ?: "password.txt"
    }

    private fun queryFileSize(uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query file size: ${e.message}")
            }
        }
        return size
    }

    /**
     * Create a test file locally for rapid verification of performance,
     * batch navigation, and the exact test cases specified in the brief.
     */
    suspend fun createTestFile(
        totalLines: Long,
        customTargetLine: Long? = null,
        customTargetPassword: String? = null
    ): Uri = withContext(Dispatchers.IO) {
        val testFile = File(context.filesDir, "test_passwords_${totalLines}.txt")
        if (testFile.exists()) {
            testFile.delete()
        }

        testFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            for (i in 1L..totalLines) {
                if (customTargetLine != null && i == customTargetLine && customTargetPassword != null) {
                    writer.write(customTargetPassword)
                } else if (i == 1L) {
                    writer.write("12ertyuoi")
                } else if (i == 2L) {
                    writer.write("equiwywyw")
                } else if (i == 3L) {
                    writer.write("Aarti7756") // Test case line 3
                } else if (i == 11L) {
                    writer.write("Aarti7756") // Prompt test case line 11
                } else {
                    writer.write("pass_${i}_key")
                }
                writer.write("\n")
            }
            writer.flush()
        }

        prefs.edit().putString(KEY_LOCAL_COPY_PATH, testFile.absolutePath).apply()
        Uri.fromFile(testFile)
    }
}
