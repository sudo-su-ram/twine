package com.tether.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tether.app.domain.model.Stroke
import com.tether.app.ui.theme.DrawingColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel = hiltViewModel()
) {
    var selectedColor by remember { mutableStateOf(DrawingColors[6]) } // Default indigo
    var selectedTool by remember { mutableStateOf(Tool.PEN) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    
    val strokes by viewModel.strokes.collectAsState()
    val canvasState by viewModel.canvasState.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Canvas background
        CanvasBackground(
            background = canvasState?.background,
            modifier = Modifier.fillMaxSize()
        )
        
        // Drawing canvas
        DrawingCanvas(
            strokes = strokes,
            currentColor = selectedColor,
            onStrokeComplete = { points ->
                if (selectedTool == Tool.PEN) {
                    viewModel.addStroke(points, selectedColor)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Toolbar at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Color palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DrawingColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = color,
                                shape = MaterialTheme.shapes.small
                            )
                            .pointerInput(color) {
                                detectDragGestures { _, _ ->
                                    selectedColor = color
                                }
                            }
                    ) {
                        if (color == selectedColor) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (color.value > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Tool bar
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Draw, contentDescription = "Draw") },
                    label = { Text("Draw") },
                    selected = selectedTool == Tool.PEN,
                    onClick = { selectedTool = Tool.PEN }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = "Stickers") },
                    label = { Text("Stickers") },
                    selected = showStickerPicker,
                    onClick = { showStickerPicker = !showStickerPicker }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Title, contentDescription = "Text") },
                    label = { Text("Text") },
                    selected = showTextDialog,
                    onClick = { showTextDialog = true }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Background") },
                    label = { Text("BG") },
                    selected = showBackgroundPicker,
                    onClick = { showBackgroundPicker = true }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Clear") },
                    label = { Text("Clear") },
                    selected = false,
                    onClick = { viewModel.clearOwnLayer() }
                )
            }
        }
        
        // Sticker picker modal
        if (showStickerPicker) {
            StickerPickerModal(
                onDismiss = { showStickerPicker = false },
                onStickerSelected = { stickerId ->
                    viewModel.addSticker(stickerId)
                    showStickerPicker = false
                }
            )
        }
        
        // Text input dialog
        if (showTextDialog) {
            TextInputDialog(
                onDismiss = { showTextDialog = false },
                onTextAdded = { text ->
                    viewModel.addText(text)
                    showTextDialog = false
                }
            )
        }
        
        // Background picker
        if (showBackgroundPicker) {
            BackgroundPickerModal(
                onDismiss = { showBackgroundPicker = false },
                onBackgroundSelected = { background ->
                    viewModel.updateBackground(background)
                    showBackgroundPicker = false
                }
            )
        }
    }
}

enum class Tool { PEN, ERASER }

@Composable
private fun DrawingCanvas(
    strokes: List<Stroke>,
    currentColor: Color,
    onStrokeComplete: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPoints = listOf(offset)
                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                },
                onDrag = { change, _ ->
                    val newOffset = change.position
                    currentPoints = currentPoints + newOffset
                    currentPath?.lineTo(newOffset.x, newOffset.y)
                    change.consume()
                },
                onDragEnd = {
                    currentPath?.let { path ->
                        onStrokeComplete(currentPoints)
                    }
                    currentPath = null
                    currentPoints = emptyList()
                }
            )
        }
    ) {
        // Draw existing strokes
        strokes.forEach { stroke ->
            if (stroke.points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    stroke.points.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color(stroke.color),
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
        
        // Draw current stroke being drawn
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun CanvasBackground(
    background: com.tether.app.domain.model.CanvasBackground?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.then(
            if (background != null) {
                when (background.type) {
                    com.tether.app.domain.model.BackgroundType.SOLID -> {
                        Modifier.background(Color(background.color ?: 0xFFFFF5E1))
                    }
                    com.tether.app.domain.model.BackgroundType.GRADIENT -> {
                        Modifier // Would implement gradient here
                    }
                    com.tether.app.domain.model.BackgroundType.PHOTO -> {
                        Modifier // Would load photo here
                    }
                }
            } else {
                Modifier.background(Color(0xFFFFF5E1))
            }
        )
    )
}

@Composable
private fun StickerPickerModal(
    onDismiss: () -> Unit,
    onStickerSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Sticker") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(300.dp)
            ) {
                items(16) { index ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "🎨",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TextInputDialog(
    onDismiss: () -> Unit,
    onTextAdded: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Enter your message") },
                maxLength = 40
            )
        },
        confirmButton = {
            Button(
                onClick = { onTextAdded(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BackgroundPickerModal(
    onDismiss: () -> Unit,
    onBackgroundSelected: (com.tether.app.domain.model.CanvasBackground) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Background") },
        text = {
            Column {
                Text("Solid Colors")
                // Would show color grid here
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
