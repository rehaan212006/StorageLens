package com.storagelens.app.data.repository

import android.content.Context
import android.net.Uri
import com.storagelens.app.data.datasource.FileSystemDataSource
import com.storagelens.app.data.datasource.MediaStoreDataSource
import com.storagelens.app.data.datasource.SafDataSource
import com.storagelens.app.data.models.DeleteResult
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.models.StorageStats
import com.storagelens.app.scanner.StorageScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class StorageRepositoryImpl(
    private val context: Context,
    private val storageScanner: StorageScanner,
    private val fileSystemDataSource: FileSystemDataSource,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val safDataSource: SafDataSource
) : StorageRepository {

    override val scanState: StateFlow<ScanState> = storageScanner.scanState
    override val scannedFiles: StateFlow<List<FileItem>> = storageScanner.scannedFiles
    override val topFolders: StateFlow<List<FolderItem>> = storageScanner.topFolders
    override val duplicates: StateFlow<List<DuplicateGroup>> = storageScanner.duplicates
    override val latestStats: StateFlow<StorageStats> = storageScanner.latestStats

    override suspend fun loadInitialStats() {
        storageScanner.loadInitialDeviceStats()
    }

    override fun startScan(scope: CoroutineScope, scanDuplicates: Boolean) {
        storageScanner.startScan(scope, scanDuplicates)
    }

    override fun cancelScan() {
        storageScanner.cancelScan()
    }

    override suspend fun listDirectory(
        directoryPath: String,
        showHidden: Boolean
    ): Pair<List<FolderItem>, List<FileItem>> {
        return fileSystemDataSource.listDirectory(directoryPath, showHidden)
    }

    override suspend fun listTree(treeUri: Uri): Pair<List<FolderItem>, List<FileItem>> {
        return safDataSource.listTree(treeUri)
    }

    override suspend fun deleteFile(item: FileItem): DeleteResult {
        val result = safDataSource.deleteDocument(item.uri, item.path)
        if (result is DeleteResult.Success) {
            storageScanner.removeDeletedFile(item.id)
        }
        return result
    }

    override suspend fun notifyFileDeleted(fileId: String) {
        storageScanner.removeDeletedFile(fileId)
    }

    override suspend fun createBatchDeleteRequest(items: List<FileItem>): DeleteResult {
        val uris = items.mapNotNull { item ->
            safDataSource.findMediaStoreUri(item.uri, item.path) ?: item.uri
        }
        return safDataSource.createDeleteRequest(uris)
    }

    override fun toggleDuplicateSelection(groupHash: String, fileId: String) {
        val currentDuplicates = storageScanner.duplicates.value
        val updated = currentDuplicates.map { group ->
            if (group.hash == groupHash) {
                val newSelected = if (group.selectedFiles.contains(fileId)) {
                    group.selectedFiles - fileId
                } else {
                    group.selectedFiles + fileId
                }
                group.copy(selectedFiles = newSelected)
            } else {
                group
            }
        }
        // Update in scanner
        (storageScanner.duplicates as? MutableStateFlow)?.value = updated
    }

    override suspend fun deleteDuplicatesInGroup(group: DuplicateGroup): Int {
        var deletedCount = 0
        for (fileId in group.selectedFiles) {
            val fileItem = group.files.find { it.id == fileId }
            if (fileItem != null) {
                if (deleteFile(fileItem) is DeleteResult.Success) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    override fun getLargeFiles(filter: LargeFileFilter, sortOrder: SortOrder): List<FileItem> {
        val files = storageScanner.scannedFiles.value.filter { it.sizeBytes >= filter.thresholdBytes }
        return storageScanner.sortFiles(files, sortOrder)
    }

    override fun getStaleFiles(thresholdDays: Int): List<FileItem> {
        val now = System.currentTimeMillis()
        val thresholdMs = thresholdDays * 24L * 3600L * 1000L
        return storageScanner.scannedFiles.value
            .filter { item ->
                val itemMs = if (item.lastModified < 10_000_000_000L) item.lastModified * 1000L else item.lastModified
                itemMs > 0 && (now - itemMs) >= thresholdMs
            }
            .sortedByDescending { it.sizeBytes }
    }

    override fun getFilesByCategory(category: StorageCategory): List<FileItem> {
        return storageScanner.scannedFiles.value
            .filter { it.category == category }
            .sortedByDescending { it.sizeBytes }
    }
}
