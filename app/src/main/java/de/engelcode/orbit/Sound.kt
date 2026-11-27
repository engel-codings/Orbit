package de.engelcode.orbit

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
        private const val FILE_VERSION = 5
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
    private var musicPlaying = -1
    private var appPaused = false

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        val dir = context.cacheDir
        Thread({
            for (i in 0 until COUNT) {
                try {
                    val f = File(dir, "orbit_snd${i}_v$FILE_VERSION.wav")
                    if (!f.exists() || f.length() < 100) writeWav(f, Synth.create(i))
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
        val track = trackId(save.musicTrack)
        if (musicStream != 0 && (!wanted || musicPlaying != track)) {
            pool.stop(musicStream)
            musicStream = 0
        }
        if (wanted && musicStream == 0 && ids[track] != 0) {
            musicStream = pool.play(ids[track], MUSIC_VOL, MUSIC_VOL, 0, -1, 1f)
            if (musicStream != 0) musicPlaying = track
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

    private fun writeWav(file: File, data: FloatArray) {
        val n = data.size
        val bb = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + n * 2); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(Synth.SR); bb.putInt(Synth.SR * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(n * 2)
        for (v in data) bb.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use { it.write(bb.array()) }
        tmp.renameTo(file)
    }
}
