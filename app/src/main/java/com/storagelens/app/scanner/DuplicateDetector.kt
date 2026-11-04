package com.storagelens.app.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
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

        android.util.Log.i("DuplicateDetector", "findDuplicates: candidate size groups=${sizeCandidates.size}, total candidate files=$totalCandidates")

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
                android.util.Log.d("DuplicateDetector", "Hash for ${fileItem.name} (size $size, path=${fileItem.path}, uri=${fileItem.uri}): $hash")
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
        // 1. Try direct file path if accessible
        if (fileItem.path.isNotEmpty()) {
            try {
                val file = File(fileItem.path)
                if (file.exists() && file.canRead()) {
                    val hash = HashUtils.calculateSha256(file)
                    if (hash != null) return hash
                }
            } catch (ignored: Exception) {}
        }

        // 2. Try direct ContentResolver URI
        try {
            if (fileItem.uri != android.net.Uri.EMPTY && fileItem.uri.scheme == "content") {
                context.contentResolver.openInputStream(fileItem.uri)?.use { stream ->
                    val hash = HashUtils.calculateSha256(stream)
                    if (hash != null) return hash
                }
            }
        } catch (ignored: Exception) {}

        // 3. Fallback: Query MediaStore for content URI by DATA column
        if (fileItem.path.isNotEmpty()) {
            try {
                val filesUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                val selectionArgs = arrayOf(fileItem.path)
                context.contentResolver.query(filesUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val contentUri = ContentUris.withAppendedId(filesUri, id)
                        context.contentResolver.openInputStream(contentUri)?.use { stream ->
                            val hash = HashUtils.calculateSha256(stream)
                            if (hash != null) return hash
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        // 4. Fallback: query specific media type URIs for Scoped Storage compatibility
        val idLong = fileItem.id.toLongOrNull()
        if (idLong != null) {
            val candidates = listOf(
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, idLong),
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, idLong),
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idLong),
                ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), idLong)
            )
            for (uri in candidates) {
                if (uri == fileItem.uri) continue
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val hash = HashUtils.calculateSha256(stream)
                        if (hash != null) return hash
                    }
                } catch (ignored: Exception) {}
            }
        }

        return null
    }
}
