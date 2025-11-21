package com.example.overlayautoclicker

object OverlayPosition {
    // Top-left corner of the box in screen pixels
    @Volatile var x: Int = 0
    @Volatile var y: Int = 0

    // Size of the box in pixels
    @Volatile var width: Int = 300
    @Volatile var height: Int = 200

    fun update(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}
