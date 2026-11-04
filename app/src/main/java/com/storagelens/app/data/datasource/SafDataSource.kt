package com.storagelens.app.data.datasource

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.storagelens.app.data.models.DeleteResult
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.utils.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles user-selected directories and documents via Storage Access Framework (SAF)
 * and Scoped Storage deletion via MediaStore and RecoverableSecurityException.
 */
class SafDataSource(private val context: Context) {

    private val tag = "SafDataSource"

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

    /**
     * Resolves the corresponding MediaStore URI for a file path or SAF document URI.
     */
    fun findMediaStoreUri(documentUri: Uri, filePath: String? = null): Uri? {
        // 1. If it's already a MediaStore URI
        if (documentUri.authority == "media" || documentUri.toString().contains("content://media/")) {
            return documentUri
        }

        // 2. If a physical file path is provided, query MediaStore for it
        if (!filePath.isNullOrEmpty()) {
            val fromPath = findMediaStoreUriByPath(filePath)
            if (fromPath != null) return fromPath
        }

        // 3. Try to resolve document URI from Storage Access Framework (SAF)
        try {
            if (DocumentsContract.isDocumentUri(context, documentUri)) {
                val docId = DocumentsContract.getDocumentId(documentUri)
                if (docId != null && docId.contains(":")) {
                    val relativePath = docId.substringAfter(":")
                    val externalStorage = Environment.getExternalStorageDirectory()
                    val fullPath = File(externalStorage, relativePath).absolutePath
                    val fromDocPath = findMediaStoreUriByPath(fullPath)
                    if (fromDocPath != null) return fromDocPath
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to resolve document ID from $documentUri: ${e.message}")
        }

        return null
    }

    private fun findMediaStoreUriByPath(path: String): Uri? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(path)

        return try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    ContentUris.withAppendedId(collection, id)
                } else null
            }
        } catch (e: Exception) {
            Log.w(tag, "Error querying MediaStore for path $path: ${e.message}")
            null
        }
    }

    /**
     * Deletes a document or file complying with Scoped Storage.
     * Uses RecoverableSecurityException on Android 10 and MediaStore.createDeleteRequest on Android 11+.
     */
    suspend fun deleteDocument(documentUri: Uri, filePath: String? = null): DeleteResult = withContext(Dispatchers.IO) {
        // Step 1: Try DocumentFile direct deletion if user granted SAF access to the tree/document
        try {
            val doc = DocumentFile.fromSingleUri(context, documentUri)
            if (doc != null && doc.exists() && doc.canWrite()) {
                if (doc.delete()) {
                    Log.d(tag, "DocumentFile.delete() succeeded for $documentUri")
                    return@withContext DeleteResult.Success
                }
            }
        } catch (se: SecurityException) {
            Log.d(tag, "DocumentFile.delete() threw SecurityException, requesting MediaStore permission")
        } catch (e: Exception) {
            Log.d(tag, "DocumentFile.delete() failed: ${e.message}")
        }

        // Step 2: Resolve the MediaStore content URI
        val mediaUri = findMediaStoreUri(documentUri, filePath)

        if (mediaUri != null) {
            // Android 11+ (API 30+): Use MediaStore.createDeleteRequest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val rows = context.contentResolver.delete(mediaUri, null, null)
                    if (rows > 0) {
                        return@withContext DeleteResult.Success
                    }
                } catch (ignored: SecurityException) {
                    // Expected when app is not the contributing owner
                }

                return@withContext try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(mediaUri))
                    DeleteResult.RequiresPermission(pendingIntent.intentSender)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to create MediaStore delete request for $mediaUri", e)
                    DeleteResult.Failure(e.message)
                }
            }

            // Android 10 (API 29): Catch RecoverableSecurityException
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                return@withContext try {
                    val rows = context.contentResolver.delete(mediaUri, null, null)
                    if (rows > 0) {
                        DeleteResult.Success
                    } else {
                        DeleteResult.Failure("MediaProvider deleted 0 items")
                    }
                } catch (e: SecurityException) {
                    if (e is RecoverableSecurityException) {
                        DeleteResult.RequiresPermission(e.userAction.actionIntent.intentSender)
                    } else {
                        DeleteResult.Failure(e.message)
                    }
                } catch (e: Exception) {
                    DeleteResult.Failure(e.message)
                }
            }
        }

        // Step 3: Direct File delete fallback if raw file is accessible and writable
        if (!filePath.isNullOrEmpty()) {
            try {
                val file = File(filePath)
                if (file.exists() && file.delete()) {
                    return@withContext DeleteResult.Success
                }
            } catch (e: Exception) {
                Log.d(tag, "File.delete() failed for $filePath: ${e.message}")
            }
        }

        DeleteResult.Failure("Cannot delete file without permission")
    }

    /**
     * Batch delete request for multiple URIs via MediaStore.createDeleteRequest (Android 11+).
     */
    suspend fun createDeleteRequest(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Success

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext DeleteResult.RequiresPermission(pendingIntent.intentSender)
            } catch (e: Exception) {
                Log.w(tag, "Failed to create batch delete request", e)
                return@withContext DeleteResult.Failure(e.message)
            }
        }
        DeleteResult.Failure("Batch deletion request requires Android 11+")
    }
}
