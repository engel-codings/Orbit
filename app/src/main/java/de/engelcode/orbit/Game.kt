package de.engelcode.orbit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.view.MotionEvent
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * ORBIT – Bildschirme, Menüs, Shop, Level-Karte, Erfolge, Einstellungen.
 * Die eigentliche Spielwelt steckt in World.kt.
 */
class Game(context: Context) : World.Listener {

    companion object {
        const val VERSION_CODE = 4
        const val VERSION_NAME = "0.4.0"
        const val W = 1000f

        // Knopf-Stile
        private const val S_PRIMARY = 0
        private const val S_GLASS = 1
        private const val S_DANGER = 2
        private const val S_ROUND = 3
        private const val S_BACK = 4
        private const val S_GREEN = 5
        private const val S_INVISIBLE = 6
        private const val S_GOLD = 7

        // Dialoge
        private const val D_NONE = 0
        private const val D_BUY = 1
        private const val D_RESET = 2
        private const val D_DAILY = 3
        private const val D_NEWS = 4

        private val NEWS = arrayOf(
            "• 150 Level: bis zum Urknall",
            "• Glücksrad: täglich gratis drehen",
            "• Tages-Challenge mit Belohnung",
            "• 32 Skins und 16 Spuren",
            "• Piloten-Ränge: Kadett bis Legende",
            "• 30 Erfolge",
            "• Bildschirmwackeln abschaltbar"
        )
        private const val CHALLENGE_GOAL = 15
        private const val CHALLENGE_REWARD = 60
        private const val SPIN_PRICE = 75

        // Glücksrad: 0 = Sterne, 1 = Wiederbelebung, 2 = Start-Schild
        private val WHEEL_KIND = intArrayOf(0, 0, 1, 0, 0, 2, 0, 0)
        private val WHEEL_VAL = intArrayOf(20, 50, 1, 10, 100, 1, 30, 15)
        private val WHEEL_WEIGHT = intArrayOf(20, 10, 8, 22, 4, 10, 14, 12)
        private val WHEEL_COL = intArrayOf(
            0xFF7B61FF.toInt(), 0xFF4CC9F0.toInt(), 0xFF2EE59D.toInt(), 0xFFF15BB5.toInt(),
            0xFFFFD166.toInt(), 0xFF5CC8FF.toInt(), 0xFFFF9F1C.toInt(), 0xFF9B5DE5.toInt()
        )
        private val RANK_NAMES = arrayOf("Kadett", "Pilot", "Kapitän", "Kommandant", "Admiral", "Legende")
        private val RANK_AT = intArrayOf(0, 50, 200, 500, 1000, 2500)
        private val TAB_NAMES = arrayOf("SKINS", "SPUREN", "EXTRAS")
    }

    private enum class Screen { MENU, MAP, PLAYING, PAUSED, GAMEOVER, LEVELDONE, SHOP, SETTINGS, STATS, ACHIEVE, WHEEL }

    private class Btn(val id: Int, var label: String, val style: Int, val icon: Int = -1) {
        var x = 0f; var y = 0f; var w = 0f; var h = 0f
        var visible = true
        var shader: Shader? = null
        fun set(x: Float, y: Float, w: Float, h: Float) { this.x = x; this.y = y; this.w = w; this.h = h }
        fun hit(px: Float, py: Float) = visible && px >= x && px <= x + w && py >= y && py <= y + h
    }

    private val save = Save(context)
    private val sound = Sound(context, save)
    private val gfx = Gfx(context)
    private val world = World(gfx)

    private var scale = 1f
    private var H = 1800f
    private var ready = false

    private var screen = Screen.MENU
    private var screenTime = 0f
    private var time = 0f

    // ---- Knöpfe
    private val bPlay = Btn(1, "", S_INVISIBLE)
    private val bEndless = Btn(2, "ENDLOS", S_GLASS, Gfx.I_INFINITY)
    private val bShop = Btn(3, "SHOP", S_ROUND, Gfx.I_SHOP)
    private val bAch = Btn(4, "ERFOLGE", S_ROUND, Gfx.I_TROPHY)
    private val bStats = Btn(5, "STATISTIK", S_ROUND, Gfx.I_CHART)
    private val bSettings = Btn(6, "OPTIONEN", S_ROUND, Gfx.I_GEAR)
    private val bBack = Btn(7, "", S_BACK)
    private val bPause = Btn(8, "", S_BACK)
    private val bResume = Btn(9, "WEITER", S_PRIMARY)
    private val bRestart = Btn(10, "NEUSTART", S_GLASS)
    private val bQuit = Btn(11, "MENÜ", S_GLASS)
    private val bRevive = Btn(12, "WIEDERBELEBEN", S_GREEN)
    private val bAgain = Btn(13, "NOCHMAL", S_PRIMARY)
    private val bLeave = Btn(14, "MENÜ", S_GLASS)
    private val bNext = Btn(15, "NÄCHSTES LEVEL", S_PRIMARY)
    private val bToMap = Btn(16, "ZUR KARTE", S_GLASS)
    private val bTab = Array(3) { Btn(17 + it, TAB_NAMES[it], S_INVISIBLE) }
    private val bRow = Array(6) { Btn(40 + it, "", S_INVISIBLE) }
    private val bReset = Btn(30, "FORTSCHRITT ZURÜCKSETZEN", S_DANGER)
    private val bDlgA = Btn(31, "OK", S_PRIMARY)
    private val bDlgB = Btn(32, "ABBRECHEN", S_GLASS)
    private val bBuyRevive = Btn(33, "KAUFEN", S_GOLD)
    private val bBuyShield = Btn(34, "KAUFEN", S_GOLD)
    private val bChallenge = Btn(35, "CHALLENGE", S_GLASS, Gfx.I_TARGET)
    private val bWheel = Btn(36, "GLÜCKSRAD", S_GLASS, Gfx.I_WHEEL)
    private val bSpin = Btn(37, "GRATIS DREHEN", S_GOLD)

    private val menuBtns = arrayOf(bPlay, bEndless, bChallenge, bWheel, bShop, bAch, bStats, bSettings)
    private val playBtns = arrayOf(bPause)
    private val pauseBtns = arrayOf(bResume, bRestart, bQuit)
    private val overBtns = arrayOf(bRevive, bAgain, bLeave)
    private val doneBtns = arrayOf(bNext, bToMap)
    private val shopBtns = arrayOf(bBack, bTab[0], bTab[1], bTab[2], bBuyRevive, bBuyShield)
    private val setBtns = arrayOf(bBack, bRow[0], bRow[1], bRow[2], bRow[3], bRow[4], bRow[5], bReset)
    private val wheelBtns = arrayOf(bBack, bSpin)
    private val backBtns = arrayOf(bBack)
    private val dlgBtns = arrayOf(bDlgA, bDlgB)

    // ---- Eingabe
    private var pressed: Btn? = null
    private var downY = 0f
    private var dragging = false
    private var scrollStart = 0f
    private var shopScroll = 0f
    private var mapScroll = 0f
    private var achScroll = 0f

    // ---- Dialog / Toast
    private var dialog = D_NONE
    private var dlgTitle = ""
    private var dlgLines: Array<String> = arrayOf()
    private var dlgTwo = false
    private var dlgTop = 0f
    private var dlgH = 0f
    private var toast = ""
    private var toastT = 0f
    private var pendingTab = 0
    private var pendingItem = 0
    private var pendingDay = 0L
    private var pendingStreak = 0
    private var pendingBonus = 0
    private var newsChecked = false

    // ---- Shop
    private var shopTab = 0
    private val skinPrice = Array(Skins.skins.size) { Skins.skins[it].price.toString() }
    private val trailPrice = Array(Skins.trails.size) { Skins.trails[it].price.toString() }
    private val skinReq = Array(Skins.skins.size) { reqText(Skins.skins[it].reqLevel, Skins.skins[it].reqCrowns) }
    private val trailReq = Array(Skins.trails.size) { reqText(Skins.trails[it].reqLevel, Skins.trails[it].reqCrowns) }
    private val prevX = FloatArray(24)
    private val prevY = FloatArray(24)

    // ---- Menü-Deko (Skin kreist um den Spielknopf)
    private val menuTX = FloatArray(24)
    private val menuTY = FloatArray(24)
    private var menuHead = 0
    private var menuCount = 0

    // ---- Lauf
    private var runLevel = 0
    private var runTime = 0f
    private var cStars = 0; private var cBonus = 0; private var cCaptures = 0; private var cPerfects = 0
    private var runCounted = false
    private var runEarned = 0
    private var runRevives = 0
    private var newRecord = false
    private var winTimer = 0f
    private var levelReward = 0
    private var firstClear = false
    private var crownsWon = 0
    private var crownsNew = 0
    private var crownSounds = 0
    private var perfOk = false

    // ---- Texte (Cache)
    private var fpsAcc = 0f; private var fpsFrames = 0; private var fpsShown = 60
    private var fpsStr = "60 FPS"; private var fpsVal = -1
    private var scoreStr = "0"; private var scoreVal = -1
    private var starsStr = "0"; private var starsVal = -1
    private var crownStr = "0"; private var crownVal = -1
    private var progStr = ""; private var progVal = -1
    private var levelStr = ""
    private var bestStr = ""
    private var earnedStr = ""
    private var perfStr = ""
    private var nextStr = ""
    private var statLabels = arrayOf<String>()
    private var statValues = arrayOf<String>()
    private val levelNum = Array(Levels.COUNT + 1) { it.toString() }
    private val achRewardStr = Array(Achievements.list.size) { Achievements.list[it].reward.toString() }
    private val achProgStr = Array(Achievements.list.size) { "" }
    private var achBadge = 0
    private var rankStr = ""
    private var rankIdx = 0

    // ---- Tages-Challenge
    private var challengeRun = false
    private var challengeStr = ""
    private var challengeVal = -1

