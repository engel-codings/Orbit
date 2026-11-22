package de.engelcode.orbit

import java.util.Locale

/**
 * Alle Texte des Spiels in drei Sprachen: Deutsch (0), Englisch (1), Russisch (2).
 * Jeder Eintrag ist ein Feld aus drei Texten. {0}, {1}, {2} werden durch Zahlen/Namen ersetzt.
 */
object L {

    const val DE = 0
    const val EN = 1
    const val RU = 2

    var cur = DE
    /** Wird bei jedem Sprachwechsel erhöht, damit zwischengespeicherte Texte neu gebaut werden. */
    var version = 0; private set

    val LANG_NAMES = arrayOf("Deutsch", "English", "Русский")

    fun systemDefault(): Int = when (Locale.getDefault().language) {
        "de" -> DE
        "ru", "uk", "be", "kk" -> RU
        else -> EN
    }

    fun set(lang: Int) {
        cur = lang.coerceIn(0, 2)
        version++
    }

    fun t(a: Array<String>): String = a[cur]

    fun f(a: Array<String>, vararg args: Any): String {
        var s = a[cur]
        for (i in args.indices) s = s.replace("{$i}", args[i].toString())
        return s
    }

    private fun ruPlural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10; val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    /** „Sterne“ in der richtigen Form für die Zahl n. */
    fun stars(n: Int): String = when (cur) {
        DE -> if (n == 1) "Stern" else "Sterne"
        EN -> if (n == 1) "star" else "stars"
        else -> ruPlural(n, "звезда", "звезды", "звёзд")
    }

    fun levels(n: Int): String = when (cur) {
        DE -> "Level"
        EN -> if (n == 1) "level" else "levels"
        else -> ruPlural(n, "уровень", "уровня", "уровней")
    }

    fun powerups(n: Int): String = when (cur) {
        DE -> if (n == 1) "Power-up" else "Power-ups"
        EN -> if (n == 1) "power-up" else "power-ups"
        else -> ruPlural(n, "бонус", "бонуса", "бонусов")
    }

    fun crowns(n: Int): String = when (cur) {
        DE -> if (n == 1) "Krone" else "Kronen"
        EN -> if (n == 1) "crown" else "crowns"
        else -> ruPlural(n, "корона", "короны", "корон")
    }

    private fun a(de: String, en: String, ru: String) = arrayOf(de, en, ru)

    // ------------------------------------------------------------- Menü
    val ENDLESS = a("ENDLOS", "ENDLESS", "БЕСКОНЕЧНО")
    val ENDLESS_BEST = a("ENDLOS  ·  {0}", "ENDLESS  ·  {0}", "БЕСКОНЕЧНО  ·  {0}")
    val CHALLENGE = a("CHALLENGE", "CHALLENGE", "ИСПЫТАНИЕ")
    val WHEEL = a("GLÜCKSRAD", "LUCKY WHEEL", "КОЛЕСО УДАЧИ")
    val SHOP = a("SHOP", "SHOP", "МАГАЗИН")
    val ACHIEVEMENTS = a("ERFOLGE", "ACHIEVEMENTS", "ДОСТИЖЕНИЯ")
    val STATS = a("STATISTIK", "STATS", "СТАТИСТИКА")
    val OPTIONS = a("OPTIONEN", "OPTIONS", "НАСТРОЙКИ")
    val LEVEL_N = a("LEVEL {0}", "LEVEL {0}", "УРОВЕНЬ {0}")
    val RANK = a("Rang: {0}  ·  {1} / {2}", "Rank: {0}  ·  {1} / {2}", "Ранг: {0}  ·  {1} / {2}")
    val RANK_MAX = a("Rang: {0}", "Rank: {0}", "Ранг: {0}")
    val RANKS = arrayOf(
        a("Kadett", "Cadet", "Кадет"),
        a("Pilot", "Pilot", "Пилот"),
        a("Kapitän", "Captain", "Капитан"),
        a("Kommandant", "Commander", "Командир"),
        a("Admiral", "Admiral", "Адмирал"),
        a("Legende", "Legend", "Легенда")
    )
    val JOURNEY = a("REISE", "JOURNEY", "ПУТЕШЕСТВИЕ")

