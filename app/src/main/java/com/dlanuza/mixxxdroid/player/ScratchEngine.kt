package com.dlanuza.mixxxdroid.player

internal class ScratchEngine {

    @Volatile
    var isActive: Boolean = false
        private set

    @Volatile
    var positionMs: Long = 0L
        private set

    @Volatile
    var speedFactor: Float = 0.0f
        private set

    fun start(positionMs: Long) {
        this.positionMs = positionMs.coerceAtLeast(0L)
        speedFactor = 0.0f
        isActive = true
    }

    fun update(speedFactor: Float) {
        if (!isActive) return

        this.speedFactor =
            speedFactor.coerceIn(-3.0f, 3.0f)
    }

    fun setPosition(positionMs: Long) {
        this.positionMs =
            positionMs.coerceAtLeast(0L)
    }

    fun stop() {
        speedFactor = 0.0f
        isActive = false
    }
}