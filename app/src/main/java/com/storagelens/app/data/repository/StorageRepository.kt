package com.storagelens.app.data.repository

import android.net.Uri
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.models.StorageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface StorageRepository {
    val scanState: StateFlow<ScanState>
    val scannedFiles: StateFlow<List<FileItem>>
    val topFolders: StateFlow<List<FolderItem>>
    val duplicates: StateFlow<List<DuplicateGroup>>
    val latestStats: StateFlow<StorageStats>

    suspend fun loadInitialStats()
    fun startScan(scope: CoroutineScope, scanDuplicates: Boolean = true)
    fun cancelScan()

    suspend fun listDirectory(
        directoryPath: String,
        showHidden: Boolean = false
    ): Pair<List<FolderItem>, List<FileItem>>

    suspend fun listTree(treeUri: Uri): Pair<List<FolderItem>, List<FileItem>>

    suspend fun deleteFile(item: FileItem): Boolean
    fun toggleDuplicateSelection(groupHash: String, fileId: String)
    suspend fun deleteDuplicatesInGroup(group: DuplicateGroup): Int

    fun getLargeFiles(filter: LargeFileFilter, sortOrder: SortOrder): List<FileItem>
    fun getStaleFiles(thresholdDays: Int = 180): List<FileItem>
    fun getFilesByCategory(category: StorageCategory): List<FileItem>
}
