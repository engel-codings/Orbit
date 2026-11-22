package de.engelcode.orbit

import kotlin.math.min

/**
 * Die Level-Reise: von der Erde bis zum Urknall (150 Level).
 * Jedes Level ist eine feste Strecke zwischen Planeten (immer gleich aufgebaut).
 */
object Levels {

    const val COUNT = 150

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

    val bodyNames = arrayOf(
        "", "ERDE", "MOND", "MARS", "ASTEROIDENGÜRTEL", "JUPITER", "SATURN",
        "URANUS", "NEPTUN", "PLUTO", "KOMET", "ANDROMEDA",
        "PULSAR", "ORIONNEBEL", "QUASAR", "SAGITTARIUS A*", "URKNALL"
    )

    /** Abschnitt 0..14 (je 10 Level). */
    fun section(level: Int) = (level - 1) / 10

    fun startBody(level: Int) = section(level) + 1

    /** Ziel ist bei jedem 10. Level der nächste Himmelskörper, sonst ein Wegpunkt. */
    fun goalBody(level: Int) = if (level % 10 == 0) section(level) + 2 else B_NONE

    /** Anzahl der Planeten bis zum Ziel. */
    fun planetCount(level: Int) = min(5 + (level - 1) / 3, 40)

    fun difficulty(level: Int) = min(1.3f, (level - 1) / 85f)

    fun seed(level: Int) = level * 7919 + 13

    /** Belohnung beim ersten Schaffen. */
    fun reward(level: Int) = 3 + level / 4 + if (level % 10 == 0) 25 else 0

    /** Perfekte Landungen für die 3. Krone. */
    fun perfectGoal(level: Int) = (planetCount(level) * 2 + 4) / 5

    // Ab welchem Level gibt es was?
    const val MOVING_FROM = 4
    const val TYPES_FROM = 6
    const val HOLES_FROM = 12
    const val POWERUPS_FROM = 8
    const val METEORS_FROM = 25
}