    // ------------------------------------------------------------- Knöpfe
    val RESUME = a("WEITER", "RESUME", "ПРОДОЛЖИТЬ")
    val RESTART = a("NEUSTART", "RESTART", "ЗАНОВО")
    val MENU = a("MENÜ", "MENU", "МЕНЮ")
    val REVIVE_BTN = a("WIEDERBELEBEN  ({0})", "REVIVE  ({0})", "ВОЗРОДИТЬСЯ  ({0})")
    val AGAIN = a("NOCHMAL", "TRY AGAIN", "ЕЩЁ РАЗ")
    val NEXT_LEVEL = a("NÄCHSTES LEVEL", "NEXT LEVEL", "СЛЕДУЮЩИЙ УРОВЕНЬ")
    val TO_MAP = a("ZUR KARTE", "TO THE MAP", "К КАРТЕ")
    val PAUSE = a("PAUSE", "PAUSED", "ПАУЗА")
    val RESET_BTN = a("FORTSCHRITT ZURÜCKSETZEN", "RESET PROGRESS", "СБРОСИТЬ ПРОГРЕСС")
    val CANCEL = a("ABBRECHEN", "CANCEL", "ОТМЕНА")
    val BUY = a("KAUFEN", "BUY", "КУПИТЬ")
    val BUY_PRICE = a("KAUFEN  ·  {0}", "BUY  ·  {0}", "КУПИТЬ  ·  {0}")
    val FULL = a("VOLL", "FULL", "МАКСИМУМ")
    val FREE_SPIN = a("GRATIS DREHEN", "FREE SPIN", "КРУТИТЬ БЕСПЛАТНО")
    val SPIN_PRICE = a("DREHEN  ·  {0}", "SPIN  ·  {0}", "КРУТИТЬ  ·  {0}")
    val SPIN_FREE_HINT = a("1x täglich gratis", "1 free spin every day", "1 раз в день бесплатно")
    val SPIN_NEXT_HINT = a("Nächste Gratis-Drehung: morgen", "Next free spin: tomorrow", "Следующее бесплатное вращение – завтра")
    val LETS_GO = a("LOS GEHT'S", "LET'S GO", "ВПЕРЁД")
    val CLAIM = a("ABHOLEN", "CLAIM", "ЗАБРАТЬ")
    val DELETE = a("LÖSCHEN", "DELETE", "УДАЛИТЬ")

