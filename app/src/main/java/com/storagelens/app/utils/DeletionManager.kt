package com.storagelens.app.utils

import android.content.IntentSender
import android.util.Log

/**
 * Coordinates Scoped Storage deletion requests between ViewModels and MainActivity.
 * Ensures the Activity catches the result and handles the deletion intent.
 */
class DeletionManager {

    private val tag = "DeletionManager"

    // Launcher function provided by MainActivity
    private var activityLauncher: ((IntentSender, (Boolean) -> Unit) -> Unit)? = null

    /**
     * Called by MainActivity to register its ActivityResultLauncher.
     */
    fun registerActivityLauncher(launcher: (IntentSender, (Boolean) -> Unit) -> Unit) {
        Log.d(tag, "MainActivity deletion launcher registered")
        this.activityLauncher = launcher
    }

    /**
     * Called by MainActivity in onDestroy to clean up references.
     */
    fun unregisterActivityLauncher() {
        Log.d(tag, "MainActivity deletion launcher unregistered")
        this.activityLauncher = null
    }

    /**
     * Requests the Activity to launch the IntentSenderRequest for the system deletion prompt.
     * The callback is invoked when the Activity catches the result (granted = true for RESULT_OK).
     */
    fun requestDeletion(intentSender: IntentSender, onResult: (Boolean) -> Unit) {
        val launcher = activityLauncher
        if (launcher != null) {
            Log.d(tag, "Forwarding deletion IntentSender to MainActivity")
            launcher.invoke(intentSender, onResult)
        } else {
            Log.w(tag, "No Activity registered to handle deletion IntentSenderRequest")
            onResult(false)
        }
    }
}
