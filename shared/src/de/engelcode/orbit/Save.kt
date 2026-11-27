package de.engelcode.orbit

import android.content.Context

/** Spielstand und Einstellungen (bleiben bei Updates erhalten). */
class Save(context: Context) {

    companion object {
        private val LONGS = setOf("achClaimedL", "achClaimed2L", "challengeDay", "chestReady", "lastDay", "missionDay", "ownedSkinsL", "ownedSkins2L", "skillTreeL", "wheelDay", "tt0", "tt1", "tt2", "tt3")
        /** Datentyp eines gespeicherten Wertes (für den Cloud-Speicher). */
        fun typeOf(k: String, v: String): Char = when {
            k in LONGS -> 'l'
            k == "playTime" -> 'f'
            k == "crowns" || k == "lt" -> 's'
            v == "true" || v == "false" -> 'b'
            v.toIntOrNull() != null -> 'i'
            v.toLongOrNull() != null -> 'l'
            v.toFloatOrNull() != null -> 'f'
            else -> 's'
        }
    }


    private val p = context.getSharedPreferences("orbit", Context.MODE_PRIVATE)

    // Fortschritt
    var best = p.getInt("best", 0)
    var stars = p.getInt("stars", 0)
    var skin = p.getInt("skin", 0)
    var trail = p.getInt("trail", 0)
    var ownedSkins: Long = if (p.contains("ownedSkinsL")) p.getLong("ownedSkinsL", 1L)
        else (p.getInt("ownedSkins", 1).toLong() and 0xFFFFFFFFL)
    var lang = p.getInt("lang", -1)   // -1 = Systemsprache
    var ownedSkins2: Long = p.getLong("ownedSkins2L", 0L)
    var avatar = p.getInt("avatar", 2)
    var photoPub = p.getBoolean("photoPub", false)
    var weekId = p.getInt("weekId", 0)
    var weekBest = p.getInt("weekBest", 0)
    private val levelTimes = HashMap<Int, Int>().also { m ->
        for (e in (p.getString("lt", "") ?: "").split(',')) {
            val kv = e.split(':'); if (kv.size == 2) { val k = kv[0].toIntOrNull(); val v = kv[1].toIntOrNull(); if (k != null && v != null) m[k] = v }
        }
    }
    fun levelTime(l: Int) = levelTimes[l] ?: 0
    fun setLevelTime(l: Int, ms: Int) { levelTimes[l] = ms }
    var bestCombo = p.getInt("bestCombo", 0)
    var skillTree = p.getInt("skillTree", 0)   // alter Baum (wird erstattet)
    var skillTreeL: Long = p.getLong("skillTreeL", 0L)
    var bgStyle = p.getInt("bgStyle", 0)
    var pcMode = p.getBoolean("pcMode", false)
    val modeBest = IntArray(6) { p.getInt("mode$it", 0) }
    var ownedTrails2 = p.getInt("ownedTrails2", 0)
    var achClaimed2 = p.getLong("achClaimed2L", 0L)
    var loginDays = p.getInt("loginDays", 0)
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
    /** Grafik: 0 = Hoch, 1 = Mittel, 2 = Sparsam. */
    var gfx = if (p.contains("gfx")) p.getInt("gfx", 0) else if (p.getBoolean("lowGfx", false)) 2 else 0
    val lowGfx get() = gfx == 2
    var shake = p.getBoolean("shake", true)
    var musicTrack = p.getInt("musicTrack", 0)
    val upg = IntArray(5) { p.getInt("upg$it", 0) }
    val ttBest = LongArray(4) { p.getLong("tt$it", 0L) }
    var ttDone = p.getInt("ttDone", 0)
    var redirects = p.getInt("redirects", 0)
    var skips = p.getInt("skips", 0)
    var worms = p.getInt("worms", 0)
    var redirectHint = p.getBoolean("redirectHint", false)
    /** Endlos-Rekord je Dimension (1–4). */
    val bestDim = IntArray(Levels.DIMS) { if (it == 0) p.getInt("bestD1", best) else p.getInt("bestD${it + 1}", 0) }

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

    fun ownsSkin(i: Int) = if (i < 64) ownedSkins and (1L shl i) != 0L else ownedSkins2 and (1L shl (i - 64)) != 0L
    fun grantSkin(i: Int) { if (i < 64) ownedSkins = ownedSkins or (1L shl i) else ownedSkins2 = ownedSkins2 or (1L shl (i - 64)) }
    fun ownsTrail(i: Int) = if (i < 32) ownedTrails and (1 shl i) != 0 else ownedTrails2 and (1 shl (i - 32)) != 0
    fun grantTrail(i: Int) { if (i < 32) ownedTrails = ownedTrails or (1 shl i) else ownedTrails2 = ownedTrails2 or (1 shl (i - 32)) }
    fun skinCount() = ownedSkins.countOneBits() + ownedSkins2.countOneBits()

