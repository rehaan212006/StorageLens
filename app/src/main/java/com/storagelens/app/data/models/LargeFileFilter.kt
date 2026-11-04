package com.storagelens.app.data.models

/**
 * Filter thresholds for large file detection.
 */
enum class LargeFileFilter(
    val thresholdBytes: Long,
    val label: String
) {
    ABOVE_100MB(100L * 1024L * 1024L, "> 100 MB"),
    ABOVE_250MB(250L * 1024L * 1024L, "> 250 MB"),
    ABOVE_500MB(500L * 1024L * 1024L, "> 500 MB"),
    ABOVE_1GB(1024L * 1024L * 1024L, "> 1 GB")
}

/**
 * Sorting options for file and folder listings.
 */
enum class SortOrder(val label: String) {
    LARGEST_FIRST("Largest first"),
    SMALLEST_FIRST("Smallest first"),
    ALPHABETICAL("Alphabetical (A-Z)"),
    RECENTLY_MODIFIED("Recently modified")
}
