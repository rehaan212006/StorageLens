package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.repository.StorageRepository

data class StorageInsights(
    val largestFiles: List<FileItem> = emptyList(),
    val largestFolders: List<FolderItem> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val duplicateWastedBytes: Long = 0L,
    val staleFiles: List<FileItem> = emptyList(),
    val staleFilesBytes: Long = 0L,
    val downloadsSizeBytes: Long = 0L,
    val downloadsCount: Int = 0,
    val categoryDistribution: Map<StorageCategory, Long> = emptyMap(),
    val totalScannedFiles: Int = 0,
    val totalScannedBytes: Long = 0L
) {
    val totalReclaimableSpace: Long
        get() = duplicateWastedBytes + downloadsSizeBytes
}

class GetStorageInsightsUseCase(private val repository: StorageRepository) {

    operator fun invoke(): StorageInsights {
        val files = repository.scannedFiles.value
        val folders = repository.topFolders.value
        val duplicates = repository.duplicates.value
        val stats = repository.latestStats.value

        val largestFiles = files.sortedByDescending { it.sizeBytes }.take(10)
        val largestFolders = folders.sortedByDescending { it.sizeBytes }.take(6)

        val duplicateWastedBytes = duplicates.sumOf { it.wastedBytes }

        val staleFiles = repository.getStaleFiles(thresholdDays = 180)
        val staleBytes = staleFiles.sumOf { it.sizeBytes }

        val downloadFiles = files.filter { it.category == StorageCategory.DOWNLOADS }
        val downloadsSize = downloadFiles.sumOf { it.sizeBytes }

        return StorageInsights(
            largestFiles = largestFiles,
            largestFolders = largestFolders,
            duplicateGroups = duplicates,
            duplicateWastedBytes = duplicateWastedBytes,
            staleFiles = staleFiles.take(10),
            staleFilesBytes = staleBytes,
            downloadsSizeBytes = downloadsSize,
            downloadsCount = downloadFiles.size,
            categoryDistribution = stats.categorySizes,
            totalScannedFiles = files.size,
            totalScannedBytes = files.sumOf { it.sizeBytes }
        )
    }
}
