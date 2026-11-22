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
    const val MOON = 15
    const val FIREBALL = 16
    const val SNOWFLAKE = 17
    const val ROCKET = 18
    const val CUBE = 19
    const val SATELLITE = 20
    const val PULSAR = 21
    const val BIGBANG = 22
    const val PRISM = 23
    const val MIRROR = 24
    const val FLOWER = 25
    const val DONUT = 26
    const val SHURIKEN = 27
    const val CLOCK = 28
    const val BEE = 29
    const val GHOST = 30
    const val ORB = 31
    const val VOID = 32
    const val PUMPKIN = 33
    const val DROP = 34
    const val ELECTRIC = 35
    const val YINYANG = 36
    const val ROBOT = 37
    const val BUTTERFLY = 38
    const val RINGPLANET = 39
    const val EMBER = 40

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
    const val T_HEARTS = 11
    const val T_COMET = 12
    const val T_PIXEL = 13
    const val T_GALAXY = 14
    const val T_WAVES = 15
    const val T_CONFETTI = 16
    const val T_SHADOW = 17
    const val T_LASER = 18
    const val T_ICE = 19
    const val T_DOTS = 20
    const val T_EMBERS = 21
    const val T_AURORA = 22

    /**
     * reqLevel / reqCrowns: muss zusätzlich zum Preis erreicht sein.
     */
    class Skin(
        private val names: Array<String>, val price: Int, val color: Int, val glow: Int, val style: Int,
        val color2: Int = color, val reqLevel: Int = 0, val reqCrowns: Int = 0
    ) {
        val name: String get() = names[L.cur]
    }

    class Trail(
        private val names: Array<String>, val price: Int, val color: Int, val style: Int,
        val reqLevel: Int = 0, val reqCrowns: Int = 0
    ) {
        val name: String get() = names[L.cur]
    }

    private fun n(de: String, en: String, ru: String) = arrayOf(de, en, ru)

    private fun c(v: Long) = v.toInt()

    val skins = arrayOf(
        Skin(n("Komet", "Comet", "Комета"), 0, c(0xFFE8FBFF), c(0xFF7FE7FF), PLAIN),
        Skin(n("Sonne", "Sun", "Солнце"), 120, c(0xFFFFD166), c(0xFFFFB703), PULSE),
        Skin(n("Rubin", "Ruby", "Рубин"), 250, c(0xFFFF4D6D), c(0xFFFF4D6D), DIAMOND),
        Skin(n("Smaragd", "Emerald", "Изумруд"), 250, c(0xFF2EE59D), c(0xFF2EE59D), DIAMOND),
        Skin(n("Saphir", "Sapphire", "Сапфир"), 300, c(0xFF4C8DFF), c(0xFF4C8DFF), DIAMOND),
        Skin(n("Saturn", "Saturn", "Сатурн"), 400, c(0xFFF4A261), c(0xFFF4A261), RING, c(0xFFFFE5B4), reqLevel = 10),
        Skin(n("Neon", "Neon", "Неон"), 500, c(0xFFF15BB5), c(0xFFF15BB5), STAR),
        Skin(n("Herz", "Heart", "Сердце"), 600, c(0xFFFF5C8A), c(0xFFFF5C8A), HEART),
        Skin(n("Eiskristall", "Ice crystal", "Ледяной кристалл"), 700, c(0xFFBDE8FF), c(0xFF7FD8FF), HEX, reqLevel = 15),
        Skin(n("Atom", "Atom", "Атом"), 900, c(0xFF7FE7FF), c(0xFF4CC9F0), ATOM, c(0xFFB388FF), reqLevel = 20),
        Skin(n("Ufo", "UFO", "НЛО"), 1100, c(0xFFB0BEC5), c(0xFF9BE564), SAUCER, c(0xFF9BE564), reqCrowns = 30),
        Skin(n("Zwilling", "Twins", "Близнецы"), 1300, c(0xFFFFD166), c(0xFFFFD166), TWIN, c(0xFF4CC9F0), reqLevel = 30),
        Skin(n("Plasma", "Plasma", "Плазма"), 1500, c(0xFFB388FF), c(0xFF9B5DE5), PULSE, reqLevel = 35),
        Skin(n("Lava", "Lava", "Лава"), 1700, c(0xFFFF7A00), c(0xFFFF3D00), HEX, reqLevel = 40),
        Skin(n("Auge", "Eye", "Глаз"), 1900, c(0xFFFFFFFF), c(0xFF2EC4B6), EYE, c(0xFF2EC4B6), reqCrowns = 60),
        Skin(n("Regenbogen", "Rainbow", "Радуга"), 2300, c(0xFFFFFFFF), c(0xFFFFFFFF), RAINBOW, reqLevel = 50),
        Skin(n("Schw. Loch", "Black hole", "Чёрная дыра"), 2800, c(0xFF9B5DE5), c(0xFF7B2CBF), HOLE, reqLevel = 60),
        Skin(n("Neutronen", "Neutron", "Нейтрон"), 3200, c(0xFFE0F7FF), c(0xFF00E5FF), ATOM, c(0xFF00E5FF), reqCrowns = 120),
        Skin(n("Galaxie", "Galaxy", "Галактика"), 3600, c(0xFFB388FF), c(0xFF7B2CBF), GALAXY, c(0xFF4CC9F0), reqLevel = 70),
        Skin(n("Phönix", "Phoenix", "Феникс"), 4200, c(0xFFFF7A00), c(0xFFFFB703), STAR, reqLevel = 80),
        Skin(n("Diamant", "Diamond", "Алмаз"), 4800, c(0xFFFFFFFF), c(0xFFBDE8FF), HEX, reqCrowns = 180),
        Skin(n("Goldstern", "Gold star", "Золотая звезда"), 5500, c(0xFFFFD700), c(0xFFFFC300), STAR, reqLevel = 90),
        Skin(n("Kronjuwel", "Crown jewel", "Корона"), 0, c(0xFFFFD166), c(0xFFFFB703), CROWN, reqCrowns = 270),
        Skin(n("Andromeda", "Andromeda", "Андромеда"), 0, c(0xFFF15BB5), c(0xFF9B5DE5), GALAXY, c(0xFFFFD166), reqLevel = 100),
        Skin(n("Mondsichel", "Crescent", "Полумесяц"), 2000, c(0xFFFFF3C4), c(0xFFFFE08A), MOON, reqLevel = 55),
        Skin(n("Feuerball", "Fireball", "Огненный шар"), 2500, c(0xFFFF7A00), c(0xFFFF3D00), FIREBALL, c(0xFFFFD166), reqLevel = 65),
        Skin(n("Schneeflocke", "Snowflake", "Снежинка"), 3000, c(0xFFE0F7FF), c(0xFF7FD8FF), SNOWFLAKE, reqCrowns = 150),
        Skin(n("Rakete", "Rocket", "Ракета"), 3500, c(0xFFECEFF1), c(0xFFFF7A00), ROCKET, c(0xFFFF4D6D), reqLevel = 85),
        Skin(n("Würfel", "Cube", "Куб"), 4000, c(0xFF4CC9F0), c(0xFF7B61FF), CUBE, c(0xFFB388FF), reqCrowns = 210),
        Skin(n("Satellit", "Satellite", "Спутник"), 5000, c(0xFFCFD8DC), c(0xFF4C8DFF), SATELLITE, c(0xFF4C8DFF), reqLevel = 110),
        Skin(n("Pulsar", "Pulsar", "Пульсар"), 6500, c(0xFFFFFFFF), c(0xFF7FE7FF), PULSAR, c(0xFF7FE7FF), reqLevel = 120),
        Skin(n("Urknall", "Big Bang", "Большой взрыв"), 0, c(0xFFFFFFFF), c(0xFFFFD166), BIGBANG, reqLevel = 150),
        Skin(n("Prisma", "Prism", "Призма"), 7500, c(0xFFFFFFFF), c(0xFFF15BB5), PRISM, c(0xFF4CC9F0), reqLevel = 170),
        Skin(n("Spiegel", "Mirror", "Зеркало"), 0, c(0xFFE0F7FF), c(0xFF9BE564), MIRROR, c(0xFF7B61FF), reqLevel = 200),
        Skin(n("Blume", "Flower", "Цветок"), 800, c(0xFFFF8FB1), c(0xFFFF5C8A), FLOWER, c(0xFFFFD166)),
        Skin(n("Donut", "Donut", "Пончик"), 1000, c(0xFFFF8FB1), c(0xFFFFB3C7), DONUT),
        Skin(n("Biene", "Bee", "Пчела"), 1200, c(0xFFFFD166), c(0xFFFFB703), BEE, reqLevel = 25),
        Skin(n("Shuriken", "Shuriken", "Сюрикен"), 1600, c(0xFFCFD8DC), c(0xFF4CC9F0), SHURIKEN, reqLevel = 45),
        Skin(n("Uhr", "Clock", "Часы"), 2400, c(0xFFFFF3C4), c(0xFFFFD166), CLOCK, c(0xFFFFB703), reqCrowns = 100),
        Skin(n("Geist", "Ghost", "Призрак"), 3000, c(0xFFE8FBFF), c(0xFF7FE7FF), GHOST, reqLevel = 140),
        Skin(n("Kristallkugel", "Crystal ball", "Хрустальный шар"), 6000, c(0xFFB388FF), c(0xFF7FE7FF), ORB, c(0xFF4CC9F0), reqCrowns = 300),
        Skin(n("Leere", "Void", "Пустота"), 0, c(0xFF05070D), c(0xFF2EE5C8), VOID, c(0xFF2EE5C8), reqLevel = 250),
        Skin(n("Kürbis", "Pumpkin", "Тыква"), 900, c(0xFFFF8C1A), c(0xFFFF7A00), PUMPKIN, c(0xFF2EE59D)),
        Skin(n("Tropfen", "Drop", "Капля"), 1100, c(0xFF4CC9F0), c(0xFF4C8DFF), DROP),
        Skin(n("Yin-Yang", "Yin-yang", "Инь-ян"), 1800, c(0xFFFFFFFF), c(0xFFB0BEC5), YINYANG, c(0xFF101418), reqLevel = 55),
        Skin(n("Roboter", "Robot", "Робот"), 2600, c(0xFFB0BEC5), c(0xFF4CC9F0), ROBOT, c(0xFF4CC9F0), reqCrowns = 120),
        Skin(n("Blitzkugel", "Ball lightning", "Шаровая молния"), 3400, c(0xFFB388FF), c(0xFF7FE7FF), ELECTRIC, c(0xFFFFFFFF), reqLevel = 160),
        Skin(n("Schmetterling", "Butterfly", "Бабочка"), 4400, c(0xFFF15BB5), c(0xFFF15BB5), BUTTERFLY, c(0xFF4CC9F0), reqLevel = 180),
        Skin(n("Ringplanet", "Ring planet", "Кольцевая планета"), 6800, c(0xFF7B61FF), c(0xFF7B61FF), RINGPLANET, reqCrowns = 360),
        Skin(n("Glut", "Ember", "Уголёк"), 0, c(0xFFFF7A00), c(0xFFFF3D00), EMBER, c(0xFFFFD166), reqLevel = 300)
    )

    val trails = arrayOf(
        Trail(n("Standard", "Standard", "Стандарт"), 0, 0, T_LINE),
        Trail(n("Plasma", "Plasma", "Плазма"), 150, c(0xFFB388FF), T_LINE),
        Trail(n("Feuer", "Fire", "Огонь"), 300, 0, T_FIRE),
        Trail(n("Funken", "Sparks", "Искры"), 400, 0, T_SPARK),
        Trail(n("Geist", "Ghost", "Призрак"), 600, c(0xFF7FE7FF), T_GHOST),
        Trail(n("Blitz", "Lightning", "Молния"), 800, c(0xFFFFF176), T_ZIGZAG, reqLevel = 15),
        Trail(n("Blasen", "Bubbles", "Пузыри"), 900, c(0xFF7FE7FF), T_BUBBLES, reqLevel = 25),
        Trail(n("Doppel", "Double", "Двойной"), 1000, c(0xFFF15BB5), T_DOUBLE, reqCrowns = 40),
        Trail(n("Sternenstaub", "Stardust", "Звёздная пыль"), 1300, 0, T_STARDUST, reqLevel = 35),
        Trail(n("Regenbogen", "Rainbow", "Радуга"), 1800, 0, T_RAINBOW, reqLevel = 45),
        Trail(n("Matrix", "Matrix", "Матрица"), 2200, c(0xFF2EE59D), T_MATRIX, reqCrowns = 100),
        Trail(n("Gold", "Gold", "Золото"), 3500, 0, T_GOLD, reqLevel = 75),
        Trail(n("Herzen", "Hearts", "Сердечки"), 1600, c(0xFFFF5C8A), T_HEARTS, reqLevel = 60),
        Trail(n("Kometenschweif", "Comet tail", "Хвост кометы"), 2000, c(0xFF7FE7FF), T_COMET, reqCrowns = 160),
        Trail(n("Pixel", "Pixels", "Пиксели"), 2700, 0, T_PIXEL, reqLevel = 95),
        Trail(n("Galaxie", "Galaxy", "Галактика"), 4500, c(0xFFB388FF), T_GALAXY, reqLevel = 130),
        Trail(n("Wellen", "Waves", "Волны"), 1200, c(0xFF4CC9F0), T_WAVES),
        Trail(n("Konfetti", "Confetti", "Конфетти"), 1500, 0, T_CONFETTI, reqLevel = 30),
        Trail(n("Schatten", "Shadow", "Тень"), 2500, c(0xFF2EE5C8), T_SHADOW, reqLevel = 205),
        Trail(n("Laser", "Laser", "Лазер"), 3000, c(0xFFFF1744), T_LASER, reqCrowns = 250),
        Trail(n("Eis", "Ice", "Лёд"), 1400, c(0xFFBDE8FF), T_ICE, reqLevel = 65),
        Trail(n("Punkte", "Dots", "Точки"), 700, c(0xFFFFFFFF), T_DOTS),
        Trail(n("Glut", "Embers", "Угли"), 3200, c(0xFFFF7A00), T_EMBERS, reqLevel = 260),
        Trail(n("Aurora", "Aurora", "Сияние"), 5000, 0, T_AURORA, reqCrowns = 400)
    )

    const val REVIVE_PRICE = 120
    const val REVIVE_MAX = 3
    const val SHIELD_PRICE = 60
    const val SHIELD_MAX = 5
}
