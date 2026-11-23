package de.engelcode.orbit

import kotlin.math.min

/**
 * Die Level-Reise: von der Erde bis zum Urknall (150 Level) und weiter in die Spiegelwelt (bis 200).
 * Jedes Level ist eine feste Strecke zwischen Planeten (immer gleich aufgebaut).
 */
object Levels {

    const val COUNT = 500

    // Himmelskörper
    const val B_NONE = 0
    const val B_EARTH = 1
    const val B_MOON = 2
    const val B_MARS = 3
    const val B_CERES = 4
    const val B_JUPITER = 5
    const val B_SATURN = 6
    const val B_URANUS = 7
    const val B_NEPTUNE = 8
    const val B_PLUTO = 9
    const val B_COMET = 10
    const val B_ANDROMEDA = 11
    const val B_PULSAR = 12
    const val B_NEBULA = 13
    const val B_QUASAR = 14
    const val B_BLACKHOLE = 15
    const val B_BIGBANG = 16
    const val B_CRYSTAL = 17
    const val B_NEONSTAR = 18
    const val B_MIRROR = 19
    const val B_VORTEX = 20
    const val B_GATE = 21
    const val B_SILENT = 22
    const val B_VOIDEYE = 23
    const val B_SHADOWMOON = 24
    const val B_ECHO = 25
    const val B_VOIDHEART = 26
    const val B_EMBER = 27
    const val B_STORMEYE = 28
    const val B_FIRERING = 29
    const val B_PHOENIX = 30
    const val B_SOLARCORE = 31
    const val B_FROSTSTAR = 32
    const val B_ICEGIANT = 33
    const val B_GLACIER = 34
    const val B_AURORA = 35
    const val B_ICECROWN = 36
    const val B_HYPERGATE = 37
    const val B_QUANTUM = 38
    const val B_SPIRAL = 39
    const val B_TACHYON = 40
    const val B_INFINITY = 41
    const val B_PULSAR2 = 42
    const val B_NEUTRON = 43
    const val B_MAGNETAR = 44
    const val B_TWINSTAR = 45
    const val B_STARGATE = 46
    const val B_EVENTHORIZON = 47
    const val B_SINGULARITY = 48
    const val B_DARKSTAR = 49
    const val B_ABYSS = 50
    const val B_OMEGA = 51

