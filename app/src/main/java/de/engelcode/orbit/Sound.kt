package de.engelcode.orbit

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.File

/** Soundeffekte, Musik und Vibration. */
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
        const val MUSIC = 11
        const val COUNT = 12
        private const val FILE_VERSION = 1
        private const val MUSIC_VOL = 0.35f
    }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = IntArray(COUNT)
    private var musicStream = 0
    private var appPaused = false

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        val dir = context.cacheDir
        Thread({
            for (i in 0 until COUNT) {
                try {
                    val f = File(dir, "orbit_snd${i}_v$FILE_VERSION.wav")
                    if (!f.exists() || f.length() < 100) Synth.writeWav(f, Synth.create(i))
                    ids[i] = pool.load(f.path, 1)
                } catch (_: Exception) {
                }
            }
        }, "OrbitAudio").start()
    }

    fun play(id: Int, rate: Float = 1f, vol: Float = 1f) {
        if (!save.sound || appPaused) return
        val s = ids[id]
        if (s != 0) pool.play(s, vol, vol, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** Musik an/aus je nach Einstellung – wird regelmäßig aufgerufen. */
    @Synchronized
    fun syncMusic() {
        val wanted = save.music && !appPaused
        if (wanted && musicStream == 0 && ids[MUSIC] != 0) {
            musicStream = pool.play(ids[MUSIC], MUSIC_VOL, MUSIC_VOL, 0, -1, 1f)
        } else if (!wanted && musicStream != 0) {
            pool.stop(musicStream)
            musicStream = 0
        }
    }

    fun vibrate(ms: Long) {
        if (!save.vibration) return
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun pauseAll() {
        appPaused = true
        pool.autoPause()
    }

    @Synchronized
    fun resumeAll() {
        if (!appPaused) return
        appPaused = false
        pool.autoResume()
    }

    fun release() {
        pool.release()
    }
}