    // ------------------------------------------------------------- Spiel
    val LEVEL_LOST = a("LEVEL {0} VERLOREN", "LEVEL {0} FAILED", "УРОВЕНЬ {0} ПРОВАЛЕН")
    val DAILY_CHALLENGE = a("TAGES-CHALLENGE", "DAILY CHALLENGE", "ЕЖЕДНЕВНОЕ ИСПЫТАНИЕ")
    val LOST_IN_SPACE = a("VERLOREN IM ALL", "LOST IN SPACE", "ПОТЕРЯН В КОСМОСЕ")
    val PLANETS_DONE = a("Planeten geschafft", "planets reached", "планет пройдено")
    val NEW_RECORD = a("NEUER REKORD!", "NEW RECORD!", "НОВЫЙ РЕКОРД!")
    val RECORD = a("Rekord: {0}", "Best: {0}", "Рекорд: {0}")
    val REACHED = a("ERREICHT!", "REACHED!", "ЦЕЛЬ ДОСТИГНУТА!")
    val COMPLETE = a("GESCHAFFT!", "COMPLETE!", "ПРОЙДЕНО!")
    val CRIT_DONE = a("Level geschafft", "Level complete", "Уровень пройден")
    val CRIT_NO_REVIVE = a("Ohne Wiederbelebung", "No revive used", "Без возрождения")
    val CRIT_PERFECT = a("{0} / {1} perfekte Landungen", "{0} / {1} perfect landings", "{0} / {1} идеальных посадок")
    val NEW_CROWN = a("Neue Krone!", "New crown!", "Новая корона!")
    val REWARD_ONCE = a("Belohnung nur beim ersten Mal", "Reward only the first time", "Награда только за первое прохождение")
    val JOURNEY_DONE = a("Du hast die Reise geschafft!", "You completed the journey!", "Ты завершил путешествие!")
    val TUTORIAL_1 = a("Tippe, wenn die Punkte", "Tap when the dots", "Нажми, когда точки")
    val TUTORIAL_2 = a("zum nächsten Planeten zeigen", "point to the next planet", "указывают на следующую планету")
    val CHALLENGE_PROGRESS = a("CHALLENGE  {0} / {1}", "CHALLENGE  {0} / {1}", "ИСПЫТАНИЕ  {0} / {1}")
    val CHALLENGE_DONE = a("CHALLENGE GESCHAFFT", "CHALLENGE COMPLETE", "ИСПЫТАНИЕ ПРОЙДЕНО")
    val PERFECT_POP = a("PERFEKT +{0}", "PERFECT +{0}", "ИДЕАЛЬНО +{0}")
    val GOAL = a("ZIEL", "GOAL", "ЦЕЛЬ")
    val DIMENSION = a("DIMENSION {0}", "DIMENSION {0}", "ИЗМЕРЕНИЕ {0}")
    val DIM_NAMES = arrayOf(
        a("Unser Universum", "Our universe", "Наша вселенная"),
        a("Spiegelwelt", "Mirror world", "Зеркальный мир"),
        a("Die Leere", "The Void", "Пустота"),
        a("Sonnensturm", "Solar storm", "Солнечная буря")
    )
    val DIM4_HINT = a("Sonnensturm: Der Wind treibt dich ab!", "Solar storm: the wind pushes you!", "Солнечная буря: ветер сносит тебя!")
    val DIM_RULES = arrayOf(
        a("Normale Schwerkraft", "Normal gravity", "Обычная гравитация"),
        a("Planeten stoßen dich ab", "Planets push you away", "Планеты отталкивают тебя"),
        a("Keine Schwerkraft", "No gravity", "Нет гравитации"),
        a("Sonnenwind treibt dich ab", "Solar wind pushes you", "Солнечный ветер сносит тебя")
    )
    val ENDLESS_TITLE = a("ENDLOS-MODUS", "ENDLESS MODE", "БЕСКОНЕЧНЫЙ РЕЖИМ")
    val ENDLESS_LOCKED = a("Erreiche zuerst Level {0}", "Reach level {0} first", "Сначала дойди до уровня {0}")
    val MUSIC_TRACKS = arrayOf(a("Kosmos", "Cosmos", "Космос"), a("Puls", "Pulse", "Пульс"))
    val DIM3_HINT = a("Die Leere: keine Schwerkraft – ziele genau!", "The Void: no gravity – aim precisely!", "Пустота: нет гравитации – целься точно!")
    val DIM2_HINT = a("Spiegelwelt: Planeten stoßen dich ab!", "Mirror world: planets push you away!", "Зеркальный мир: планеты отталкивают тебя!")

    // ------------------------------------------------------------- Meldungen
    val T_START_SHIELD = a("Start-Schild aktiv", "Starting shield active", "Стартовый щит активен")
    val T_SHIELD = a("Schild aktiv!", "Shield active!", "Щит активен!")
    val T_MAGNET = a("Sternen-Magnet!", "Star magnet!", "Звёздный магнит!")
    val T_SAVED = a("Der Schild hat dich gerettet!", "The shield saved you!", "Щит спас тебя!")
    val T_CHALLENGE = a("Challenge geschafft! +{0} {1}", "Challenge complete! +{0} {1}", "Испытание пройдено! +{0} {1}")
    val T_PLUS_STARS = a("+{0} {1}!", "+{0} {1}!", "+{0} {1}!")
    val T_RESET = a("Fortschritt gelöscht", "Progress deleted", "Прогресс удалён")
    val T_MISSING = a("Dir fehlen noch {0} {1}", "You need {0} more {1}", "Не хватает ещё {0} {1}")
    val T_LEVEL_FIRST = a("Schaffe zuerst Level {0}", "Complete level {0} first", "Сначала пройди уровень {0}")
    val T_NEED_CROWNS = a("Du brauchst {0} {1}", "You need {0} {1}", "Нужно {0} {1}")
    val T_MAX_REVIVES = a("Maximal {0} Wiederbelebungen", "Maximum of {0} revives", "Максимум {0} возрождения")
    val T_MAX_SHIELDS = a("Maximal {0} Start-Schilde", "Maximum of {0} starting shields", "Максимум {0} стартовых щитов")
    val T_BOUGHT = a("Gekauft!", "Purchased!", "Куплено!")
    val T_UNLOCKED = a("Freigeschaltet!", "Unlocked!", "Открыто!")
    val T_WON_STARS = a("Gewonnen: {0} {1}!", "You won {0} {1}!", "Выигрыш: {0} {1}!")
    val T_WON_REVIVE = a("Gewonnen: 1 Wiederbelebung!", "You won 1 revive!", "Выигрыш: 1 возрождение!")
    val T_WON_SHIELD = a("Gewonnen: 1 Start-Schild!", "You won 1 starting shield!", "Выигрыш: 1 стартовый щит!")

