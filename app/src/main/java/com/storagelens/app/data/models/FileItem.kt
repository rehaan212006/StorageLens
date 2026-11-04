package com.storagelens.app.data.models

import android.net.Uri

/**
 * Represents a single file or directory item with metadata.
 */
data class FileItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extension: String,
    val lastModified: Long,
    val isDirectory: Boolean = false,
    val category: StorageCategory = StorageCategory.OTHER,
    val isSelected: Boolean = false
) {
    val isLargeFile: Boolean
        get() = sizeBytes >= 100 * 1024 * 1024L // >= 100 MB
}
