package de.engelcode.orbit

/** Alle Spieler-Skins und Spuren aus dem Shop. */
object Skins {

    // Skin-Stile
    const val PLAIN = 0
    const val PULSE = 1
    const val RING = 2
    const val DIAMOND = 3
    const val STAR = 4
    const val RAINBOW = 5
    const val HOLE = 6

    // Spur-Stile
    const val T_LINE = 0
    const val T_FIRE = 1
    const val T_SPARK = 2
    const val T_RAINBOW = 3
    const val T_GOLD = 4
    const val T_GHOST = 5

    class Skin(val name: String, val price: Int, val color: Int, val glow: Int, val style: Int, val color2: Int = color)
    class Trail(val name: String, val price: Int, val color: Int, val style: Int)

    private fun c(v: Long) = v.toInt()

    val skins = arrayOf(
        Skin("Komet", 0, c(0xFFE8FBFF), c(0xFF7FE7FF), PLAIN),
        Skin("Sonne", 40, c(0xFFFFD166), c(0xFFFFB703), PULSE),
        Skin("Rubin", 80, c(0xFFFF4D6D), c(0xFFFF4D6D), DIAMOND),
        Skin("Smaragd", 80, c(0xFF2EE59D), c(0xFF2EE59D), DIAMOND),
        Skin("Saturn", 120, c(0xFFF4A261), c(0xFFF4A261), RING, c(0xFFFFE5B4)),
        Skin("Neon", 150, c(0xFFF15BB5), c(0xFFF15BB5), STAR),
        Skin("Eiskristall", 200, c(0xFFBDE8FF), c(0xFF7FD8FF), STAR),
        Skin("Ufo", 250, c(0xFF9BE564), c(0xFF9BE564), RING, c(0xFFE0E0E0)),
        Skin("Plasma", 300, c(0xFFB388FF), c(0xFF9B5DE5), PULSE),
        Skin("Regenbogen", 400, c(0xFFFFFFFF), c(0xFFFFFFFF), RAINBOW),
        Skin("Schw. Loch", 600, c(0xFF9B5DE5), c(0xFF7B2CBF), HOLE),
        Skin("Goldstern", 1000, c(0xFFFFD700), c(0xFFFFC300), STAR)
    )

    val trails = arrayOf(
        Trail("Standard", 0, 0, T_LINE),
        Trail("Plasma", 60, c(0xFFB388FF), T_LINE),
        Trail("Feuer", 120, 0, T_FIRE),
        Trail("Funken", 150, 0, T_SPARK),
        Trail("Geist", 200, c(0xFF7FE7FF), T_GHOST),
        Trail("Regenbogen", 300, 0, T_RAINBOW),
        Trail("Gold", 500, 0, T_GOLD)
    )

    const val REVIVE_PRICE = 60
    const val REVIVE_MAX = 3
}