    // ------------------------------------------------------------- Dialoge
    val BUY_TITLE = a("Kaufen?", "Buy?", "Купить?")
    val BUY_FOR = a("für {0} {1}", "for {0} {1}", "за {0} {1}")
    val ITEM_SKIN = a("Skin „{0}“", "Skin “{0}”", "Скин «{0}»")
    val ITEM_TRAIL = a("Spur „{0}“", "Trail “{0}”", "След «{0}»")
    val ITEM_REVIVE = a("1 Wiederbelebung", "1 revive", "1 возрождение")
    val ITEM_SHIELD = a("1 Start-Schild", "1 starting shield", "1 стартовый щит")
    val RESET_TITLE = a("Wirklich zurücksetzen?", "Really reset?", "Точно сбросить?")
    val RESET_LINE_1 = a("Sterne, Skins, Level, Kronen", "Stars, skins, levels, crowns", "Звёзды, скины, уровни, короны")
    val RESET_LINE_2 = a("und Erfolge werden gelöscht.", "and achievements will be deleted.", "и достижения будут удалены.")
    val DAILY_TITLE = a("Täglicher Bonus", "Daily bonus", "Ежедневный бонус")
    val DAILY_DAY = a("Tag {0} in Folge", "Day {0} in a row", "День {0} подряд")
    val NEWS_TITLE = a("Neu in Version {0}", "New in version {0}", "Новое в версии {0}")
    val NEWS = arrayOf(
        a("• 300 Level: Dimension 4 „Sonnensturm“", "• 300 levels: dimension 4 “Solar storm”", "• 300 уровней: измерение 4 «Солнечная буря»"),
        a("• Endlos-Modus in jeder Dimension", "• Endless mode in every dimension", "• Бесконечный режим в каждом измерении"),
        a("• Zweites Musikstück „Puls“", "• Second music track “Pulse”", "• Второй музыкальный трек «Пульс»"),
        a("• 50 Skins und 24 Spuren", "• 50 skins and 24 trails", "• 50 скинов и 24 следа"),
        a("• 44 Erfolge", "• 44 achievements", "• 44 достижения")
    )

    // ------------------------------------------------------------- Missionen & Truhe
    val MISSIONS = a("MISSIONEN", "MISSIONS", "МИССИИ")
    val MISSION_DESC = arrayOf(
        a("Sammle {0} {1}", "Collect {0} {1}", "Собери {0} {1}"),
        a("{0} perfekte Landungen", "{0} perfect landings", "{0} идеальных посадок"),
        a("Erreiche {0} Planeten", "Reach {0} planets", "Достигни {0} планет"),
        a("Schaffe {0} {1}", "Complete {0} {1}", "Пройди {0} {1}"),
        a("{0} Punkte in einer Endlos-Runde", "{0} points in one endless run", "{0} очков за одну бесконечную игру"),
        a("Sammle {0} {1}", "Collect {0} {1}", "Собери {0} {1}"),
        a("Drehe am Glücksrad", "Spin the lucky wheel", "Покрути колесо удачи")
    )
    val MISSION_NEW_IN = a("Neue Missionen in {0} h {1} min", "New missions in {0} h {1} min", "Новые миссии через {0} ч {1} мин")
    val MISSION_DONE_ALL = a("Alle Missionen erledigt – stark!", "All missions done – great job!", "Все миссии выполнены – отлично!")
    val T_MISSION = a("Mission erfüllt! Hol dir die Belohnung", "Mission complete! Claim your reward", "Миссия выполнена! Забери награду")
    val CHEST = a("TRUHE", "CHEST", "СУНДУК")
    val T_CHEST_WAIT = a("Truhe bereit in {0} h {1} min", "Chest ready in {0} h {1} min", "Сундук будет готов через {0} ч {1} мин")
    val T_CHEST = a("Truhe geöffnet: +{0} {1}!", "Chest opened: +{0} {1}!", "Сундук открыт: +{0} {1}!")

