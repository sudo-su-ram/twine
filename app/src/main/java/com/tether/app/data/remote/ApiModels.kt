package com.tether.app.data.remote

import com.tether.app.domain.model.*
import kotlinx.serialization.Serializable

/**
 * API requests and responses for server communication
 * Note: All canvas content is E2E encrypted before sending
 */

@Serializable
data class AuthRequest(
    val phoneNumberHash: String,
    val publicKey: String // Base64 encoded X25519 public key
)

@Serializable
data class AuthResponse(
    val userId: String,
    val displayName: String?,
    val isPaired: Boolean,
    val pairedWithUserId: String?
)

@Serializable
data class PairingInviteRequest(
    val inviterUserId: String,
    val partnerPhoneNumberHash: String
)

@Serializable
data class PairingInviteResponse(
    val inviteId: String,
    val inviteCode: String, // 4-word phrase
    val expiresAt: Long
)

@Serializable
data class ConfirmPairingRequest(
    val inviteId: String,
    val inviteCode: String,
    val acceptorUserId: String,
    val acceptorPublicKey: String
)

@Serializable
data class PairingConfirmResponse(
    val pairingId: String,
    val partnerUserId: String,
    val partnerPublicKey: String
)

@Serializable
data class CanvasSyncRequest(
    val pairingId: String,
    val lastSequenceNumber: Long,
    val events: List<EncryptedCanvasEvent> = emptyList()
)

@Serializable
data class CanvasSyncResponse(
    val snapshot: EncryptedCanvasSnapshot?,
    val incrementalEvents: List<EncryptedCanvasEvent>,
    val nextSequenceNumber: Long
)

@Serializable
data class EncryptedCanvasSnapshot(
    val background: String, // Encrypted JSON
    val layerA: String, // Encrypted JSON
    val layerB: String, // Encrypted JSON
    val sequenceNumber: Long
)

@Serializable
data class EncryptedCanvasEvent(
    val eventType: CanvasEventType,
    val payload: String, // Encrypted JSON of Stroke, Sticker, TextItem, or Gesture
    val sequenceNumber: Long,
    val senderUserId: String,
    val timestamp: Long
)

enum class CanvasEventType {
    STROKE,
    STICKER,
    TEXT_ITEM,
    GESTURE,
    BACKGROUND_CHANGE,
    LAYER_CLEAR
}

@Serializable
data class BackgroundUpdateRequest(
    val pairingId: String,
    val encryptedBackground: String
)

@Serializable
data class UnpairRequest(
    val pairingId: String,
    val initiatorUserId: String
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
