package com.storagelens.app.data.datasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.utils.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Accesses indexed media and files using Android's MediaStore API,
 * strictly complying with Android Scoped Storage.
 */
class MediaStoreDataSource(private val context: Context) {

    suspend fun queryAllAccessibleFiles(
        onProgress: ((count: Int, bytes: Long) -> Unit)? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<FileItem>()
        val contentResolver = context.contentResolver

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        // Only query files with size > 0 and exclude directories in MediaStore query
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0"
        val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC"

        var totalBytes = 0L

        try {
            contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                var count = 0
                var docCount = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    if (size <= 0L) continue

                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "file_$id" else "file_$id"
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                    val dateModified = cursor.getLong(dateCol)
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val mediaType = if (mediaTypeCol != -1) cursor.getInt(mediaTypeCol) else -1

                    if (name.contains("PETIGO", ignoreCase = true) || path.contains("Documents", ignoreCase = true)) {
                        android.util.Log.d("MediaStoreDataSource", "Found document/PETIGO in query: id=$id, name=$name, path=$path, mediaType=$mediaType, mime=$mime")
                    }

                    val specificUri = when (mediaType) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> collectionUri
                    }
                    val fileUri = ContentUris.withAppendedId(specificUri, id)
                    val category = MimeUtils.resolveCategory(mime, path.ifEmpty { name })
                    if (category == StorageCategory.DOCUMENTS) {
                        docCount++
                    }
                    val extension = MimeUtils.getExtension(name)

                    val item = FileItem(
                        id = id.toString(),
                        uri = fileUri,
                        name = name,
                        path = path,
                        sizeBytes = size,
                        mimeType = mime.ifEmpty { MimeUtils.getMimeType(name) },
                        extension = extension,
                        lastModified = dateModified,
                        isDirectory = false,
                        category = category
                    )
                    fileList.add(item)
                    totalBytes += size
                    count++

                    if (count % 250 == 0) {
                        onProgress?.invoke(count, totalBytes)
                    }
                }
                android.util.Log.i("MediaStoreDataSource", "Query finished: total count=$count, docCount=$docCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreDataSource", "MediaStore query error: ${e.message}", e)
        }

        fileList
    }

    /**
     * Delete a file via MediaStore ContentResolver.
     */
    suspend fun deleteFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }
}
