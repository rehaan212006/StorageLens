package com.storagelens.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Streaming SHA-256 calculation to avoid OutOfMemory errors on large files.
 */
object HashUtils {

    suspend fun calculateSha256(file: File): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead() || file.isDirectory) {
            return@withContext null
        }
        try {
            file.inputStream().use { inputStream ->
                calculateSha256(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun calculateSha256(inputStream: InputStream): String? = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024) // 64 KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            val hashBytes = digest.digest()
            val hexString = StringBuilder()
            for (b in hashBytes) {
                hexString.append(String.format("%02x", b))
            }
            hexString.toString()
        } catch (e: Exception) {
            null
        }
    }
}
