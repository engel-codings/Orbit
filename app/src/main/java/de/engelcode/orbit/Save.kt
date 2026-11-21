package de.engelcode.orbit

import android.content.Context

/** Spielstand und Einstellungen (bleiben bei Updates erhalten). */
class Save(context: Context) {

    private val p = context.getSharedPreferences("orbit", Context.MODE_PRIVATE)

    // Fortschritt
    var best = p.getInt("best", 0)
    var stars = p.getInt("stars", 0)
    var skin = p.getInt("skin", 0)
    var trail = p.getInt("trail", 0)
    var ownedSkins = p.getInt("ownedSkins", 1)
    var ownedTrails = p.getInt("ownedTrails", 1)
    var revives = p.getInt("revives", 0)
    var levelDone = p.getInt("levelDone", 0)      // höchstes geschafftes Level

    // Einstellungen
    var sound = p.getBoolean("sound", true)
    var music = p.getBoolean("music", true)
    var vibration = p.getBoolean("vibration", true)
    var showFps = p.getBoolean("showFps", true)
    var lowGfx = p.getBoolean("lowGfx", false)

    // Statistik
    var games = p.getInt("games", 0)
    var planets = p.getInt("planets", 0)
    var perfects = p.getInt("perfects", 0)
    var bestStreak = p.getInt("bestStreak", 0)
    var starsCollected = p.getInt("starsCollected", 0)
    var playTime = p.getFloat("playTime", 0f)

    // Täglicher Bonus / Versionen
    var lastDay = p.getLong("lastDay", 0L)
    var dayStreak = p.getInt("dayStreak", 0)
    var lastVersion = p.getInt("lastVersion", 0)

    fun ownsSkin(i: Int) = ownedSkins and (1 shl i) != 0
    fun ownsTrail(i: Int) = ownedTrails and (1 shl i) != 0

    fun flush() {
        p.edit()
            .putInt("best", best).putInt("stars", stars)
            .putInt("skin", skin).putInt("trail", trail)
            .putInt("ownedSkins", ownedSkins).putInt("ownedTrails", ownedTrails)
            .putInt("revives", revives).putInt("levelDone", levelDone)
            .putBoolean("sound", sound).putBoolean("music", music)
            .putBoolean("vibration", vibration).putBoolean("showFps", showFps)
            .putBoolean("lowGfx", lowGfx)
            .putInt("games", games).putInt("planets", planets)
            .putInt("perfects", perfects).putInt("bestStreak", bestStreak)
            .putInt("starsCollected", starsCollected).putFloat("playTime", playTime)
            .putLong("lastDay", lastDay).putInt("dayStreak", dayStreak)
            .putInt("lastVersion", lastVersion)
            .apply()
    }

    /** Setzt alles außer den Einstellungen zurück. */
    fun resetProgress() {
        best = 0; stars = 0; skin = 0; trail = 0
        ownedSkins = 1; ownedTrails = 1; revives = 0; levelDone = 0
        games = 0; planets = 0; perfects = 0; bestStreak = 0
        starsCollected = 0; playTime = 0f
        lastDay = 0L; dayStreak = 0
        flush()
    }
}