    // ---- Glücksrad
    private var wheelAngle = 0f
    private var spinning = false
    private var spinT = 0f
    private var spinFrom = 0f
    private var spinTo = 0f
    private var spinPrize = 0
    private var spinTick = 0
    private val wheelLabel = Array(8) { if (WHEEL_KIND[it] == 0) WHEEL_VAL[it].toString() else "" }
    private val pointerPath = android.graphics.Path().apply { moveTo(-34f, 0f); lineTo(34f, 0f); lineTo(0f, 58f); close() }
    private var musicTimer = 0f

    init {
        world.listener = this
        world.reduced = save.lowGfx
        world.shakeOn = save.shake
    }

    private fun reqText(lv: Int, cr: Int) = when {
        lv > 0 -> "Ab Level $lv"
        cr > 0 -> "$cr Kronen nötig"
        else -> ""
    }

    // ===================================================================== Von außen

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        scale = w / W
        H = h / scale
        world.resize(H)
        if (!ready) {
            world.reset(0)
            ready = true
            refreshMenuTexts()
        }
        layout()
    }

    fun onPause() {
        if (screen == Screen.PLAYING && !world.won) go(Screen.PAUSED)
        sound.pauseAll()
    }

    fun onResume() = sound.resumeAll()

    fun release() = sound.release()

    fun onBack(): Boolean {
        if (dialog != D_NONE) {
            if (dialog == D_DAILY) claimDaily() else closeDialog()
            return true
        }
        when (screen) {
            Screen.MENU -> return false
            Screen.PLAYING -> if (!world.won) go(Screen.PAUSED)
            Screen.PAUSED -> go(Screen.PLAYING)
            Screen.GAMEOVER, Screen.LEVELDONE -> leaveRun()
            else -> go(Screen.MENU)
        }
        sound.play(Sound.CLICK)
        return true
    }

    // ===================================================================== Layout

    private fun grad(b: Btn, c1: Int, c2: Int) {
        b.shader = gfx.gradient(b.x, b.y, b.x + b.w, b.y + b.h, c1, c2)
    }

    private fun place(b: Btn, x: Float, y: Float, w: Float, h: Float) {
        b.set(x, y, w, h)
        when (b.style) {
            S_PRIMARY, S_INVISIBLE -> grad(b, Gfx.ACCENT, Gfx.VIOLET)
            S_GREEN -> grad(b, Gfx.GREEN, 0xFF00B4D8.toInt())
            S_DANGER -> grad(b, Gfx.RED, 0xFFFF8A5B.toInt())
            S_GOLD -> grad(b, Gfx.GOLD, Gfx.ORANGE)
        }
    }

    private fun playCY() = H * 0.43f
    private fun wheelCY() = H * 0.42f

    private fun layout() {
        val cx = W / 2f
        val pcy = playCY()
        place(bPlay, cx - 160f, pcy - 160f, 320f, 320f)
        place(bEndless, cx - 240f, pcy + 330f, 480f, 110f)
        place(bChallenge, cx - 340f, pcy + 465f, 330f, 105f)
        place(bWheel, cx + 10f, pcy + 465f, 330f, 105f)
        place(bSpin, cx - 290f, wheelCY() + 420f, 580f, 140f)
        val ny = H - 300f
        val xs = floatArrayOf(155f, 385f, 615f, 845f)
        val nav = arrayOf(bShop, bAch, bStats, bSettings)
        for (i in 0 until 4) place(nav[i], xs[i] - 70f, ny, 140f, 140f)
        place(bBack, 40f, 60f, 110f, 110f)
        place(bPause, 40f, 60f, 110f, 110f)
        val py = H * 0.42f
        place(bResume, cx - 280f, py, 560f, 140f)
        place(bRestart, cx - 280f, py + 170f, 560f, 125f)
        place(bQuit, cx - 280f, py + 320f, 560f, 125f)
        val gy = H * 0.6f
        place(bRevive, cx - 290f, gy, 580f, 135f)
        place(bAgain, cx - 290f, gy + 160f, 580f, 135f)
        place(bLeave, cx - 290f, gy + 320f, 580f, 120f)
        place(bNext, cx - 300f, H * 0.66f, 600f, 140f)
        place(bToMap, cx - 300f, H * 0.66f + 165f, 600f, 125f)
        for (i in 0 until 3) place(bTab[i], 50f + i * 300f, 215f, 300f, 92f)
        for (i in 0 until 6) place(bRow[i], 40f, 250f + i * 132f, 920f, 112f)
        place(bReset, 80f, 250f + 6 * 132f + 30f, 840f, 110f)
    }

    // ===================================================================== Ablauf

    private fun go(s: Screen) {
        screen = s
        screenTime = 0f
        pressed = null
        dragging = false
        when (s) {
            Screen.STATS -> buildStats()
            Screen.ACHIEVE -> { buildAch(); achScroll = 0f }
            Screen.MAP -> {
                val cur = min(save.levelDone + 1, Levels.COUNT)
                mapScroll = clampMap(mapU(cur) - H / 2f + 450f)
            }
            Screen.SHOP -> shopScroll = 0f
            Screen.MENU -> refreshMenuTexts()
            else -> {}
        }
    }

    private fun refreshMenuTexts() {
        val next = min(save.levelDone + 1, Levels.COUNT)
        nextStr = "LEVEL $next"
        bEndless.label = if (save.best > 0) "ENDLOS  ·  ${save.best}" else "ENDLOS"
        achBadge = Achievements.claimableCount(save)
        var r = 0
        for (i in RANK_AT.indices) if (save.planets >= RANK_AT[i]) r = i
        rankIdx = r
        rankStr = if (r < RANK_AT.size - 1) "Rang: ${RANK_NAMES[r]}  ·  ${save.planets} / ${RANK_AT[r + 1]}" else "Rang: ${RANK_NAMES[r]}"
    }

    private fun startRun(level: Int, seed: Long = 0L) {
        world.reduced = save.lowGfx
        world.shakeOn = save.shake
        world.reset(level, seed)
        challengeRun = seed != 0L
        challengeVal = -1
        runLevel = level
        runTime = 0f
        cStars = 0; cBonus = 0; cCaptures = 0; cPerfects = 0
        runCounted = false; runEarned = 0; runRevives = 0; newRecord = false
        winTimer = 0f
        levelStr = if (level > 0) "LEVEL $level" else ""
        progVal = -1
        go(Screen.PLAYING)
        if (save.shieldStarts > 0) {
            save.shieldStarts--
            save.flush()
            world.shield = true
            showToast("Start-Schild aktiv")
        }
    }

    private fun leaveRun() {
        world.reset(0)
        go(if (runLevel > 0) Screen.MAP else Screen.MENU)
    }

    // ===================================================================== Glücksrad

    private fun freeSpin() = save.wheelDay != today()

    private fun startSpin() {
        if (spinning) return
        val free = freeSpin()
        if (!free) {
            if (save.stars < SPIN_PRICE) {
                sound.play(Sound.ERROR)
                showToast("Dir fehlen noch ${SPIN_PRICE - save.stars} Sterne")
                return
            }
            save.stars -= SPIN_PRICE
        } else save.wheelDay = today()
        // Gewinn auswürfeln (gewichtet)
        var total = 0
        for (w in WHEEL_WEIGHT) total += w
        var roll = (Math.random() * total).toInt()
        var prize = 0
        for (i in WHEEL_WEIGHT.indices) { roll -= WHEEL_WEIGHT[i]; if (roll < 0) { prize = i; break } }
        spinPrize = prize
        // Gewinn sofort gutschreiben (sicher, falls man den Bildschirm verlässt)
        when (WHEEL_KIND[prize]) {
            0 -> save.stars += WHEEL_VAL[prize]
            1 -> if (save.revives < Skins.REVIVE_MAX) save.revives++ else save.stars += 40
            else -> if (save.shieldStarts < Skins.SHIELD_MAX) save.shieldStarts++ else save.stars += 30
        }
        save.spins++
        save.flush()
        val cur = ((wheelAngle % 360f) + 360f) % 360f
        val desired = ((-prize * 45f) % 360f + 360f) % 360f
        var delta = desired - cur
        if (delta < 0f) delta += 360f
        spinFrom = wheelAngle
        spinTo = wheelAngle + 360f * 5f + delta + ((Math.random() - 0.5) * 28.0).toFloat()
        spinT = 0f
        spinTick = ((wheelAngle + 22.5f) / 45f).toInt()
        spinning = true
    }

    private fun updateWheel(dt: Float) {
        if (!spinning) return
        spinT += dt
        val d = 3.6f
        val f = min(1f, spinT / d)
        val e = 1f - (1f - f) * (1f - f) * (1f - f)
        wheelAngle = spinFrom + (spinTo - spinFrom) * e
        val tick = ((wheelAngle + 22.5f) / 45f).toInt()
        if (tick != spinTick) { spinTick = tick; sound.play(Sound.CLICK, 1.2f, 0.6f) }
        if (f >= 1f) {
            spinning = false
            sound.play(Sound.BONUS)
            sound.vibrate(50)
            showToast(when (WHEEL_KIND[spinPrize]) {
                0 -> "Gewonnen: ${WHEEL_VAL[spinPrize]} Sterne!"
                1 -> "Gewonnen: 1 Wiederbelebung!"
                else -> "Gewonnen: 1 Start-Schild!"
            })
        }
    }

    private fun commitRun() {
        val bonus = if (runLevel == 0) world.score / 10 else 0
        val add = (world.runStars - cStars) + (bonus - cBonus)
        save.stars += add
        save.starsCollected += world.runStars - cStars
        save.planets += world.captures - cCaptures
        save.perfects += world.perfects - cPerfects
        if (world.bestStreak > save.bestStreak) save.bestStreak = world.bestStreak
        if (runLevel == 0 && world.score > save.best) { save.best = world.score; newRecord = true }
        save.playTime += runTime; runTime = 0f
        if (!runCounted) { save.games++; runCounted = true }
        cStars = world.runStars; cBonus = bonus; cCaptures = world.captures; cPerfects = world.perfects
        runEarned += add
        save.flush()
    }

    // ===================================================================== World-Ereignisse

    override fun onLaunch() = sound.play(Sound.LAUNCH, 1f, 0.6f)

    override fun onCapture(perfect: Boolean, streak: Int) {
        sound.play(Sound.CAPTURE, 1f + 0.1f * min(streak, 6))
        if (perfect) sound.play(Sound.PERFECT, 1f, 0.8f)
        sound.vibrate(15)
    }

    override fun onStar() = sound.play(Sound.STAR)

    override fun onPowerup(type: Int) {
        save.powerups++
        sound.play(Sound.POWER)
        sound.vibrate(30)
        showToast(if (type == World.P_SHIELD) "Schild aktiv!" else "Sternen-Magnet!")
    }

    override fun onShieldBreak() {
        sound.play(Sound.SHIELD)
        sound.vibrate(90)
        showToast("Der Schild hat dich gerettet!")
    }

    override fun onDeath() {
        sound.play(Sound.DEATH)
        sound.vibrate(160)
        commitRun()
        earnedStr = "+$runEarned"
        bestStr = "Rekord: ${save.best}"
        bRevive.label = "WIEDERBELEBEN  (${save.revives})"
        bLeave.label = if (runLevel > 0) "ZUR KARTE" else "MENÜ"
        go(Screen.GAMEOVER)
    }

    override fun onGoal() {
        sound.play(Sound.WIN)
        sound.vibrate(80)
        commitRun()
        firstClear = runLevel > save.levelDone
        levelReward = if (firstClear) Levels.reward(runLevel) else 0
        if (firstClear) {
            save.levelDone = runLevel
            save.stars += levelReward
        }
        val goal = Levels.perfectGoal(runLevel)
        perfOk = world.perfects >= goal
        crownsWon = 1 + (if (runRevives == 0) 1 else 0) + (if (perfOk) 1 else 0)
        crownsNew = save.setCrowns(runLevel, crownsWon)
        perfStr = "${min(world.perfects, goal)} / $goal perfekte Landungen"
        save.flush()
        runEarned += levelReward
        earnedStr = "+$runEarned"
        crownSounds = 0
        winTimer = 1.3f
    }

    // ===================================================================== Update

    fun update(dt: Float) {
        if (!ready) return
        fpsAcc += dt; fpsFrames++
        if (fpsAcc >= 0.5f) {
            fpsShown = (fpsFrames / fpsAcc + 0.5f).toInt()
            fpsAcc = 0f; fpsFrames = 0
        }
        time += dt
        screenTime += dt
        musicTimer -= dt
        if (musicTimer <= 0f) { musicTimer = 0.5f; sound.syncMusic() }
        if (toastT > 0f) toastT -= dt

        when (screen) {
            Screen.PAUSED -> {}
            Screen.PLAYING -> {
                world.update(dt, true)
                runTime += dt
                if (challengeRun && world.captures >= CHALLENGE_GOAL && save.challengeDay != today()) {
                    save.challengeDay = today()
                    save.challengesDone++
                    save.stars += CHALLENGE_REWARD
                    save.flush()
                    sound.play(Sound.WIN)
                    sound.vibrate(60)
                    showToast("Challenge geschafft! +$CHALLENGE_REWARD Sterne")
                }
                if (winTimer > 0f) {
                    winTimer -= dt
                    if (winTimer <= 0f) go(Screen.LEVELDONE)
                }
            }
            Screen.LEVELDONE -> {
                world.update(dt, false)
                val shown = ((screenTime - 0.35f) / 0.3f).toInt().coerceIn(0, crownsWon)
                if (shown > crownSounds) { crownSounds = shown; sound.play(Sound.CROWN, 0.9f + shown * 0.1f) }
            }
            Screen.WHEEL -> {
                world.update(dt, false)
                updateWheel(dt)
            }
            else -> world.update(dt, false)
        }

        if (screen == Screen.MENU) {
            val a = time * 1.6f
            menuTX[menuHead] = W / 2f + cos(a) * 215f
            menuTY[menuHead] = playCY() + sin(a) * 215f
            menuHead = (menuHead + 1) % 24
            if (menuCount < 24) menuCount++
            if (dialog == D_NONE) checkPopups()
        }
    }

    private fun checkPopups() {
        if (!newsChecked) {
            newsChecked = true
            if (save.lastVersion < VERSION_CODE) {
                val fresh = save.games == 0 && save.best == 0
                save.lastVersion = VERSION_CODE
                save.flush()
                if (!fresh) {
                    openDialog(D_NEWS, "Neu in Version $VERSION_NAME", NEWS, "LOS GEHT'S", null)
                    return
                }
            }
        }
        val today = today()
        if (today > save.lastDay && screenTime > 0.4f) {
            pendingDay = today
            pendingStreak = if (today == save.lastDay + 1) save.dayStreak + 1 else 1
            pendingBonus = 10 + 3 * min(pendingStreak - 1, 7)
            openDialog(D_DAILY, "Täglicher Bonus", arrayOf("Tag $pendingStreak in Folge", "+$pendingBonus Sterne"), "ABHOLEN", null)
        }
    }

    private fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    // ===================================================================== Eingabe

    fun onTouch(action: Int, px: Float, py: Float) {
        if (!ready) return
        val x = px / scale
        val y = py / scale
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downY = y; dragging = false
                pressed = null
                if (dialog != D_NONE) { pressed = hit(dlgBtns, x, y); return }
                if (screen == Screen.PLAYING) {
                    if (bPause.hit(x, y)) { pressed = bPause; return }
                    world.launch()
                    return
                }
                if ((screen == Screen.GAMEOVER || screen == Screen.LEVELDONE) && screenTime < 0.6f) return
                scrollStart = when (screen) { Screen.MAP -> mapScroll; Screen.ACHIEVE -> achScroll; else -> shopScroll }
                pressed = hit(activeButtons(), x, y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (dialog == D_NONE && screen == Screen.PLAYING) world.launch()
            }
            MotionEvent.ACTION_MOVE -> {
                if (dialog != D_NONE) return
                if (screen != Screen.SHOP && screen != Screen.MAP && screen != Screen.ACHIEVE) return
                if (!dragging && abs(y - downY) > 22f) { dragging = true; pressed = null }
                if (dragging) when (screen) {
                    Screen.MAP -> mapScroll = clampMap(scrollStart + (y - downY))
                    Screen.ACHIEVE -> achScroll = clampAch(scrollStart - (y - downY))
                    else -> shopScroll = clampShop(scrollStart - (y - downY))
                }
            }
            MotionEvent.ACTION_UP -> {
                val p = pressed
                pressed = null
                if (p != null) {
                    if (p.hit(x, y)) onButton(p.id)
                    return
                }
                if (dialog != D_NONE || dragging) return
                when (screen) {
                    Screen.SHOP -> shopTap(x, y)
                    Screen.MAP -> mapTap(x, y)
                    Screen.ACHIEVE -> achTap(x, y)
                    else -> {}
                }
            }
            MotionEvent.ACTION_CANCEL -> { pressed = null; dragging = false }
        }
    }

    private fun hit(btns: Array<Btn>, x: Float, y: Float): Btn? {
        for (b in btns) if (b.hit(x, y)) return b
        return null
    }

    private fun activeButtons(): Array<Btn> = when (screen) {
        Screen.MENU -> menuBtns
        Screen.PLAYING -> playBtns
        Screen.PAUSED -> pauseBtns
        Screen.GAMEOVER -> { bRevive.visible = save.revives > 0; overBtns }
        Screen.LEVELDONE -> { bNext.visible = runLevel < Levels.COUNT; doneBtns }
        Screen.SHOP -> { bBuyRevive.visible = shopTab == 2; bBuyShield.visible = shopTab == 2; shopBtns }
        Screen.SETTINGS -> setBtns
        Screen.MAP, Screen.STATS, Screen.ACHIEVE -> backBtns
        Screen.WHEEL -> wheelBtns
    }

    private fun onButton(id: Int) {
        sound.play(Sound.CLICK)
        when (id) {
            bPlay.id -> go(Screen.MAP)
            bEndless.id -> startRun(0)
            bShop.id -> go(Screen.SHOP)
            bAch.id -> go(Screen.ACHIEVE)
            bStats.id -> go(Screen.STATS)
            bSettings.id -> go(Screen.SETTINGS)
            bBack.id -> go(Screen.MENU)
            bPause.id -> go(Screen.PAUSED)
            bResume.id -> go(Screen.PLAYING)
            bRestart.id -> { commitRun(); startRun(runLevel) }
            bQuit.id -> { commitRun(); leaveRun() }
            bRevive.id -> doRevive()
            bAgain.id -> startRun(runLevel)
            bLeave.id -> leaveRun()
            bNext.id -> startRun(min(runLevel + 1, Levels.COUNT))
            bToMap.id -> { world.reset(0); go(Screen.MAP) }
            bTab[0].id -> { shopTab = 0; shopScroll = 0f }
            bTab[1].id -> { shopTab = 1; shopScroll = 0f }
            bTab[2].id -> { shopTab = 2; shopScroll = 0f }
            bBuyRevive.id -> askBuy(2, 0)
            bBuyShield.id -> askBuy(3, 0)
            bRow[0].id -> { save.sound = !save.sound; save.flush() }
            bRow[1].id -> { save.music = !save.music; save.flush(); sound.syncMusic() }
            bRow[2].id -> { save.vibration = !save.vibration; save.flush(); sound.vibrate(40) }
            bRow[3].id -> { save.shake = !save.shake; world.shakeOn = save.shake; save.flush() }
            bRow[4].id -> { save.showFps = !save.showFps; save.flush() }
            bRow[5].id -> { save.lowGfx = !save.lowGfx; world.reduced = save.lowGfx; save.flush() }
            bChallenge.id -> startRun(0, today() * 7919L + 17L)
            bWheel.id -> go(Screen.WHEEL)
            bSpin.id -> startSpin()
            bReset.id -> openDialog(
                D_RESET, "Wirklich zurücksetzen?",
                arrayOf("Sterne, Skins, Level, Kronen", "und Erfolge werden gelöscht."), "LÖSCHEN", "ABBRECHEN"
            )
            bDlgA.id -> dialogAnswer(true)
            bDlgB.id -> dialogAnswer(false)
        }
    }

    private fun doRevive() {
        if (save.revives <= 0) return
        if (!world.revive()) return
        save.revives--
        save.revivesUsed++
        runRevives++
        save.flush()
        sound.play(Sound.REVIVE)
        sound.vibrate(60)
        go(Screen.PLAYING)
    }

    // ===================================================================== Dialoge

    private fun openDialog(kind: Int, title: String, lines: Array<String>, a: String, b: String?) {
        dialog = kind
        dlgTitle = title
        dlgLines = lines
        dlgTwo = b != null
        bDlgA.label = a
        bDlgB.label = b ?: ""
        dlgH = 160f + lines.size * 62f + 190f
        dlgTop = (H - dlgH) / 2f
        val by = dlgTop + dlgH - 165f
        if (dlgTwo) {
            place(bDlgA, 110f, by, 375f, 120f)
            place(bDlgB, 515f, by, 375f, 120f)
            bDlgB.visible = true
        } else {
            place(bDlgA, 210f, by, 580f, 120f)
            bDlgB.visible = false
        }
        pressed = null
    }

    private fun closeDialog() { dialog = D_NONE; pressed = null }

    private fun dialogAnswer(yes: Boolean) {
        val kind = dialog
        closeDialog()
        when (kind) {
            D_BUY -> if (yes) buy(pendingTab, pendingItem)
            D_RESET -> if (yes) { save.resetProgress(); refreshMenuTexts(); showToast("Fortschritt gelöscht") }
            D_DAILY -> claimDaily()
            else -> {}
        }
    }

    private fun claimDaily() {
        closeDialog()
        save.lastDay = pendingDay
        save.dayStreak = pendingStreak
        if (pendingStreak > save.bestDayStreak) save.bestDayStreak = pendingStreak
        save.stars += pendingBonus
        save.flush()
        refreshMenuTexts()
        sound.play(Sound.BONUS)
        showToast("+$pendingBonus Sterne!")
    }

    private fun showToast(msg: String) { toast = msg; toastT = 2.2f }

    // ===================================================================== Shop

    private val cardW = (900f - 48f) / 3f
    private val cardH = 360f
    private val gridTop = 345f

    private fun shopCount() = when (shopTab) { 0 -> Skins.skins.size; 1 -> Skins.trails.size; else -> 0 }

    private fun clampShop(v: Float): Float {
        val rows = (shopCount() + 2) / 3
        val content = gridTop + rows * (cardH + 24f) + 80f
        return v.coerceIn(0f, max(0f, content - H))
    }

    private fun reqMet(lv: Int, cr: Int) = save.levelDone >= lv && save.totalCrowns >= cr

    private fun shopTap(x: Float, y: Float) {
        if (shopTab == 2 || y < gridTop - 10f || x < 50f) return
        val yy = y + shopScroll - gridTop
        if (yy < 0f) return
        val col = ((x - 50f) / (cardW + 24f)).toInt()
        val row = (yy / (cardH + 24f)).toInt()
        if (col !in 0..2) return
        val lx = x - 50f - col * (cardW + 24f)
        val ly = yy - row * (cardH + 24f)
        if (lx > cardW || ly > cardH) return
        val i = row * 3 + col
        if (i >= shopCount()) return
        if (shopTab == 0) {
            if (save.ownsSkin(i)) { save.skin = i; save.flush(); sound.play(Sound.CLICK) } else askBuy(0, i)
        } else {
            if (save.ownsTrail(i)) { save.trail = i; save.flush(); sound.play(Sound.CLICK) } else askBuy(1, i)
        }
    }

    private fun priceOf(tab: Int, i: Int) = when (tab) {
        0 -> Skins.skins[i].price
        1 -> Skins.trails[i].price
        2 -> Skins.REVIVE_PRICE
        else -> Skins.SHIELD_PRICE
    }

    private fun nameOf(tab: Int, i: Int) = when (tab) {
        0 -> "Skin „${Skins.skins[i].name}“"
        1 -> "Spur „${Skins.trails[i].name}“"
        2 -> "1 Wiederbelebung"
        else -> "1 Start-Schild"
    }

    private fun askBuy(tab: Int, i: Int) {
        if (tab == 0 || tab == 1) {
            val lv = if (tab == 0) Skins.skins[i].reqLevel else Skins.trails[i].reqLevel
            val cr = if (tab == 0) Skins.skins[i].reqCrowns else Skins.trails[i].reqCrowns
            if (!reqMet(lv, cr)) {
                sound.play(Sound.ERROR)
                showToast(if (lv > 0) "Schaffe zuerst Level $lv" else "Du brauchst $cr Kronen")
                return
            }
        }
        if (tab == 2 && save.revives >= Skins.REVIVE_MAX) { sound.play(Sound.ERROR); showToast("Maximal ${Skins.REVIVE_MAX} Wiederbelebungen"); return }
        if (tab == 3 && save.shieldStarts >= Skins.SHIELD_MAX) { sound.play(Sound.ERROR); showToast("Maximal ${Skins.SHIELD_MAX} Start-Schilde"); return }
        val price = priceOf(tab, i)
        if (price == 0) { buy(tab, i); return }
        if (save.stars < price) {
            sound.play(Sound.ERROR)
            showToast("Dir fehlen noch ${price - save.stars} Sterne")
            return
        }
        pendingTab = tab; pendingItem = i
        openDialog(D_BUY, "Kaufen?", arrayOf(nameOf(tab, i), "für $price Sterne"), "KAUFEN", "ABBRECHEN")
    }

    private fun buy(tab: Int, i: Int) {
        val price = priceOf(tab, i)
        if (save.stars < price) return
        save.stars -= price
        when (tab) {
            0 -> { save.ownedSkins = save.ownedSkins or (1 shl i); save.skin = i }
            1 -> { save.ownedTrails = save.ownedTrails or (1 shl i); save.trail = i }
            2 -> save.revives = min(Skins.REVIVE_MAX, save.revives + 1)
            else -> save.shieldStarts = min(Skins.SHIELD_MAX, save.shieldStarts + 1)
        }
        save.flush()
        sound.play(Sound.BUY)
        sound.vibrate(30)
        showToast(if (tab >= 2) "Gekauft!" else "Freigeschaltet!")
    }

    // ===================================================================== Level-Karte

    private fun mapU(level: Int) = (level - 1) * 170f + Levels.section(level) * 300f
    private fun bodyU(s: Int) = if (s == 0) -320f else mapU(s * 10) + 245f
    private fun mapX(level: Int) = W / 2f + sin(level * 0.9f) * 250f
    private fun mapScreenY(u: Float) = H - 450f - u + mapScroll

    private fun clampMap(v: Float): Float {
        val top = bodyU(Levels.COUNT / 10) + 280f
        return v.coerceIn(0f, max(0f, top + 750f - H))
    }

    private fun mapTap(x: Float, y: Float) {
        if (y < 210f) return
        for (i in 1..Levels.COUNT) {
            val dx = x - mapX(i); val dy = y - mapScreenY(mapU(i))
            if (dx * dx + dy * dy < 75f * 75f) {
                if (i <= save.levelDone + 1) { sound.play(Sound.CLICK); startRun(i) }
                else { sound.play(Sound.ERROR); showToast("Schaffe zuerst Level ${save.levelDone + 1}") }
                return
            }
        }
    }

    // ===================================================================== Erfolge

    private val achTop = 240f
    private val achH = 175f

    private fun clampAch(v: Float) = v.coerceIn(0f, max(0f, achTop + Achievements.list.size * (achH + 20f) + 60f - H))

    private fun buildAch() {
        for (i in Achievements.list.indices) {
            val a = Achievements.list[i]
            val v = min(Achievements.value(a, save), a.goal)
            achProgStr[i] = "$v / ${a.goal}"
        }
        achBadge = Achievements.claimableCount(save)
    }

    private fun achTap(x: Float, y: Float) {
        if (y < 210f) return
        val i = ((y + achScroll - achTop) / (achH + 20f)).toInt()
        if (y + achScroll < achTop || i !in Achievements.list.indices || x < 40f || x > 960f) return
        if (Achievements.done(i, save) && !Achievements.claimed(i, save)) {
            val a = Achievements.list[i]
            save.achClaimed = save.achClaimed or (1 shl i)
            save.stars += a.reward
            save.flush()
            buildAch()
            sound.play(Sound.BONUS)
            sound.vibrate(40)
            showToast("+${a.reward} Sterne!")
        }
    }

    // ===================================================================== Statistik

    private fun buildStats() {
        val mins = (save.playTime / 60f).toInt()
        statLabels = arrayOf(
            "Level geschafft", "Kronen", "Rekord (Endlos)", "Spiele", "Planeten erreicht",
            "Perfekte Landungen", "Beste Perfekt-Serie", "Sterne gesammelt", "Power-ups",
            "Wiederbelebungen", "Tages-Challenges", "Glücksrad", "Spielzeit"
        )
        statValues = arrayOf(
            "${save.levelDone} / ${Levels.COUNT}", "${save.totalCrowns} / ${Levels.COUNT * 3}",
            save.best.toString(), save.games.toString(), save.planets.toString(), save.perfects.toString(),
            save.bestStreak.toString(), save.starsCollected.toString(), save.powerups.toString(),
            save.revivesUsed.toString(), save.challengesDone.toString(), save.spins.toString(), if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
        )
    }

    // ===================================================================== Zeichnen

    fun draw(c: Canvas) {
        c.drawColor(Gfx.BG)
        if (!ready) return
        c.save()
        c.scale(scale, scale)

        when (screen) {
            Screen.PLAYING, Screen.PAUSED, Screen.GAMEOVER, Screen.LEVELDONE -> world.draw(c, save.skin, save.trail)
            else -> world.drawBackground(c)
        }

        when (screen) {
            Screen.MENU -> drawMenu(c)
            Screen.MAP -> drawMap(c)
            Screen.PLAYING -> drawHud(c)
            Screen.PAUSED -> { drawHud(c); drawPause(c) }
            Screen.GAMEOVER -> drawGameOver(c)
            Screen.LEVELDONE -> drawLevelDone(c)
            Screen.SHOP -> drawShop(c)
            Screen.SETTINGS -> drawSettings(c)
            Screen.STATS -> drawStats(c)
            Screen.ACHIEVE -> drawAch(c)
            Screen.WHEEL -> drawWheel(c)
        }

        if (save.showFps) {
            if (fpsShown != fpsVal) { fpsVal = fpsShown; fpsStr = "$fpsShown FPS" }
            gfx.text(c, fpsStr, 30f, H - 30f, 28f, Color.WHITE, false, Paint.Align.LEFT, 110)
        }
        if (dialog != D_NONE) drawDialog(c)
        if (toastT > 0f) drawToast(c)

        if (screenTime < 0.22f && screen != Screen.PLAYING) {
            gfx.fill.color = Gfx.BG; gfx.fill.alpha = ((1f - screenTime / 0.22f) * 200f).toInt()
            c.drawRect(0f, 0f, W, H, gfx.fill)
        }
        c.restore()
    }

    private fun dim(c: Canvas, alpha: Int) {
        gfx.fill.color = Gfx.BG; gfx.fill.alpha = alpha
        c.drawRect(0f, 0f, W, H, gfx.fill)
    }

    private fun starPill(c: Canvas, value: Int) {
        if (value != starsVal) { starsVal = value; starsStr = value.toString() }
        gfx.pill(c, W - 40f, 115f, starsStr, -1, Gfx.GOLD)
    }

    private fun crownPill(c: Canvas, right: Float) {
        val v = save.totalCrowns
        if (v != crownVal) { crownVal = v; crownStr = v.toString() }
        gfx.pill(c, right, 115f, crownStr, Gfx.I_CROWN, Gfx.GOLD)
    }

    private fun drawBtn(c: Canvas, b: Btn) {
        if (!b.visible || b.style == S_INVISIBLE) return
        val p = b === pressed
        val ins = if (p) 6f else 0f
        val l = b.x + ins; val t = b.y + ins; val r = b.x + b.w - ins; val bt = b.y + b.h - ins
        val cx = b.x + b.w / 2f
        val cy = b.y + b.h / 2f
        when (b.style) {
            S_PRIMARY, S_GREEN, S_DANGER, S_GOLD -> {
                val sh = b.shader
                if (sh != null) gfx.gradRoundRect(c, l, t, r, bt, (bt - t) / 2f, sh, if (p) 210 else 255)
                gfx.roundRect(c, l + 14f, t + 5f, r - 14f, t + (bt - t) * 0.42f, (bt - t) / 4f, Color.WHITE, 30)
                val tc = if (b.style == S_GOLD) Gfx.BG else Color.WHITE
                gfx.text(c, b.label, cx, cy + b.h * 0.11f, b.h * 0.3f, tc)
            }
            S_GLASS -> {
                gfx.glass(c, l, t, r, bt, (bt - t) / 2f)
                if (b.icon >= 0) {
                    val tw = gfx.textWidth(b.label, b.h * 0.3f)
                    gfx.icon(c, b.icon, cx - tw / 2f - 34f, cy, b.h * 0.2f, Gfx.ACCENT)
                    gfx.text(c, b.label, cx + 28f, cy + b.h * 0.11f, b.h * 0.3f, Color.WHITE)
                } else gfx.text(c, b.label, cx, cy + b.h * 0.11f, b.h * 0.3f, Color.WHITE)
            }
            S_ROUND -> {
                val rr = b.w / 2f - 8f - ins
                val ry = b.y + b.w / 2f
                gfx.circle(c, cx, ry, rr, Gfx.PANEL, 225)
                gfx.ring(c, cx, ry, rr, Color.WHITE, 2.5f, 45)
                gfx.icon(c, b.icon, cx, ry, rr * 0.42f, Color.WHITE)
                gfx.text(c, b.label, cx, b.y + b.h + 30f, 26f, Gfx.DIM)
            }
            S_BACK -> {
                gfx.glass(c, l, t, r, bt, (bt - t) / 2f)
                if (b === bPause) {
                    gfx.roundRect(c, cx - 19f, cy - 24f, cx - 7f, cy + 24f, 5f, Color.WHITE)
                    gfx.roundRect(c, cx + 7f, cy - 24f, cx + 19f, cy + 24f, 5f, Color.WHITE)
                } else {
                    gfx.line.color = Color.WHITE; gfx.line.alpha = 255; gfx.line.strokeWidth = 8f
                    c.drawLine(cx + 9f, cy - 22f, cx - 13f, cy, gfx.line)
                    c.drawLine(cx - 13f, cy, cx + 9f, cy + 22f, gfx.line)
                }
            }
        }
    }

    private fun header(c: Canvas, title: String) {
        dim(c, 150)
        gfx.roundRect(c, -10f, -10f, W + 10f, 205f, 0f, Gfx.BG, 235)
        drawBtn(c, bBack)
        gfx.text(c, title, W / 2f, 132f, 54f, Color.WHITE)
    }

    // ---------------------------------------------------------------- Menü

    private fun drawMenu(c: Canvas) {
        val cx = W / 2f
        gfx.drawBody(c, Levels.B_EARTH, cx, H + 440f, 560f, time, save.lowGfx)

        crownPill(c, 300f)
        starPill(c, save.stars)

        val ty = H * 0.17f
        if (!save.lowGfx) gfx.glowA(c, gfx.planetGlow[4], cx, ty - 60f, 420f, 150)
        gfx.text(c, "ORBIT", cx, ty, 200f, Color.WHITE)
        gfx.spaced(c, "PLANET JUMP", cx, ty + 70f, 36f, Gfx.ACCENT, 0.5f)
        gfx.text(c, rankStr, cx, ty + 130f, 30f, Gfx.DIM, false)

        val pcy = playCY()
        val pulse = sin(time * 3f)
        gfx.ring(c, cx, pcy, 215f, Color.WHITE, 2f, 30)
        gfx.ring(c, cx, pcy, 168f + pulse * 6f, Gfx.ACCENT, 4f, 90)
        if (!save.lowGfx) gfx.glowA(c, gfx.planetGlow[4], cx, pcy, 330f, 200)
        val sh = bPlay.shader
        if (sh != null) gfx.gradCircle(c, cx, pcy, if (pressed === bPlay) 140f else 150f, sh)
        gfx.circle(c, cx - 40f, pcy - 50f, 70f, Color.WHITE, 25)
        gfx.icon(c, Gfx.I_PLAY, cx + 8f, pcy, 70f, Color.WHITE)

        val start = (menuHead - menuCount + 24) % 24
        gfx.drawTrail(c, save.trail, menuTX, menuTY, start, menuCount, 24, gfx.skinColor(save.skin, time), time)
        val last = (menuHead + 23) % 24
        gfx.drawSkin(c, save.skin, menuTX[last], menuTY[last], 1.2f, time, save.lowGfx)

        gfx.text(c, nextStr, cx, pcy + 290f, 46f, Color.WHITE)
        drawBtn(c, bEndless)
        drawBtn(c, bChallenge); drawBtn(c, bWheel)
        if (save.challengeDay != today()) gfx.circle(c, bChallenge.x + bChallenge.w - 14f, bChallenge.y + 14f, 14f, Gfx.RED)
        else gfx.icon(c, Gfx.I_CHECK, bChallenge.x + bChallenge.w - 18f, bChallenge.y + 18f, 14f, Gfx.GREEN)
        if (freeSpin()) gfx.circle(c, bWheel.x + bWheel.w - 14f, bWheel.y + 14f, 14f, Gfx.RED)

        drawBtn(c, bShop); drawBtn(c, bAch); drawBtn(c, bStats); drawBtn(c, bSettings)
        if (achBadge > 0) {
            val bx = bAch.x + bAch.w - 22f; val by = bAch.y + 22f
            gfx.circle(c, bx, by, 24f, Gfx.RED)
            gfx.text(c, levelNum[min(achBadge, Levels.COUNT)], bx, by + 10f, 28f, Color.WHITE)
        }
        gfx.text(c, "v$VERSION_NAME · Engelcode", cx, H - 30f, 26f, Gfx.DIM, false, alpha = 150)
    }

    // ---------------------------------------------------------------- Spiel-HUD

    private fun drawHud(c: Canvas) {
        drawBtn(c, bPause)
        starPill(c, save.stars + world.runStars - cStars)
        val cx = W / 2f
        if (runLevel > 0) {
            val prog = min(world.progress, world.target)
            if (prog != progVal) { progVal = prog; progStr = "$prog / ${world.target}" }
            gfx.glass(c, cx - 190f, 62f, cx + 190f, 168f, 40f)
            gfx.text(c, levelStr, cx, 102f, 30f, Gfx.DIM)
            val bw = 300f; val bx = cx - bw / 2f
            gfx.roundRect(c, bx, 122f, bx + bw, 138f, 8f, Gfx.PANEL2)
            gfx.roundRect(c, bx, 122f, bx + max(16f, bw * prog / max(1, world.target)), 138f, 8f, Gfx.GREEN)
            gfx.text(c, progStr, cx, 260f, 70f, Color.WHITE, alpha = 230)
        } else {
            if (world.score != scoreVal) { scoreVal = world.score; scoreStr = world.score.toString() }
            gfx.text(c, scoreStr, cx, 205f, 130f, Color.WHITE, alpha = 235)
            if (challengeRun) {
                val v = min(world.captures, CHALLENGE_GOAL)
                if (v != challengeVal) {
                    challengeVal = v
                    challengeStr = if (save.challengeDay == today() && v >= CHALLENGE_GOAL) "CHALLENGE GESCHAFFT" else "CHALLENGE  $v / $CHALLENGE_GOAL"
                }
                val done = save.challengeDay == today()
                val tw = gfx.textWidth(challengeStr, 30f)
                gfx.glass(c, cx - tw / 2f - 50f, 240f, cx + tw / 2f + 50f, 310f, 35f, border = if (done) Gfx.GREEN else Gfx.GOLD)
                gfx.text(c, challengeStr, cx, 286f, 30f, if (done) Gfx.GREEN else Gfx.GOLD)
            }
        }
        var iy = 230f
        if (world.shield) {
            gfx.glass(c, 40f, iy, 150f, iy + 110f, 55f, border = Gfx.SHIELD)
            gfx.icon(c, Gfx.I_SHIELD, 95f, iy + 55f, 30f, Gfx.SHIELD)
            iy += 130f
        }
        if (world.magnetT > 0f) {
            gfx.glass(c, 40f, iy, 150f, iy + 110f, 55f, border = Gfx.RED)
            gfx.icon(c, Gfx.I_MAGNET, 95f, iy + 60f, 28f, Gfx.RED)
            gfx.stroke.color = Gfx.RED; gfx.stroke.alpha = 255; gfx.stroke.strokeWidth = 6f
            gfx.rect.set(48f, iy + 8f, 142f, iy + 102f)
            c.drawArc(gfx.rect, -90f, 360f * world.magnetT / World.MAGNET_TIME, false, gfx.stroke)
        }
        if (screen == Screen.PLAYING && save.games == 0 && world.captures == 0 && world.orbiting && runLevel <= 1) {
            val a = (170 + 85 * sin(time * 3f)).toInt().coerceIn(0, 255)
            gfx.glass(c, 90f, H * 0.3f, 910f, H * 0.3f + 150f, 40f, a)
            gfx.text(c, "Tippe, wenn die Punkte", cx, H * 0.3f + 65f, 42f, Color.WHITE, false, alpha = a)
            gfx.text(c, "zum nächsten Planeten zeigen", cx, H * 0.3f + 118f, 42f, Color.WHITE, false, alpha = a)
        }
    }

    private fun drawPause(c: Canvas) {
        dim(c, 200)
        gfx.text(c, "PAUSE", W / 2f, H * 0.32f, 110f, Color.WHITE)
        for (b in pauseBtns) drawBtn(c, b)
    }

    // ---------------------------------------------------------------- Game Over / Level geschafft

    private fun drawGameOver(c: Canvas) {
        val f = min(1f, screenTime / 0.35f)
        dim(c, (180 * f).toInt())
        val a = (255 * f).toInt()
        val top = H * 0.14f
        gfx.glass(c, 70f, top, 930f, top + 560f, 50f, a)
        val cx = W / 2f
        gfx.text(c, if (runLevel > 0) "LEVEL $runLevel VERLOREN" else if (challengeRun) "TAGES-CHALLENGE" else "VERLOREN IM ALL", cx, top + 95f, 44f, Gfx.RED, alpha = a)
        if (runLevel > 0) {
            gfx.text(c, progStr, cx, top + 280f, 150f, Color.WHITE, alpha = a)
            gfx.text(c, "Planeten geschafft", cx, top + 345f, 38f, Gfx.DIM, false, alpha = a)
        } else {
            if (world.score != scoreVal) { scoreVal = world.score; scoreStr = world.score.toString() }
            gfx.text(c, scoreStr, cx, top + 290f, 190f, Color.WHITE, alpha = a)
            if (newRecord) gfx.text(c, "NEUER REKORD!", cx, top + 360f, 44f, Gfx.GOLD, alpha = a)
            else gfx.text(c, bestStr, cx, top + 360f, 40f, Gfx.DIM, false, alpha = a)
        }
        val tw = gfx.textWidth(earnedStr, 54f)
        gfx.glass(c, cx - tw / 2f - 90f, top + 410f, cx + tw / 2f + 70f, top + 500f, 45f, a)
        gfx.starShape(c, cx - tw / 2f - 40f, top + 455f, 24f, 0f, Gfx.GOLD, a)
        gfx.text(c, earnedStr, cx + 15f, top + 474f, 54f, Gfx.GOLD, alpha = a)
        if (screenTime > 0.6f) {
            bRevive.visible = save.revives > 0
            for (b in overBtns) drawBtn(c, b)
        }
    }

    private fun drawLevelDone(c: Canvas) {
        val f = min(1f, screenTime / 0.35f)
        dim(c, (180 * f).toInt())
        val a = (255 * f).toInt()
        val cx = W / 2f
        val top = H * 0.08f
        gfx.glass(c, 70f, top, 930f, top + 870f, 50f, a)
        val body = Levels.goalBody(runLevel)
        if (body != 0) {
            gfx.drawBody(c, body, cx, top + 150f, 95f, time, save.lowGfx)
            gfx.text(c, "${Levels.bodyNames[body]} ERREICHT!", cx, top + 330f, if (body == Levels.B_CERES) 40f else 50f, Gfx.GREEN, alpha = a)
        } else {
            gfx.text(c, "GESCHAFFT!", cx, top + 200f, 110f, Gfx.GREEN, alpha = a)
            gfx.text(c, levelStr, cx, top + 290f, 44f, Gfx.DIM, alpha = a)
        }
        for (k in 0 until 3) {
            val appear = screenTime - 0.35f - k * 0.3f
            val kx = cx + (k - 1) * 150f
            val ky = top + 450f - if (k == 1) 25f else 0f
            if (k < crownsWon && appear > 0f) {
                val s = min(1f, appear / 0.2f)
                val pop = 1f + 0.35f * (1f - s)
                if (!save.lowGfx) gfx.glowA(c, gfx.starGlow, kx, ky, 120f, (255 * s).toInt())
                gfx.icon(c, Gfx.I_CROWN, kx, ky, 55f * pop, Gfx.GOLD, (255 * s).toInt())
            } else {
                gfx.icon(c, Gfx.I_CROWN, kx, ky, 55f, Gfx.PANEL2, a)
            }
        }
        val ly = top + 570f
        crit(c, ly, true, "Level geschafft", a)
        crit(c, ly + 62f, runRevives == 0, "Ohne Wiederbelebung", a)
        crit(c, ly + 124f, perfOk, perfStr, a)
        val tw = gfx.textWidth(earnedStr, 54f)
        gfx.starShape(c, cx - tw / 2f - 40f, top + 780f, 24f, time * 40f, Gfx.GOLD, a)
        gfx.text(c, earnedStr, cx + 15f, top + 799f, 54f, Gfx.GOLD, alpha = a)
        if (!firstClear) gfx.text(c, if (crownsNew > 0) "Neue Krone!" else "Belohnung nur beim ersten Mal", cx, top + 845f, 29f, Gfx.DIM, false, alpha = a)
        if (screenTime > 0.6f) {
            bNext.visible = runLevel < Levels.COUNT
            for (b in doneBtns) drawBtn(c, b)
        }
        if (runLevel >= Levels.COUNT) gfx.text(c, "Du hast die Reise geschafft!", cx, H * 0.64f, 40f, Gfx.ACCENT, alpha = a)
    }

    private fun crit(c: Canvas, y: Float, ok: Boolean, label: String, a: Int) {
        gfx.circle(c, 190f, y - 12f, 22f, if (ok) Gfx.GREEN else Gfx.PANEL2, a)
        if (ok) gfx.icon(c, Gfx.I_CHECK, 190f, y - 12f, 14f, Gfx.BG, a)
        gfx.text(c, label, 235f, y, 36f, if (ok) Color.WHITE else Gfx.DIM, false, Paint.Align.LEFT, a)
    }

    // ---------------------------------------------------------------- Level-Karte

    private fun drawMap(c: Canvas) {
        val cur = min(save.levelDone + 1, Levels.COUNT)
        for (i in 1 until Levels.COUNT) {
            val y1 = mapScreenY(mapU(i)); val y2 = mapScreenY(mapU(i + 1))
            if (max(y1, y2) < 150f || min(y1, y2) > H + 80f) continue
            val done = i < cur
            for (k in 1 until 7) {
                val f = k / 7f
                gfx.circle(c, mapX(i) + (mapX(i + 1) - mapX(i)) * f, y1 + (y2 - y1) * f,
                    if (done) 7f else 5f, if (done) Gfx.ACCENT else Gfx.DIM, if (done) 210 else 80)
            }
        }
        for (s in 0..(Levels.COUNT / 10)) {
            val by = mapScreenY(bodyU(s))
            if (by < -300f || by > H + 300f) continue
            val body = s + 1
            val r = if (s == 0) 120f else 100f
            gfx.drawBody(c, body, W / 2f, by, r, time, save.lowGfx)
            val reached = s == 0 || save.levelDone >= s * 10
            val name = Levels.bodyNames[body]
            val tw = gfx.textWidth(name, 38f)
            gfx.glass(c, W / 2f - tw / 2f - 36f, by + r + 30f, W / 2f + tw / 2f + 36f, by + r + 100f, 35f)
            gfx.text(c, name, W / 2f, by + r + 79f, 38f, if (reached) Color.WHITE else Gfx.DIM)
        }
        for (i in 1..Levels.COUNT) {
            val ny = mapScreenY(mapU(i))
            if (ny < 120f || ny > H + 80f) continue
            val nx = mapX(i)
            val r = if (i % 10 == 0) 60f else 50f
            when {
                i <= save.levelDone -> {
                    gfx.circle(c, nx, ny, r, Gfx.VIOLET)
                    gfx.circle(c, nx - 4f, ny - 4f, r - 6f, Gfx.ACCENT)
                    gfx.text(c, levelNum[i], nx, ny + 14f, 40f, Color.WHITE)
                    val cr = save.crownsOf(i)
                    for (k in 0 until 3) gfx.icon(c, Gfx.I_CROWN, nx + (k - 1) * 30f, ny + r + 22f, 13f, if (k < cr) Gfx.GOLD else Gfx.PANEL2)
                }
                i == cur -> {
                    if (!save.lowGfx) gfx.glow(c, gfx.starGlow, nx, ny, 150f)
                    gfx.ring(c, nx, ny, r + 14f + sin(time * 4f) * 6f, Gfx.GOLD, 5f, 180)
                    gfx.circle(c, nx, ny, r, Gfx.ORANGE)
                    gfx.circle(c, nx - 4f, ny - 4f, r - 6f, Gfx.GOLD)
                    gfx.text(c, levelNum[i], nx, ny + 14f, 42f, Gfx.BG)
                }
                else -> {
                    gfx.circle(c, nx, ny, r, Gfx.PANEL, 230)
                    gfx.ring(c, nx, ny, r, Color.WHITE, 2f, 35)
                    gfx.text(c, levelNum[i], nx, ny + 2f, 34f, Gfx.DIM)
                    gfx.icon(c, Gfx.I_LOCK, nx, ny + 26f, 10f, Gfx.DIM)
                }
            }
        }
        gfx.roundRect(c, -10f, -10f, W + 10f, 205f, 0f, Gfx.BG, 235)
        drawBtn(c, bBack)
        gfx.text(c, "REISE", W / 2f, 132f, 54f, Color.WHITE)
        crownPill(c, W - 40f)
    }

    // ---------------------------------------------------------------- Shop

    private fun drawShop(c: Canvas) {
        dim(c, 150)
        bBuyRevive.visible = shopTab == 2
        bBuyShield.visible = shopTab == 2
        if (shopTab == 2) drawExtras(c)
        else {
            c.save()
            c.clipRect(0f, gridTop - 20f, W, H)
            val n = shopCount()
            for (i in 0 until n) {
                val col = i % 3; val row = i / 3
                val x = 50f + col * (cardW + 24f)
                val y = gridTop + row * (cardH + 24f) - shopScroll
                if (y > H || y + cardH < gridTop - 20f) continue
                drawCard(c, i, x, y)
            }
            c.restore()
        }
        gfx.roundRect(c, -10f, -10f, W + 10f, gridTop - 20f, 0f, Gfx.BG, 245)
        drawBtn(c, bBack)
        gfx.text(c, "SHOP", W / 2f, 132f, 54f, Color.WHITE)
        starPill(c, save.stars)
        gfx.glass(c, 50f, 215f, 950f, 307f, 46f)
        val tb = bTab[shopTab]
        val sh = tb.shader
        if (sh != null) gfx.gradRoundRect(c, tb.x + 6f, tb.y + 6f, tb.x + tb.w - 6f, tb.y + tb.h - 6f, 40f, sh)
        for (i in 0 until 3) gfx.text(c, TAB_NAMES[i], bTab[i].x + 150f, 274f, 34f, if (i == shopTab) Color.WHITE else Gfx.DIM)
    }

    private fun drawCard(c: Canvas, i: Int, x: Float, y: Float) {
        val skinsTab = shopTab == 0
        val owned = if (skinsTab) save.ownsSkin(i) else save.ownsTrail(i)
        val selected = if (skinsTab) save.skin == i else save.trail == i
        val lv = if (skinsTab) Skins.skins[i].reqLevel else Skins.trails[i].reqLevel
        val cr = if (skinsTab) Skins.skins[i].reqCrowns else Skins.trails[i].reqCrowns
        val locked = !owned && !reqMet(lv, cr)
        gfx.glass(c, x, y, x + cardW, y + cardH, 36f)
        if (selected) gfx.strokeRoundRect(c, x, y, x + cardW, y + cardH, 36f, Gfx.GREEN, 5f)
        val cx = x + cardW / 2f
        val py = y + 130f
        if (skinsTab) {
            gfx.drawSkin(c, i, cx, py, 2.2f, time, save.lowGfx)
            gfx.text(c, Skins.skins[i].name, cx, y + 262f, 34f, Color.WHITE)
        } else {
            for (k in 0 until 24) {
                prevX[k] = cx - 105f + k * (210f / 23f)
                prevY[k] = py + sin(k * 0.35f + time * 4f) * 28f
            }
            gfx.drawTrail(c, i, prevX, prevY, 0, 24, 24, gfx.skinColor(save.skin, time), time)
            gfx.drawSkin(c, save.skin, prevX[23], prevY[23], 1f, time, true)
            gfx.text(c, Skins.trails[i].name, cx, y + 262f, 34f, Color.WHITE)
        }
        if (locked) {
            gfx.roundRect(c, x + 3f, y + 3f, x + cardW - 3f, y + 215f, 34f, Gfx.BG, 150)
            gfx.circle(c, cx, py, 34f, Gfx.PANEL, 230)
            gfx.icon(c, Gfx.I_LOCK, cx, py, 18f, Gfx.DIM)
        }
        when {
            selected -> {
                gfx.icon(c, Gfx.I_CHECK, cx - 58f, y + 319f, 14f, Gfx.GREEN)
                gfx.text(c, "AKTIV", cx + 12f, y + 330f, 32f, Gfx.GREEN)
            }
            owned -> gfx.text(c, "Auswählen", cx, y + 330f, 32f, Gfx.DIM, false)
            locked -> gfx.text(c, if (skinsTab) skinReq[i] else trailReq[i], cx, y + 330f, 27f, Gfx.DIM, false)
            else -> {
                val pv = if (skinsTab) Skins.skins[i].price else Skins.trails[i].price
                if (pv == 0) gfx.text(c, "GRATIS", cx, y + 330f, 32f, Gfx.GREEN)
                else {
                    val price = if (skinsTab) skinPrice[i] else trailPrice[i]
                    val tw = gfx.textWidth(price, 34f)
                    gfx.starShape(c, cx - tw / 2f - 20f, y + 318f, 15f, 0f, Gfx.GOLD)
                    gfx.text(c, price, cx + 12f, y + 330f, 34f, if (save.stars >= pv) Gfx.GOLD else Gfx.DIM)
                }
            }
        }
    }

    private fun drawExtras(c: Canvas) {
        extraCard(c, gridTop, Gfx.I_PLUS, Gfx.GREEN, "Wiederbelebung",
            "Nach einem Absturz weiterspielen", save.revives, Skins.REVIVE_MAX, Skins.REVIVE_PRICE, bBuyRevive)
        extraCard(c, gridTop + 560f, Gfx.I_SHIELD, Gfx.SHIELD, "Start-Schild",
            "Schützt einmal – startet automatisch", save.shieldStarts, Skins.SHIELD_MAX, Skins.SHIELD_PRICE, bBuyShield)
    }

    private fun extraCard(c: Canvas, y: Float, icon: Int, col: Int, title: String, desc: String,
                          have: Int, maxN: Int, price: Int, b: Btn) {
        val cx = W / 2f
        gfx.glass(c, 50f, y, 950f, y + 530f, 44f)
        if (!save.lowGfx) gfx.glow(c, if (icon == Gfx.I_SHIELD) gfx.shieldGlow else gfx.goalGlow, cx, y + 115f, 140f)
        gfx.circle(c, cx, y + 115f, 62f, col)
        gfx.icon(c, icon, cx, y + 115f, 32f, Color.WHITE)
        gfx.text(c, title, cx, y + 240f, 50f, Color.WHITE)
        gfx.text(c, desc, cx, y + 295f, 34f, Gfx.DIM, false)
        for (k in 0 until maxN) {
            val dx = cx + (k - (maxN - 1) / 2f) * 62f
            if (k < have) gfx.circle(c, dx, y + 345f, 19f, col) else gfx.ring(c, dx, y + 345f, 19f, Gfx.DIM, 3.5f)
        }
        if (b.y != y + 390f) place(b, cx - 250f, y + 390f, 500f, 115f)
        b.label = if (have >= maxN) "VOLL" else "KAUFEN  ·  $price"
        b.visible = true
        drawBtn(c, b)
        if (have < maxN) {
            val tw = gfx.textWidth(b.label, b.h * 0.3f)
            gfx.starShape(c, cx + tw / 2f + 28f, b.y + b.h / 2f, 18f, 0f, Gfx.BG)
        }
    }

    // ---------------------------------------------------------------- Erfolge

    private fun drawAch(c: Canvas) {
        dim(c, 150)
        c.save()
        c.clipRect(0f, 205f, W, H)
        for (i in Achievements.list.indices) {
            val y = achTop + i * (achH + 20f) - achScroll
            if (y > H || y + achH < 205f) continue
            val a = Achievements.list[i]
            val done = Achievements.done(i, save)
            val claimed = Achievements.claimed(i, save)
            gfx.glass(c, 40f, y, 960f, y + achH, 36f, border = if (done && !claimed) Gfx.GOLD else Color.WHITE)
            gfx.circle(c, 125f, y + achH / 2f, 52f, if (done) Gfx.GOLD else Gfx.PANEL2)
            gfx.icon(c, Gfx.I_TROPHY, 125f, y + achH / 2f + 4f, 28f, if (done) Gfx.BG else Gfx.DIM)
            gfx.text(c, a.title, 205f, y + 62f, 40f, Color.WHITE, align = Paint.Align.LEFT)
            gfx.text(c, a.desc, 205f, y + 105f, 30f, Gfx.DIM, false, Paint.Align.LEFT)
            val v = min(Achievements.value(a, save), a.goal)
            val bw = 420f
            gfx.roundRect(c, 205f, y + 128f, 205f + bw, y + 144f, 8f, Gfx.PANEL2)
            gfx.roundRect(c, 205f, y + 128f, 205f + max(16f, bw * v / a.goal), y + 144f, 8f, if (done) Gfx.GOLD else Gfx.ACCENT)
            gfx.text(c, achProgStr[i], 645f, y + 144f, 26f, Gfx.DIM, false, Paint.Align.LEFT)
            when {
                claimed -> {
                    gfx.circle(c, 885f, y + achH / 2f, 34f, Gfx.GREEN)
                    gfx.icon(c, Gfx.I_CHECK, 885f, y + achH / 2f, 18f, Gfx.BG)
                }
                done -> {
                    gfx.roundRect(c, 790f, y + 45f, 940f, y + 130f, 42f, Gfx.GOLD)
                    gfx.text(c, "+" + achRewardStr[i], 865f, y + 102f, 36f, Gfx.BG)
                }
                else -> {
                    gfx.starShape(c, 840f, y + 60f, 15f, 0f, Gfx.GOLD)
                    gfx.text(c, achRewardStr[i], 925f, y + 72f, 32f, Gfx.GOLD, align = Paint.Align.RIGHT)
                }
            }
        }
        c.restore()
        gfx.roundRect(c, -10f, -10f, W + 10f, 205f, 0f, Gfx.BG, 245)
        drawBtn(c, bBack)
        gfx.text(c, "ERFOLGE", W / 2f, 132f, 54f, Color.WHITE)
        starPill(c, save.stars)
    }

    // ---------------------------------------------------------------- Einstellungen

    private val settingLabels = arrayOf("Soundeffekte", "Musik", "Vibration", "Bildschirmwackeln", "FPS-Anzeige", "Grafik")
    private val settingIcons = intArrayOf(Gfx.I_SOUND, Gfx.I_MUSIC, Gfx.I_VIBRATE, Gfx.I_SHAKE, Gfx.I_SPEED, Gfx.I_EYE)

    private fun drawSettings(c: Canvas) {
        header(c, "OPTIONEN")
        for (i in 0 until 6) {
            val b = bRow[i]
            gfx.glass(c, b.x, b.y, b.x + b.w, b.y + b.h, 36f, if (b === pressed) 255 else 235)
            gfx.circle(c, b.x + 70f, b.y + b.h / 2f, 38f, Gfx.PANEL2)
            gfx.icon(c, settingIcons[i], b.x + 70f, b.y + b.h / 2f, 20f, Gfx.ACCENT)
            gfx.text(c, settingLabels[i], b.x + 130f, b.y + b.h / 2f + 14f, 40f, Color.WHITE, false, Paint.Align.LEFT)
            if (i == 5) {
                val v = if (save.lowGfx) "Sparsam" else "Hoch"
                gfx.roundRect(c, b.x + b.w - 260f, b.y + 26f, b.x + b.w - 30f, b.y + b.h - 26f, 33f, Gfx.PANEL2)
                gfx.text(c, v, b.x + b.w - 145f, b.y + b.h / 2f + 12f, 34f, Gfx.ACCENT)
            } else {
                val on = when (i) { 0 -> save.sound; 1 -> save.music; 2 -> save.vibration; 3 -> save.shake; else -> save.showFps }
                toggle(c, b.x + b.w - 175f, b.y + b.h / 2f, on)
            }
        }
        drawBtn(c, bReset)
        gfx.text(c, "Grafik „Sparsam“ = noch flüssiger auf alten Handys", W / 2f, bReset.y + bReset.h + 75f, 29f, Gfx.DIM, false)
        gfx.text(c, "Orbit v$VERSION_NAME · Engelcode", W / 2f, H - 30f, 26f, Gfx.DIM, false, alpha = 150)
    }

    private fun toggle(c: Canvas, x: Float, cy: Float, on: Boolean) {
        gfx.roundRect(c, x, cy - 36f, x + 140f, cy + 36f, 36f, if (on) Gfx.ACCENT else Gfx.PANEL2)
        gfx.circle(c, if (on) x + 104f else x + 36f, cy, 28f, Color.WHITE)
    }

    // ---------------------------------------------------------------- Statistik

    private fun drawStats(c: Canvas) {
        header(c, "STATISTIK")
        for (i in statLabels.indices) {
            val y = 235f + i * 108f
            gfx.glass(c, 40f, y, 960f, y + 92f, 30f)
            gfx.text(c, statLabels[i], 80f, y + 60f, 36f, Gfx.DIM, false, Paint.Align.LEFT)
            gfx.text(c, statValues[i], 920f, y + 62f, 42f, Color.WHITE, align = Paint.Align.RIGHT)
        }
    }

    // ---------------------------------------------------------------- Glücksrad

    private fun drawWheel(c: Canvas) {
        header(c, "GLÜCKSRAD")
        starPill(c, save.stars)
        val cx = W / 2f
        val cy = wheelCY()
        val r = 360f
        if (!save.lowGfx) gfx.glowA(c, gfx.planetGlow[2], cx, cy, r * 1.6f, 200)
        gfx.circle(c, cx, cy, r + 26f, Gfx.PANEL)
        gfx.rect.set(cx - r, cy - r, cx + r, cy + r)
        for (i in 0 until 8) {
            gfx.fill.color = WHEEL_COL[i]; gfx.fill.alpha = 255
            c.drawArc(gfx.rect, wheelAngle + i * 45f - 90f - 22.5f, 45f, true, gfx.fill)
        }
        gfx.circle(c, cx, cy, r, Color.BLACK, 40)
        for (i in 0 until 8) {
            c.save()
            c.translate(cx, cy)
            c.rotate(wheelAngle + i * 45f)
            when (WHEEL_KIND[i]) {
                0 -> {
                    gfx.text(c, wheelLabel[i], 0f, -r * 0.62f, 64f, Color.WHITE)
                    gfx.starShape(c, 0f, -r * 0.42f, 22f, 0f, Color.WHITE)
                }
                1 -> gfx.icon(c, Gfx.I_PLUS, 0f, -r * 0.6f, 40f, Color.WHITE)
                else -> gfx.icon(c, Gfx.I_SHIELD, 0f, -r * 0.6f, 42f, Color.WHITE)
            }
            c.restore()
        }
        // Lichter am Rand
        for (k in 0 until 24) {
            val a = k * 0.2618f
            val on = ((time * 6f).toInt() + k) % 2 == 0
            gfx.circle(c, cx + cos(a) * (r + 13f), cy + sin(a) * (r + 13f), 7f, if (on) Gfx.GOLD else Color.WHITE, if (on) 255 else 90)
        }
        gfx.circle(c, cx, cy, 70f, Gfx.PANEL)
        gfx.ring(c, cx, cy, 70f, Gfx.GOLD, 6f)
        gfx.starShape(c, cx, cy, 34f, time * 30f, Gfx.GOLD)
        // Zeiger oben
        c.save()
        c.translate(cx, cy - r - 40f)
        gfx.fill.color = Color.WHITE; gfx.fill.alpha = 255
        c.drawPath(pointerPath, gfx.fill)
        c.restore()

        bSpin.label = if (freeSpin()) "GRATIS DREHEN" else "DREHEN  ·  $SPIN_PRICE"
        bSpin.visible = !spinning
        if (!spinning) {
            drawBtn(c, bSpin)
            if (!freeSpin()) {
                val tw = gfx.textWidth(bSpin.label, bSpin.h * 0.3f)
                gfx.starShape(c, cx + tw / 2f + 28f, bSpin.y + bSpin.h / 2f, 18f, 0f, Gfx.BG)
            }
        }
        gfx.text(c, if (freeSpin()) "1x täglich gratis" else "Nächste Gratis-Drehung: morgen", cx, bSpin.y + bSpin.h + 70f, 32f, Gfx.DIM, false)
    }

    // ---------------------------------------------------------------- Dialog & Toast

    private fun drawDialog(c: Canvas) {
        dim(c, 190)
        gfx.glass(c, 70f, dlgTop, 930f, dlgTop + dlgH, 48f, border = Gfx.ACCENT)
        gfx.text(c, dlgTitle, W / 2f, dlgTop + 110f, 52f, Color.WHITE)
        val news = dialog == D_NEWS
        for (i in dlgLines.indices) {
            val ly = dlgTop + 190f + i * 62f
            if (news) gfx.text(c, dlgLines[i], 120f, ly, 35f, Color.WHITE, false, Paint.Align.LEFT)
            else gfx.text(c, dlgLines[i], W / 2f, ly, 42f,
                if (i == dlgLines.size - 1 && dialog == D_DAILY) Gfx.GOLD else Color.WHITE, dialog == D_DAILY)
        }
        drawBtn(c, bDlgA)
        if (dlgTwo) drawBtn(c, bDlgB)
    }

    private fun drawToast(c: Canvas) {
        val a = (min(1f, toastT / 0.4f) * 255f).toInt()
        val tw = gfx.textWidth(toast, 38f, false) + 90f
        val y = H * 0.84f
        gfx.glass(c, W / 2f - tw / 2f, y - 62f, W / 2f + tw / 2f, y + 32f, 47f, a)
        gfx.text(c, toast, W / 2f, y, 38f, Color.WHITE, false, alpha = a)
    }
}
