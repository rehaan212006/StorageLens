package com.storagelens.app.data.datasource

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
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
 * Handles Storage Access Framework (SAF) tree browsing and Scoped Storage file deletion.
 * Uses MediaStore.createDeleteRequest(contentResolver, listOf(uri)) to obtain an IntentSender
 * for triggering system confirmation dialogs on modern Android.
 */
class SafDataSource(private val context: Context) {

    private val tag = "SafDataSource"

    /**
     * Lists directories and files inside a user-granted SAF tree URI.
     */
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
     * Resolves a file URI or file path to its canonical MediaStore content URI
     * (e.g. content://media/external/images/media/123 or content://media/external/file/123).
     * This is strictly required by MediaStore.createDeleteRequest.
     */
    fun resolveToMediaStoreUri(uri: Uri, filePath: String? = null): Uri? {
        // 1. Check if the URI is already a MediaStore URI
        if (uri.authority == "media" || uri.toString().startsWith("content://media/")) {
            return uri
        }

        val contentResolver = context.contentResolver

        // 2. Resolve path from parameter or file:// URI
        val path = filePath?.takeIf { it.isNotEmpty() }
            ?: if (uri.scheme == "file") uri.path else null

        if (!path.isNullOrEmpty()) {
            val file = File(path)
            val absolutePath = file.absolutePath

            // Query MediaStore.Files by DATA column
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )
            val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
            val selectionArgs = arrayOf(absolutePath)

            try {
                contentResolver.query(filesUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                        val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                        val mediaType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else -1

                        val specificUri = when (mediaType) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> filesUri
                        }
                        return ContentUris.withAppendedId(specificUri, id)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Querying MediaStore by DATA failed for $absolutePath: ${e.message}")
            }

            // Fallback for Android 10+ where DATA column may be obscured
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val fileName = file.name
                    val parentPath = file.parent ?: ""
                    val relativePath = if (parentPath.contains("/storage/emulated/0/")) {
                        parentPath.substringAfter("/storage/emulated/0/").trim('/') + "/"
                    } else null

                    if (relativePath != null) {
                        val relSelection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                        val relArgs = arrayOf(fileName, relativePath)
                        contentResolver.query(filesUri, projection, relSelection, relArgs, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                                return ContentUris.withAppendedId(filesUri, id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Querying MediaStore by RELATIVE_PATH failed: ${e.message}")
                }
            }
        }

        // 3. Resolve SAF document URI (e.g. content://com.android.externalstorage.documents/document/primary%3A...)
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId != null && docId.contains(":")) {
                    val relativePath = docId.substringAfter(":")
                    val externalStorage = Environment.getExternalStorageDirectory()
                    val fullPath = File(externalStorage, relativePath).absolutePath
                    return resolveToMediaStoreUri(Uri.EMPTY, fullPath)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to resolve SAF DocumentId: ${e.message}")
        }

        return null
    }

    /**
     * Initiates deletion of a file or document complying with Android Scoped Storage.
     * Uses MediaStore.createDeleteRequest(contentResolver, listOf(uri)) to request deletion permission.
     */
    suspend fun deleteDocument(documentUri: Uri, filePath: String? = null): DeleteResult = withContext(Dispatchers.IO) {
        // Step 1: If document was directly selected via SAF tree, attempt direct delete
        try {
            if (DocumentsContract.isDocumentUri(context, documentUri)) {
                val doc = DocumentFile.fromSingleUri(context, documentUri)
                if (doc != null && doc.exists() && doc.canWrite()) {
                    if (doc.delete()) {
                        Log.i(tag, "Direct SAF delete succeeded for $documentUri")
                        return@withContext DeleteResult.Success
                    }
                }
            }
        } catch (se: SecurityException) {
            Log.d(tag, "SAF delete threw SecurityException, proceeding to MediaStore delete request")
        } catch (e: Exception) {
            Log.d(tag, "SAF DocumentFile delete failed: ${e.message}")
        }

        // Step 2: Resolve to a canonical MediaStore URI
        val mediaUri = resolveToMediaStoreUri(documentUri, filePath)
        Log.d(tag, "Resolved mediaUri: $mediaUri from documentUri: $documentUri, path: $filePath")

        if (mediaUri != null) {
            // Android 11+ (API 30+): MediaStore.createDeleteRequest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Check if direct delete works (e.g. file owned by StorageLens)
                try {
                    val rows = context.contentResolver.delete(mediaUri, null, null)
                    if (rows > 0) {
                        Log.i(tag, "Direct ContentResolver delete succeeded ($rows rows)")
                        return@withContext DeleteResult.Success
                    }
                } catch (ignored: SecurityException) {
                    // Normal when file is created by camera or other apps
                }

                return@withContext try {
                    Log.i(tag, "Creating Scoped Storage delete request via MediaStore.createDeleteRequest for $mediaUri")
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(mediaUri))
                    DeleteResult.RequiresPermission(pendingIntent.intentSender)
                } catch (e: Exception) {
                    Log.e(tag, "MediaStore.createDeleteRequest failed", e)
                    DeleteResult.Failure(e.message)
                }
            }

            // Android 10 (API 29): RecoverableSecurityException
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                return@withContext try {
                    val rows = context.contentResolver.delete(mediaUri, null, null)
                    if (rows > 0) {
                        DeleteResult.Success
                    } else {
                        DeleteResult.Failure("MediaProvider returned 0 rows deleted")
                    }
                } catch (e: SecurityException) {
                    if (e is RecoverableSecurityException) {
                        Log.i(tag, "Caught RecoverableSecurityException on Android 10, returning intentSender")
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
                    Log.i(tag, "Direct File.delete() succeeded for $filePath")
                    return@withContext DeleteResult.Success
                }
            } catch (e: Exception) {
                Log.d(tag, "Direct File.delete() failed: ${e.message}")
            }
        }

        DeleteResult.Failure("Unable to delete file: Scoped Storage permission required but MediaStore URI could not be resolved.")
    }

    /**
     * Batch delete request for multiple files via MediaStore.createDeleteRequest (Android 11+).
     */
    suspend fun createBatchDeleteRequest(items: List<FileItem>): DeleteResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext DeleteResult.Success

        val mediaUris = items.mapNotNull { item ->
            resolveToMediaStoreUri(item.uri, item.path)
        }

        if (mediaUris.isEmpty()) {
            return@withContext DeleteResult.Failure("No valid MediaStore URIs found for batch delete")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext try {
                Log.i(tag, "Creating batch delete request for ${mediaUris.size} files")
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, mediaUris)
                DeleteResult.RequiresPermission(pendingIntent.intentSender)
            } catch (e: Exception) {
                Log.e(tag, "Batch MediaStore.createDeleteRequest failed", e)
                DeleteResult.Failure(e.message)
            }
        }

        DeleteResult.Failure("Batch delete request requires Android 11+")
    }
}
