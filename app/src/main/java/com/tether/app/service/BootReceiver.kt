package com.tether.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver to restart sync service on device boot
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Start the sync service
            val syncIntent = Intent(context, CanvasSyncService::class.java)
            syncIntent.action = "ACTION_START_SYNC"
            
            try {
                context.startForegroundService(syncIntent)
            } catch (e: Exception) {
                // Handle exception for older Android versions
                context.startService(syncIntent)
            }
        }
    }
}
