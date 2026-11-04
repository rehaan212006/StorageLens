package com.storagelens.app.data.models

/**
 * Represents a folder with aggregated metrics (size, file count, subfolder count).
 */
data class FolderItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val subfolderCount: Int,
    val lastModified: Long,
    val isAccessible: Boolean = true
)
