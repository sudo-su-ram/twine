package com.tether.app.data.remote

import android.util.Log
import com.tether.app.domain.model.*
import com.tether.app.util.EncryptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket manager for real-time canvas synchronization
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val WS_URL = "wss://api.tether.example.com/ws" // Replace with actual URL
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val messageQueue = mutableListOf<String>()
    private val mutex = Mutex()
    
    private var reconnectAttempts = 0
    private var currentPairingId: String? = null
    private var lastSequenceNumber = 0L

    fun connect(pairingId: String, authToken: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connected")
            return
        }

        currentPairingId = pairingId
        
        val request = Request.Builder()
            .url("$WS_URL?pairing_id=$pairingId&token=$authToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                
                // Send queued messages
                sendQueuedMessages()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect(authToken)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect(authToken)
            }
        })
    }

    private fun scheduleReconnect(authToken: String) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached")
            return
        }

        reconnectAttempts++
        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${RECONNECT_DELAY_MS}ms")
        
        client.dispatcher.executorService.schedule({
            if (currentPairingId != null) {
                connect(currentPairingId!!, authToken)
            }
        }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        currentPairingId = null
    }

    suspend fun sendMessage(message: String) = mutex.withLock {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            webSocket?.send(message)
        } else {
            // Queue for later
            messageQueue.add(message)
            Log.d(TAG, "Message queued (not connected)")
        }
    }

    private fun sendQueuedMessages() {
        if (messageQueue.isEmpty()) return

        Log.d(TAG, "Sending ${messageQueue.size} queued messages")
        
        val queued = messageQueue.toList()
        messageQueue.clear()
        
        queued.forEach { message ->
            webSocket?.send(message)
        }
    }

    private fun handleIncomingMessage(message: String) {
        // Parse and route to appropriate handler
        // This would typically emit to a Flow or callback
        try {
            val json = org.json.JSONObject(message)
            val type = json.getString("type")
            
            when (type) {
                "canvas_update" -> {
                    // Handle canvas update
                    val payload = json.getString("payload")
                    onCanvasUpdateReceived(payload)
                }
                "gesture" -> {
                    // Handle gesture
                    val gestureType = json.getString("gesture_type")
                    onGestureReceived(gestureType)
                }
                "sync_complete" -> {
                    val seqNum = json.getLong("sequence_number")
                    lastSequenceNumber = seqNum
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming message", e)
        }
    }

    private var onCanvasUpdateCallback: ((String) -> Unit)? = null
    private var onGestureCallback: ((String) -> Unit)? = null

    fun setOnCanvasUpdateListener(callback: (String) -> Unit) {
        onCanvasUpdateCallback = callback
    }

    fun setOnGestureListener(callback: (String) -> Unit) {
        onGestureCallback = callback
    }

    private fun onCanvasUpdateReceived(payload: String) {
        onCanvasUpdateCallback?.invoke(payload)
    }

    private fun onGestureReceived(gestureType: String) {
        onGestureCallback?.invoke(gestureType)
    }

    fun getLastSequenceNumber(): Long = lastSequenceNumber
    
    fun updateSequenceNumber(seqNum: Long) {
        lastSequenceNumber = seqNum
    }
}

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}
