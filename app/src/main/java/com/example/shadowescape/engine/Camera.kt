package com.example.shadowescape.engine

class Camera(var viewportWidth: Float, var viewportHeight: Float) {
    var x: Float = 0f
    var y: Float = 0f

    fun follow(targetX: Float, targetY: Float, levelWidth: Float, levelHeight: Float) {
        val desiredX = targetX - viewportWidth / 2f
        val desiredY = targetY - viewportHeight / 2f

        x += (desiredX - x) * 0.1f
        y += (desiredY - y) * 0.1f

        val maxX = maxOf(0f, levelWidth - viewportWidth)
        val maxY = maxOf(0f, levelHeight - viewportHeight)

        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }

    fun updateViewport(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
    }
}
