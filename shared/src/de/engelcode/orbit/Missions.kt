package de.engelcode.orbit

import kotlin.random.Random

/** Tägliche Missionen: jeden Tag 3 neue Aufgaben (leicht, mittel, schwer). */
object Missions {

    const val STARS = 0
    const val PERFECTS = 1
    const val PLANETS = 2
    const val LEVELS = 3
    const val ENDLESS = 4
    const val POWERUPS = 5
    const val SPIN = 6

    private val GOALS = arrayOf(
        intArrayOf(15, 25, 40),
        intArrayOf(5, 10, 15),
        intArrayOf(20, 40, 60),
        intArrayOf(2, 3, 5),
        intArrayOf(15, 25, 40),
        intArrayOf(2, 3, 5),
        intArrayOf(1, 1, 1)
    )
    val REWARD = intArrayOf(25, 40, 60)

    val type = IntArray(3)
    val goal = IntArray(3)
    private var forDay = -1L

    /** Missionen für einen Tag auswählen (für alle Spieler gleich). */
    fun prepare(day: Long) {
        if (day == forDay) return
        forDay = day
        val rnd = Random(day * 131L + 7L)
        val pool = mutableListOf(0, 1, 2, 3, 4, 5, 6)
        for (k in 0 until 3) {
            val t = pool.removeAt(rnd.nextInt(pool.size))
            type[k] = t
            goal[k] = GOALS[t][k]
        }
    }

    fun desc(k: Int): String {
        val g = goal[k]
        return when (type[k]) {
            STARS -> L.f(L.MISSION_DESC[STARS], g, L.stars(g))
            LEVELS -> L.f(L.MISSION_DESC[LEVELS], g, L.levels(g))
            POWERUPS -> L.f(L.MISSION_DESC[POWERUPS], g, L.powerups(g))
            else -> L.f(L.MISSION_DESC[type[k]], g)
        }
    }
}
