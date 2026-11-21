package de.engelcode.orbit

import kotlin.math.min

/**
 * Die Level-Reise: von der Erde bis zum Saturn.
 * Jedes Level ist eine feste Strecke zwischen Planeten (immer gleich aufgebaut).
 */
object Levels {

    const val COUNT = 50

    // Himmelskörper
    const val B_NONE = 0
    const val B_EARTH = 1
    const val B_MOON = 2
    const val B_MARS = 3
    const val B_CERES = 4
    const val B_JUPITER = 5
    const val B_SATURN = 6

    val bodyNames = arrayOf("", "ERDE", "MOND", "MARS", "ASTEROIDENGÜRTEL", "JUPITER", "SATURN")

    /** Abschnitt 0..4 (je 10 Level). */
    fun section(level: Int) = (level - 1) / 10

    /** Startplanet des Levels (Erde, Mond, Mars, ...). */
    fun startBody(level: Int) = section(level) + 1

    /** Ziel ist bei jedem 10. Level der nächste Himmelskörper, sonst ein Wegpunkt. */
    fun goalBody(level: Int) = if (level % 10 == 0) section(level) + 2 else B_NONE

    /** Anzahl der Planeten bis zum Ziel (inklusive Ziel, ohne Start). */
    fun planetCount(level: Int) = 5 + (level - 1) / 2

    fun difficulty(level: Int) = min(1f, (level - 1) / 45f)

    fun seed(level: Int) = level * 7919 + 13

    /** Belohnung beim ersten Schaffen. */
    fun reward(level: Int) = 10 + level + if (level % 10 == 0) 40 else 0
}
