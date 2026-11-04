package com.storagelens.app.data.models

import androidx.compose.ui.graphics.Color

/**
 * Represents storage content categories categorized by MIME types and file extensions.
 */
enum class StorageCategory(
    val displayName: String,
    val hexColor: Long
) {
    IMAGES("Images", 0xFF00E5FF),       // Electric Cyan
    VIDEOS("Videos", 0xFF2979FF),       // Electric Blue
    AUDIO("Audio", 0xFF7C4DFF),         // Deep Purple
    DOCUMENTS("Documents", 0xFF00E676), // Emerald Green
    DOWNLOADS("Downloads", 0xFFFFD600), // Amber
    APPS("Apps & System", 0xFFFF6D00),  // Vivid Orange
    OTHER("Other Files", 0xFF90A4AE);   // Cool Grey

    val color: Color
        get() = Color(hexColor)
}
