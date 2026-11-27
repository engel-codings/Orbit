package de.engelcode.orbit

import android.content.Context
import kotlinx.browser.window

/**
 * Sound für die Web-Version (Web Audio). Dieselben Klänge wie auf Android –
 * sie werden mit Synth.kt im Browser erzeugt.
 */
class Sound(context: Context, private val save: Save) {

    companion object {
        const val LAUNCH = 0
        const val CAPTURE = 1
        const val PERFECT = 2
        const val STAR = 3
        const val DEATH = 4
        const val CLICK = 5
        const val BUY = 6
        const val ERROR = 7
        const val BONUS = 8
        const val WIN = 9
        const val REVIVE = 10
        const val POWER = 11
        const val CROWN = 12
        const val SHIELD = 13
        const val MUSIC = 14
        const val MUSIC2 = 15
        const val EXPLODE = 16
        const val BOOST = 17
        const val MUSIC3 = 18
        const val MUSIC4 = 19
        const val MUSIC5 = 20
        const val MUSIC6 = 21
        const val MUSIC7 = 22
        const val MUSIC8 = 23
        const val COUNT = 24
        fun trackId(t: Int) = when (t) { 1 -> MUSIC2; 2 -> MUSIC3; 3 -> MUSIC4; 4 -> MUSIC5; 5 -> MUSIC6; 6 -> MUSIC7; 7 -> MUSIC8; else -> MUSIC }
        private const val MUSIC_VOL = 0.35

        private var instance: Sound? = null

        /** Muss bei der ersten Berührung aufgerufen werden (iPhone erlaubt Ton erst dann). */
        fun unlockAll() { instance?.unlock() }
    }

    private var ac: dynamic = null
    private val buffers = arrayOfNulls<Any>(COUNT)
    private var musicSrc: dynamic = null
    private var musicPlaying = -1
    private var appPaused = false
    private var nextToMake = 0

    init { instance = this }

    private fun unlock() {
        if (ac == null) {
            ac = js("new (window.AudioContext || window.webkitAudioContext)()")
            makeNext()
        }
        try { ac.resume() } catch (_: Throwable) {}
    }

    /** Klänge nacheinander erzeugen, damit der Browser nicht hängt. */
    private fun makeNext() {
        if (nextToMake >= COUNT) return
        val id = nextToMake++
        try {
            val data = Synth.create(id)
            val buf = ac.createBuffer(1, data.size, Synth.SR)
            val ch = buf.getChannelData(0)
            for (i in data.indices) ch[i] = data[i]
            buffers[id] = buf
        } catch (_: Throwable) {}
        window.setTimeout({ makeNext(); syncMusic() }, 30)
    }

    fun play(id: Int, rate: Float = 1f, vol: Float = 1f) {
        if (!save.sound || appPaused || ac == null) return
        val b = buffers[id] ?: return
        try {
            val src = ac.createBufferSource()
            src.buffer = b
            src.playbackRate.value = rate.coerceIn(0.5f, 2f)
            val g = ac.createGain()
            g.gain.value = vol
            src.connect(g); g.connect(ac.destination)
            src.start()
        } catch (_: Throwable) {}
    }

    fun syncMusic() {
        if (ac == null) return
        val wanted = save.music && !appPaused
        val track = trackId(save.musicTrack)
        if (musicSrc != null && (!wanted || musicPlaying != track)) {
            try { musicSrc.stop() } catch (_: Throwable) {}
            musicSrc = null
        }
        if (wanted && musicSrc == null) {
            val b = buffers[track] ?: return
            try {
                val src = ac.createBufferSource()
                src.buffer = b
                src.loop = true
                val g = ac.createGain()
                g.gain.value = MUSIC_VOL
                src.connect(g); g.connect(ac.destination)
                src.start()
                musicSrc = src
                musicPlaying = track
            } catch (_: Throwable) {}
        }
    }

    fun vibrate(ms: Long) {
        if (!save.vibration) return
        try {
            val nav: dynamic = window.navigator
            if (nav.vibrate != undefined) nav.vibrate(ms.toInt())
        } catch (_: Throwable) {}
    }

    fun pauseAll() {
        appPaused = true
        try { ac?.suspend() } catch (_: Throwable) {}
    }

    fun resumeAll() {
        appPaused = false
        try { ac?.resume() } catch (_: Throwable) {}
    }

    fun release() {}
}
