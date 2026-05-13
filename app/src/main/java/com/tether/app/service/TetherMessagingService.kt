package com.tether.app.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for push notifications
 * Triggers background sync when WebSocket is unavailable
 */
@AndroidEntryPoint
class TetherMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "TetherMessaging"
    }
    
    @Inject
    lateinit var canvasRepository: com.tether.app.data.repository.CanvasRepository // Would inject properly
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Send token to server for push notifications
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "FCM message received from: ${message.from}")
        
        message.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${message.data}")
            
            when (message.data["type"]) {
                "canvas_update" -> {
                    // Trigger background sync
                    triggerCanvasSync()
                }
                "gesture" -> {
                    // Handle gesture notification
                    handleGestureNotification(message.data)
                }
                "new_pairing" -> {
                    // Notify about new pairing request
                    handlePairingNotification(message.data)
                }
            }
        }
        
        message.notification?.let {
            Log.d(TAG, "Message Notification Title: ${it.title}")
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }
    
    private fun triggerCanvasSync() {
        // Start sync service or trigger WorkManager job
        // This would fetch new canvas events from the server
        Log.d(TAG, "Triggering canvas sync...")
    }
    
    private fun handleGestureNotification(data: Map<String, String>) {
        val gestureType = data["gesture_type"] ?: return
        Log.d(TAG, "Gesture received: $gestureType")
        // Would show ephemeral notification or trigger wallpaper animation
    }
    
    private fun handlePairingNotification(data: Map<String, String>) {
        val inviteCode = data["invite_code"] ?: return
        Log.d(TAG, "New pairing invite with code: $inviteCode")
        // Would show notification to user
    }
}
