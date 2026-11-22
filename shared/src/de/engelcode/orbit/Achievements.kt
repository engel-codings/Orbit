package de.engelcode.orbit

/** Erfolge mit Sterne-Belohnung (Texte in 3 Sprachen). */
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
    private const val MISSIONS = 14
    private const val CHESTS = 15
    private const val REDIRECTS = 16
    private const val SKIPS = 17
    private const val WORMS = 18
    private const val TT = 19

    class Ach(private val titles: Array<String>, val kind: Int, val goal: Int, val reward: Int) {
        val title: String get() = titles[L.cur]
    }

    private fun n(de: String, en: String, ru: String) = arrayOf(de, en, ru)

    val list = arrayOf(
        Ach(n("Erster Sprung", "First jump", "Первый прыжок"), PLANETS, 1, 10),
        Ach(n("Weltenbummler", "Globetrotter", "Путешественник"), PLANETS, 250, 80),
        Ach(n("Perfektionist", "Perfectionist", "Перфекционист"), PERFECTS, 10, 30),
        Ach(n("Perfekt-Meister", "Perfect master", "Мастер посадки"), PERFECTS, 150, 150),
        Ach(n("Im Flow", "In the flow", "В потоке"), STREAK, 5, 60),
        Ach(n("Mondlandung", "Moon landing", "Высадка на Луну"), LEVEL, 10, 50),
        Ach(n("Roter Planet", "Red planet", "Красная планета"), LEVEL, 20, 75),
        Ach(n("Riesenschritt", "Giant leap", "Гигантский шаг"), LEVEL, 40, 120),
        Ach(n("Herr der Ringe", "Lord of the rings", "Повелитель колец"), LEVEL, 50, 150),
        Ach(n("Eisriesen", "Ice giants", "Ледяные гиганты"), LEVEL, 70, 200),
        Ach(n("Am Rand", "On the edge", "На краю"), LEVEL, 90, 250),
        Ach(n("Andromeda", "Andromeda", "Андромеда"), LEVEL, 100, 500),
        Ach(n("Kronensammler", "Crown collector", "Собиратель корон"), CROWNS, 50, 100),
        Ach(n("Kronenkönig", "Crown king", "Король корон"), CROWNS, 200, 300),
        Ach(n("Läufer", "Runner", "Бегун"), BEST, 50, 80),
        Ach(n("Unaufhaltsam", "Unstoppable", "Неудержимый"), BEST, 150, 200),
        Ach(n("Sternensammler", "Star collector", "Собиратель звёзд"), STARS, 500, 100),
        Ach(n("Vielspieler", "Regular", "Завсегдатай"), GAMES, 100, 100),
        Ach(n("Modebewusst", "Fashionable", "Модник"), SKINS, 5, 80),
        Ach(n("Stammgast", "Loyal pilot", "Постоянный гость"), DAYS, 7, 150),
        Ach(n("Power!", "Power!", "Сила!"), POWERUPS, 25, 80),
        Ach(n("Ausdauer", "Endurance", "Выносливость"), TIME, 120, 120),
        Ach(n("Galaktisch", "Galactic", "Галактический"), LEVEL, 120, 300),
        Ach(n("Am Anfang", "At the beginning", "У истоков"), LEVEL, 150, 800),
        Ach(n("Kronen-Kaiser", "Crown emperor", "Император корон"), CROWNS, 350, 500),
        Ach(n("Herausforderer", "Challenger", "Претендент"), CHALLENGES, 5, 150),
        Ach(n("Glückspilz", "Lucky one", "Везунчик"), SPINS, 10, 100),
        Ach(n("Sammler", "Collector", "Коллекционер"), SKINS, 15, 300),
        Ach(n("Admiral", "Admiral", "Адмирал"), PLANETS, 1000, 250),
        Ach(n("Power-Profi", "Power pro", "Мастер бонусов"), POWERUPS, 100, 200),
        Ach(n("Dimensionsspringer", "Dimension jumper", "Прыгун измерений"), LEVEL, 160, 400),
        Ach(n("Spiegelmeister", "Mirror master", "Мастер зеркал"), LEVEL, 200, 1000),
        Ach(n("Ins Nichts", "Into the void", "В пустоту"), LEVEL, 210, 500),
        Ach(n("Herr der Leere", "Lord of the void", "Повелитель пустоты"), LEVEL, 250, 1500),
        Ach(n("Missions-Ass", "Mission ace", "Мастер миссий"), MISSIONS, 30, 300),
        Ach(n("Schatzsucher", "Treasure hunter", "Кладоискатель"), CHESTS, 20, 150),
        Ach(n("Großsammler", "Big collector", "Большой коллекционер"), SKINS, 25, 600),
        Ach(n("Kronen-Gott", "Crown god", "Бог корон"), CROWNS, 500, 1000),
        Ach(n("Sturmreiter", "Storm rider", "Покоритель бурь"), LEVEL, 260, 600),
        Ach(n("Sonnenkern", "Solar core", "Солнечное ядро"), LEVEL, 300, 2000),
        Ach(n("Endlos-Profi", "Endless pro", "Профи бесконечности"), BEST, 250, 400),
        Ach(n("Sammelwut", "Collecting frenzy", "Страсть к коллекциям"), SKINS, 35, 800),
        Ach(n("Truhen-König", "Chest king", "Король сундуков"), CHESTS, 60, 300),
        Ach(n("Missions-Legende", "Mission legend", "Легенда миссий"), MISSIONS, 100, 600),
        Ach(n("Kurskorrektur", "Course correction", "Коррекция курса"), REDIRECTS, 50, 200),
        Ach(n("Überflieger", "High flyer", "Высокий полёт"), SKIPS, 50, 250),
        Ach(n("Wurmloch-Reisender", "Wormhole traveller", "Путник червоточин"), WORMS, 20, 200),
        Ach(n("Rennfahrer", "Racer", "Гонщик"), TT, 10, 300),
        Ach(n("Frostbeule", "Frostbite", "Обморожение"), LEVEL, 310, 700),
        Ach(n("Eiskönig", "Ice king", "Ледяной король"), LEVEL, 350, 2500),
        Ach(n("Lichtgeschwindigkeit", "Light speed", "Скорость света"), LEVEL, 360, 900),
        Ach(n("Unendlichkeit", "Infinity", "Бесконечность"), LEVEL, 400, 4000)
    )

    private val DESC = arrayOf(
        n("Erreiche {0} Planeten", "Reach {0} planets", "Достигни {0} планет"),
        n("{0} perfekte Landungen", "{0} perfect landings", "{0} идеальных посадок"),
        n("Perfekt-Serie von {0}", "Perfect streak of {0}", "Серия из {0} идеальных посадок"),
        n("Schaffe Level {0}", "Complete level {0}", "Пройди уровень {0}"),
        n("Sammle {0} Kronen", "Collect {0} crowns", "Собери {0} корон"),
        n("{0} Punkte im Endlos-Modus", "{0} points in endless mode", "{0} очков в бесконечном режиме"),
        n("Sammle {0} Sterne", "Collect {0} stars", "Собери {0} звёзд"),
        n("Spiele {0} Runden", "Play {0} rounds", "Сыграй {0} раундов"),
        n("Besitze {0} Skins", "Own {0} skins", "Собери {0} скинов"),
        n("{0} Tage in Folge gespielt", "Play {0} days in a row", "Играй {0} дней подряд"),
        n("Sammle {0} Power-ups", "Collect {0} power-ups", "Собери {0} бонусов"),
        n("Spiele insgesamt {0} Stunden", "Play for {0} hours in total", "Играй {0} часа в сумме"),
        n("Schaffe {0} Tages-Challenges", "Complete {0} daily challenges", "Пройди {0} ежедневных испытаний"),
        n("Drehe {0}-mal am Glücksrad", "Spin the lucky wheel {0} times", "Крути колесо удачи {0} раз"),
        n("Erfülle {0} Missionen", "Complete {0} missions", "Выполни {0} миссий"),
        n("Öffne {0} Truhen", "Open {0} chests", "Открой {0} сундуков"),
        n("Lenke {0}-mal um", "Redirect {0} times", "Смени курс {0} раз"),
        n("Überspringe {0} Planeten", "Skip {0} planets", "Пропусти {0} планет"),
        n("Reise {0}-mal durch Wurmlöcher", "Travel through {0} wormholes", "Пройди через {0} червоточин"),
        n("Beende {0} Zeitrennen", "Finish {0} time trials", "Заверши {0} гонок на время")
    )
    private val FIRST_PLANET = n("Erreiche deinen ersten Planeten", "Reach your first planet", "Достигни первой планеты")

    fun desc(a: Ach): String {
        if (a.kind == PLANETS && a.goal == 1) return L.t(FIRST_PLANET)
        val g = if (a.kind == TIME) a.goal / 60 else a.goal
        return L.f(DESC[a.kind], g)
    }

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
        MISSIONS -> s.missionsDone
        CHESTS -> s.chests
        REDIRECTS -> s.redirects
        SKIPS -> s.skips
        WORMS -> s.worms
        TT -> s.ttDone
        else -> (s.playTime / 60f).toInt()
    }

    fun done(i: Int, s: Save) = value(list[i], s) >= list[i].goal
    fun claimed(i: Int, s: Save) = s.achClaimed and (1L shl i) != 0L

    fun claimableCount(s: Save): Int {
        var n = 0
        for (i in list.indices) if (done(i, s) && !claimed(i, s)) n++
        return n
    }
}
