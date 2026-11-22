package de.engelcode.orbit

/** Erfolge mit Sterne-Belohnung. */
object Achievements {

    // Was wird gemessen?
    private const val PLANETS = 0
    private const val PERFECTS = 1
    private const val STREAK = 2
    private const val LEVEL = 3
    private const val CROWNS = 4
    private const val BEST = 5
    private const val STARS = 6
    private const val GAMES = 7
    private const val SKINS = 8
    private const val DAYS = 9
    private const val POWERUPS = 10
    private const val TIME = 11
    private const val CHALLENGES = 12
    private const val SPINS = 13

    class Ach(val title: String, val desc: String, val kind: Int, val goal: Int, val reward: Int)

    val list = arrayOf(
        Ach("Erster Sprung", "Erreiche deinen ersten Planeten", PLANETS, 1, 10),
        Ach("Weltenbummler", "Erreiche 250 Planeten", PLANETS, 250, 80),
        Ach("Perfektionist", "10 perfekte Landungen", PERFECTS, 10, 30),
        Ach("Perfekt-Meister", "150 perfekte Landungen", PERFECTS, 150, 150),
        Ach("Im Flow", "Perfekt-Serie von 5", STREAK, 5, 60),
        Ach("Mondlandung", "Schaffe Level 10", LEVEL, 10, 50),
        Ach("Roter Planet", "Schaffe Level 20", LEVEL, 20, 75),
        Ach("Riesenschritt", "Schaffe Level 40", LEVEL, 40, 120),
        Ach("Herr der Ringe", "Schaffe Level 50", LEVEL, 50, 150),
        Ach("Eisriesen", "Schaffe Level 70", LEVEL, 70, 200),
        Ach("Am Rand", "Schaffe Level 90", LEVEL, 90, 250),
        Ach("Andromeda", "Schaffe Level 100", LEVEL, 100, 500),
        Ach("Kronensammler", "Sammle 50 Kronen", CROWNS, 50, 100),
        Ach("Kronenkönig", "Sammle 200 Kronen", CROWNS, 200, 300),
        Ach("Läufer", "50 Punkte im Endlos-Modus", BEST, 50, 80),
        Ach("Unaufhaltsam", "150 Punkte im Endlos-Modus", BEST, 150, 200),
        Ach("Sternensammler", "Sammle 500 Sterne", STARS, 500, 100),
        Ach("Vielspieler", "Spiele 100 Runden", GAMES, 100, 100),
        Ach("Modebewusst", "Besitze 5 Skins", SKINS, 5, 80),
        Ach("Stammgast", "7 Tage in Folge gespielt", DAYS, 7, 150),
        Ach("Power!", "Sammle 25 Power-ups", POWERUPS, 25, 80),
        Ach("Ausdauer", "Spiele insgesamt 2 Stunden", TIME, 120, 120),
        Ach("Galaktisch", "Schaffe Level 120", LEVEL, 120, 300),
        Ach("Am Anfang", "Schaffe Level 150", LEVEL, 150, 800),
        Ach("Kronen-Kaiser", "Sammle 350 Kronen", CROWNS, 350, 500),
        Ach("Herausforderer", "Schaffe 5 Tages-Challenges", CHALLENGES, 5, 150),
        Ach("Glückspilz", "Drehe 10-mal am Glücksrad", SPINS, 10, 100),
        Ach("Sammler", "Besitze 15 Skins", SKINS, 15, 300),
        Ach("Admiral", "Erreiche 1000 Planeten", PLANETS, 1000, 250),
        Ach("Power-Profi", "Sammle 100 Power-ups", POWERUPS, 100, 200)
    )

    fun value(a: Ach, s: Save): Int = when (a.kind) {
        PLANETS -> s.planets
        PERFECTS -> s.perfects
        STREAK -> s.bestStreak
        LEVEL -> s.levelDone
        CROWNS -> s.totalCrowns
        BEST -> s.best
        STARS -> s.starsCollected
        GAMES -> s.games
        SKINS -> s.skinCount()
        DAYS -> s.bestDayStreak
        POWERUPS -> s.powerups
        CHALLENGES -> s.challengesDone
        SPINS -> s.spins
        else -> (s.playTime / 60f).toInt()
    }

    fun done(i: Int, s: Save) = value(list[i], s) >= list[i].goal
    fun claimed(i: Int, s: Save) = s.achClaimed and (1 shl i) != 0

    fun claimableCount(s: Save): Int {
        var n = 0
        for (i in list.indices) if (done(i, s) && !claimed(i, s)) n++
        return n
    }
}
