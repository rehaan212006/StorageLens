package com.storagelens.app.data.datasource

import android.net.Uri
import android.os.Build
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
                        lastModified = getEffectiveFileTimestamp(child),
                        isAccessible = child.canRead()
                    )
                )
            } else {
                val size = child.length()
                val mime = MimeUtils.getMimeType(child.name)
                val cat = MimeUtils.resolveCategory(mime, child.absolutePath)
                files.add(
                    FileItem(
                        id = child.absolutePath,
                        uri = Uri.fromFile(child),
                        name = child.name,
                        path = child.absolutePath,
                        sizeBytes = size,
                        mimeType = mime,
                        extension = MimeUtils.getExtension(child.name),
                        lastModified = getEffectiveFileTimestamp(child),
                        isDirectory = false,
                        category = cat
                    )
                )
            }
        }

        Pair(folders, files)
    }

    /**
     * Recursively traverses accessible shared storage directories (Documents, My Documents,
     * Download, DCIM, Pictures, etc.) and discovers all accessible documents and media files.
     */
    suspend fun scanAllAccessibleFiles(
        onProgress: ((count: Int, bytes: Long) -> Unit)? = null
    ): Pair<List<FolderItem>, List<FileItem>> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists() || !root.canRead()) {
            return@withContext Pair(emptyList(), emptyList())
        }

        val allFiles = mutableListOf<FileItem>()
        val topFolders = mutableListOf<FolderItem>()
        var count = 0
        var totalBytes = 0L

        val rootChildren = try {
            root.listFiles() ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        for (child in rootChildren) {
            if (child.name.startsWith(".")) continue
            val canonical = try { child.canonicalPath } catch (e: Exception) { child.absolutePath }
            if (canonical.contains("/Android/data") || canonical.contains("/Android/obb")) continue

            if (child.isDirectory) {
                val stats = calculateFolderQuickStats(child, showHidden = false)
                topFolders.add(
                    FolderItem(
                        path = child.absolutePath,
                        name = child.name,
                        sizeBytes = stats.first,
                        fileCount = stats.second,
                        subfolderCount = stats.third,
                        lastModified = getEffectiveFileTimestamp(child),
                        isAccessible = child.canRead()
                    )
                )
            } else {
                val size = child.length()
                if (size > 0L) {
                    val mime = MimeUtils.getMimeType(child.name)
                    val cat = MimeUtils.resolveCategory(mime, child.absolutePath)
                    allFiles.add(
                        FileItem(
                            id = child.absolutePath,
                            uri = Uri.fromFile(child),
                            name = child.name,
                            path = child.absolutePath,
                            sizeBytes = size,
                            mimeType = mime,
                            extension = MimeUtils.getExtension(child.name),
                            lastModified = getEffectiveFileTimestamp(child),
                            isDirectory = false,
                            category = cat
                        )
                    )
                    count++
                    totalBytes += size
                }
            }
        }

        // Traverse accessible subfolders with BFS (max depth 6)
        val visited = HashSet<String>()
        val queue = ArrayDeque<Pair<File, Int>>()
        for (folder in topFolders) {
            android.util.Log.d("FileSystemDataSource", "Top folder: ${folder.name}, isAccessible=${folder.isAccessible}, path=${folder.path}")
            if (!folder.path.endsWith("/Android")) {
                queue.add(Pair(File(folder.path), 1))
            }
        }

        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val canonical = try { dir.canonicalPath } catch (e: Exception) { dir.absolutePath }
            if (visited.contains(canonical)) continue
            visited.add(canonical)

            val children = try {
                dir.listFiles() ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }

            android.util.Log.d("FileSystemDataSource", "Dir: ${dir.name}, canRead=${dir.canRead()}, children=${children.size}")


            for (child in children) {
                if (child.name.startsWith(".")) continue
                val childCanonical = try { child.canonicalPath } catch (e: Exception) { child.absolutePath }
                if (childCanonical.contains("/Android/data") || childCanonical.contains("/Android/obb")) continue

                if (child.isDirectory) {
                    if (depth < 6) {
                        queue.add(Pair(child, depth + 1))
                    }
                } else {
                    val size = child.length()
                    if (child.name.contains("PETIGO", ignoreCase = true) || child.parent?.contains("Documents") == true) {
                        android.util.Log.d("FileSystemDataSource", "Found file in FS: ${child.name}, size=$size, canRead=${child.canRead()}")
                    }
                    if (size > 0L) {
                        val mime = MimeUtils.getMimeType(child.name)
                        val cat = MimeUtils.resolveCategory(mime, child.absolutePath)
                        allFiles.add(
                            FileItem(
                                id = child.absolutePath,
                                uri = Uri.fromFile(child),
                                name = child.name,
                                path = child.absolutePath,
                                sizeBytes = size,
                                mimeType = mime,
                                extension = MimeUtils.getExtension(child.name),
                                lastModified = getEffectiveFileTimestamp(child),
                                isDirectory = false,
                                category = cat
                            )
                        )
                        count++
                        totalBytes += size

                        if (count % 100 == 0) {
                            onProgress?.invoke(count, totalBytes)
                        }
                    }
                }
            }
        }

        android.util.Log.i("FileSystemDataSource", "scanAllAccessibleFiles found ${allFiles.size} files across ${topFolders.size} folders")
        Pair(topFolders, allFiles)
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

    /**
     * Resolves an accurate, valid timestamp for a file, correcting for DOS epoch (Jan 1, 1980)
     * and Unix epoch (Jan 1, 1970) timestamps common in unzipped archives and assets.
     */
    private fun getEffectiveFileTimestamp(file: File): Long {
        val lm = file.lastModified()
        if (lm >= 946_684_800_000L) {
            return lm
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val attrs = java.nio.file.Files.readAttributes(
                    file.toPath(),
                    java.nio.file.attribute.BasicFileAttributes::class.java
                )
                val cTime = attrs.creationTime().toMillis()
                if (cTime >= 946_684_800_000L) {
                    return cTime
                }
            } catch (ignored: Exception) {}
        }
        val parentLm = file.parentFile?.lastModified() ?: 0L
        if (parentLm >= 946_684_800_000L) {
            return parentLm
        }
        return System.currentTimeMillis()
    }
}
