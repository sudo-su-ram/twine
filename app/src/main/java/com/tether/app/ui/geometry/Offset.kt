package com.tether.app.ui.geometry

/**
 * Simple Offset class for geometry operations
 * This mirrors androidx.compose.ui.geometry.Offset but is serializable
 */
data class Offset(
    val x: Float,
    val y: Float
) {
    companion object {
        val Zero = Offset(0f, 0f)
    }
    
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(scale: Float): Offset = Offset(x * scale, y * scale)
    operator fun div(scale: Float): Offset = Offset(x / scale, y / scale)
    
    fun distanceTo(other: Offset): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    fun getDistance(): Float = kotlin.math.sqrt(x * x + y * y)
}
