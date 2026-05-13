package com.tether.app.domain.model

import kotlinx.serialization.Serializable

/**
 * User account model - represents a local user
 */
@Serializable
data class User(
    val id: String,
    val phoneNumberHash: String,
    val displayName: String? = null,
    val accentColor: Long = 0xFF6366F1, // Default indigo
    val createdAt: Long = System.currentTimeMillis(),
    val isPaired: Boolean = false,
    val pairedWithUserId: String? = null
)

/**
 * Partner relationship model
 */
@Serializable
data class Pairing(
    val id: String,
    val localUserId: String,
    val partnerUserId: String,
    val partnerPhoneNumberHash: String,
    val partnerDisplayName: String? = null,
    val pairingCode: String, // 4-word confirmation phrase
    val status: PairingStatus = PairingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val pairedAt: Long? = null
)

enum class PairingStatus {
    PENDING,
    ACTIVE,
    DISSOLVED
}

/**
 * Canvas layer - each user has their own layer
 */
@Serializable
data class CanvasLayer(
    val userId: String,
    val strokes: List<Stroke> = emptyList(),
    val stickers: List<Sticker> = emptyList(),
    val textItems: List<TextItem> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * A single drawing stroke
 */
@Serializable
data class Stroke(
    val id: String,
    val userId: String,
    val points: List<Point>,
    val color: Long,
    val strokeWidth: Float,
    val sequenceNumber: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    @Serializable
    data class Point(
        val x: Float,
        val y: Float,
        val pressure: Float = 1.0f
    )
}

/**
 * Sticker placed on canvas
 */
@Serializable
data class Sticker(
    val id: String,
    val userId: String,
    val stickerPackId: String,
    val stickerIndex: Int,
    val x: Float, // Relative 0.0-1.0
    val y: Float, // Relative 0.0-1.0
    val rotation: Float = 0f,
    val scale: Float = 1.0f,
    val sequenceNumber: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Text item on canvas
 */
@Serializable
data class TextItem(
    val id: String,
    val userId: String,
    val content: String,
    val fontId: Int, // 0, 1, 2 for handwriting fonts
    val size: TextSize,
    val color: Long,
    val x: Float, // Relative 0.0-1.0
    val y: Float, // Relative 0.0-1.0
    val rotation: Float = 0f,
    val sequenceNumber: Long,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TextSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Gesture (ephemeral animation)
 */
@Serializable
data class Gesture(
    val id: String,
    val senderUserId: String,
    val gestureType: GestureType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GestureType {
    HEART,
    DOUBLE_TAP
}

/**
 * Canvas background
 */
@Serializable
data class CanvasBackground(
    val type: BackgroundType,
    val color: Long? = null, // For solid color
    val gradientStart: Long? = null, // For gradient
    val gradientEnd: Long? = null,
    val photoUri: String? = null, // For photo background
    val setByUserId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BackgroundType {
    SOLID,
    GRADIENT,
    PHOTO
}

/**
 * Complete canvas state
 */
@Serializable
data class CanvasState(
    val canvasId: String,
    val pairingId: String,
    val background: CanvasBackground,
    val layerA: CanvasLayer,
    val layerB: CanvasLayer,
    val lastResetTime: Long,
    val resetSchedule: ResetSchedule = ResetSchedule.DAILY,
    val snapshotSequenceNumber: Long
)

enum class ResetSchedule {
    DAILY,
    WEEKLY,
    MANUAL
}

/**
 * Canvas archive entry
 */
@Serializable
data class CanvasArchiveEntry(
    val id: String,
    val canvasId: String,
    val pairingId: String,
    val archivedAt: Long,
    val thumbnailData: String? = null, // Base64 encoded thumbnail
    val eventCount: Int = 0
)
