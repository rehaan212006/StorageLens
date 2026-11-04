package com.storagelens.app.utils

import java.text.DecimalFormat
import java.util.Locale

/**
 * Utility for human-readable byte formatting.
 */
object StorageFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    private val decimalFormat = DecimalFormat("#,##0.0#")
    private val integerFormat = DecimalFormat("#,##0")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "${integerFormat.format(bytes)} B"

        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())

        val formattedValue = if (value >= 100.0) {
            integerFormat.format(value)
        } else {
            decimalFormat.format(value)
        }

        return "$formattedValue ${units[index]}"
    }

    fun formatPercentage(percentage: Float): String {
        return String.format(Locale.US, "%.1f%%", percentage)
    }

    fun formatCount(count: Int): String {
        return integerFormat.format(count)
    }
}
