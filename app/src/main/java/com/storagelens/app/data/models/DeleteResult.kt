package com.storagelens.app.data.models

import android.content.IntentSender

/**
 * Result of attempting to delete a file under Android's Scoped Storage model.
 */
sealed interface DeleteResult {
    /** File was successfully deleted directly. */
    data object Success : DeleteResult

    /**
     * File requires explicit user permission via system dialog.
     * Returned on Android 10 (via RecoverableSecurityException)
     * and Android 11+ (via MediaStore.createDeleteRequest).
     */
    data class RequiresPermission(val intentSender: IntentSender) : DeleteResult

    /** Deletion failed with an optional error message. */
    data class Failure(val message: String? = null) : DeleteResult
}
