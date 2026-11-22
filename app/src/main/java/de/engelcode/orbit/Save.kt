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
    var ownedSkins: Long = if (p.contains("ownedSkinsL")) p.getLong("ownedSkinsL", 1L)
        else (p.getInt("ownedSkins", 1).toLong() and 0xFFFFFFFFL)
    var lang = p.getInt("lang", -1)   // -1 = Systemsprache
    var ownedTrails = p.getInt("ownedTrails", 1)
    var revives = p.getInt("revives", 0)
    var shieldStarts = p.getInt("shieldStarts", 0)
    var levelDone = p.getInt("levelDone", 0)      // höchstes geschafftes Level
    private val crowns = ByteArray(Levels.COUNT + 1)
    var achClaimed: Long = if (p.contains("achClaimedL")) p.getLong("achClaimedL", 0L)
        else (p.getInt("achClaimed", 0).toLong() and 0xFFFFFFFFL)

    // Missionen & Truhe
    var missionDay = p.getLong("missionDay", 0L)
    val missionProg = IntArray(3) { p.getInt("mProg$it", 0) }
    var missionClaimed = p.getInt("mClaimed", 0)
    var missionsDone = p.getInt("missionsDone", 0)
    var chestReady = p.getLong("chestReady", 0L)
    var chests = p.getInt("chests", 0)

    // Einstellungen
    var sound = p.getBoolean("sound", true)
    var music = p.getBoolean("music", true)
    var vibration = p.getBoolean("vibration", true)
    var showFps = p.getBoolean("showFps", true)
    var lowGfx = p.getBoolean("lowGfx", false)
    var shake = p.getBoolean("shake", true)
    var musicTrack = p.getInt("musicTrack", 0)
    /** Endlos-Rekord je Dimension (1–4). */
    val bestDim = IntArray(4) { if (it == 0) p.getInt("bestD1", best) else p.getInt("bestD${it + 1}", 0) }

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

    fun ownsSkin(i: Int) = ownedSkins and (1L shl i) != 0L
    fun ownsTrail(i: Int) = ownedTrails and (1 shl i) != 0
    fun skinCount() = java.lang.Long.bitCount(ownedSkins)

    fun flush() {
        val sb = StringBuilder(Levels.COUNT)
        for (i in 1..Levels.COUNT) sb.append(('0' + crowns[i].toInt()))
        p.edit()
            .putInt("best", best).putInt("stars", stars)
            .putInt("skin", skin).putInt("trail", trail)
            .putLong("ownedSkinsL", ownedSkins).putInt("ownedTrails", ownedTrails)
            .putInt("lang", lang)
            .putInt("revives", revives).putInt("shieldStarts", shieldStarts)
            .putInt("levelDone", levelDone).putString("crowns", sb.toString())
            .putLong("achClaimedL", achClaimed)
            .putLong("missionDay", missionDay).putInt("mClaimed", missionClaimed)
            .putInt("mProg0", missionProg[0]).putInt("mProg1", missionProg[1]).putInt("mProg2", missionProg[2])
            .putInt("missionsDone", missionsDone)
            .putLong("chestReady", chestReady).putInt("chests", chests)
            .putBoolean("sound", sound).putBoolean("music", music)
            .putBoolean("vibration", vibration).putBoolean("showFps", showFps)
            .putBoolean("lowGfx", lowGfx).putBoolean("shake", shake).putInt("musicTrack", musicTrack)
            .putInt("bestD1", bestDim[0]).putInt("bestD2", bestDim[1]).putInt("bestD3", bestDim[2]).putInt("bestD4", bestDim[3])
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
        ownedSkins = 1L; ownedTrails = 1; revives = 0; shieldStarts = 0; levelDone = 0
        for (i in crowns.indices) crowns[i] = 0
        recount()
        achClaimed = 0L
        for (i in 0 until 4) bestDim[i] = 0
        missionDay = 0L; missionClaimed = 0; missionsDone = 0; chestReady = 0L; chests = 0
        for (i in 0 until 3) missionProg[i] = 0
        games = 0; planets = 0; perfects = 0; bestStreak = 0
        starsCollected = 0; playTime = 0f; powerups = 0; revivesUsed = 0; bestDayStreak = 0
        challengesDone = 0; spins = 0; challengeDay = 0L; wheelDay = 0L
        lastDay = 0L; dayStreak = 0
        flush()
    }
}
