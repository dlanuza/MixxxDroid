package com.dlanuza.mixxxdroid.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class EqBand { LOW, MID, HIGH }

/**
 * Ecualizador de 3 bandas para un deck (graves, medios, agudos).
 *
 *  - LOW : shelf de graves en 250 Hz
 *  - MID : filtro de campana en 1 kHz
 *  - HIGH: shelf de agudos en 3,5 kHz
 *
 * Rango: de -24 dB (kill) a +6 dB. Con las tres bandas en 0 dB se salta por completo
 * (no gasta CPU ni toca el audio).
 *
 * Los valores se cambian desde el hilo principal; los coeficientes se recalculan en el
 * hilo de audio, dentro de process(). Fórmulas: "Audio EQ Cookbook" de R. Bristow-Johnson.
 */
internal class ThreeBandEq {

    companion object {
        const val MIN_DB = -24f
        const val MAX_DB = 6f

        private const val KILL_DB = -60.0
        private const val LOW_HZ = 250.0
        private const val MID_HZ = 1000.0
        private const val HIGH_HZ = 3500.0
        private const val MID_Q = 0.7
    }

    private class Biquad {
        var b0 = 1.0
        var b1 = 0.0
        var b2 = 0.0
        var a1 = 0.0
        var a2 = 0.0

        fun set(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) {
            this.b0 = b0 / a0
            this.b1 = b1 / a0
            this.b2 = b2 / a0
            this.a1 = a1 / a0
            this.a2 = a2 / a0
        }
    }

    @Volatile private var lowDb = 0f
    @Volatile private var midDb = 0f
    @Volatile private var highDb = 0f

    private var appliedLow = Float.NaN
    private var appliedMid = Float.NaN
    private var appliedHigh = Float.NaN
    private var appliedSampleRate = 0
    private var wasBypassed = true

    private val low = Biquad()
    private val mid = Biquad()
    private val high = Biquad()

    // Estado de los filtros: [canal][banda * 2 + 0/1]. Hasta 2 canales.
    private val state = Array(2) { DoubleArray(6) }

    fun setGains(lowDb: Float, midDb: Float, highDb: Float) {
        this.lowDb = lowDb
        this.midDb = midDb
        this.highDb = highDb
    }

    fun reset() {
        for (s in state) s.fill(0.0)
    }

    fun process(buffer: ShortArray, shortCount: Int, channels: Int, sampleRate: Int) {
        if (sampleRate <= 0 || shortCount <= 0) return

        val l = lowDb
        val m = midDb
        val h = highDb

        // Todo a 0 dB: no hacer nada.
        if (l == 0f && m == 0f && h == 0f) {
            if (!wasBypassed) {
                reset()
                wasBypassed = true
            }
            return
        }
        wasBypassed = false

        if (l != appliedLow || m != appliedMid || h != appliedHigh || sampleRate != appliedSampleRate) {
            lowShelf(low, sampleRate, effectiveDb(l))
            peak(mid, sampleRate, effectiveDb(m))
            highShelf(high, sampleRate, effectiveDb(h))
            appliedLow = l
            appliedMid = m
            appliedHigh = h
            appliedSampleRate = sampleRate
        }

        val ch = channels.coerceIn(1, 2)
        var i = 0
        while (i < shortCount) {
            for (c in 0 until ch) {
                if (i + c >= shortCount) break

                var x = buffer[i + c].toDouble()
                val s = state[c]

                var y = low.b0 * x + s[0]
                s[0] = low.b1 * x - low.a1 * y + s[1]
                s[1] = low.b2 * x - low.a2 * y
                x = y

                y = mid.b0 * x + s[2]
                s[2] = mid.b1 * x - mid.a1 * y + s[3]
                s[3] = mid.b2 * x - mid.a2 * y
                x = y

                y = high.b0 * x + s[4]
                s[4] = high.b1 * x - high.a1 * y + s[5]
                s[5] = high.b2 * x - high.a2 * y
                x = y

                buffer[i + c] = x.coerceIn(-32768.0, 32767.0).roundToInt().toShort()
            }
            i += ch
        }
    }

    // En el mínimo del control (-24 dB) se corta la banda casi por completo.
    private fun effectiveDb(db: Float): Double =
        if (db <= MIN_DB + 0.05f) KILL_DB
        else db.toDouble().coerceIn(MIN_DB.toDouble(), MAX_DB.toDouble())

    private fun lowShelf(f: Biquad, fs: Int, db: Double) {
        val a = 10.0.pow(db / 40.0)
        val w0 = 2.0 * PI * LOW_HZ / fs
        val cosW = cos(w0)
        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val k = 2.0 * sqrt(a) * alpha
        f.set(
            a * ((a + 1) - (a - 1) * cosW + k),
            2.0 * a * ((a - 1) - (a + 1) * cosW),
            a * ((a + 1) - (a - 1) * cosW - k),
            (a + 1) + (a - 1) * cosW + k,
            -2.0 * ((a - 1) + (a + 1) * cosW),
            (a + 1) + (a - 1) * cosW - k
        )
    }

    private fun highShelf(f: Biquad, fs: Int, db: Double) {
        val a = 10.0.pow(db / 40.0)
        val w0 = 2.0 * PI * HIGH_HZ / fs
        val cosW = cos(w0)
        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val k = 2.0 * sqrt(a) * alpha
        f.set(
            a * ((a + 1) + (a - 1) * cosW + k),
            -2.0 * a * ((a - 1) + (a + 1) * cosW),
            a * ((a + 1) + (a - 1) * cosW - k),
            (a + 1) - (a - 1) * cosW + k,
            2.0 * ((a - 1) - (a + 1) * cosW),
            (a + 1) - (a - 1) * cosW - k
        )
    }

    private fun peak(f: Biquad, fs: Int, db: Double) {
        val a = 10.0.pow(db / 40.0)
        val w0 = 2.0 * PI * MID_HZ / fs
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * MID_Q)
        f.set(
            1.0 + alpha * a,
            -2.0 * cosW,
            1.0 - alpha * a,
            1.0 + alpha / a,
            -2.0 * cosW,
            1.0 - alpha / a
        )
    }
}