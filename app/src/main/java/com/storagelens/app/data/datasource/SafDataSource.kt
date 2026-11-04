package com.storagelens.app.data.datasource

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.utils.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles user-selected directories via Storage Access Framework (SAF).
 * Allows users to safely grant access to specific storage locations without broad permissions.
 */
class SafDataSource(private val context: Context) {

    suspend fun listTree(treeUri: Uri): Pair<List<FolderItem>, List<FileItem>> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.canRead()) {
            return@withContext Pair(emptyList(), emptyList())
        }

        val folders = mutableListOf<FolderItem>()
        val files = mutableListOf<FileItem>()

        val documents = rootDoc.listFiles()
        for (doc in documents) {
            val name = doc.name ?: "Unknown"
            if (doc.isDirectory) {
                var count = 0
                var size = 0L
                try {
                    val children = doc.listFiles()
                    count = children.size
                    for (c in children) {
                        if (!c.isDirectory) size += c.length()
                    }
                } catch (ignored: Exception) {}

                folders.add(
                    FolderItem(
                        path = doc.uri.toString(),
                        name = name,
                        sizeBytes = size,
                        fileCount = count,
                        subfolderCount = 0,
                        lastModified = doc.lastModified(),
                        isAccessible = doc.canRead()
                    )
                )
            } else {
                val size = doc.length()
                val mime = doc.type ?: MimeUtils.getMimeType(name)
                val cat = MimeUtils.resolveCategory(mime, name)

                files.add(
                    FileItem(
                        id = doc.uri.toString(),
                        uri = doc.uri,
                        name = name,
                        path = doc.uri.toString(),
                        sizeBytes = size,
                        mimeType = mime,
                        extension = MimeUtils.getExtension(name),
                        lastModified = doc.lastModified(),
                        isDirectory = false,
                        category = cat
                    )
                )
            }
        }

        Pair(folders, files)
    }

    suspend fun deleteDocument(documentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, documentUri)
            doc?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }
}
