package com.storagelens.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats epoch timestamps into human-readable strings.
 */
object DateFormatter {
    private val standardDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val detailedDateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDate(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Unknown"
        // If timestamp is in seconds instead of ms (common in MediaStore)
        val ms = if (timestampMs < 10_000_000_000L) timestampMs * 1000L else timestampMs

        // Guard against dummy DOS/Unix epoch timestamps (e.g. 1980 or 1970)
        if (ms < 946_684_800_000L) {
            return "Recently added"
        }

        val now = System.currentTimeMillis()
        val diff = now - ms
        if (diff < 0L) {
            return "Just now"
        }

        return when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L}m ago"
            diff < 86400_000L -> "Today, ${timeFormat.format(Date(ms))}"
            diff < 172800_000L -> "Yesterday"
            diff < 30L * 86400_000L -> "${diff / 86400_000L}d ago"
            else -> standardDateFormat.format(Date(ms))
        }
    }

    fun formatDetailed(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Unknown"
        val ms = if (timestampMs < 10_000_000_000L) timestampMs * 1000L else timestampMs
        if (ms < 946_684_800_000L) {
            return "Recently added"
        }
        return detailedDateFormat.format(Date(ms))
    }
}
