package de.engelcode.orbit

/** Alle Spieler-Skins, Spuren und Extras aus dem Shop. */
object Skins {

    // Skin-Stile
    const val PLAIN = 0
    const val PULSE = 1
    const val RING = 2
    const val DIAMOND = 3
    const val STAR = 4
    const val RAINBOW = 5
    const val HOLE = 6
    const val HEART = 7
    const val HEX = 8
    const val ATOM = 9
    const val SAUCER = 10
    const val TWIN = 11
    const val EYE = 12
    const val GALAXY = 13
    const val CROWN = 14

    // Spur-Stile
    const val T_LINE = 0
    const val T_FIRE = 1
    const val T_SPARK = 2
    const val T_RAINBOW = 3
    const val T_GOLD = 4
    const val T_GHOST = 5
    const val T_ZIGZAG = 6
    const val T_BUBBLES = 7
    const val T_DOUBLE = 8
    const val T_STARDUST = 9
    const val T_MATRIX = 10

    /**
     * reqLevel / reqCrowns: muss zusätzlich zum Preis erreicht sein.
     */
    class Skin(
        val name: String, val price: Int, val color: Int, val glow: Int, val style: Int,
        val color2: Int = color, val reqLevel: Int = 0, val reqCrowns: Int = 0
    )

    class Trail(
        val name: String, val price: Int, val color: Int, val style: Int,
        val reqLevel: Int = 0, val reqCrowns: Int = 0
    )

    private fun c(v: Long) = v.toInt()

    val skins = arrayOf(
        Skin("Komet", 0, c(0xFFE8FBFF), c(0xFF7FE7FF), PLAIN),
        Skin("Sonne", 150, c(0xFFFFD166), c(0xFFFFB703), PULSE),
        Skin("Rubin", 300, c(0xFFFF4D6D), c(0xFFFF4D6D), DIAMOND),
        Skin("Smaragd", 300, c(0xFF2EE59D), c(0xFF2EE59D), DIAMOND),
        Skin("Saphir", 350, c(0xFF4C8DFF), c(0xFF4C8DFF), DIAMOND),
        Skin("Saturn", 500, c(0xFFF4A261), c(0xFFF4A261), RING, c(0xFFFFE5B4), reqLevel = 10),
        Skin("Neon", 600, c(0xFFF15BB5), c(0xFFF15BB5), STAR),
        Skin("Herz", 700, c(0xFFFF5C8A), c(0xFFFF5C8A), HEART),
        Skin("Eiskristall", 800, c(0xFFBDE8FF), c(0xFF7FD8FF), HEX, reqLevel = 15),
        Skin("Atom", 1000, c(0xFF7FE7FF), c(0xFF4CC9F0), ATOM, c(0xFFB388FF), reqLevel = 20),
        Skin("Ufo", 1200, c(0xFFB0BEC5), c(0xFF9BE564), SAUCER, c(0xFF9BE564), reqCrowns = 30),
        Skin("Zwilling", 1400, c(0xFFFFD166), c(0xFFFFD166), TWIN, c(0xFF4CC9F0), reqLevel = 30),
        Skin("Plasma", 1600, c(0xFFB388FF), c(0xFF9B5DE5), PULSE, reqLevel = 35),
        Skin("Lava", 1800, c(0xFFFF7A00), c(0xFFFF3D00), HEX, reqLevel = 40),
        Skin("Auge", 2000, c(0xFFFFFFFF), c(0xFF2EC4B6), EYE, c(0xFF2EC4B6), reqCrowns = 60),
        Skin("Regenbogen", 2500, c(0xFFFFFFFF), c(0xFFFFFFFF), RAINBOW, reqLevel = 50),
        Skin("Schw. Loch", 3000, c(0xFF9B5DE5), c(0xFF7B2CBF), HOLE, reqLevel = 60),
        Skin("Neutronen", 3500, c(0xFFE0F7FF), c(0xFF00E5FF), ATOM, c(0xFF00E5FF), reqCrowns = 120),
        Skin("Galaxie", 4000, c(0xFFB388FF), c(0xFF7B2CBF), GALAXY, c(0xFF4CC9F0), reqLevel = 70),
        Skin("Phönix", 4500, c(0xFFFF7A00), c(0xFFFFB703), STAR, reqLevel = 80),
        Skin("Diamant", 5000, c(0xFFFFFFFF), c(0xFFBDE8FF), HEX, reqCrowns = 180),
        Skin("Goldstern", 6000, c(0xFFFFD700), c(0xFFFFC300), STAR, reqLevel = 90),
        Skin("Kronjuwel", 0, c(0xFFFFD166), c(0xFFFFB703), CROWN, reqCrowns = 270),
        Skin("Andromeda", 0, c(0xFFF15BB5), c(0xFF9B5DE5), GALAXY, c(0xFFFFD166), reqLevel = 100)
    )

    val trails = arrayOf(
        Trail("Standard", 0, 0, T_LINE),
        Trail("Plasma", 200, c(0xFFB388FF), T_LINE),
        Trail("Feuer", 400, 0, T_FIRE),
        Trail("Funken", 500, 0, T_SPARK),
        Trail("Geist", 700, c(0xFF7FE7FF), T_GHOST),
        Trail("Blitz", 900, c(0xFFFFF176), T_ZIGZAG, reqLevel = 15),
        Trail("Blasen", 1000, c(0xFF7FE7FF), T_BUBBLES, reqLevel = 25),
        Trail("Doppel", 1200, c(0xFFF15BB5), T_DOUBLE, reqCrowns = 40),
        Trail("Sternenstaub", 1500, 0, T_STARDUST, reqLevel = 35),
        Trail("Regenbogen", 2000, 0, T_RAINBOW, reqLevel = 45),
        Trail("Matrix", 2500, c(0xFF2EE59D), T_MATRIX, reqCrowns = 100),
        Trail("Gold", 4000, 0, T_GOLD, reqLevel = 75)
    )

    const val REVIVE_PRICE = 150
    const val REVIVE_MAX = 3
    const val SHIELD_PRICE = 80
    const val SHIELD_MAX = 5
}
