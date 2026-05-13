package com.tether.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tether.app.data.repository.CanvasRepository
import com.tether.app.domain.model.*
import com.tether.app.ui.geometry.Offset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val canvasRepository: CanvasRepository
) : ViewModel() {
    
    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes
    
    private val _canvasState = MutableStateFlow<CanvasState?>(null)
    val canvasState: StateFlow<CanvasState?> = _canvasState
    
    private var currentUserId: String = "user_demo_001" // Would get from auth
    private var pairingId: String = "pairing_demo_001" // Would get from active pairing
    private var sequenceNumber = 0L
    
    init {
        loadCanvas()
    }
    
    private fun loadCanvas() {
        viewModelScope.launch {
            // Initialize or load existing canvas
            val state = canvasRepository.initializeCanvas(pairingId)
            _canvasState.value = state
            
            // Load strokes for current user's layer
            canvasRepository.getStrokesForLayer(state.canvasId, currentUserId)
                .collect { strokeList ->
                    _strokes.value = strokeList
                }
        }
    }
    
    fun addStroke(points: List<Offset>, color: androidx.compose.ui.graphics.Color) {
        viewModelScope.launch {
            val strokePoints = points.map { p ->
                Stroke.Point(x = p.x, y = p.y, pressure = 1.0f)
            }
            
            val stroke = Stroke(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                points = strokePoints,
                color = color.value,
                strokeWidth = 8f,
                sequenceNumber = ++sequenceNumber
            )
            
            canvasRepository.addStroke(stroke, pairingId)
        }
    }
    
    fun addSticker(stickerId: String) {
        viewModelScope.launch {
            val sticker = Sticker(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                stickerPackId = "pack_default",
                stickerIndex = 0,
                x = 0.5f, // Center
                y = 0.5f,
                sequenceNumber = ++sequenceNumber
            )
            
            canvasRepository.addSticker(sticker, pairingId)
        }
    }
    
    fun addText(text: String) {
        viewModelScope.launch {
            val textItem = TextItem(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                content = text,
                fontId = 0,
                size = TextSize.MEDIUM,
                color = 0xFF1F2937,
                x = 0.5f,
                y = 0.5f,
                sequenceNumber = ++sequenceNumber
            )
            
            canvasRepository.addTextItem(textItem, pairingId)
        }
    }
    
    fun clearOwnLayer() {
        viewModelScope.launch {
            val state = _canvasState.value ?: return@launch
            canvasRepository.clearOwnLayer(state.canvasId, currentUserId, pairingId)
        }
    }
    
    fun updateBackground(background: CanvasBackground) {
        viewModelScope.launch {
            canvasRepository.updateBackground(background, pairingId)
        }
    }
    
    fun sendGesture(gestureType: GestureType) {
        viewModelScope.launch {
            val gesture = Gesture(
                id = UUID.randomUUID().toString(),
                senderUserId = currentUserId,
                gestureType = gestureType
            )
            
            canvasRepository.sendGesture(gesture, pairingId)
        }
    }
}
