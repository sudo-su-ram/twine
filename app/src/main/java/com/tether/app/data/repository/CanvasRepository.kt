package com.tether.app.data.repository

import com.tether.app.data.local.CanvasDao
import com.tether.app.data.local.PendingStroke
import com.tether.app.data.remote.*
import com.tether.app.domain.model.*
import com.tether.app.util.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for canvas operations and synchronization
 */
@Singleton
class CanvasRepository @Inject constructor(
    private val canvasDao: CanvasDao,
    private val apiService: TetherApiService,
    private val encryptionManager: EncryptionManager,
    private val webSocketManager: WebSocketManager,
    private val userRepository: UserRepository
) {
    private val mutex = Mutex()
    private var sequenceNumber = 0L
    
    /**
     * Get canvas state for active pairing
     */
    fun getCanvasState(pairingId: String): Flow<CanvasState?> = 
        canvasDao.getCanvasState(pairingId)
    
    suspend fun getCanvasStateSync(pairingId: String): CanvasState? = 
        canvasDao.getCanvasStateSync(pairingId)
    
    /**
     * Initialize or create new canvas for pairing
     */
    suspend fun initializeCanvas(pairingId: String): CanvasState {
        return mutex.withLock {
            val existing = canvasDao.getCanvasStateSync(pairingId)
            if (existing != null) {
                existing
            } else {
                val currentUser = userRepository.getCurrentUserSync()
                    ?: throw IllegalStateException("No user logged in")
                
                val defaultBackground = CanvasBackground(
                    type = BackgroundType.SOLID,
                    color = 0xFFFFF5E1 // Warm cream default
                )
                
                val initialState = CanvasState(
                    canvasId = "canvas_$pairingId",
                    pairingId = pairingId,
                    background = defaultBackground,
                    layerA = CanvasLayer(userId = currentUser.id),
                    layerB = CanvasLayer(userId = ""), // Will be set when partner joins
                    lastResetTime = System.currentTimeMillis(),
                    resetSchedule = ResetSchedule.DAILY,
                    snapshotSequenceNumber = 0
                )
                
                canvasDao.insertCanvasState(initialState)
                initialState
            }
        }
    }
    
    /**
     * Add a stroke to the canvas
     */
    suspend fun addStroke(stroke: Stroke, pairingId: String): Result<Unit> {
        return mutex.withLock {
            try {
                // Save locally immediately (optimistic)
                canvasDao.insertStroke(stroke)
                
                // Encrypt and send to server
                val pairing = userRepository.getActivePairingSync()
                    ?: return@withLock Result.failure(IllegalStateException("No active pairing"))
                
                val partnerPublicKey = getPartnerPublicKey()
                    ?: return@withLock Result.failure(IllegalStateException("Partner public key not found"))
                
                val encryptedPayload = encryptionManager.encryptEvent(stroke, partnerPublicKey)
                
                val event = EncryptedCanvasEvent(
                    eventType = CanvasEventType.STROKE,
                    payload = encryptedPayload,
                    sequenceNumber = ++sequenceNumber,
                    senderUserId = stroke.userId,
                    timestamp = System.currentTimeMillis()
                )
                
                // Send via WebSocket
                val jsonEvent = kotlinx.serialization.json.Json.encodeToString(event)
                webSocketManager.sendMessage(jsonEvent)
                
                Result.success(Unit)
            } catch (e: Exception) {
                // Queue for retry
                queuePendingStroke(stroke, pairingId)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add a sticker to the canvas
     */
    suspend fun addSticker(sticker: Sticker, pairingId: String): Result<Unit> {
        return mutex.withLock {
            try {
                canvasDao.insertSticker(sticker)
                
                val partnerPublicKey = getPartnerPublicKey()
                    ?: return@withLock Result.failure(IllegalStateException("Partner public key not found"))
                
                val encryptedPayload = encryptionManager.encryptEvent(sticker, partnerPublicKey)
                
                val event = EncryptedCanvasEvent(
                    eventType = CanvasEventType.STICKER,
                    payload = encryptedPayload,
                    sequenceNumber = ++sequenceNumber,
                    senderUserId = sticker.userId,
                    timestamp = System.currentTimeMillis()
                )
                
                val jsonEvent = kotlinx.serialization.json.Json.encodeToString(event)
                webSocketManager.sendMessage(jsonEvent)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add text to the canvas
     */
    suspend fun addTextItem(textItem: TextItem, pairingId: String): Result<Unit> {
        return mutex.withLock {
            try {
                canvasDao.insertTextItem(textItem)
                
                val partnerPublicKey = getPartnerPublicKey()
                    ?: return@withLock Result.failure(IllegalStateException("Partner public key not found"))
                
                val encryptedPayload = encryptionManager.encryptEvent(textItem, partnerPublicKey)
                
                val event = EncryptedCanvasEvent(
                    eventType = CanvasEventType.TEXT_ITEM,
                    payload = encryptedPayload,
                    sequenceNumber = ++sequenceNumber,
                    senderUserId = textItem.userId,
                    timestamp = System.currentTimeMillis()
                )
                
                val jsonEvent = kotlinx.serialization.json.Json.encodeToString(event)
                webSocketManager.sendMessage(jsonEvent)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clear user's own layer
     */
    suspend fun clearOwnLayer(canvasId: String, userId: String, pairingId: String): Result<Unit> {
        return mutex.withLock {
            try {
                canvasDao.clearLayer(canvasId, userId)
                
                val partnerPublicKey = getPartnerPublicKey()
                    ?: return@withLock Result.failure(IllegalStateException("Partner public key not found"))
                
                // Create clear event
                val clearEvent = mapOf(
                    "userId" to userId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val encryptedPayload = encryptionManager.encryptEvent(clearEvent, partnerPublicKey)
                
                val event = EncryptedCanvasEvent(
                    eventType = CanvasEventType.LAYER_CLEAR,
                    payload = encryptedPayload,
                    sequenceNumber = ++sequenceNumber,
                    senderUserId = userId,
                    timestamp = System.currentTimeMillis()
                )
                
                val jsonEvent = kotlinx.serialization.json.Json.encodeToString(event)
                webSocketManager.sendMessage(jsonEvent)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update canvas background
     */
    suspend fun updateBackground(background: CanvasBackground, pairingId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUserSync()
                ?: return Result.failure(IllegalStateException("No user logged in"))
            
            val updatedBackground = background.copy(
                setByUserId = currentUser.id,
                timestamp = System.currentTimeMillis()
            )
            
            val partnerPublicKey = getPartnerPublicKey()
                ?: return Result.failure(IllegalStateException("Partner public key not found"))
            
            val encryptedBackground = encryptionManager.encryptEvent(updatedBackground, partnerPublicKey)
            
            val request = BackgroundUpdateRequest(
                pairingId = pairingId,
                encryptedBackground = encryptedBackground
            )
            
            val response = apiService.updateBackground(request)
            
            if (response.isSuccessful) {
                // Update local canvas state
                val canvasState = canvasDao.getCanvasStateSync(pairingId)
                if (canvasState != null) {
                    val updatedState = canvasState.copy(background = updatedBackground)
                    canvasDao.updateCanvasState(updatedState)
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update background"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send gesture to partner
     */
    suspend fun sendGesture(gesture: Gesture, pairingId: String): Result<Unit> {
        return try {
            val partnerPublicKey = getPartnerPublicKey()
                ?: return Result.failure(IllegalStateException("Partner public key not found"))
            
            val encryptedPayload = encryptionManager.encryptEvent(gesture, partnerPublicKey)
            
            val event = EncryptedCanvasEvent(
                eventType = CanvasEventType.GESTURE,
                payload = encryptedPayload,
                sequenceNumber = ++sequenceNumber,
                senderUserId = gesture.senderUserId,
                timestamp = gesture.timestamp
            )
            
            val jsonEvent = kotlinx.serialization.json.Json.encodeToString(event)
            webSocketManager.sendMessage(jsonEvent)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync canvas with server
     */
    suspend fun syncCanvas(pairingId: String): Result<CanvasSyncResponse> {
        return try {
            val lastSeqNum = webSocketManager.getLastSequenceNumber()
            
            val request = CanvasSyncRequest(
                pairingId = pairingId,
                lastSequenceNumber = lastSeqNum
            )
            
            val response = apiService.syncCanvas(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Sync failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process incoming canvas events from WebSocket
     */
    suspend fun processIncomingEvent(event: EncryptedCanvasEvent, pairingId: String) {
        try {
            val partnerPublicKey = getPartnerPublicKey()
                ?: return
            
            when (event.eventType) {
                CanvasEventType.STROKE -> {
                    val stroke = encryptionManager.decryptEvent<Stroke>(event.payload, partnerPublicKey)
                    canvasDao.insertStroke(stroke)
                }
                CanvasEventType.STICKER -> {
                    val sticker = encryptionManager.decryptEvent<Sticker>(event.payload, partnerPublicKey)
                    canvasDao.insertSticker(sticker)
                }
                CanvasEventType.TEXT_ITEM -> {
                    val textItem = encryptionManager.decryptEvent<TextItem>(event.payload, partnerPublicKey)
                    canvasDao.insertTextItem(textItem)
                }
                CanvasEventType.GESTURE -> {
                    // Handle gesture - show animation
                }
                CanvasEventType.BACKGROUND_CHANGE -> {
                    val background = encryptionManager.decryptEvent<CanvasBackground>(event.payload, partnerPublicKey)
                    // Update background
                }
                CanvasEventType.LAYER_CLEAR -> {
                    val data = encryptionManager.decryptEvent<Map<String, Any>>(event.payload, partnerPublicKey)
                    val userId = data["userId"] as String
                    val canvasState = canvasDao.getCanvasStateSync(pairingId)
                    if (canvasState != null) {
                        canvasDao.clearLayer(canvasState.canvasId, userId)
                    }
                }
            }
            
            webSocketManager.updateSequenceNumber(event.sequenceNumber)
        } catch (e: Exception) {
            // Handle decryption error
        }
    }
    
    /**
     * Get strokes for user's layer
     */
    fun getStrokesForLayer(canvasId: String, userId: String): Flow<List<Stroke>> =
        canvasDao.getStrokesForLayer(canvasId, userId)
    
    /**
     * Get stickers for user's layer
     */
    suspend fun getStickersForLayer(canvasId: String, userId: String): List<Sticker> =
        canvasDao.getStickersForLayerSync(canvasId, userId)
    
    /**
     * Get text items for user's layer
     */
    suspend fun getTextItemsForLayer(canvasId: String, userId: String): List<TextItem> =
        canvasDao.getTextItemsForLayerSync(canvasId, userId)
    
    private suspend fun queuePendingStroke(stroke: Stroke, canvasId: String) {
        val pendingStroke = PendingStroke(
            strokeJson = kotlinx.serialization.json.Json.encodeToString(stroke),
            canvasId = canvasId
        )
        canvasDao.insertPendingStroke(pendingStroke)
    }
    
    private suspend fun getPartnerPublicKey(): ByteArray? {
        val pairing = userRepository.getActivePairingSync()
            ?: return null
        
        // In production, this would be retrieved from secure storage
        // For now, we'd need to fetch from server or cache
        return null // TODO: Implement proper key retrieval
    }
}