    private val bodyNames = arrayOf(
        arrayOf("", "", ""),
        arrayOf("ERDE", "EARTH", "ЗЕМЛЯ"),
        arrayOf("MOND", "MOON", "ЛУНА"),
        arrayOf("MARS", "MARS", "МАРС"),
        arrayOf("ASTEROIDENGÜRTEL", "ASTEROID BELT", "ПОЯС АСТЕРОИДОВ"),
        arrayOf("JUPITER", "JUPITER", "ЮПИТЕР"),
        arrayOf("SATURN", "SATURN", "САТУРН"),
        arrayOf("URANUS", "URANUS", "УРАН"),
        arrayOf("NEPTUN", "NEPTUNE", "НЕПТУН"),
        arrayOf("PLUTO", "PLUTO", "ПЛУТОН"),
        arrayOf("KOMET", "COMET", "КОМЕТА"),
        arrayOf("ANDROMEDA", "ANDROMEDA", "АНДРОМЕДА"),
        arrayOf("PULSAR", "PULSAR", "ПУЛЬСАР"),
        arrayOf("ORIONNEBEL", "ORION NEBULA", "ТУМАННОСТЬ ОРИОНА"),
        arrayOf("QUASAR", "QUASAR", "КВАЗАР"),
        arrayOf("SAGITTARIUS A*", "SAGITTARIUS A*", "СТРЕЛЕЦ A*"),
        arrayOf("URKNALL", "BIG BANG", "БОЛЬШОЙ ВЗРЫВ"),
        arrayOf("KRISTALLWELT", "CRYSTAL WORLD", "КРИСТАЛЬНЫЙ МИР"),
        arrayOf("NEONSTERN", "NEON STAR", "НЕОНОВАЯ ЗВЕЗДА"),
        arrayOf("SPIEGELPLANET", "MIRROR PLANET", "ЗЕРКАЛЬНАЯ ПЛАНЕТА"),
        arrayOf("ZEITSTRUDEL", "TIME VORTEX", "ВИХРЬ ВРЕМЕНИ"),
        arrayOf("DIMENSIONSTOR", "DIMENSION GATE", "ВРАТА ИЗМЕРЕНИЙ"),
        arrayOf("STILLER STERN", "SILENT STAR", "БЕЗМОЛВНАЯ ЗВЕЗДА"),
        arrayOf("LEERENAUGE", "VOID EYE", "ОКО ПУСТОТЫ"),
        arrayOf("SCHATTENMOND", "SHADOW MOON", "ТЕНЕВАЯ ЛУНА"),
        arrayOf("ECHO", "ECHO", "ЭХО"),
        arrayOf("HERZ DER LEERE", "HEART OF THE VOID", "СЕРДЦЕ ПУСТОТЫ"),
        arrayOf("GLUTSTERN", "EMBER STAR", "ТЛЕЮЩАЯ ЗВЕЗДА"),
        arrayOf("STURMAUGE", "STORM EYE", "ОКО БУРИ"),
        arrayOf("FEUERRING", "RING OF FIRE", "ОГНЕННОЕ КОЛЬЦО"),
        arrayOf("PHÖNIXNEBEL", "PHOENIX NEBULA", "ТУМАННОСТЬ ФЕНИКСА"),
        arrayOf("SONNENKERN", "SOLAR CORE", "СОЛНЕЧНОЕ ЯДРО"),
        arrayOf("FROSTSTERN", "FROST STAR", "МОРОЗНАЯ ЗВЕЗДА"),
        arrayOf("EISRIESE", "ICE GIANT", "ЛЕДЯНОЙ ГИГАНТ"),
        arrayOf("GLETSCHERMOND", "GLACIER MOON", "ЛЕДНИКОВАЯ ЛУНА"),
        arrayOf("POLARLICHT", "POLAR LIGHTS", "ПОЛЯРНОЕ СИЯНИЕ"),
        arrayOf("EISKRONE", "ICE CROWN", "ЛЕДЯНАЯ КОРОНА"),
        arrayOf("HYPERTOR", "HYPER GATE", "ГИПЕРВРАТА"),
        arrayOf("QUANTENKERN", "QUANTUM CORE", "КВАНТОВОЕ ЯДРО"),
        arrayOf("LICHTSPIRALE", "LIGHT SPIRAL", "СВЕТОВАЯ СПИРАЛЬ"),
        arrayOf("TACHYONENSTERN", "TACHYON STAR", "ТАХИОННАЯ ЗВЕЗДА"),
        arrayOf("UNENDLICHKEIT", "INFINITY", "БЕСКОНЕЧНОСТЬ"),
        arrayOf("PULSARKERN", "PULSAR CORE", "ЯДРО ПУЛЬСАРА"),
        arrayOf("NEUTRONENSTERN", "NEUTRON STAR", "НЕЙТРОННАЯ ЗВЕЗДА"),
        arrayOf("MAGNETAR", "MAGNETAR", "МАГНЕТАР"),
        arrayOf("DOPPELSTERN", "TWIN STAR", "ДВОЙНАЯ ЗВЕЗДА"),
        arrayOf("STERNENTOR", "STAR GATE", "ЗВЁЗДНЫЕ ВРАТА"),
        arrayOf("EREIGNISHORIZONT", "EVENT HORIZON", "ГОРИЗОНТ СОБЫТИЙ"),
        arrayOf("SINGULARITÄT", "SINGULARITY", "СИНГУЛЯРНОСТЬ"),
        arrayOf("DUNKELSTERN", "DARK STAR", "ТЁМНАЯ ЗВЕЗДА"),
        arrayOf("ABGRUND", "ABYSS", "БЕЗДНА"),
        arrayOf("OMEGA", "OMEGA", "ОМЕГА")
    )

    fun bodyName(b: Int): String = bodyNames[b][L.cur]

    /** 1 = unser Universum (1–150), 2 = Spiegelwelt (151–200), 3 = Die Leere (201–250). */
    fun dimension(level: Int) = when {
        level > 450 -> 8
        level > 400 -> 7
        level > 350 -> 6
        level > 300 -> 5
        level > 250 -> 4
        level > 200 -> 3
        level > 150 -> 2
        else -> 1
    }
    const val DIM2_FROM = 151
    const val DIM3_FROM = 201
    const val DIM4_FROM = 251
    const val DIM5_FROM = 301
    const val DIM6_FROM = 351
    const val DIM7_FROM = 401
    const val DIM8_FROM = 451
    const val DIMS = 8
    /** Ab welchem geschafften Level ist der Endlos-Modus einer Dimension frei? */
    val DIM_UNLOCK = intArrayOf(0, 150, 200, 250, 300, 350, 400, 450)

    /** Abschnitt 0..19 (je 10 Level). */
    fun section(level: Int) = (level - 1) / 10

    fun startBody(level: Int) = section(level) + 1

    /** Ziel ist bei jedem 10. Level der nächste Himmelskörper, sonst ein Wegpunkt. */
    fun goalBody(level: Int) = if (level % 10 == 0) section(level) + 2 else B_NONE

    /** Anzahl der Planeten bis zum Ziel. */
    fun planetCount(level: Int) = min(6 + level / 4, 80)

    fun difficulty(level: Int) = min(1.3f, (level - 1) / 85f)

    fun seed(level: Int) = level * 7919 + 13

    /** Belohnung beim ersten Schaffen. */
    fun reward(level: Int) = 5 + level / 2 + if (level % 10 == 0) 30 else 0

    /** Perfekte Landungen für die 3. Krone. */
    fun perfectGoal(level: Int) = (planetCount(level) * 2 + 4) / 5

    // Ab welchem Level gibt es was?
    const val MOVING_FROM = 4
    const val TYPES_FROM = 6
    const val HOLES_FROM = 12
    const val POWERUPS_FROM = 8
    const val METEORS_FROM = 25
    const val BOOST_FROM = 5
    const val ROCKS_FROM = 15
    const val WORM_FROM = 30
    const val LASER_FROM = 40
}
