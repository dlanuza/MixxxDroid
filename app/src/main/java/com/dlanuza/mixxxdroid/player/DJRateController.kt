package com.dlanuza.mixxxdroid.player

import kotlin.math.pow

class DJRateController {

// =========================================================
// ESTADO DJ
// =========================================================

    private var tempoRatio = 1.0f

    private var pitchAdjustSemitones = 0.0f

    private var keylock = false

// =========================================================
// RESULTADO
// =========================================================

    data class RateResult(
        val tempoRatio: Float,
        val pitchRatio: Float,
        val keylock: Boolean
    )

// =========================================================
// TEMPO
// =========================================================

    fun setTempo(
        ratio: Float
    ) {

        tempoRatio =
            ratio.coerceIn(
                0.8f,
                1.2f
            )
    }

// =========================================================
// PITCH ADJUST
// =========================================================

    fun setPitchAdjust(
        semitones: Float
    ) {

        pitchAdjustSemitones =
            semitones.coerceIn(
                -3.0f,
                3.0f
            )
    }

// =========================================================
// KEYLOCK
// =========================================================

    fun setKeylock(
        enabled: Boolean
    ) {

        keylock = enabled
    }

// =========================================================
// CALCULATE
// =========================================================
//
// Devuelve los parametros que debe utilizar el motor
// de audio.
//
// TEMPO:
//   0.8 .. 1.2
//
// PITCH:
//   semitonos -> ratio
//
// KEYLOCK OFF:
//   el tempo modifica tambien la tonalidad.
//
// KEYLOCK ON:
//   el tempo NO modifica la tonalidad.
//
// =========================================================

    fun calculate(): RateResult {

        val pitchAdjustRatio =
            2.0.pow(
                pitchAdjustSemitones.toDouble() / 12.0
            ).toFloat()

        val pitchRatio =
            if (keylock) {

                pitchAdjustRatio

            } else {

                tempoRatio *
                        pitchAdjustRatio
            }

        return RateResult(
            tempoRatio = tempoRatio,
            pitchRatio = pitchRatio,
            keylock = keylock
        )
    }

// =========================================================
// GETTERS
// =========================================================

    fun getTempo(): Float {
        return tempoRatio
    }

    fun getPitchAdjust(): Float {
        return pitchAdjustSemitones
    }

    fun isKeylockEnabled(): Boolean {
        return keylock
    }
}
