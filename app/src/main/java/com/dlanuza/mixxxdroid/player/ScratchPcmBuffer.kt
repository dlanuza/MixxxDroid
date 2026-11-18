package com.dlanuza.mixxxdroid.player

internal class ScratchPcmBuffer(
    private val maxFrames: Int
) {

    private var samples = ShortArray(0)

    private var channelCount = 0

    private var startFrame = 0L
    private var frameCount = 0

    fun configure(channels: Int) {
        require(channels > 0)

        if (
            channelCount == channels &&
            samples.isNotEmpty()
        ) {
            return
        }

        channelCount = channels

        samples = ShortArray(
            maxFrames * channelCount
        )

        startFrame = 0L
        frameCount = 0
    }

    fun clear() {
        startFrame = 0L
        frameCount = 0
    }

    fun append(
        input: ShortArray,
        frames: Int,
        sourceStartFrame: Long
    ) {
        if (
            frames <= 0 ||
            input.isEmpty() ||
            channelCount <= 0
        ) {
            return
        }

        if (samples.isEmpty()) {
            configure(channelCount)
        }

        val safeFrames = minOf(
            frames,
            input.size / channelCount
        )

        if (safeFrames <= 0) {
            return
        }

        if (frameCount == 0) {
            startFrame = sourceStartFrame
        }

        if (
            sourceStartFrame !=
            startFrame + frameCount
        ) {
            clear()
            startFrame = sourceStartFrame
        }

        val framesToKeep = minOf(
            safeFrames,
            maxFrames
        )

        val sourceOffsetFrames =
            safeFrames - framesToKeep

        if (frameCount + framesToKeep > maxFrames) {

            val framesToDiscard =
                frameCount +
                        framesToKeep -
                        maxFrames

            val remainingFrames =
                frameCount -
                        framesToDiscard

            if (remainingFrames > 0) {

                System.arraycopy(
                    samples,
                    framesToDiscard * channelCount,
                    samples,
                    0,
                    remainingFrames * channelCount
                )
            }

            startFrame +=
                framesToDiscard.toLong()

            frameCount = remainingFrames
        }

        System.arraycopy(
            input,
            sourceOffsetFrames * channelCount,
            samples,
            frameCount * channelCount,
            framesToKeep * channelCount
        )

        frameCount += framesToKeep
    }

    fun contains(frame: Long): Boolean {
        return frame >= startFrame &&
                frame < startFrame + frameCount
    }

    fun getFrame(
        frame: Long,
        output: ShortArray
    ): Boolean {

        if (
            output.size < channelCount ||
            !contains(frame)
        ) {
            return false
        }

        val relativeFrame =
            (frame - startFrame).toInt()

        val sourceIndex =
            relativeFrame * channelCount

        System.arraycopy(
            samples,
            sourceIndex,
            output,
            0,
            channelCount
        )

        return true
    }

    fun getStartFrame(): Long {
        return startFrame
    }

    fun getEndFrame(): Long {
        return startFrame + frameCount
    }

    fun getFrameCount(): Int {
        return frameCount
    }

    fun getChannelCount(): Int {
        return channelCount
    }
}