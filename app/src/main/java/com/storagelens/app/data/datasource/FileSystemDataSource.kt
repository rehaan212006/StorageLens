package com.storagelens.app.data.datasource

import android.net.Uri
import android.os.Environment
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.utils.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Traverses accessible directories in shared storage safely under Android scoped storage.
 * Never attempts to access private app directories (Android/data or Android/obb).
 */
class FileSystemDataSource {

    val rootPath: String
        get() = try {
            Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard"
        } catch (e: Exception) {
            "/sdcard"
        }

    /**
     * Lists direct children (folders and files) of a directory path.
     */
    suspend fun listDirectory(
        directoryPath: String,
        showHidden: Boolean = false
    ): Pair<List<FolderItem>, List<FileItem>> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return@withContext Pair(emptyList(), emptyList())
        }

        val folders = mutableListOf<FolderItem>()
        val files = mutableListOf<FileItem>()

        val children = try {
            dir.listFiles() ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        for (child in children) {
            if (!showHidden && child.name.startsWith(".")) {
                continue
            }

            // Respect scoped storage: skip inaccessible Android/data and Android/obb
            val canonical = try { child.canonicalPath } catch (e: Exception) { child.absolutePath }
            if (canonical.contains("/Android/data") || canonical.contains("/Android/obb")) {
                continue
            }

            if (child.isDirectory) {
                val stats = calculateFolderQuickStats(child, showHidden)
                folders.add(
                    FolderItem(
                        path = child.absolutePath,
                        name = child.name,
                        sizeBytes = stats.first,
                        fileCount = stats.second,
                        subfolderCount = stats.third,
                        lastModified = child.lastModified(),
                        isAccessible = child.canRead()
                    )
                )
            } else {
                val size = child.length()
                val mime = MimeUtils.getMimeType(child.name)
                val cat = MimeUtils.resolveCategory(mime, child.absolutePath)
                files.add(
                    FileItem(
                        id = child.absolutePath.hashCode().toString(),
                        uri = Uri.fromFile(child),
                        name = child.name,
                        path = child.absolutePath,
                        sizeBytes = size,
                        mimeType = mime,
                        extension = MimeUtils.getExtension(child.name),
                        lastModified = child.lastModified(),
                        isDirectory = false,
                        category = cat
                    )
                )
            }
        }

        Pair(folders, files)
    }

    /**
     * Quickly estimates direct child size and count for a folder.
     */
    private fun calculateFolderQuickStats(dir: File, showHidden: Boolean): Triple<Long, Int, Int> {
        var size = 0L
        var fileCount = 0
        var subfolderCount = 0

        val children = try {
            dir.listFiles() ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        for (child in children) {
            if (!showHidden && child.name.startsWith(".")) continue
            if (child.isDirectory) {
                subfolderCount++
            } else {
                fileCount++
                size += child.length()
            }
        }

        return Triple(size, fileCount, subfolderCount)
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
