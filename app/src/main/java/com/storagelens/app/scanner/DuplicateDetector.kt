package com.storagelens.app.scanner

import android.content.Context
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.utils.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Two-stage duplicate file detector:
 * 1. Filter files with identical size (fast preliminary sieve).
 * 2. Compute streaming SHA-256 hash for candidate groups to verify exact duplicates.
 *
 * Never compares by filename alone.
 */
class DuplicateDetector(private val context: Context) {

    suspend fun findDuplicates(
        files: List<FileItem>,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        // Stage 1: Group by exact size. Only consider files >= 1KB to avoid trivial zero-byte files
        val sizeCandidates = files
            .filter { it.sizeBytes >= 1024L && !it.isDirectory }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }

        val totalCandidates = sizeCandidates.values.sumOf { it.size }
        var processedCount = 0

        val duplicateGroups = mutableListOf<DuplicateGroup>()

        // Stage 2: Compute SHA-256 for each candidate
        for ((size, candidateFiles) in sizeCandidates) {
            coroutineContext.ensureActive()

            val hashMap = mutableMapOf<String, MutableList<FileItem>>()

            for (fileItem in candidateFiles) {
                coroutineContext.ensureActive()
                processedCount++
                onProgress?.invoke(processedCount, totalCandidates, fileItem.name)

                val hash = calculateHashForFileItem(fileItem)
                if (hash != null) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(fileItem)
                }
            }

            for ((hash, matchingFiles) in hashMap) {
                if (matchingFiles.size > 1) {
                    // Pre-select copies after the first one so user can review and delete with 1 tap
                    val duplicateIds = matchingFiles.drop(1).map { it.id }.toSet()
                    duplicateGroups.add(
                        DuplicateGroup(
                            hash = hash,
                            fileSize = size,
                            files = matchingFiles,
                            selectedFiles = duplicateIds
                        )
                    )
                }
            }
        }

        // Sort groups by total wasted space descending
        duplicateGroups.sortedByDescending { it.wastedBytes }
    }

    private suspend fun calculateHashForFileItem(fileItem: FileItem): String? {
        // Try file path first
        if (fileItem.path.isNotEmpty()) {
            val file = File(fileItem.path)
            if (file.exists() && file.canRead()) {
                val hash = HashUtils.calculateSha256(file)
                if (hash != null) return hash
            }
        }

        // Fall back to ContentResolver input stream for MediaStore Uri
        return try {
            context.contentResolver.openInputStream(fileItem.uri)?.use { stream ->
                HashUtils.calculateSha256(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
