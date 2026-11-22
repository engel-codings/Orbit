package de.engelcode.orbit

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Erzeugt alle Sounds und die Musik per Code (keine Audiodateien nötig).
 * Wird einmal beim ersten Start berechnet und als WAV im Cache gespeichert.
 */
object Synth {

    const val SR = 22050
    private const val TAU = 2.0 * PI

    fun create(id: Int): FloatArray = when (id) {
        Sound.LAUNCH -> launch()
        Sound.CAPTURE -> capture()
        Sound.PERFECT -> perfect()
        Sound.STAR -> star()
        Sound.DEATH -> death()
        Sound.CLICK -> click()
        Sound.BUY -> buy()
        Sound.ERROR -> error()
        Sound.BONUS -> bonus()
        Sound.WIN -> win()
        Sound.REVIVE -> revive()
        Sound.POWER -> power()
        Sound.CROWN -> crown()
        Sound.SHIELD -> shield()
        Sound.MUSIC2 -> music2()
        else -> music()
    }

    private fun buf(sec: Double) = FloatArray((sec * SR).toInt())
    private fun att(t: Double, a: Double = 0.004) = min(1.0, t / a)

    private fun launch(): FloatArray {
        val d = 0.14; val b = buf(d); var ph = 0.0
        val rnd = java.util.Random(1)
        for (i in b.indices) {
            val t = i / SR.toDouble(); val k = t / d
            ph += TAU * (260 + 1100 * k) / SR
            val env = (1 - k) * (1 - k) * att(t)
            b[i] = ((sin(ph) * 0.5 + (rnd.nextDouble() * 2 - 1) * 0.12 * (1 - k)) * env * 0.6).toFloat()
        }
        return b
    }

