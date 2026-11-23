package de.engelcode.orbit


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

    fun systemDefault(): Int = when (Platform.systemLanguage()) {
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
        a("Sonnensturm", "Solar storm", "Солнечная буря"),
        a("Frostwelt", "Frost world", "Ледяной мир"),
        a("Hyperraum", "Hyperspace", "Гиперпространство")
    )
    val DIM5_HINT = a("Frostwelt: Die Kälte bremst deinen Flug!", "Frost world: the cold slows your flight!", "Ледяной мир: холод замедляет полёт!")
    val DIM6_HINT = a("Hyperraum: Alles ist schneller!", "Hyperspace: everything is faster!", "Гиперпространство: всё быстрее!")
    val DIM4_HINT = a("Sonnensturm: Der Wind treibt dich ab!", "Solar storm: the wind pushes you!", "Солнечная буря: ветер сносит тебя!")
    val DIM_RULES = arrayOf(
        a("Normale Schwerkraft", "Normal gravity", "Обычная гравитация"),
        a("Planeten stoßen dich ab", "Planets push you away", "Планеты отталкивают тебя"),
        a("Keine Schwerkraft", "No gravity", "Нет гравитации"),
        a("Sonnenwind treibt dich ab", "Solar wind pushes you", "Солнечный ветер сносит тебя"),
        a("Kälte bremst dich", "The cold slows you", "Холод тормозит тебя"),
        a("Alles ist schneller", "Everything is faster", "Всё быстрее")
    )
    val ENDLESS_TITLE = a("SPIELMODI", "GAME MODES", "РЕЖИМЫ ИГРЫ")
    val ONLINE = a("ONLINE", "ONLINE", "ОНЛАЙН")
    val LEADERBOARD = a("BESTENLISTE", "LEADERBOARD", "ТАБЛИЦА ЛИДЕРОВ")
    val LB_TABS = arrayOf(a("LEVEL", "LEVEL", "УРОВЕНЬ"), a("ENDLOS", "ENDLESS", "БЕСКОНЕЧНО"), a("SIEGE", "WINS", "ПОБЕДЫ"))
    val VS_QUICK = a("1v1 SCHNELLSPIEL", "1v1 QUICK MATCH", "1 НА 1: БЫСТРАЯ ИГРА")
    val FRIENDS = a("FREUNDE", "FRIENDS", "ДРУЗЬЯ")
    val ACCOUNT = a("KONTO", "ACCOUNT", "АККАУНТ")
    val LOGIN = a("ANMELDEN", "LOG IN", "ВОЙТИ")
    val REGISTER = a("REGISTRIEREN", "REGISTER", "РЕГИСТРАЦИЯ")
    val LOGOUT = a("ABMELDEN", "LOG OUT", "ВЫЙТИ")
    val RENAME = a("NAME ÄNDERN", "CHANGE NAME", "СМЕНИТЬ ИМЯ")
    val ASK_NAME = a("Benutzername (3–16 Zeichen)", "Username (3–16 characters)", "Имя пользователя (3–16 символов)")
    val ASK_NEW_NAME = a("Neuer Benutzername", "New username", "Новое имя пользователя")
    val ASK_PW = a("Passwort (mind. 6 Zeichen)", "Password (at least 6 characters)", "Пароль (минимум 6 символов)")
    val ASK_EMAIL = a("E-Mail-Adresse", "Email address", "Адрес электронной почты")
    val FORGOT_PW = a("Passwort vergessen?", "Forgot password?", "Забыли пароль?")
    val NET_EMAIL_TAKEN = a("Diese E-Mail hat schon ein Konto – bitte anmelden", "This email already has an account – please log in", "Для этой почты уже есть аккаунт – войди")
    val NET_BAD_EMAIL = a("Ungültige E-Mail-Adresse", "Invalid email address", "Неверный адрес почты")
    val NET_TOO_MANY = a("Zu viele Versuche – bitte später nochmal", "Too many attempts – please try later", "Слишком много попыток – попробуй позже")
    val NET_RESET_SENT = a("E-Mail zum Zurücksetzen wurde gesendet", "Password reset email sent", "Письмо для сброса пароля отправлено")
    val ASK_FRIEND = a("Benutzername deines Freundes", "Your friend's username", "Имя пользователя друга")
    val NET_NAME_TAKEN = a("Dieser Name ist schon vergeben", "This name is already taken", "Это имя уже занято")
    val NET_OFFLINE = a("Keine Verbindung zum Server", "No connection to the server", "Нет связи с сервером")
    val NET_WRONG = a("E-Mail oder Passwort falsch", "Wrong email or password", "Неверная почта или пароль")
    val NET_NOT_FOUND = a("Spieler nicht gefunden", "Player not found", "Игрок не найден")
    val NET_FRIEND_ADDED = a("{0} ist jetzt dein Freund", "{0} is now your friend", "{0} теперь твой друг")
    val NET_GONE = a("Das Spiel gibt es nicht mehr", "This match no longer exists", "Этой игры больше нет")
    val NET_NO_ANSWER = a("Keine Antwort auf die Einladung", "No answer to the invitation", "Нет ответа на приглашение")
    val NET_BAD_NAME = a("Name: 3–16 Buchstaben oder Zahlen", "Name: 3–16 letters or numbers", "Имя: 3–16 букв или цифр")
    val NET_BAD_PW = a("Passwort: mindestens 6 Zeichen", "Password: at least 6 characters", "Пароль: минимум 6 символов")
    val NET_LOGIN_NEEDED = a("Melde dich zuerst an", "Please log in first", "Сначала войди в аккаунт")
    val NET_WELCOME = a("Willkommen, {0}!", "Welcome, {0}!", "Добро пожаловать, {0}!")
    val NET_RENAMED = a("Neuer Name: {0}", "New name: {0}", "Новое имя: {0}")
    val NET_LOADING = a("Lade…", "Loading…", "Загрузка…")
    val NET_CLOUD = a("Spielstand online gespeichert (iPhone & Android)", "Progress saved online (iPhone & Android)", "Прогресс сохранён онлайн (iPhone и Android)")
    val NET_GUEST = a("Nicht angemeldet", "Not logged in", "Вход не выполнен")
    val NET_GUEST_INFO = a("Melde dich an für Bestenliste, 1v1 und Freunde", "Log in for leaderboard, 1v1 and friends", "Войди для таблицы лидеров, 1 на 1 и друзей")
    val NET_PW_HINT = a("Tipp: Benutze kein wichtiges Passwort", "Tip: don't use an important password", "Совет: не используй важный пароль")
    val NET_LOGGED_OUT = a("Abgemeldet", "Logged out", "Ты вышел из аккаунта")
    val QUEUE_SEARCH = a("Suche Gegner…", "Searching for opponent…", "Поиск соперника…")
    val QUEUE_HINT = a("Sobald ein anderer Spieler 1v1 wählt, geht es los", "As soon as another player picks 1v1, it starts", "Игра начнётся, как только другой игрок выберет 1 на 1")
    val INVITE_WAIT = a("Warte auf {0}…", "Waiting for {0}…", "Ждём {0}…")
    val INVITE_SENT = a("Einladung gesendet", "Invitation sent", "Приглашение отправлено")
    val ADD_FRIEND = a("+ FREUND HINZUFÜGEN", "+ ADD FRIEND", "+ ДОБАВИТЬ ДРУГА")
    val DUEL = a("DUELL", "DUEL", "ДУЭЛЬ")
    val NET_ON = a("online", "online", "в сети")
    val NET_OFF = a("offline", "offline", "не в сети")
    val INVITE_TITLE = a("Duell-Einladung!", "Duel invitation!", "Вызов на дуэль!")
    val INVITE_TEXT = a("{0} fordert dich heraus", "{0} challenges you", "{0} бросает тебе вызов")
    val ACCEPT = a("ANNEHMEN", "ACCEPT", "ПРИНЯТЬ")
    val DECLINE = a("ABLEHNEN", "DECLINE", "ОТКЛОНИТЬ")
    val REMOVE_FRIEND = a("Freund entfernen?", "Remove friend?", "Удалить друга?")
    val REMOVE = a("ENTFERNEN", "REMOVE", "УДАЛИТЬ")
    val VS_YOU = a("DU", "YOU", "ТЫ")
    val VS_WIN = a("GEWONNEN!", "YOU WIN!", "ПОБЕДА!")
    val VS_LOSE = a("VERLOREN", "YOU LOSE", "ПОРАЖЕНИЕ")
    val VS_DRAW = a("UNENTSCHIEDEN", "DRAW", "НИЧЬЯ")
    val VS_WAITING = a("Warte, bis {0} fertig ist…", "Waiting for {0} to finish…", "Ждём, пока {0} закончит…")
    val VS_LEFT = a("Gegner hat das Spiel verlassen", "Opponent left the match", "Соперник покинул игру")
    val VS_RULES = a("Gleiche Strecke – wer mehr Punkte schafft, gewinnt", "Same course – most points wins", "Одна трасса – побеждает тот, у кого больше очков")
    val VS_AGAIN = a("NOCHMAL", "AGAIN", "ЕЩЁ РАЗ")
    val NO_FRIENDS = a("Noch keine Freunde – füge welche hinzu!", "No friends yet – add some!", "Друзей пока нет – добавь кого-нибудь!")
    val RANK_YOU = a("Dein Platz: {0}", "Your rank: {0}", "Твоё место: {0}")
    val NET_GOOGLE = a("Konto gilt auf iPhone und Android", "Account works on iPhone and Android", "Аккаунт работает на iPhone и Android")
    val GUEST_PLAY = a("ALS GAST SPIELEN", "PLAY AS GUEST", "ИГРАТЬ КАК ГОСТЬ")
    val WELCOME_Q = a("Wie möchtest du spielen?", "How do you want to play?", "Как ты хочешь играть?")
    val WELCOME_GUEST = a("Gast: zufälliger Name – du kannst dich später registrieren", "Guest: random name – you can register later", "Гость: случайное имя – зарегистрироваться можно позже")
    val WELCOME_ACC = a("Konto: Spielstand auf iPhone & Android, Freunde und Duelle", "Account: progress on iPhone & Android, friends and duels", "Аккаунт: прогресс на iPhone и Android, друзья и дуэли")
    val NET_GUEST_ACC = a("Gast-Konto – registriere dich, um alles zu sichern", "Guest account – register to keep everything", "Гостевой аккаунт – зарегистрируйся, чтобы всё сохранить")
    val NET_FRIENDS_NEED = a("Für Freunde brauchst du ein Konto", "You need an account for friends", "Для друзей нужен аккаунт")
    val SKIP_POP = a("SPRUNG +{0}", "SKIP +{0}", "ПРЫЖОК +{0}")
    val T_REDIRECT_HINT = a("Tipp: Im Flug tippen = Umlenken", "Tip: tap in flight to redirect", "Совет: нажми в полёте, чтобы сменить курс")
    val TIME_TRIAL = a("ZEITRENNEN", "TIME TRIAL", "ГОНКА НА ВРЕМЯ")
    val TT_NAMES = arrayOf(a("Kurze Strecke", "Short course", "Короткая трасса"), a("Mittlere Strecke", "Medium course", "Средняя трасса"),
        a("Lange Strecke", "Long course", "Длинная трасса"), a("Dimensionen-Tour", "Dimension tour", "Тур по измерениям"))
    val TT_LEVELS = a("{0} {1} am Stück", "{0} {1} in a row", "{0} {1} подряд")
    val TT_BEST = a("Bestzeit: {0}", "Best time: {0}", "Лучшее время: {0}")
    val TT_NONE = a("Noch keine Zeit", "No time yet", "Времени пока нет")
    val TT_PENALTY = a("+5 Sekunden", "+5 seconds", "+5 секунд")
    val TT_DONE = a("ZIEL ERREICHT!", "FINISHED!", "ФИНИШ!")
    val TT_NEW_BEST = a("NEUE BESTZEIT!", "NEW BEST TIME!", "НОВЫЙ РЕКОРД!")
    val LEVEL_OF = a("LEVEL {0} / {1}", "LEVEL {0} / {1}", "УРОВЕНЬ {0} / {1}")
    val UPG_NAMES = arrayOf(a("Umlenken", "Redirect", "Смена курса"), a("Fangzone", "Capture zone", "Зона захвата"),
        a("Magnet-Dauer", "Magnet time", "Время магнита"), a("Sternen-Bonus", "Star bonus", "Бонус звёзд"), a("Zielhilfe", "Aim assist", "Прицел"))
    val UPG_DESC = arrayOf(
        a("+1 Umlenkung pro Lauf (im Flug tippen)", "+1 redirect per run (tap in flight)", "+1 смена курса за забег (нажми в полёте)"),
        a("Planeten fangen dich früher", "Planets catch you earlier", "Планеты ловят тебя раньше"),
        a("+4 Sekunden Magnet", "+4 seconds of magnet", "+4 секунды магнита"),
        a("+10 % Sterne pro Stufe", "+10% stars per level", "+10 % звёзд за уровень"),
        a("Längere Ziellinie", "Longer aim line", "Длиннее линия прицела"))
    val ITEM_UPG = a("{0} – Stufe {1}", "{0} – level {1}", "{0} – уровень {1}")
    val MAX = a("MAX", "MAX", "МАКС")
    val ENDLESS_LOCKED = a("Erreiche zuerst Level {0}", "Reach level {0} first", "Сначала дойди до уровня {0}")
    val MUSIC_TRACKS = arrayOf(a("Kosmos", "Cosmos", "Космос"), a("Puls", "Pulse", "Пульс"), a("Nebel", "Nebula", "Туманность"), a("Arcade", "Arcade", "Аркада"))
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
        a("• ONLINE: Konto mit Login & Cloud-Speicher", "• ONLINE: account with login & cloud save", "• ОНЛАЙН: аккаунт со входом и облачным сохранением"),
        a("• Weltweite Bestenliste (live)", "• Worldwide leaderboard (live)", "• Мировая таблица лидеров (в реальном времени)"),
        a("• 1v1 gegen echte Spieler", "• 1v1 against real players", "• 1 на 1 против реальных игроков"),
        a("• Freundesliste und Duell-Einladungen", "• Friends list and duel invitations", "• Список друзей и вызовы на дуэль"),
        a("• iPhone und Android spielen zusammen", "• iPhone and Android play together", "• iPhone и Android играют вместе"),
        a("• Absturz-Schutz für ältere Handys", "• Crash protection for older phones", "• Защита от вылетов на старых телефонах")
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
    val TABS = arrayOf(a("SKINS", "SKINS", "СКИНЫ"), a("SPUREN", "TRAILS", "СЛЕДЫ"), a("EXTRAS", "EXTRAS", "БОНУСЫ"), a("KÖNNEN", "SKILLS", "НАВЫКИ"))
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
        a("Musikstück", "Music track", "Музыка: трек"),
        a("Als App installieren", "Install as app", "Установить как приложение")
    )
    val GFX_HIGH = a("Hoch", "High", "Высокая")
    val GFX_MEDIUM = a("Mittel", "Medium", "Средняя")
    val GFX_LOW = a("Sparsam", "Low", "Экономная")
    val GFX_HINT = a("Grafik „Mittel“ oder „Sparsam“ = flüssiger auf alten Handys", "“Medium” or “Low” graphics = smoother on old phones", "Графика «Средняя» или «Экономная» – плавнее на старых телефонах")

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
        a("Umlenkungen", "Redirects", "Смены курса"),
        a("Übersprungene Planeten", "Planets skipped", "Пропущено планет"),
        a("Spielzeit", "Play time", "Время игры")
    )
    val TIME_HM = a("{0} h {1} min", "{0} h {1} min", "{0} ч {1} мин")
    val TIME_M = a("{0} min", "{0} min", "{0} мин")
}
