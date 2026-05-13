package com.tether.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tether.app.R
import com.tether.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for maintaining canvas sync
 */
@AndroidEntryPoint
class CanvasSyncService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "canvas_sync_channel"
    }
    
    @Inject
    lateinit var syncManager: SyncManager // Would be implemented
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): CanvasSyncService = this@CanvasSyncService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START_SYNC" -> startSyncing()
            "ACTION_STOP_SYNC" -> stopSyncing()
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_sync),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps your canvas synchronized with your partner"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_syncing))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun startSyncing() {
        // Start WebSocket connection and background sync
        // This would integrate with the WebSocketManager and CanvasRepository
    }
    
    private fun stopSyncing() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up connections
    }
}

/**
 * Placeholder for sync management logic
 * In production, this would coordinate WebSocket, FCM, and WorkManager
 */
class SyncManager {
    fun startSync() {
        // Implementation would go here
    }
    
    fun stopSync() {
        // Implementation would go here
    }
}