    private fun capture(): FloatArray {
        val b = buf(0.2)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            val s = sin(TAU * 880 * t) * 0.6 + sin(TAU * 1320 * t) * 0.25 + sin(TAU * 1760 * t) * 0.15 * exp(-t * 30)
            b[i] = (s * exp(-t * 22) * att(t, 0.003) * 0.7).toFloat()
        }
        return b
    }

    private fun notes(d: Double, freqs: DoubleArray, step: Double, decay: Double, vol: Double, harm: Double = 0.0): FloatArray {
        val b = buf(d)
        for (i in b.indices) {
            val t = i / SR.toDouble(); var s = 0.0
            for (n in freqs.indices) {
                val st = n * step
                if (t >= st) {
                    val dt = t - st
                    s += (sin(TAU * freqs[n] * dt) + harm * sin(TAU * freqs[n] * 3 * dt)) * exp(-dt * decay) * att(dt, 0.003)
                }
            }
            b[i] = (s * vol).toFloat()
        }
        return b
    }

    private fun perfect() = notes(0.6, doubleArrayOf(1047.0, 1319.0, 1568.0, 2093.0), 0.06, 9.0, 0.26)
    private fun bonus() = notes(0.9, doubleArrayOf(784.0, 988.0, 1175.0, 1568.0, 1976.0), 0.08, 7.0, 0.24)
    private fun win() = notes(1.3, doubleArrayOf(523.0, 659.0, 784.0, 1047.0, 1319.0, 1568.0), 0.1, 4.5, 0.2, 0.2)
    private fun revive() = notes(0.7, doubleArrayOf(392.0, 523.0, 659.0, 784.0), 0.07, 6.0, 0.25, 0.15)

    private fun power(): FloatArray {
        val d = 0.45; val b = buf(d); var ph = 0.0
        for (i in b.indices) {
            val t = i / SR.toDouble(); val k = t / d
            ph += TAU * (500 + 1500 * k * k) / SR
            val trem = 0.75 + 0.25 * sin(TAU * 18 * t)
            b[i] = ((sin(ph) + 0.3 * sin(2 * ph)) * trem * (1 - k) * att(t) * 0.35).toFloat()
        }
        return b
    }

    private fun crown(): FloatArray {
        val b = buf(0.9)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            val s = sin(TAU * 1568 * t) * 0.5 + sin(TAU * 2093 * t) * 0.3 + sin(TAU * 3136 * t) * 0.15
            b[i] = (s * exp(-t * 5) * att(t, 0.002) * 0.5).toFloat()
        }
        return b
    }

    private fun shield(): FloatArray {
        val b = buf(0.5); var lp = 0.0
        val rnd = java.util.Random(3)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            lp += ((rnd.nextDouble() * 2 - 1) - lp) * 0.5
            val s = sin(TAU * 2637 * t) * 0.4 + sin(TAU * 3520 * t) * 0.25 + lp * 0.4 * exp(-t * 20)
            b[i] = (s * exp(-t * 7) * att(t, 0.002) * 0.45).toFloat()
        }
        return b
    }

    private fun star(): FloatArray {
        val b = buf(0.32)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            val trem = 0.7 + 0.3 * sin(TAU * 28 * t)
            val s = sin(TAU * 1568 * t) * 0.5 + sin(TAU * 2349 * t) * 0.35
            b[i] = (s * exp(-t * 9) * trem * att(t) * 0.6).toFloat()
        }
        return b
    }

    private fun death(): FloatArray {
        val b = buf(0.8); var ph = 0.0; var lp = 0.0
        val rnd = java.util.Random(2)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            ph += TAU * (170 * exp(-t * 2.2) + 30) / SR
            lp += ((rnd.nextDouble() * 2 - 1) - lp) * 0.08
            b[i] = ((sin(ph) * 0.7 + lp * 0.9) * exp(-t * 3.5) * att(t) * 0.8).toFloat()
        }
        return b
    }

    private fun click(): FloatArray {
        val b = buf(0.06)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            b[i] = (sin(TAU * 1400 * t) * exp(-t * 90) * 0.45).toFloat()
        }
        return b
    }

    private fun buy(): FloatArray {
        val b = buf(0.45)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            val s = if (t < 0.09) {
                val x = TAU * 988 * t
                (sin(x) + 0.25 * sin(3 * x)) * att(t)
            } else {
                val dt = t - 0.09; val x = TAU * 1319 * dt
                (sin(x) + 0.25 * sin(3 * x)) * exp(-dt * 7) * att(dt)
            }
            b[i] = (s * 0.3).toFloat()
        }
        return b
    }

    private fun error(): FloatArray {
        val d = 0.28; val b = buf(d)
        for (i in b.indices) {
            val t = i / SR.toDouble()
            val gate = if ((t % 0.14) < 0.1) 1.0 else 0.0
            val x = TAU * 150 * t
            b[i] = ((sin(x) + 0.3 * sin(3 * x) + 0.2 * sin(5 * x)) * gate * (1 - t / d) * 0.3).toFloat()
        }
        return b
    }

    /** Ruhige 16-Sekunden-Weltraum-Musik, nahtlos loopbar (Am – F – C – G). */
    private fun music(): FloatArray {
        val len = 16.0
        val b = buf(len)
        val chords = arrayOf(
            doubleArrayOf(220.0, 261.63, 329.63),
            doubleArrayOf(174.61, 220.0, 261.63),
            doubleArrayOf(261.63, 329.63, 392.0),
            doubleArrayOf(196.0, 246.94, 293.66)
        )
        val roots = doubleArrayOf(110.0, 87.31, 130.81, 98.0)
        fun q(f: Double) = Math.round(f * len) / len   // nahtloser Loop
        val padA = Array(4) { k -> DoubleArray(3) { q(chords[k][it]) } }
        val padB = Array(4) { k -> DoubleArray(3) { q(chords[k][it] * 1.004) } }
        val rootQ = DoubleArray(4) { q(roots[it]) }
        val pattern = intArrayOf(0, 1, 2, 1, 0, 2, 1, 2)
        val pluck = DoubleArray(32) { j -> chords[j / 8][pattern[j % 8]] * 2.0 }

        for (i in b.indices) {
            val t = i / SR.toDouble()
            var s = 0.0
            for (k in 0 until 4) {
                val local = ((t - k * 4 + 1 + len) % len)
                if (local < 6.0) {
                    val env = when {
                        local < 2.0 -> (1 - Math.cos(PI * local / 2)) / 2
                        local > 4.0 -> (1 - Math.cos(PI * (6 - local) / 2)) / 2
                        else -> 1.0
                    }
                    var p = 0.0
                    for (n in 0 until 3) p += sin(TAU * padA[k][n] * t) + sin(TAU * padB[k][n] * t)
                    s += (p * 0.045 + sin(TAU * rootQ[k] * t) * 0.10) * env
                }
            }
            val jc = floor(t / 0.5).toInt()
            for (o in 0..1) {
                val j = ((jc - o) % 32 + 32) % 32
                val dt = ((t - j * 0.5) % len + len) % len
                if (dt < 1.0) s += sin(TAU * pluck[j] * dt) * exp(-dt * 5) * att(dt, 0.005) * 0.06
            }
            b[i] = s.toFloat()
        }
        return b
    }

    /** Zweites Musikstück „Puls“: 16 s, Dm – B – F – C, schnellere Arpeggios und Bass-Puls. */
    private fun music2(): FloatArray {
        val len = 16.0
        val b = buf(len)
        val chords = arrayOf(
            doubleArrayOf(293.66, 349.23, 440.0),
            doubleArrayOf(233.08, 293.66, 349.23),
            doubleArrayOf(349.23, 440.0, 523.25),
            doubleArrayOf(261.63, 329.63, 392.0)
        )
        val roots = doubleArrayOf(73.42, 58.27, 87.31, 65.41)
        fun q(f: Double) = Math.round(f * len) / len
        val padA = Array(4) { k -> DoubleArray(3) { q(chords[k][it]) } }
        val rootQ = DoubleArray(4) { q(roots[it]) }
        val pattern = intArrayOf(0, 1, 2, 1, 2, 0, 1, 2)
        val arp = DoubleArray(64) { j -> chords[(j / 16) % 4][pattern[j % 8]] * 2.0 }
        for (i in b.indices) {
            val t = i / SR.toDouble()
            var s = 0.0
            for (k in 0 until 4) {
                val local = ((t - k * 4 + 1 + len) % len)
                if (local < 6.0) {
                    val env = when {
                        local < 2.0 -> (1 - Math.cos(PI * local / 2)) / 2
                        local > 4.0 -> (1 - Math.cos(PI * (6 - local) / 2)) / 2
                        else -> 1.0
                    }
                    var p = 0.0
                    for (n in 0 until 3) p += sin(TAU * padA[k][n] * t)
                    s += p * 0.035 * env
                }
            }
            // Bass-Puls auf jeder Viertel
            val sec = ((t / 4.0).toInt()) % 4
            val bt = t % 0.5
            s += sin(TAU * rootQ[sec] * t) * exp(-bt * 6) * 0.16
            // Arpeggio auf jeder Achtel
            val jc = floor(t / 0.25).toInt()
            for (o in 0..1) {
                val j = ((jc - o) % 64 + 64) % 64
                val dt = ((t - j * 0.25) % len + len) % len
                if (dt < 0.6) s += sin(TAU * arp[j] * dt) * exp(-dt * 7) * att(dt, 0.004) * 0.055
            }
            b[i] = s.toFloat()
        }
        return b
    }

    fun writeWav(file: File, data: FloatArray) {
        val n = data.size
        val bb = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + n * 2); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(SR); bb.putInt(SR * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(n * 2)
        for (v in data) bb.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use { it.write(bb.array()) }
        tmp.renameTo(file)
    }
}
