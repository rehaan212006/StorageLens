package com.storagelens.app.scanner

import android.content.Context
import com.storagelens.app.data.datasource.DeviceStorageDataSource
import com.storagelens.app.data.datasource.FileSystemDataSource
import com.storagelens.app.data.datasource.MediaStoreDataSource
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.models.StorageStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Storage scan orchestrator that coordinates asynchronous scanning,
 * category size aggregation, duplicate detection, and progress reporting.
 */
class StorageScanner(
    private val context: Context,
    private val deviceStorageDataSource: DeviceStorageDataSource,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val fileSystemDataSource: FileSystemDataSource,
    private val duplicateDetector: DuplicateDetector
) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scannedFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val scannedFiles: StateFlow<List<FileItem>> = _scannedFiles.asStateFlow()

    private val _topFolders = MutableStateFlow<List<FolderItem>>(emptyList())
    val topFolders: StateFlow<List<FolderItem>> = _topFolders.asStateFlow()

    private val _duplicates = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicates: StateFlow<List<DuplicateGroup>> = _duplicates.asStateFlow()

    private val _latestStats = MutableStateFlow(StorageStats())
    val latestStats: StateFlow<StorageStats> = _latestStats.asStateFlow()

    private var currentScanJob: Job? = null

    suspend fun loadInitialDeviceStats() = withContext(Dispatchers.IO) {
        val stats = deviceStorageDataSource.getDeviceStorageStats()
        _latestStats.value = stats
    }

    fun startScan(scope: CoroutineScope, scanDuplicates: Boolean = true) {
        currentScanJob?.cancel()
        currentScanJob = scope.launch(Dispatchers.IO) {
            try {
                _scanState.value = ScanState.Scanning(
                    progress = 0.05f,
                    currentPhase = "Querying device storage metrics...",
                    scannedFilesCount = 0,
                    scannedBytes = 0L
                )

                // 1. Device total/used/free stats
                val baseStats = deviceStorageDataSource.getDeviceStorageStats()
                _latestStats.value = baseStats
                coroutineContext.ensureActive()

                // 2. Query MediaStore files
                _scanState.value = ScanState.Scanning(
                    progress = 0.15f,
                    currentPhase = "Analyzing accessible media and documents...",
                    scannedFilesCount = 0,
                    scannedBytes = 0L
                )

                val mediaFiles = mediaStoreDataSource.queryAllAccessibleFiles { count, bytes ->
                    _scanState.value = ScanState.Scanning(
                        progress = 0.15f + (0.35f * (count.coerceAtMost(5000) / 5000f)),
                        currentPhase = "Scanning files ($count indexed)...",
                        scannedFilesCount = count,
                        scannedBytes = bytes
                    )
                }

                coroutineContext.ensureActive()

                // 3. Scan top-level shared storage folders
                _scanState.value = ScanState.Scanning(
                    progress = 0.55f,
                    currentPhase = "Cataloging shared directory structures...",
                    scannedFilesCount = mediaFiles.size,
                    scannedBytes = mediaFiles.sumOf { it.sizeBytes }
                )

                val (rootFolders, rootFiles) = fileSystemDataSource.listDirectory(
                    fileSystemDataSource.rootPath,
                    showHidden = false
                )

                // Merge and deduplicate file records
                val allFilesMap = LinkedHashMap<String, FileItem>()
                for (f in mediaFiles) {
                    allFilesMap[f.path.ifEmpty { f.name }] = f
                }
                for (f in rootFiles) {
                    allFilesMap[f.path] = f
                }
                val mergedFiles = allFilesMap.values.toList()
                _scannedFiles.value = mergedFiles
                _topFolders.value = rootFolders.sortedByDescending { it.sizeBytes }

                coroutineContext.ensureActive()

                // 4. Calculate category sizes and counts
                _scanState.value = ScanState.Scanning(
                    progress = 0.70f,
                    currentPhase = "Calculating category allocations...",
                    scannedFilesCount = mergedFiles.size,
                    scannedBytes = mergedFiles.sumOf { it.sizeBytes }
                )

                val catSizes = mutableMapOf<StorageCategory, Long>()
                val catCounts = mutableMapOf<StorageCategory, Int>()

                for (cat in StorageCategory.values()) {
                    catSizes[cat] = 0L
                    catCounts[cat] = 0
                }

                var categorizedSum = 0L
                for (file in mergedFiles) {
                    val cat = file.category
                    catSizes[cat] = (catSizes[cat] ?: 0L) + file.sizeBytes
                    catCounts[cat] = (catCounts[cat] ?: 0) + 1
                    categorizedSum += file.sizeBytes
                }

                // Unaccounted used storage is assigned to Apps & System
                val systemOrAppsBytes = (baseStats.usedBytes - categorizedSum).coerceAtLeast(0L)
                catSizes[StorageCategory.APPS] = (catSizes[StorageCategory.APPS] ?: 0L) + systemOrAppsBytes

                val finalStats = baseStats.copy(
                    categorySizes = catSizes,
                    categoryCounts = catCounts,
                    lastScanTimestamp = System.currentTimeMillis()
                )
                _latestStats.value = finalStats

                coroutineContext.ensureActive()

                // 5. Duplicate detection if requested
                if (scanDuplicates && mergedFiles.isNotEmpty()) {
                    _scanState.value = ScanState.Scanning(
                        progress = 0.85f,
                        currentPhase = "Scanning for duplicate files (size & SHA-256)...",
                        scannedFilesCount = mergedFiles.size,
                        scannedBytes = mergedFiles.sumOf { it.sizeBytes }
                    )

                    val duplicateGroups = duplicateDetector.findDuplicates(mergedFiles) { cur, tot, name ->
                        val dupProgress = 0.85f + (0.14f * (cur.toFloat() / tot.coerceAtLeast(1)))
                        _scanState.value = ScanState.Scanning(
                            progress = dupProgress.coerceIn(0.85f, 0.99f),
                            currentPhase = "Hashing duplicates ($cur / $tot)...",
                            currentItem = name,
                            scannedFilesCount = mergedFiles.size,
                            scannedBytes = mergedFiles.sumOf { it.sizeBytes }
                        )
                    }
                    _duplicates.value = duplicateGroups
                }

                _scanState.value = ScanState.Completed(
                    stats = finalStats,
                    scannedFilesCount = mergedFiles.size,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: CancellationException) {
                _scanState.value = ScanState.Cancelled
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Failed to complete storage scan")
            }
        }
    }

    fun cancelScan() {
        currentScanJob?.cancel()
        _scanState.value = ScanState.Cancelled
    }

    fun sortFiles(files: List<FileItem>, order: SortOrder): List<FileItem> {
        return when (order) {
            SortOrder.LARGEST_FIRST -> files.sortedByDescending { it.sizeBytes }
            SortOrder.SMALLEST_FIRST -> files.sortedBy { it.sizeBytes }
            SortOrder.ALPHABETICAL -> files.sortedBy { it.name.lowercase() }
            SortOrder.RECENTLY_MODIFIED -> files.sortedByDescending { it.lastModified }
        }
    }

    fun sortFolders(folders: List<FolderItem>, order: SortOrder): List<FolderItem> {
        return when (order) {
            SortOrder.LARGEST_FIRST -> folders.sortedByDescending { it.sizeBytes }
            SortOrder.SMALLEST_FIRST -> folders.sortedBy { it.sizeBytes }
            SortOrder.ALPHABETICAL -> folders.sortedBy { it.name.lowercase() }
            SortOrder.RECENTLY_MODIFIED -> folders.sortedByDescending { it.lastModified }
        }
    }

    fun removeDeletedFile(fileId: String) {
        _scannedFiles.value = _scannedFiles.value.filter { it.id != fileId }
        _duplicates.value = _duplicates.value.mapNotNull { group ->
            val remaining = group.files.filter { it.id != fileId }
            if (remaining.size > 1) {
                group.copy(
                    files = remaining,
                    selectedFiles = group.selectedFiles - fileId
                )
            } else {
                null
            }
        }
    }
}
