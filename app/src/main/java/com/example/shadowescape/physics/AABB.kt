package com.example.shadowescape.physics

data class AABB(var x: Float, var y: Float, var width: Float, var height: Float) {
    val left: Float get() = x
    val right: Float get() = x + width
    val top: Float get() = y
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f

    fun intersects(other: AABB): Boolean {
        return left < other.right && right > other.left &&
                top < other.bottom && bottom > other.top
    }

    fun setPosition(nx: Float, ny: Float) {
        x = nx
        y = ny
    }
}
