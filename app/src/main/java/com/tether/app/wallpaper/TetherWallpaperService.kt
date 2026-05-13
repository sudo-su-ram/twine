package com.tether.app.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.tether.app.domain.model.CanvasBackground
import com.tether.app.domain.model.Stroke

/**
 * Live Wallpaper Service that renders the shared canvas
 */
class TetherWallpaperService : WallpaperService() {
    
    override fun onCreateEngine(): Engine = TetherWallpaperEngine()
    
    inner class TetherWallpaperEngine : Engine() {
        
        private val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        
        private var currentBitmap: Bitmap? = null
        private var backgroundColor = Color.parseColor("#FFF5E1")
        private val strokes = mutableListOf<Stroke>()
        
        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            // Initialize bitmap when surface is created
        }
        
        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            
            // Create bitmap matching wallpaper dimensions
            currentBitmap?.recycle()
            currentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            drawWallpaper()
        }
        
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                drawWallpaper()
            }
        }
        
        /**
         * Update the canvas with new data from the repository
         * This would be called when new strokes arrive via WebSocket
         */
        fun updateCanvas(background: CanvasBackground, newStrokes: List<Stroke>) {
            // Update background color
            backgroundColor = when (background.type) {
                CanvasBackground::class.java.declaredClasses.first { it.simpleName == "BackgroundType" }.declaredFields.first { it.name == "SOLID" } -> {
                    background.color ?: Color.parseColor("#FFF5E1")
                }
                else -> Color.parseColor("#FFF5E1")
            }
            
            // Update strokes
            strokes.clear()
            strokes.addAll(newStrokes)
            
            drawWallpaper()
        }
        
        private fun drawWallpaper() {
            val bitmap = currentBitmap ?: return
            val canvas = Canvas(bitmap)
            
            // Draw background
            canvas.drawColor(backgroundColor)
            
            // Draw all strokes
            strokes.forEach { stroke ->
                if (stroke.points.isNotEmpty()) {
                    paint.color = stroke.color.toInt()
                    paint.strokeWidth = stroke.strokeWidth
                    
                    val path = android.graphics.Path()
                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                    
                    stroke.points.drop(1).forEach { point ->
                        path.lineTo(point.x, point.y)
                    }
                    
                    canvas.drawPath(path, paint)
                }
            }
            
            // Render to surface
            renderToSurface(bitmap)
        }
        
        private fun renderToSurface(bitmap: Bitmap) {
            val holder = surfaceHolder
            var canvas: android.graphics.Canvas? = null
            
            try {
                canvas = holder.lockCanvas()
                canvas?.drawBitmap(bitmap, 0f, 0f, null)
            } finally {
                try {
                    canvas?.let {
                        holder.unlockCanvasAndPost(it)
                    }
                } catch (e: IllegalArgumentException) {
                    // Surface was destroyed
                }
            }
        }
        
        override fun onDestroy() {
            super.onDestroy()
            currentBitmap?.recycle()
            currentBitmap = null
        }
    }
}
