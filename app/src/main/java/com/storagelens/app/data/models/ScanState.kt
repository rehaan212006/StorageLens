package com.storagelens.app.data.models

/**
 * State representing asynchronous storage scanning lifecycle.
 */
sealed interface ScanState {
    object Idle : ScanState

    data class Scanning(
        val progress: Float = 0f,
        val currentPhase: String = "Scanning storage...",
        val currentItem: String = "",
        val scannedFilesCount: Int = 0,
        val scannedBytes: Long = 0L
    ) : ScanState

    data class Completed(
        val stats: StorageStats,
        val scannedFilesCount: Int,
        val timestamp: Long
    ) : ScanState

    data class Error(val message: String) : ScanState

    object Cancelled : ScanState
}