    fun flush() {
        val sb = StringBuilder(Levels.COUNT)
        for (i in 1..Levels.COUNT) sb.append(('0' + crowns[i].toInt()))
        p.edit()
            .putInt("best", best).putInt("stars", stars)
            .putInt("skin", skin).putInt("trail", trail)
            .putLong("ownedSkinsL", ownedSkins).putLong("ownedSkins2L", ownedSkins2).putInt("avatar", avatar).putBoolean("photoPub", photoPub).putInt("weekId", weekId).putInt("weekBest", weekBest).putString("lt", levelTimes.entries.joinToString(",") { "${it.key}:${it.value}" }).putInt("bestCombo", bestCombo).putInt("skillTree", skillTree).putLong("skillTreeL", skillTreeL).putInt("bgStyle", bgStyle).putInt("mode0", modeBest[0]).putInt("mode1", modeBest[1]).putInt("mode2", modeBest[2]).putInt("mode3", modeBest[3]).putInt("mode4", modeBest[4]).putInt("mode5", modeBest[5]).putInt("ownedTrails", ownedTrails).putInt("ownedTrails2", ownedTrails2).putLong("achClaimed2L", achClaimed2).putInt("loginDays", loginDays)
            .putInt("lang", lang)
            .putInt("revives", revives).putInt("shieldStarts", shieldStarts)
            .putInt("levelDone", levelDone).putString("crowns", sb.toString())
            .putLong("achClaimedL", achClaimed)
            .putLong("missionDay", missionDay).putInt("mClaimed", missionClaimed)
            .putInt("mProg0", missionProg[0]).putInt("mProg1", missionProg[1]).putInt("mProg2", missionProg[2])
            .putInt("missionsDone", missionsDone)
            .putLong("chestReady", chestReady).putInt("chests", chests)
            .putBoolean("sound", sound).putBoolean("music", music)
            .putBoolean("vibration", vibration).putBoolean("showFps", showFps).putBoolean("pcMode", pcMode)
            .putInt("gfx", gfx).putBoolean("shake", shake).putInt("musicTrack", musicTrack)
            .putInt("upg0", upg[0]).putInt("upg1", upg[1]).putInt("upg2", upg[2]).putInt("upg3", upg[3]).putInt("upg4", upg[4])
            .putLong("tt0", ttBest[0]).putLong("tt1", ttBest[1]).putLong("tt2", ttBest[2]).putLong("tt3", ttBest[3]).putInt("ttDone", ttDone)
            .putInt("redirects", redirects).putInt("skips", skips).putInt("worms", worms).putBoolean("redirectHint", redirectHint)
            .putInt("bestD1", bestDim[0]).putInt("bestD2", bestDim[1]).putInt("bestD3", bestDim[2]).putInt("bestD4", bestDim[3]).putInt("bestD5", bestDim[4]).putInt("bestD6", bestDim[5]).putInt("bestD7", bestDim[6]).putInt("bestD8", bestDim[7]).putInt("bestD9", bestDim[8]).putInt("bestD10", bestDim[9])
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
        ownedSkins = 1L; ownedSkins2 = 0L; ownedTrails = 1; ownedTrails2 = 0; achClaimed2 = 0L; loginDays = 0; for (i in modeBest.indices) modeBest[i] = 0; revives = 0; shieldStarts = 0; levelDone = 0
        for (i in crowns.indices) crowns[i] = 0
        recount()
        achClaimed = 0L
        for (i in 0 until Levels.DIMS) bestDim[i] = 0
        for (i in 0 until 5) upg[i] = 0
        for (i in 0 until 4) ttBest[i] = 0L
        ttDone = 0; redirects = 0; skips = 0; worms = 0
        missionDay = 0L; missionClaimed = 0; missionsDone = 0; chestReady = 0L; chests = 0
        for (i in 0 until 3) missionProg[i] = 0
        games = 0; planets = 0; perfects = 0; bestStreak = 0
        starsCollected = 0; playTime = 0f; powerups = 0; revivesUsed = 0; bestDayStreak = 0
        challengesDone = 0; spins = 0; challengeDay = 0L; wheelDay = 0L
        lastDay = 0L; dayStreak = 0
        flush()
    }
}
