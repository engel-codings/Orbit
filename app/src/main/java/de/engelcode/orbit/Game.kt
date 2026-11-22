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
        const val VERSION_CODE = 6
        const val VERSION_NAME = "0.6.0"
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

        private const val CHALLENGE_GOAL = 15
        private const val CHALLENGE_REWARD = 60
        private const val SPIN_PRICE = 60

        // Glücksrad: 0 = Sterne, 1 = Wiederbelebung, 2 = Start-Schild
        private val WHEEL_KIND = intArrayOf(0, 0, 1, 0, 0, 2, 0, 0)
        private val WHEEL_VAL = intArrayOf(20, 50, 1, 10, 100, 1, 30, 15)
        private val WHEEL_WEIGHT = intArrayOf(20, 10, 8, 22, 4, 10, 14, 12)
        private val WHEEL_COL = intArrayOf(
            0xFF7B61FF.toInt(), 0xFF4CC9F0.toInt(), 0xFF2EE59D.toInt(), 0xFFF15BB5.toInt(),
            0xFFFFD166.toInt(), 0xFF5CC8FF.toInt(), 0xFFFF9F1C.toInt(), 0xFF9B5DE5.toInt()
        )
        private val RANK_AT = intArrayOf(0, 50, 200, 500, 1000, 2500)
    }

    private enum class Screen { MENU, MAP, PLAYING, PAUSED, GAMEOVER, LEVELDONE, SHOP, SETTINGS, STATS, ACHIEVE, WHEEL, MISSIONS }

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
    private val bEndless = Btn(2, "", S_GLASS, Gfx.I_INFINITY)
    private val bShop = Btn(3, "", S_ROUND, Gfx.I_SHOP)
    private val bAch = Btn(4, "", S_ROUND, Gfx.I_TROPHY)
    private val bStats = Btn(5, "", S_ROUND, Gfx.I_CHART)
    private val bSettings = Btn(6, "", S_ROUND, Gfx.I_GEAR)
    private val bBack = Btn(7, "", S_BACK)
    private val bPause = Btn(8, "", S_BACK)
    private val bResume = Btn(9, "", S_PRIMARY)
    private val bRestart = Btn(10, "", S_GLASS)
    private val bQuit = Btn(11, "", S_GLASS)
    private val bRevive = Btn(12, "", S_GREEN)
    private val bAgain = Btn(13, "", S_PRIMARY)
    private val bLeave = Btn(14, "", S_GLASS)
    private val bNext = Btn(15, "", S_PRIMARY)
    private val bToMap = Btn(16, "", S_GLASS)
    private val bTab = Array(3) { Btn(17 + it, "", S_INVISIBLE) }
    private val bRow = Array(7) { Btn(40 + it, "", S_INVISIBLE) }
    private val bReset = Btn(30, "", S_DANGER)
    private val bDlgA = Btn(31, "OK", S_PRIMARY)
    private val bDlgB = Btn(32, "", S_GLASS)
    private val bBuyRevive = Btn(33, "", S_GOLD)
    private val bBuyShield = Btn(34, "", S_GOLD)
    private val bChallenge = Btn(35, "", S_GLASS, Gfx.I_TARGET)
    private val bWheel = Btn(36, "", S_GLASS, Gfx.I_WHEEL)
    private val bSpin = Btn(37, "", S_GOLD)
    private val bMissions = Btn(38, "", S_ROUND, Gfx.I_LIST)
    private val bChest = Btn(39, "", S_GLASS, Gfx.I_CHEST)

    private val menuBtns = arrayOf(bPlay, bEndless, bChallenge, bWheel, bChest, bShop, bMissions, bAch, bStats, bSettings)
    private val playBtns = arrayOf(bPause)
    private val pauseBtns = arrayOf(bResume, bRestart, bQuit)
    private val overBtns = arrayOf(bRevive, bAgain, bLeave)
    private val doneBtns = arrayOf(bNext, bToMap)
    private val shopBtns = arrayOf(bBack, bTab[0], bTab[1], bTab[2], bBuyRevive, bBuyShield)
    private val setBtns = arrayOf(bBack, bRow[0], bRow[1], bRow[2], bRow[3], bRow[4], bRow[5], bRow[6], bReset)
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
    private val skinReq = Array(Skins.skins.size) { "" }
    private val trailReq = Array(Skins.trails.size) { "" }
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
    private val achDescStr = Array(Achievements.list.size) { "" }
    private val achTitleStr = Array(Achievements.list.size) { "" }
    private var achBadge = 0
    private var rankStr = ""
    private var dimTitle = ""
    private var dimTitle3 = ""
    private var missionBadge = 0
    private var chestLabelMin = -1
    private val missionDescStr = arrayOf("", "", "")
    private val missionProgStr = arrayOf("", "", "")
    private var missionTimerStr = ""
    private var missionTimerMin = -1
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
        L.set(if (save.lang < 0) L.systemDefault() else save.lang)
        applyTexts()
    }

    private fun reqText(lv: Int, cr: Int) = when {
        lv > 0 -> L.f(L.REQ_LEVEL, lv)
        cr > 0 -> L.f(L.REQ_CROWNS, cr, L.crowns(cr))
        else -> ""
    }

    /** Alle festen Texte in der aktuellen Sprache setzen. */
    private fun applyTexts() {
        gfx.applyLanguage(L.cur)
        bMissions.label = L.t(L.MISSIONS)
        bShop.label = L.t(L.SHOP); bAch.label = L.t(L.ACHIEVEMENTS); bStats.label = L.t(L.STATS); bSettings.label = L.t(L.OPTIONS)
        bChallenge.label = L.t(L.CHALLENGE); bWheel.label = L.t(L.WHEEL)
        bResume.label = L.t(L.RESUME); bRestart.label = L.t(L.RESTART); bQuit.label = L.t(L.MENU)
        bAgain.label = L.t(L.AGAIN); bNext.label = L.t(L.NEXT_LEVEL); bToMap.label = L.t(L.TO_MAP)
        bReset.label = L.t(L.RESET_BTN)
        for (i in 0 until 3) bTab[i].label = L.t(L.TABS[i])
        for (i in skinReq.indices) skinReq[i] = reqText(Skins.skins[i].reqLevel, Skins.skins[i].reqCrowns)
        for (i in trailReq.indices) trailReq[i] = reqText(Skins.trails[i].reqLevel, Skins.trails[i].reqCrowns)
        starsVal = -1; crownVal = -1; scoreVal = -1; progVal = -1; challengeVal = -1
        refreshMenuTexts()
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
        place(bChallenge, 40f, pcy + 465f, 293f, 105f)
        place(bWheel, 353f, pcy + 465f, 293f, 105f)
        place(bChest, 666f, pcy + 465f, 293f, 105f)
        place(bSpin, cx - 290f, wheelCY() + 420f, 580f, 140f)
        val ny = H - 300f
        val nav = arrayOf(bShop, bMissions, bAch, bStats, bSettings)
        for (i in 0 until 5) place(nav[i], 100f + i * 200f - 65f, ny, 130f, 130f)
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
        for (i in 0 until 7) place(bRow[i], 40f, 240f + i * 122f, 920f, 104f)
        place(bReset, 80f, 240f + 7 * 122f + 24f, 840f, 106f)
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
        nextStr = L.f(L.LEVEL_N, next)
        dimTitle = L.f(L.DIMENSION, 2)
        dimTitle3 = L.f(L.DIMENSION, 3)
        missionsToday()
        buildMissions()
        chestLabelMin = -1
        bEndless.label = if (save.best > 0) L.f(L.ENDLESS_BEST, save.best) else L.t(L.ENDLESS)
        achBadge = Achievements.claimableCount(save)
        var r = 0
        for (i in RANK_AT.indices) if (save.planets >= RANK_AT[i]) r = i
        rankIdx = r
        rankStr = if (r < RANK_AT.size - 1) L.f(L.RANK, L.t(L.RANKS[r]), save.planets, RANK_AT[r + 1]) else L.f(L.RANK_MAX, L.t(L.RANKS[r]))
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
        levelStr = if (level > 0) L.f(L.LEVEL_N, level) else ""
        progVal = -1
        go(Screen.PLAYING)
        if (level >= Levels.DIM2_FROM) showToast(L.t(if (Levels.dimension(level) == 3) L.DIM3_HINT else L.DIM2_HINT))
        if (save.shieldStarts > 0) {
            save.shieldStarts--
            save.flush()
            world.shield = true
            showToast(L.t(L.T_START_SHIELD))
        }
    }

    private fun leaveRun() {
        world.reset(0)
        go(if (runLevel > 0) Screen.MAP else Screen.MENU)
    }

    // ===================================================================== Missionen

    private fun missionsToday() {
        val d = today()
        if (save.missionDay != d) {
            save.missionDay = d
            save.missionClaimed = 0
            for (i in 0 until 3) save.missionProg[i] = 0
            save.flush()
        }
        Missions.prepare(d)
    }

    /** Fortschritt melden. absolute = true: Wert ist ein Höchststand (Endlos-Punkte). */
    private fun mission(type: Int, amount: Int, absolute: Boolean = false) {
        missionsToday()
        for (k in 0 until 3) {
            if (Missions.type[k] != type) continue
            val g = Missions.goal[k]
            val before = save.missionProg[k]
            val now = if (absolute) max(before, amount) else before + amount
            save.missionProg[k] = min(now, g)
            if (before < g && save.missionProg[k] >= g) {
                sound.play(Sound.CROWN)
                showToast(L.t(L.T_MISSION))
            }
        }
        buildMissions()
    }

    private fun buildMissions() {
        var badge = 0
        for (k in 0 until 3) {
            missionDescStr[k] = Missions.desc(k)
            missionProgStr[k] = "${save.missionProg[k]} / ${Missions.goal[k]}"
            if (save.missionProg[k] >= Missions.goal[k] && save.missionClaimed and (1 shl k) == 0) badge++
        }
        missionBadge = badge
    }

    private fun missionTap(y: Float) {
        for (k in 0 until 3) {
            val top = 260f + k * 300f
            if (y < top || y > top + 270f) continue
            if (save.missionProg[k] >= Missions.goal[k] && save.missionClaimed and (1 shl k) == 0) {
                val r = Missions.REWARD[k]
                save.missionClaimed = save.missionClaimed or (1 shl k)
                save.missionsDone++
                save.stars += r
                save.flush()
                buildMissions()
                sound.play(Sound.BONUS)
                sound.vibrate(40)
                showToast(L.f(L.T_PLUS_STARS, r, L.stars(r)))
            }
        }
    }

    // ===================================================================== Truhe

    private fun chestReady() = System.currentTimeMillis() >= save.chestReady

    private fun updateChestLabel() {
        if (chestReady()) {
            if (chestLabelMin != 0) { chestLabelMin = 0; bChest.label = L.t(L.CHEST) }
            return
        }
        val mins = ((save.chestReady - System.currentTimeMillis()) / 60000L).toInt() + 1
        if (mins != chestLabelMin) {
            chestLabelMin = mins
            bChest.label = "${mins / 60}:${(mins % 60).toString().padStart(2, '0')} h"
        }
    }

    private fun openChest() {
        if (!chestReady()) {
            val mins = ((save.chestReady - System.currentTimeMillis()) / 60000L).toInt() + 1
            sound.play(Sound.ERROR)
            showToast(L.f(L.T_CHEST_WAIT, mins / 60, mins % 60))
            return
        }
        val r = 8 + (Math.random() * 13).toInt()
        save.stars += r
        save.chests++
        save.chestReady = System.currentTimeMillis() + 4L * 60L * 60L * 1000L
        save.flush()
        chestLabelMin = -1
        sound.play(Sound.BONUS)
        sound.vibrate(40)
        showToast(L.f(L.T_CHEST, r, L.stars(r)))
    }

    // ===================================================================== Glücksrad

    private fun freeSpin() = save.wheelDay != today()

    private fun startSpin() {
        if (spinning) return
        val free = freeSpin()
        if (!free) {
            if (save.stars < SPIN_PRICE) {
                sound.play(Sound.ERROR)
                showToast(L.f(L.T_MISSING, SPIN_PRICE - save.stars, L.stars(SPIN_PRICE - save.stars)))
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
        mission(Missions.SPIN, 1)
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
                0 -> L.f(L.T_WON_STARS, WHEEL_VAL[spinPrize], L.stars(WHEEL_VAL[spinPrize]))
                1 -> L.t(L.T_WON_REVIVE)
                else -> L.t(L.T_WON_SHIELD)
            })
        }
    }

    private fun commitRun() {
        val bonus = if (runLevel == 0) world.score / 8 else 0
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
        mission(Missions.PLANETS, 1)
        if (perfect) mission(Missions.PERFECTS, 1)
        if (runLevel == 0) mission(Missions.ENDLESS, world.score, true)
        if (perfect) sound.play(Sound.PERFECT, 1f, 0.8f)
        sound.vibrate(15)
    }

    override fun onStar() {
        sound.play(Sound.STAR)
        mission(Missions.STARS, 1)
    }

    override fun onPowerup(type: Int) {
        save.powerups++
        mission(Missions.POWERUPS, 1)
        sound.play(Sound.POWER)
        sound.vibrate(30)
        showToast(L.t(if (type == World.P_SHIELD) L.T_SHIELD else L.T_MAGNET))
    }

    override fun onShieldBreak() {
        sound.play(Sound.SHIELD)
        sound.vibrate(90)
        showToast(L.t(L.T_SAVED))
    }

    override fun onDeath() {
        sound.play(Sound.DEATH)
        sound.vibrate(160)
        commitRun()
        earnedStr = "+$runEarned"
        bestStr = L.f(L.RECORD, save.best)
        bRevive.label = L.f(L.REVIVE_BTN, save.revives)
        bLeave.label = L.t(if (runLevel > 0) L.TO_MAP else L.MENU)
        go(Screen.GAMEOVER)
    }

    override fun onGoal() {
        sound.play(Sound.WIN)
        sound.vibrate(80)
        mission(Missions.LEVELS, 1)
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
        perfStr = L.f(L.CRIT_PERFECT, min(world.perfects, goal), goal)
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
                    showToast(L.f(L.T_CHALLENGE, CHALLENGE_REWARD, L.stars(CHALLENGE_REWARD)))
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
                    openDialog(D_NEWS, L.f(L.NEWS_TITLE, VERSION_NAME), Array(L.NEWS.size) { L.t(L.NEWS[it]) }, L.t(L.LETS_GO), null)
                    return
                }
            }
        }
        val today = today()
        if (today > save.lastDay && screenTime > 0.4f) {
            pendingDay = today
            pendingStreak = if (today == save.lastDay + 1) save.dayStreak + 1 else 1
            pendingBonus = 15 + 5 * min(pendingStreak - 1, 7)
            openDialog(D_DAILY, L.t(L.DAILY_TITLE), arrayOf(L.f(L.DAILY_DAY, pendingStreak), L.f(L.T_PLUS_STARS, pendingBonus, L.stars(pendingBonus)).removeSuffix("!")), L.t(L.CLAIM), null)
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
                    Screen.MISSIONS -> missionTap(y)
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
        Screen.MISSIONS -> backBtns
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
            bRow[6].id -> {
                save.lang = (L.cur + 1) % 3
                save.flush()
                L.set(save.lang)
                applyTexts()
                buildStats(); buildAch()
            }
            bChallenge.id -> startRun(0, today() * 7919L + 17L)
            bWheel.id -> go(Screen.WHEEL)
            bSpin.id -> startSpin()
            bMissions.id -> go(Screen.MISSIONS)
            bChest.id -> openChest()
            bReset.id -> openDialog(
                D_RESET, L.t(L.RESET_TITLE),
                arrayOf(L.t(L.RESET_LINE_1), L.t(L.RESET_LINE_2)), L.t(L.DELETE), L.t(L.CANCEL)
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
            D_RESET -> if (yes) { save.resetProgress(); refreshMenuTexts(); showToast(L.t(L.T_RESET)) }
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
        showToast(L.f(L.T_PLUS_STARS, pendingBonus, L.stars(pendingBonus)))
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
        0 -> L.f(L.ITEM_SKIN, Skins.skins[i].name)
        1 -> L.f(L.ITEM_TRAIL, Skins.trails[i].name)
        2 -> L.t(L.ITEM_REVIVE)
        else -> L.t(L.ITEM_SHIELD)
    }

    private fun askBuy(tab: Int, i: Int) {
        if (tab == 0 || tab == 1) {
            val lv = if (tab == 0) Skins.skins[i].reqLevel else Skins.trails[i].reqLevel
            val cr = if (tab == 0) Skins.skins[i].reqCrowns else Skins.trails[i].reqCrowns
            if (!reqMet(lv, cr)) {
                sound.play(Sound.ERROR)
                showToast(if (lv > 0) L.f(L.T_LEVEL_FIRST, lv) else L.f(L.T_NEED_CROWNS, cr, L.crowns(cr)))
                return
            }
        }
        if (tab == 2 && save.revives >= Skins.REVIVE_MAX) { sound.play(Sound.ERROR); showToast(L.f(L.T_MAX_REVIVES, Skins.REVIVE_MAX)); return }
        if (tab == 3 && save.shieldStarts >= Skins.SHIELD_MAX) { sound.play(Sound.ERROR); showToast(L.f(L.T_MAX_SHIELDS, Skins.SHIELD_MAX)); return }
        val price = priceOf(tab, i)
        if (price == 0) { buy(tab, i); return }
        if (save.stars < price) {
            sound.play(Sound.ERROR)
            showToast(L.f(L.T_MISSING, price - save.stars, L.stars(price - save.stars)))
            return
        }
        pendingTab = tab; pendingItem = i
        openDialog(D_BUY, L.t(L.BUY_TITLE), arrayOf(nameOf(tab, i), L.f(L.BUY_FOR, price, L.stars(price))), L.t(L.BUY), L.t(L.CANCEL))
    }

    private fun buy(tab: Int, i: Int) {
        val price = priceOf(tab, i)
        if (save.stars < price) return
        save.stars -= price
        when (tab) {
            0 -> { save.ownedSkins = save.ownedSkins or (1L shl i); save.skin = i }
            1 -> { save.ownedTrails = save.ownedTrails or (1 shl i); save.trail = i }
            2 -> save.revives = min(Skins.REVIVE_MAX, save.revives + 1)
            else -> save.shieldStarts = min(Skins.SHIELD_MAX, save.shieldStarts + 1)
        }
        save.flush()
        sound.play(Sound.BUY)
        sound.vibrate(30)
        showToast(L.t(if (tab >= 2) L.T_BOUGHT else L.T_UNLOCKED))
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
                else { sound.play(Sound.ERROR); showToast(L.f(L.T_LEVEL_FIRST, save.levelDone + 1)) }
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
            achDescStr[i] = Achievements.desc(a)
            achTitleStr[i] = a.title
        }
        achBadge = Achievements.claimableCount(save)
    }

    private fun achTap(x: Float, y: Float) {
        if (y < 210f) return
        val i = ((y + achScroll - achTop) / (achH + 20f)).toInt()
        if (y + achScroll < achTop || i !in Achievements.list.indices || x < 40f || x > 960f) return
        if (Achievements.done(i, save) && !Achievements.claimed(i, save)) {
            val a = Achievements.list[i]
            save.achClaimed = save.achClaimed or (1L shl i)
            save.stars += a.reward
            save.flush()
            buildAch()
            sound.play(Sound.BONUS)
            sound.vibrate(40)
            showToast(L.f(L.T_PLUS_STARS, a.reward, L.stars(a.reward)))
        }
    }

    // ===================================================================== Statistik

    private fun buildStats() {
        val mins = (save.playTime / 60f).toInt()
        statLabels = Array(L.STAT_LABELS.size) { L.t(L.STAT_LABELS[it]) }
        statValues = arrayOf(
            "${save.levelDone} / ${Levels.COUNT}", "${save.totalCrowns} / ${Levels.COUNT * 3}",
            save.best.toString(), save.games.toString(), save.planets.toString(), save.perfects.toString(),
            save.bestStreak.toString(), save.starsCollected.toString(), save.powerups.toString(),
            save.revivesUsed.toString(), save.challengesDone.toString(), save.spins.toString(),
            save.missionsDone.toString(), save.chests.toString(), if (mins >= 60) L.f(L.TIME_HM, mins / 60, mins % 60) else L.f(L.TIME_M, mins)
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
            Screen.MISSIONS -> drawMissions(c)
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
                gfx.text(c, b.label, cx, cy + b.h * 0.11f, b.h * 0.3f, tc, maxW = b.w - 60f)
            }
            S_GLASS -> {
                gfx.glass(c, l, t, r, bt, (bt - t) / 2f)
                if (b.icon >= 0) {
                    val tw = min(gfx.textWidth(b.label, b.h * 0.3f), b.w - 130f)
                    gfx.icon(c, b.icon, cx - tw / 2f - 34f, cy, b.h * 0.2f, Gfx.ACCENT)
                    gfx.text(c, b.label, cx + 28f, cy + b.h * 0.11f, b.h * 0.3f, Color.WHITE, maxW = b.w - 130f)
                } else gfx.text(c, b.label, cx, cy + b.h * 0.11f, b.h * 0.3f, Color.WHITE, maxW = b.w - 60f)
            }
            S_ROUND -> {
                val rr = b.w / 2f - 8f - ins
                val ry = b.y + b.w / 2f
                gfx.circle(c, cx, ry, rr, Gfx.PANEL, 225)
                gfx.ring(c, cx, ry, rr, Color.WHITE, 2.5f, 45)
                gfx.icon(c, b.icon, cx, ry, rr * 0.42f, Color.WHITE)
                gfx.text(c, b.label, cx, b.y + b.h + 30f, 26f, Gfx.DIM, maxW = 220f)
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
        gfx.text(c, title, W / 2f, 132f, 54f, Color.WHITE, maxW = 560f)
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
        gfx.text(c, rankStr, cx, ty + 130f, 30f, Gfx.DIM, false, maxW = 860f)

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

        gfx.text(c, nextStr, cx, pcy + 290f, 46f, Color.WHITE, maxW = 700f)
        drawBtn(c, bEndless)
        drawBtn(c, bChallenge); drawBtn(c, bWheel)
        if (save.challengeDay != today()) gfx.circle(c, bChallenge.x + bChallenge.w - 14f, bChallenge.y + 14f, 14f, Gfx.RED)
        else gfx.icon(c, Gfx.I_CHECK, bChallenge.x + bChallenge.w - 18f, bChallenge.y + 18f, 14f, Gfx.GREEN)
        if (freeSpin()) gfx.circle(c, bWheel.x + bWheel.w - 14f, bWheel.y + 14f, 14f, Gfx.RED)
        updateChestLabel()
        drawBtn(c, bChest)
        if (chestReady()) gfx.circle(c, bChest.x + bChest.w - 14f, bChest.y + 14f, 14f, Gfx.RED)

        drawBtn(c, bShop); drawBtn(c, bMissions); drawBtn(c, bAch); drawBtn(c, bStats); drawBtn(c, bSettings)
        if (missionBadge > 0) {
            val bx = bMissions.x + bMissions.w - 18f; val by = bMissions.y + 18f
            gfx.circle(c, bx, by, 22f, Gfx.RED)
            gfx.text(c, levelNum[missionBadge], bx, by + 9f, 26f, Color.WHITE)
        }
        if (achBadge > 0) {
            val bx = bAch.x + bAch.w - 18f; val by = bAch.y + 18f
            gfx.circle(c, bx, by, 22f, Gfx.RED)
            gfx.text(c, levelNum[min(achBadge, Levels.COUNT)], bx, by + 9f, 26f, Color.WHITE)
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
                    challengeStr = if (save.challengeDay == today() && v >= CHALLENGE_GOAL) L.t(L.CHALLENGE_DONE) else L.f(L.CHALLENGE_PROGRESS, v, CHALLENGE_GOAL)
                }
                val done = save.challengeDay == today()
                val tw = min(gfx.textWidth(challengeStr, 30f), 700f)
                gfx.glass(c, cx - tw / 2f - 50f, 240f, cx + tw / 2f + 50f, 310f, 35f, border = if (done) Gfx.GREEN else Gfx.GOLD)
                gfx.text(c, challengeStr, cx, 286f, 30f, if (done) Gfx.GREEN else Gfx.GOLD, maxW = 700f)
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
            gfx.text(c, L.t(L.TUTORIAL_1), cx, H * 0.3f + 65f, 42f, Color.WHITE, false, alpha = a, maxW = 780f)
            gfx.text(c, L.t(L.TUTORIAL_2), cx, H * 0.3f + 118f, 42f, Color.WHITE, false, alpha = a, maxW = 780f)
        }
    }

    private fun drawPause(c: Canvas) {
        dim(c, 200)
        gfx.text(c, L.t(L.PAUSE), W / 2f, H * 0.32f, 110f, Color.WHITE, maxW = 860f)
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
        gfx.text(c, if (runLevel > 0) L.f(L.LEVEL_LOST, runLevel) else L.t(if (challengeRun) L.DAILY_CHALLENGE else L.LOST_IN_SPACE), cx, top + 95f, 44f, Gfx.RED, alpha = a, maxW = 780f)
        if (runLevel > 0) {
            gfx.text(c, progStr, cx, top + 280f, 150f, Color.WHITE, alpha = a)
            gfx.text(c, L.t(L.PLANETS_DONE), cx, top + 345f, 38f, Gfx.DIM, false, alpha = a, maxW = 780f)
        } else {
            if (world.score != scoreVal) { scoreVal = world.score; scoreStr = world.score.toString() }
            gfx.text(c, scoreStr, cx, top + 290f, 190f, Color.WHITE, alpha = a)
            if (newRecord) gfx.text(c, L.t(L.NEW_RECORD), cx, top + 360f, 44f, Gfx.GOLD, alpha = a)
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
            gfx.text(c, Levels.bodyName(body), cx, top + 310f, 54f, Gfx.GREEN, alpha = a, maxW = 780f)
            gfx.text(c, L.t(L.REACHED), cx, top + 365f, 34f, Color.WHITE, alpha = a, maxW = 780f)
        } else {
            gfx.text(c, L.t(L.COMPLETE), cx, top + 200f, 110f, Gfx.GREEN, alpha = a, maxW = 780f)
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
        crit(c, ly, true, L.t(L.CRIT_DONE), a)
        crit(c, ly + 62f, runRevives == 0, L.t(L.CRIT_NO_REVIVE), a)
        crit(c, ly + 124f, perfOk, perfStr, a)
        val tw = gfx.textWidth(earnedStr, 54f)
        gfx.starShape(c, cx - tw / 2f - 40f, top + 780f, 24f, time * 40f, Gfx.GOLD, a)
        gfx.text(c, earnedStr, cx + 15f, top + 799f, 54f, Gfx.GOLD, alpha = a)
        if (!firstClear) gfx.text(c, L.t(if (crownsNew > 0) L.NEW_CROWN else L.REWARD_ONCE), cx, top + 845f, 29f, Gfx.DIM, false, alpha = a, maxW = 780f)
        if (screenTime > 0.6f) {
            bNext.visible = runLevel < Levels.COUNT
            for (b in doneBtns) drawBtn(c, b)
        }
        if (runLevel >= Levels.COUNT) gfx.text(c, L.t(L.JOURNEY_DONE), cx, H * 0.64f, 40f, Gfx.ACCENT, alpha = a, maxW = 860f)
    }

    private fun crit(c: Canvas, y: Float, ok: Boolean, label: String, a: Int) {
        gfx.circle(c, 190f, y - 12f, 22f, if (ok) Gfx.GREEN else Gfx.PANEL2, a)
        if (ok) gfx.icon(c, Gfx.I_CHECK, 190f, y - 12f, 14f, Gfx.BG, a)
        gfx.text(c, label, 235f, y, 36f, if (ok) Color.WHITE else Gfx.DIM, false, Paint.Align.LEFT, a, maxW = 640f)
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
            val name = Levels.bodyName(body)
            val tw = min(gfx.textWidth(name, 38f), 760f)
            gfx.glass(c, W / 2f - tw / 2f - 36f, by + r + 30f, W / 2f + tw / 2f + 36f, by + r + 100f, 35f)
            gfx.text(c, name, W / 2f, by + r + 79f, 38f, if (reached) Color.WHITE else Gfx.DIM, maxW = 760f)
            if (s == 15 || s == 20) {
                // Übergang in die Spiegelwelt
                val dy = by - r - 150f
                gfx.glass(c, 120f, dy - 70f, 880f, dy + 60f, 50f, border = if (s == 15) 0xFFF15BB5.toInt() else 0xFF2EE5C8.toInt())
                gfx.text(c, if (s == 15) dimTitle else dimTitle3, W / 2f, dy - 12f, 42f, if (s == 15) 0xFFF15BB5.toInt() else 0xFF2EE5C8.toInt(), maxW = 720f)
                gfx.text(c, L.t(L.DIM_NAMES[if (s == 15) 1 else 2]), W / 2f, dy + 34f, 32f, Color.WHITE, false, maxW = 720f)
            }
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
        gfx.text(c, L.t(L.JOURNEY), W / 2f, 132f, 54f, Color.WHITE, maxW = 440f)
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
        gfx.text(c, L.t(L.SHOP), W / 2f, 132f, 54f, Color.WHITE, maxW = 440f)
        starPill(c, save.stars)
        gfx.glass(c, 50f, 215f, 950f, 307f, 46f)
        val tb = bTab[shopTab]
        val sh = tb.shader
        if (sh != null) gfx.gradRoundRect(c, tb.x + 6f, tb.y + 6f, tb.x + tb.w - 6f, tb.y + tb.h - 6f, 40f, sh)
        for (i in 0 until 3) gfx.text(c, bTab[i].label, bTab[i].x + 150f, 274f, 34f, if (i == shopTab) Color.WHITE else Gfx.DIM, maxW = 260f)
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
            gfx.text(c, Skins.skins[i].name, cx, y + 262f, 34f, Color.WHITE, maxW = cardW - 24f)
        } else {
            for (k in 0 until 24) {
                prevX[k] = cx - 105f + k * (210f / 23f)
                prevY[k] = py + sin(k * 0.35f + time * 4f) * 28f
            }
            gfx.drawTrail(c, i, prevX, prevY, 0, 24, 24, gfx.skinColor(save.skin, time), time)
            gfx.drawSkin(c, save.skin, prevX[23], prevY[23], 1f, time, true)
            gfx.text(c, Skins.trails[i].name, cx, y + 262f, 34f, Color.WHITE, maxW = cardW - 24f)
        }
        if (locked) {
            gfx.roundRect(c, x + 3f, y + 3f, x + cardW - 3f, y + 215f, 34f, Gfx.BG, 150)
            gfx.circle(c, cx, py, 34f, Gfx.PANEL, 230)
            gfx.icon(c, Gfx.I_LOCK, cx, py, 18f, Gfx.DIM)
        }
        when {
            selected -> {
                gfx.icon(c, Gfx.I_CHECK, cx - 58f, y + 319f, 14f, Gfx.GREEN)
                gfx.text(c, L.t(L.ACTIVE), cx + 12f, y + 330f, 32f, Gfx.GREEN, maxW = 200f)
            }
            owned -> gfx.text(c, L.t(L.SELECT), cx, y + 330f, 32f, Gfx.DIM, false, maxW = cardW - 30f)
            locked -> gfx.text(c, if (skinsTab) skinReq[i] else trailReq[i], cx, y + 330f, 27f, Gfx.DIM, false, maxW = cardW - 24f)
            else -> {
                val pv = if (skinsTab) Skins.skins[i].price else Skins.trails[i].price
                if (pv == 0) gfx.text(c, L.t(L.FREE), cx, y + 330f, 32f, Gfx.GREEN, maxW = cardW - 30f)
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
        extraCard(c, gridTop, Gfx.I_PLUS, Gfx.GREEN, L.t(L.X_REVIVE),
            L.t(L.X_REVIVE_DESC), save.revives, Skins.REVIVE_MAX, Skins.REVIVE_PRICE, bBuyRevive)
        extraCard(c, gridTop + 560f, Gfx.I_SHIELD, Gfx.SHIELD, L.t(L.X_SHIELD),
            L.t(L.X_SHIELD_DESC), save.shieldStarts, Skins.SHIELD_MAX, Skins.SHIELD_PRICE, bBuyShield)
    }

    private fun extraCard(c: Canvas, y: Float, icon: Int, col: Int, title: String, desc: String,
                          have: Int, maxN: Int, price: Int, b: Btn) {
        val cx = W / 2f
        gfx.glass(c, 50f, y, 950f, y + 530f, 44f)
        if (!save.lowGfx) gfx.glow(c, if (icon == Gfx.I_SHIELD) gfx.shieldGlow else gfx.goalGlow, cx, y + 115f, 140f)
        gfx.circle(c, cx, y + 115f, 62f, col)
        gfx.icon(c, icon, cx, y + 115f, 32f, Color.WHITE)
        gfx.text(c, title, cx, y + 240f, 50f, Color.WHITE, maxW = 820f)
        gfx.text(c, desc, cx, y + 295f, 34f, Gfx.DIM, false, maxW = 820f)
        for (k in 0 until maxN) {
            val dx = cx + (k - (maxN - 1) / 2f) * 62f
            if (k < have) gfx.circle(c, dx, y + 345f, 19f, col) else gfx.ring(c, dx, y + 345f, 19f, Gfx.DIM, 3.5f)
        }
        if (b.y != y + 390f) place(b, cx - 250f, y + 390f, 500f, 115f)
        b.label = if (have >= maxN) L.t(L.FULL) else L.f(L.BUY_PRICE, price)
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
            gfx.text(c, achTitleStr[i], 205f, y + 62f, 40f, Color.WHITE, align = Paint.Align.LEFT, maxW = 570f)
            gfx.text(c, achDescStr[i], 205f, y + 105f, 30f, Gfx.DIM, false, Paint.Align.LEFT, maxW = 570f)
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
        gfx.text(c, L.t(L.ACHIEVEMENTS), W / 2f, 132f, 54f, Color.WHITE, maxW = 440f)
        starPill(c, save.stars)
    }

    // ---------------------------------------------------------------- Einstellungen

    private val settingIcons = intArrayOf(Gfx.I_SOUND, Gfx.I_MUSIC, Gfx.I_VIBRATE, Gfx.I_SHAKE, Gfx.I_SPEED, Gfx.I_EYE, Gfx.I_GLOBE)

    private fun drawSettings(c: Canvas) {
        header(c, L.t(L.OPTIONS))
        for (i in 0 until 7) {
            val b = bRow[i]
            gfx.glass(c, b.x, b.y, b.x + b.w, b.y + b.h, 36f, if (b === pressed) 255 else 235)
            gfx.circle(c, b.x + 70f, b.y + b.h / 2f, 38f, Gfx.PANEL2)
            gfx.icon(c, settingIcons[i], b.x + 70f, b.y + b.h / 2f, 20f, Gfx.ACCENT)
            gfx.text(c, L.t(L.SETTINGS[i]), b.x + 130f, b.y + b.h / 2f + 14f, 40f, Color.WHITE, false, Paint.Align.LEFT, maxW = if (i >= 5) 440f else 560f)
            if (i >= 5) {
                val v = if (i == 6) L.LANG_NAMES[L.cur] else L.t(if (save.lowGfx) L.GFX_LOW else L.GFX_HIGH)
                gfx.roundRect(c, b.x + b.w - 280f, b.y + 24f, b.x + b.w - 30f, b.y + b.h - 24f, 33f, Gfx.PANEL2)
                gfx.text(c, v, b.x + b.w - 155f, b.y + b.h / 2f + 12f, 34f, Gfx.ACCENT, maxW = 220f)
            } else {
                val on = when (i) { 0 -> save.sound; 1 -> save.music; 2 -> save.vibration; 3 -> save.shake; else -> save.showFps }
                toggle(c, b.x + b.w - 175f, b.y + b.h / 2f, on)
            }
        }
        drawBtn(c, bReset)
        gfx.text(c, L.t(L.GFX_HINT), W / 2f, bReset.y + bReset.h + 70f, 29f, Gfx.DIM, false, maxW = 900f)
        gfx.text(c, "Orbit v$VERSION_NAME · Engelcode", W / 2f, H - 30f, 26f, Gfx.DIM, false, alpha = 150)
    }

    private fun toggle(c: Canvas, x: Float, cy: Float, on: Boolean) {
        gfx.roundRect(c, x, cy - 36f, x + 140f, cy + 36f, 36f, if (on) Gfx.ACCENT else Gfx.PANEL2)
        gfx.circle(c, if (on) x + 104f else x + 36f, cy, 28f, Color.WHITE)
    }

    // ---------------------------------------------------------------- Statistik

    private fun drawStats(c: Canvas) {
        header(c, L.t(L.STATS))
        for (i in statLabels.indices) {
            val y = 235f + i * 108f
            gfx.glass(c, 40f, y, 960f, y + 92f, 30f)
            gfx.text(c, statLabels[i], 80f, y + 60f, 36f, Gfx.DIM, false, Paint.Align.LEFT, maxW = 560f)
            gfx.text(c, statValues[i], 920f, y + 62f, 42f, Color.WHITE, align = Paint.Align.RIGHT)
        }
    }

    // ---------------------------------------------------------------- Missionen

    private val missionIcons = intArrayOf(-1, Gfx.I_CHECK, Gfx.I_TARGET, Gfx.I_CROWN, Gfx.I_INFINITY, Gfx.I_SHIELD, Gfx.I_WHEEL)

    private fun drawMissions(c: Canvas) {
        header(c, L.t(L.MISSIONS))
        starPill(c, save.stars)
        var allDone = true
        for (k in 0 until 3) {
            val top = 260f + k * 300f
            val g = Missions.goal[k]
            val p = save.missionProg[k]
            val done = p >= g
            val claimed = save.missionClaimed and (1 shl k) != 0
            if (!claimed) allDone = false
            gfx.glass(c, 40f, top, 960f, top + 270f, 40f, border = if (done && !claimed) Gfx.GOLD else Color.WHITE)
            gfx.circle(c, 135f, top + 105f, 58f, if (done) Gfx.GOLD else Gfx.PANEL2)
            val ic = missionIcons[Missions.type[k]]
            if (ic < 0) gfx.starShape(c, 135f, top + 105f, 30f, 0f, if (done) Gfx.BG else Gfx.GOLD)
            else gfx.icon(c, ic, 135f, top + 105f, 28f, if (done) Gfx.BG else Color.WHITE)
            gfx.text(c, missionDescStr[k], 225f, top + 95f, 40f, Color.WHITE, align = Paint.Align.LEFT, maxW = 700f)
            val bw = 470f
            gfx.roundRect(c, 225f, top + 135f, 225f + bw, top + 155f, 10f, Gfx.PANEL2)
            gfx.roundRect(c, 225f, top + 135f, 225f + max(20f, bw * p / g), top + 155f, 10f, if (done) Gfx.GOLD else Gfx.ACCENT)
            gfx.text(c, missionProgStr[k], 715f, top + 155f, 30f, Gfx.DIM, false, Paint.Align.LEFT)
            when {
                claimed -> {
                    gfx.circle(c, 500f, top + 218f, 30f, Gfx.GREEN)
                    gfx.icon(c, Gfx.I_CHECK, 500f, top + 218f, 16f, Gfx.BG)
                }
                done -> {
                    gfx.roundRect(c, 300f, top + 180f, 700f, top + 252f, 36f, Gfx.GOLD)
                    gfx.text(c, L.t(L.CLAIM) + "  +" + Missions.REWARD[k], 500f, top + 228f, 34f, Gfx.BG, maxW = 360f)
                }
                else -> {
                    gfx.starShape(c, 470f, top + 214f, 16f, 0f, Gfx.GOLD)
                    gfx.text(c, "+" + Missions.REWARD[k], 500f, top + 228f, 34f, Gfx.GOLD, align = Paint.Align.LEFT)
                }
            }
        }
        // Zeit bis zu neuen Missionen
        val now = System.currentTimeMillis()
        val off = TimeZone.getDefault().getOffset(now)
        val minsLeft = (((today() + 1) * 86_400_000L - (now + off)) / 60000L).toInt()
        if (minsLeft != missionTimerMin) {
            missionTimerMin = minsLeft
            missionTimerStr = L.f(L.MISSION_NEW_IN, minsLeft / 60, minsLeft % 60)
        }
        val y = 260f + 3 * 300f + 40f
        if (allDone) gfx.text(c, L.t(L.MISSION_DONE_ALL), W / 2f, y, 36f, Gfx.GREEN, maxW = 880f)
        gfx.text(c, missionTimerStr, W / 2f, y + 60f, 32f, Gfx.DIM, false, maxW = 880f)
    }

    // ---------------------------------------------------------------- Glücksrad

    private fun drawWheel(c: Canvas) {
        header(c, L.t(L.WHEEL))
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

        bSpin.label = if (freeSpin()) L.t(L.FREE_SPIN) else L.f(L.SPIN_PRICE, SPIN_PRICE)
        bSpin.visible = !spinning
        if (!spinning) {
            drawBtn(c, bSpin)
            if (!freeSpin()) {
                val tw = gfx.textWidth(bSpin.label, bSpin.h * 0.3f)
                gfx.starShape(c, cx + tw / 2f + 28f, bSpin.y + bSpin.h / 2f, 18f, 0f, Gfx.BG)
            }
        }
        gfx.text(c, L.t(if (freeSpin()) L.SPIN_FREE_HINT else L.SPIN_NEXT_HINT), cx, bSpin.y + bSpin.h + 70f, 32f, Gfx.DIM, false, maxW = 880f)
    }

    // ---------------------------------------------------------------- Dialog & Toast

    private fun drawDialog(c: Canvas) {
        dim(c, 190)
        gfx.glass(c, 70f, dlgTop, 930f, dlgTop + dlgH, 48f, border = Gfx.ACCENT)
        gfx.text(c, dlgTitle, W / 2f, dlgTop + 110f, 52f, Color.WHITE, maxW = 780f)
        val news = dialog == D_NEWS
        for (i in dlgLines.indices) {
            val ly = dlgTop + 190f + i * 62f
            if (news) gfx.text(c, dlgLines[i], 120f, ly, 35f, Color.WHITE, false, Paint.Align.LEFT, maxW = 770f)
            else gfx.text(c, dlgLines[i], W / 2f, ly, 42f,
                if (i == dlgLines.size - 1 && dialog == D_DAILY) Gfx.GOLD else Color.WHITE, dialog == D_DAILY, maxW = 780f)
        }
        drawBtn(c, bDlgA)
        if (dlgTwo) drawBtn(c, bDlgB)
    }

    private fun drawToast(c: Canvas) {
        val a = (min(1f, toastT / 0.4f) * 255f).toInt()
        val tw = min(gfx.textWidth(toast, 38f, false), 860f) + 90f
        val y = H * 0.84f
        gfx.glass(c, W / 2f - tw / 2f, y - 62f, W / 2f + tw / 2f, y + 32f, 47f, a)
        gfx.text(c, toast, W / 2f, y, 38f, Color.WHITE, false, alpha = a, maxW = 860f)
    }
}