    // ------------------------------------------------------------- Shop
    val TABS = arrayOf(a("SKINS", "SKINS", "СКИНЫ"), a("SPUREN", "TRAILS", "СЛЕДЫ"), a("EXTRAS", "EXTRAS", "БОНУСЫ"))
    val REQ_LEVEL = a("Ab Level {0}", "From level {0}", "С уровня {0}")
    val REQ_CROWNS = a("{0} {1} nötig", "{0} {1} needed", "Нужно {0} {1}")
    val ACTIVE = a("AKTIV", "ACTIVE", "ВЫБРАН")
    val SELECT = a("Auswählen", "Select", "Выбрать")
    val FREE = a("GRATIS", "FREE", "БЕСПЛАТНО")
    val X_REVIVE = a("Wiederbelebung", "Revive", "Возрождение")
    val X_REVIVE_DESC = a("Nach einem Absturz weiterspielen", "Keep playing after a crash", "Продолжай игру после крушения")
    val X_SHIELD = a("Start-Schild", "Starting shield", "Стартовый щит")
    val X_SHIELD_DESC = a("Schützt einmal – startet automatisch", "Protects once – activates automatically", "Защищает один раз – включается сам")

    // ------------------------------------------------------------- Einstellungen
    val SETTINGS = arrayOf(
        a("Soundeffekte", "Sound effects", "Звуковые эффекты"),
        a("Musik", "Music", "Музыка"),
        a("Vibration", "Vibration", "Вибрация"),
        a("Bildschirmwackeln", "Screen shake", "Тряска экрана"),
        a("FPS-Anzeige", "FPS counter", "Счётчик FPS"),
        a("Grafik", "Graphics", "Графика"),
        a("Sprache", "Language", "Язык"),
        a("Musikstück", "Music track", "Музыка: трек")
    )
    val GFX_HIGH = a("Hoch", "High", "Высокая")
    val GFX_LOW = a("Sparsam", "Low", "Экономная")
    val GFX_HINT = a("Grafik „Sparsam“ = noch flüssiger auf alten Handys", "“Low” graphics = even smoother on old phones", "Графика «Экономная» – ещё плавнее на старых телефонах")

    // ------------------------------------------------------------- Statistik
    val STAT_LABELS = arrayOf(
        a("Level geschafft", "Levels completed", "Пройдено уровней"),
        a("Kronen", "Crowns", "Короны"),
        a("Rekord (Endlos)", "Best (endless)", "Рекорд (бесконечно)"),
        a("Spiele", "Games", "Игры"),
        a("Planeten erreicht", "Planets reached", "Достигнуто планет"),
        a("Perfekte Landungen", "Perfect landings", "Идеальные посадки"),
        a("Beste Perfekt-Serie", "Best perfect streak", "Лучшая серия"),
        a("Sterne gesammelt", "Stars collected", "Собрано звёзд"),
        a("Power-ups", "Power-ups", "Бонусы"),
        a("Wiederbelebungen", "Revives", "Возрождения"),
        a("Tages-Challenges", "Daily challenges", "Ежедневные испытания"),
        a("Glücksrad", "Lucky wheel", "Колесо удачи"),
        a("Missionen", "Missions", "Миссии"),
        a("Truhen", "Chests", "Сундуки"),
        a("Spielzeit", "Play time", "Время игры")
    )
    val TIME_HM = a("{0} h {1} min", "{0} h {1} min", "{0} ч {1} мин")
    val TIME_M = a("{0} min", "{0} min", "{0} мин")
}
