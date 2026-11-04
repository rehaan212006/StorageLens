package com.storagelens.app.utils

import android.webkit.MimeTypeMap
import com.storagelens.app.data.models.StorageCategory
import java.io.File
import java.util.Locale

/**
 * Utility for resolving extensions, MIME types, and StorageCategory mapping.
 */
object MimeUtils {

    fun getExtension(fileNameOrPath: String): String {
        val lastDotIndex = fileNameOrPath.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < fileNameOrPath.length - 1) {
            fileNameOrPath.substring(lastDotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun getMimeType(fileNameOrPath: String): String {
        val extension = getExtension(fileNameOrPath)
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }

    fun resolveCategory(mimeType: String, pathOrName: String): StorageCategory {
        val lowerMime = mimeType.lowercase(Locale.ROOT)
        val extension = getExtension(pathOrName)
        val lowerPath = pathOrName.lowercase(Locale.ROOT)

        if (lowerPath.contains("/download/") || lowerPath.contains("/downloads/")) {
            return StorageCategory.DOWNLOADS
        }

        if (lowerMime.startsWith("image/") || extension in imageExtensions) {
            return StorageCategory.IMAGES
        }
        if (lowerMime.startsWith("video/") || extension in videoExtensions) {
            return StorageCategory.VIDEOS
        }
        if (lowerMime.startsWith("audio/") || extension in audioExtensions) {
            return StorageCategory.AUDIO
        }
        if (lowerMime.startsWith("text/") ||
            lowerMime.contains("pdf") ||
            lowerMime.contains("document") ||
            lowerMime.contains("spreadsheet") ||
            lowerMime.contains("presentation") ||
            lowerMime.contains("msword") ||
            extension in docExtensions
        ) {
            return StorageCategory.DOCUMENTS
        }
        if (extension in apkExtensions || lowerMime.contains("android.package-archive")) {
            return StorageCategory.APPS
        }

        return StorageCategory.OTHER
    }

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "raw", "dng"
    )

    private val videoExtensions = setOf(
        "mp4", "mkv", "mov", "avi", "wmv", "flv", "webm", "3gp", "m4v", "ts"
    )

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "wma", "opus", "mid", "amr"
    )

    private val docExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "xml", "json", "html", "epub", "md"
    )

    private val apkExtensions = setOf("apk", "apks", "xapk", "aab")
}
