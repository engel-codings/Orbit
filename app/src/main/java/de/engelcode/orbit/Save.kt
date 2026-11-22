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
    var shieldStarts = p.getInt("shieldStarts", 0)
    var levelDone = p.getInt("levelDone", 0)      // höchstes geschafftes Level
    private val crowns = ByteArray(Levels.COUNT + 1)
    var achClaimed = p.getInt("achClaimed", 0)

    // Einstellungen
    var sound = p.getBoolean("sound", true)
    var music = p.getBoolean("music", true)
    var vibration = p.getBoolean("vibration", true)
    var showFps = p.getBoolean("showFps", true)
    var lowGfx = p.getBoolean("lowGfx", false)
    var shake = p.getBoolean("shake", true)

    // Statistik
    var games = p.getInt("games", 0)
    var planets = p.getInt("planets", 0)
    var perfects = p.getInt("perfects", 0)
    var bestStreak = p.getInt("bestStreak", 0)
    var starsCollected = p.getInt("starsCollected", 0)
    var playTime = p.getFloat("playTime", 0f)
    var powerups = p.getInt("powerups", 0)
    var revivesUsed = p.getInt("revivesUsed", 0)
    var bestDayStreak = p.getInt("bestDayStreak", 0)
    var challengesDone = p.getInt("challengesDone", 0)
    var spins = p.getInt("spins", 0)
    var challengeDay = p.getLong("challengeDay", 0L)
    var wheelDay = p.getLong("wheelDay", 0L)

    // Täglicher Bonus / Versionen
    var lastDay = p.getLong("lastDay", 0L)
    var dayStreak = p.getInt("dayStreak", 0)
    var lastVersion = p.getInt("lastVersion", 0)

    var totalCrowns = 0; private set

    init {
        val s = p.getString("crowns", "") ?: ""
        for (i in 1..Levels.COUNT) {
            crowns[i] = if (i - 1 < s.length) (s[i - 1] - '0').toByte() else 0
            // Spielstand aus v0.2: geschaffte Level bekommen 1 Krone
            if (s.isEmpty() && i <= levelDone) crowns[i] = 1
        }
        if (bestDayStreak < dayStreak) bestDayStreak = dayStreak
        recount()
    }

    private fun recount() {
        var t = 0
        for (i in 1..Levels.COUNT) t += crowns[i]
        totalCrowns = t
    }

    fun crownsOf(level: Int): Int = if (level in 1..Levels.COUNT) crowns[level].toInt() else 0

    /** Speichert die Kronen nur, wenn sie besser sind. Gibt die Verbesserung zurück. */
    fun setCrowns(level: Int, n: Int): Int {
        val old = crownsOf(level)
        if (n <= old) return 0
        crowns[level] = n.toByte()
        recount()
        return n - old
    }

    fun ownsSkin(i: Int) = ownedSkins and (1 shl i) != 0
    fun ownsTrail(i: Int) = ownedTrails and (1 shl i) != 0
    fun skinCount() = Integer.bitCount(ownedSkins)

    fun flush() {
        val sb = StringBuilder(Levels.COUNT)
        for (i in 1..Levels.COUNT) sb.append(('0' + crowns[i].toInt()))
        p.edit()
            .putInt("best", best).putInt("stars", stars)
            .putInt("skin", skin).putInt("trail", trail)
            .putInt("ownedSkins", ownedSkins).putInt("ownedTrails", ownedTrails)
            .putInt("revives", revives).putInt("shieldStarts", shieldStarts)
            .putInt("levelDone", levelDone).putString("crowns", sb.toString())
            .putInt("achClaimed", achClaimed)
            .putBoolean("sound", sound).putBoolean("music", music)
            .putBoolean("vibration", vibration).putBoolean("showFps", showFps)
            .putBoolean("lowGfx", lowGfx).putBoolean("shake", shake)
            .putInt("challengesDone", challengesDone).putInt("spins", spins)
            .putLong("challengeDay", challengeDay).putLong("wheelDay", wheelDay)
            .putInt("games", games).putInt("planets", planets)
            .putInt("perfects", perfects).putInt("bestStreak", bestStreak)
            .putInt("starsCollected", starsCollected).putFloat("playTime", playTime)
            .putInt("powerups", powerups).putInt("revivesUsed", revivesUsed)
            .putInt("bestDayStreak", bestDayStreak)
            .putLong("lastDay", lastDay).putInt("dayStreak", dayStreak)
            .putInt("lastVersion", lastVersion)
            .apply()
    }

    /** Setzt alles außer den Einstellungen zurück. */
    fun resetProgress() {
        best = 0; stars = 0; skin = 0; trail = 0
        ownedSkins = 1; ownedTrails = 1; revives = 0; shieldStarts = 0; levelDone = 0
        for (i in crowns.indices) crowns[i] = 0
        recount()
        achClaimed = 0
        games = 0; planets = 0; perfects = 0; bestStreak = 0
        starsCollected = 0; playTime = 0f; powerups = 0; revivesUsed = 0; bestDayStreak = 0
        challengesDone = 0; spins = 0; challengeDay = 0L; wheelDay = 0L
        lastDay = 0L; dayStreak = 0
        flush()
    }
}
