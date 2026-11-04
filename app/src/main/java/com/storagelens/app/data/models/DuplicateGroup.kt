package com.storagelens.app.data.models

/**
 * Group of exact duplicate files confirmed via matching SHA-256 hash and byte size.
 */
data class DuplicateGroup(
    val hash: String,
    val fileSize: Long,
    val files: List<FileItem>,
    val selectedFiles: Set<String> = emptySet()
) {
    /**
     * Total space wasted by duplicate copies (excluding the primary copy).
     */
    val wastedBytes: Long
        get() = if (files.size > 1) fileSize * (files.size - 1) else 0L

    val selectedBytes: Long
        get() = fileSize * selectedFiles.size
}
