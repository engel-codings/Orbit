package de.engelcode.orbit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * ORBIT – Bildschirme, Menüs, Shop, Level-Karte, Einstellungen.
 * Die eigentliche Spielwelt steckt in World.kt.
 */
class Game(context: Context) : World.Listener {

    companion object {
        const val VERSION_CODE = 2
        const val VERSION_NAME = "0.2.0"
        const val W = 1000f

        // Knopf-Stile
        private const val S_PRIMARY = 0
        private const val S_SECONDARY = 1
        private const val S_DANGER = 2
        private const val S_PAUSE = 3
        private const val S_BACK = 4
        private const val S_TAB = 5
        private const val S_ROW = 6
        private const val S_GREEN = 7

        // Dialoge
        private const val D_NONE = 0
        private const val D_BUY = 1
        private const val D_RESET = 2
        private const val D_DAILY = 3
        private const val D_NEWS = 4

        private val NEWS = arrayOf(
            "• Level-Reise: Erde bis Saturn (50 Level)",
            "• Shop mit 12 Skins und 7 Spuren",
            "• Wiederbelebungen (bis zu 3)",
            "• Sound, Musik und Vibration",
            "• Einstellungen, Statistik, Pause",
            "• Täglicher Bonus und Perfekt-Serien",
            "• Neu: Eis, Gasriesen, Schwarze Löcher"
        )
    }

    private enum class Screen { MENU, MAP, PLAYING, PAUSED, GAMEOVER, LEVELDONE, SHOP, SETTINGS, STATS }

    private class Btn(val id: Int, var label: String, val style: Int) {
        var x = 0f; var y = 0f; var w = 0f; var h = 0f
        var visible = true
        fun set(x: Float, y: Float, w: Float, h: Float) { this.x = x; this.y = y; this.w = w; this.h = h }
        fun hit(px: Float, py: Float) = visible && px >= x && px <= x + w && py >= y && py <= y + h
    }

    private val save = Save(context)
    private val sound = Sound(context, save)
    private val gfx = Gfx()
    private val world = World(gfx)

    private var scale = 1f
    private var H = 1800f
    private var ready = false

    private var screen = Screen.MENU
    private var screenTime = 0f
    private var time = 0f

    // ---- Knöpfe
    private val bLevels = Btn(1, "LEVEL", S_PRIMARY)
    private val bEndless = Btn(2, "ENDLOS", S_SECONDARY)
    private val bShop = Btn(3, "SHOP", S_SECONDARY)
    private val bStats = Btn(4, "STATISTIK", S_SECONDARY)
    private val bSettings = Btn(5, "EINSTELLUNGEN", S_SECONDARY)
    private val bBack = Btn(6, "", S_BACK)
    private val bPause = Btn(7, "", S_PAUSE)
    private val bResume = Btn(8, "WEITER", S_PRIMARY)
    private val bRestart = Btn(9, "NEUSTART", S_SECONDARY)
    private val bQuit = Btn(10, "MENÜ", S_SECONDARY)
    private val bRevive = Btn(11, "WIEDERBELEBEN", S_GREEN)
    private val bAgain = Btn(12, "NOCHMAL", S_PRIMARY)
    private val bLeave = Btn(13, "MENÜ", S_SECONDARY)
    private val bNext = Btn(14, "NÄCHSTES LEVEL", S_PRIMARY)
    private val bToMap = Btn(15, "ZUR KARTE", S_SECONDARY)
    private val bTab0 = Btn(16, "SKINS", S_TAB)
    private val bTab1 = Btn(17, "SPUREN", S_TAB)
    private val bTab2 = Btn(18, "EXTRAS", S_TAB)
    private val bRow = Array(5) { Btn(20 + it, "", S_ROW) }
    private val bReset = Btn(30, "FORTSCHRITT ZURÜCKSETZEN", S_DANGER)
    private val bDlgA = Btn(31, "OK", S_PRIMARY)
    private val bDlgB = Btn(32, "ABBRECHEN", S_SECONDARY)
    private val bBuyRevive = Btn(33, "KAUFEN", S_PRIMARY)

    private val menuBtns = arrayOf(bLevels, bEndless, bShop, bStats, bSettings)
    private val playBtns = arrayOf(bPause)
    private val pauseBtns = arrayOf(bResume, bRestart, bQuit)
    private val overBtns = arrayOf(bRevive, bAgain, bLeave)
    private val doneBtns = arrayOf(bNext, bToMap)
    private val shopBtns = arrayOf(bBack, bTab0, bTab1, bTab2, bBuyRevive)
    private val setBtns = arrayOf(bBack, bRow[0], bRow[1], bRow[2], bRow[3], bRow[4], bReset)
    private val backBtns = arrayOf(bBack)
    private val dlgBtns = arrayOf(bDlgA, bDlgB)
    private val noBtns = arrayOf<Btn>()

    // ---- Eingabe
    private var pressed: Btn? = null
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var scrollStart = 0f
    private var shopScroll = 0f
    private var mapScroll = 0f

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
    private val prevX = FloatArray(24)
    private val prevY = FloatArray(24)

    // ---- Lauf
    private var runLevel = 0
    private var runTime = 0f
    private var cStars = 0; private var cBonus = 0; private var cCaptures = 0; private var cPerfects = 0
    private var runCounted = false
    private var runEarned = 0
    private var newRecord = false
    private var winTimer = 0f
    private var levelReward = 0
    private var firstClear = false

    // ---- Texte (Cache)
    private var fpsAcc = 0f; private var fpsFrames = 0; private var fpsShown = 60
    private var fpsStr = "60 FPS"; private var fpsVal = -1
    private var scoreStr = "0"; private var scoreVal = -1
    private var starsStr = "0"; private var starsVal = -1
    private var progStr = ""; private var progVal = -1
    private var levelStr = ""
    private var bestStr = ""
    private var earnedStr = ""
    private var reviveStr = ""
    private var statLabels = arrayOf<String>()
    private var statValues = arrayOf<String>()
    private val levelNum = Array(Levels.COUNT + 1) { it.toString() }
    private var musicTimer = 0f

    init {
        world.listener = this
        world.reduced = save.lowGfx
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
        }
        layout()
    }

    fun onPause() {
        if (screen == Screen.PLAYING && !world.won) go(Screen.PAUSED)
        sound.pauseAll()
    }

    fun onResume() {
        sound.resumeAll()
    }

    fun release() = sound.release()

    /** Zurück-Taste. false = App verlassen. */
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
            Screen.MAP, Screen.SHOP, Screen.SETTINGS, Screen.STATS -> go(Screen.MENU)
        }
        sound.play(Sound.CLICK)
        return true
    }

    // ===================================================================== Layout

    private fun layout() {
        val cx = W / 2f
        // Menü
        val my = H * 0.38f
        bLevels.set(cx - 300f, my, 600f, 150f)
        bEndless.set(cx - 300f, my + 175f, 600f, 125f)
        bShop.set(cx - 300f, my + 325f, 290f, 120f)
        bStats.set(cx + 10f, my + 325f, 290f, 120f)
        bSettings.set(cx - 300f, my + 470f, 600f, 120f)
        // Allgemein
        bBack.set(40f, 50f, 120f, 120f)
        bPause.set(40f, 50f, 120f, 120f)
        // Pause
        val py = H * 0.42f
        bResume.set(cx - 280f, py, 560f, 140f)
        bRestart.set(cx - 280f, py + 170f, 560f, 125f)
        bQuit.set(cx - 280f, py + 320f, 560f, 125f)
        // Game Over
        val gy = H * 0.58f
        bRevive.set(cx - 280f, gy, 560f, 135f)
        bAgain.set(cx - 280f, gy + 160f, 560f, 135f)
        bLeave.set(cx - 280f, gy + 320f, 560f, 120f)
        // Level geschafft
        bNext.set(cx - 300f, H * 0.62f, 600f, 140f)
        bToMap.set(cx - 300f, H * 0.62f + 170f, 600f, 125f)
        // Shop
        bTab0.set(40f, 210f, 296f, 96f)
        bTab1.set(352f, 210f, 296f, 96f)
        bTab2.set(664f, 210f, 296f, 96f)
        bBuyRevive.set(cx - 260f, 820f, 520f, 130f)
        // Einstellungen
        for (i in 0 until 5) bRow[i].set(40f, 260f + i * 140f, 920f, 115f)
        bReset.set(80f, 260f + 5 * 140f + 40f, 840f, 115f)
    }

    // ===================================================================== Ablauf

    private fun go(s: Screen) {
        screen = s
        screenTime = 0f
        pressed = null
        dragging = false
        when (s) {
            Screen.STATS -> buildStats()
            Screen.MAP -> {
                val cur = min(save.levelDone + 1, Levels.COUNT)
                mapScroll = clampMap(mapU(cur) - H / 2f + 450f)
            }
            Screen.SHOP -> shopScroll = 0f
            else -> {}
        }
    }

    private fun startRun(level: Int) {
        world.reduced = save.lowGfx
        world.reset(level)
        runLevel = level
        runTime = 0f
        cStars = 0; cBonus = 0; cCaptures = 0; cPerfects = 0
        runCounted = false; runEarned = 0; newRecord = false
        winTimer = 0f
        levelStr = if (level > 0) "LEVEL $level" else ""
        progVal = -1
        go(Screen.PLAYING)
    }

    /** Lauf beenden und zurück (Karte oder Menü). */
    private fun leaveRun() {
        world.reset(0)
        go(if (runLevel > 0) Screen.MAP else Screen.MENU)
    }

    /** Überträgt Sterne und Statistik des Laufs in den Speicherstand (nur die Änderung). */
    private fun commitRun() {
        val bonus = if (runLevel == 0) world.score / 5 else 0
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

    override fun onLaunch() {
        sound.play(Sound.LAUNCH, 1f, 0.6f)
    }

    override fun onCapture(perfect: Boolean, streak: Int) {
        sound.play(Sound.CAPTURE, 1f + 0.1f * min(streak, 6))
        if (perfect) sound.play(Sound.PERFECT, 1f, 0.8f)
        sound.vibrate(15)
    }

    override fun onStar() {
        sound.play(Sound.STAR)
    }

    override fun onDeath() {
        sound.play(Sound.DEATH)
        sound.vibrate(160)
        commitRun()
        earnedStr = "+$runEarned"
        bestStr = "Rekord: ${save.best}"
        reviveStr = "WIEDERBELEBEN (${save.revives})"
        bRevive.label = reviveStr
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
            save.flush()
        }
        runEarned += levelReward
        earnedStr = "+$runEarned"
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
                if (winTimer > 0f) {
                    winTimer -= dt
                    if (winTimer <= 0f) go(Screen.LEVELDONE)
                }
            }
            else -> world.update(dt, false)
        }

        if (screen == Screen.MENU && dialog == D_NONE) checkPopups()
    }

    private fun checkPopups() {
        if (!newsChecked) {
            newsChecked = true
            if (save.lastVersion < VERSION_CODE) {
                val fresh = save.games == 0 && save.best == 0
                save.lastVersion = VERSION_CODE
                save.flush()
                if (!fresh) {
                    openDialog(D_NEWS, "Neu in Version $VERSION_NAME", NEWS, "SUPER", null)
                    return
                }
            }
        }
        val today = today()
        if (today > save.lastDay && screenTime > 0.4f) {
            pendingDay = today
            pendingStreak = if (today == save.lastDay + 1) save.dayStreak + 1 else 1
            pendingBonus = 15 + 5 * min(pendingStreak - 1, 7)
            openDialog(
                D_DAILY, "Täglicher Bonus",
                arrayOf("Tag $pendingStreak in Folge", "+$pendingBonus Sterne"), "ABHOLEN", null
            )
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
                downX = x; downY = y; dragging = false
                pressed = null
                if (dialog != D_NONE) { pressed = hit(dlgBtns, x, y); return }
                if (screen == Screen.PLAYING) {
                    if (bPause.hit(x, y)) { pressed = bPause; return }
                    world.launch()
                    return
                }
                if ((screen == Screen.GAMEOVER || screen == Screen.LEVELDONE) && screenTime < 0.5f) return
                scrollStart = if (screen == Screen.MAP) mapScroll else shopScroll
                pressed = hit(activeButtons(), x, y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (dialog == D_NONE && screen == Screen.PLAYING) world.launch()
            }
            MotionEvent.ACTION_MOVE -> {
                if (dialog != D_NONE) return
                if (screen != Screen.SHOP && screen != Screen.MAP) return
                if (!dragging && abs(y - downY) > 22f) { dragging = true; pressed = null }
                if (dragging) {
                    if (screen == Screen.MAP) mapScroll = clampMap(scrollStart + (y - downY))
                    else shopScroll = clampShop(scrollStart - (y - downY))
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
                if (screen == Screen.SHOP) shopTap(x, y)
                else if (screen == Screen.MAP) mapTap(x, y)
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
        Screen.SHOP -> { bBuyRevive.visible = shopTab == 2; shopBtns }
        Screen.SETTINGS -> setBtns
        Screen.MAP, Screen.STATS -> backBtns
    }

    private fun onButton(id: Int) {
        sound.play(Sound.CLICK)
        when (id) {
            bLevels.id -> go(Screen.MAP)
            bEndless.id -> startRun(0)
            bShop.id -> go(Screen.SHOP)
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
            bTab0.id -> { shopTab = 0; shopScroll = 0f }
            bTab1.id -> { shopTab = 1; shopScroll = 0f }
            bTab2.id -> { shopTab = 2; shopScroll = 0f }
            bBuyRevive.id -> askBuy(2, 0)
            bRow[0].id -> { save.sound = !save.sound; save.flush() }
            bRow[1].id -> { save.music = !save.music; save.flush(); sound.syncMusic() }
            bRow[2].id -> { save.vibration = !save.vibration; save.flush(); sound.vibrate(40) }
            bRow[3].id -> { save.showFps = !save.showFps; save.flush() }
            bRow[4].id -> { save.lowGfx = !save.lowGfx; world.reduced = save.lowGfx; save.flush() }
            bReset.id -> openDialog(
                D_RESET, "Wirklich zurücksetzen?",
                arrayOf("Sterne, Skins, Level und Statistik", "werden gelöscht."), "LÖSCHEN", "ABBRECHEN"
            )
            bDlgA.id -> dialogAnswer(true)
            bDlgB.id -> dialogAnswer(false)
        }
    }

    private fun doRevive() {
        if (save.revives <= 0) return
        if (!world.revive()) return
        save.revives--
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
        dlgH = 150f + lines.size * 62f + 190f
        dlgTop = (H - dlgH) / 2f
        val by = dlgTop + dlgH - 165f
        if (dlgTwo) {
            bDlgA.set(110f, by, 375f, 120f)
            bDlgB.set(515f, by, 375f, 120f)
            bDlgB.visible = true
        } else {
            bDlgA.set(210f, by, 580f, 120f)
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
            D_RESET -> if (yes) {
                save.resetProgress()
                showToast("Fortschritt gelöscht")
            }
            D_DAILY -> claimDaily()
            else -> {}
        }
    }

    private fun claimDaily() {
        closeDialog()
        save.lastDay = pendingDay
        save.dayStreak = pendingStreak
        save.stars += pendingBonus
        save.flush()
        sound.play(Sound.BONUS)
        showToast("+$pendingBonus Sterne!")
    }

    private fun showToast(msg: String) { toast = msg; toastT = 2.2f }

    // ===================================================================== Shop

    private val cardW = (920f - 48f) / 3f
    private val cardH = 340f
    private val gridTop = 340f

    private fun shopCount() = when (shopTab) { 0 -> Skins.skins.size; 1 -> Skins.trails.size; else -> 0 }

    private fun clampShop(v: Float): Float {
        val rows = (shopCount() + 2) / 3
        val content = gridTop + rows * (cardH + 24f) + 60f
        return v.coerceIn(0f, max(0f, content - H))
    }

    private fun shopTap(x: Float, y: Float) {
        if (shopTab == 2 || y < gridTop - 10f) return
        val yy = y + shopScroll - gridTop
        val col = ((x - 40f) / (cardW + 24f)).toInt()
        val row = (yy / (cardH + 24f)).toInt()
        if (col !in 0..2 || yy < 0f) return
        val lx = x - 40f - col * (cardW + 24f)
        val ly = yy - row * (cardH + 24f)
        if (lx > cardW || ly > cardH) return
        val i = row * 3 + col
        if (i >= shopCount()) return
        if (shopTab == 0) {
            if (save.ownsSkin(i)) { save.skin = i; save.flush(); sound.play(Sound.CLICK) }
            else askBuy(0, i)
        } else {
            if (save.ownsTrail(i)) { save.trail = i; save.flush(); sound.play(Sound.CLICK) }
            else askBuy(1, i)
        }
    }

    private fun priceOf(tab: Int, i: Int) = when (tab) {
        0 -> Skins.skins[i].price
        1 -> Skins.trails[i].price
        else -> Skins.REVIVE_PRICE
    }

    private fun nameOf(tab: Int, i: Int) = when (tab) {
        0 -> Skins.skins[i].name
        1 -> "Spur „${Skins.trails[i].name}“"
        else -> "Wiederbelebung"
    }

    private fun askBuy(tab: Int, i: Int) {
        if (tab == 2 && save.revives >= Skins.REVIVE_MAX) {
            sound.play(Sound.ERROR); showToast("Maximal ${Skins.REVIVE_MAX} Wiederbelebungen"); return
        }
        val price = priceOf(tab, i)
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
            else -> save.revives = min(Skins.REVIVE_MAX, save.revives + 1)
        }
        save.flush()
        sound.play(Sound.BUY)
        sound.vibrate(30)
        showToast(if (tab == 2) "Wiederbelebung gekauft!" else "Freigeschaltet!")
    }

    // ===================================================================== Level-Karte

    /** Höhe eines Levels auf der Karte (von unten gezählt). */
    private fun mapU(level: Int) = (level - 1) * 170f + Levels.section(level) * 300f
    private fun bodyU(s: Int) = if (s == 0) -320f else mapU(s * 10) + 245f
    private fun mapX(level: Int) = W / 2f + sin(level * 0.9f) * 250f
    private fun mapScreenY(u: Float) = H - 450f - u + mapScroll

    private fun clampMap(v: Float): Float {
        val top = bodyU(5) + 260f
        return v.coerceIn(0f, max(0f, top + 750f - H))
    }

    private fun mapTap(x: Float, y: Float) {
        if (y < 200f) return
        for (i in 1..Levels.COUNT) {
            val nx = mapX(i); val ny = mapScreenY(mapU(i))
            val dx = x - nx; val dy = y - ny
            if (dx * dx + dy * dy < 75f * 75f) {
                if (i <= save.levelDone + 1) {
                    sound.play(Sound.CLICK)
                    startRun(i)
                } else {
                    sound.play(Sound.ERROR)
                    showToast("Schaffe zuerst Level ${save.levelDone + 1}")
                }
                return
            }
        }
    }

    // ===================================================================== Statistik

    private fun buildStats() {
        val mins = (save.playTime / 60f).toInt()
        statLabels = arrayOf(
            "Rekord (Endlos)", "Level geschafft", "Spiele", "Planeten erreicht",
            "Perfekte Landungen", "Beste Perfekt-Serie", "Sterne gesammelt", "Spielzeit"
        )
        statValues = arrayOf(
            save.best.toString(), "${save.levelDone} / ${Levels.COUNT}", save.games.toString(),
            save.planets.toString(), save.perfects.toString(), save.bestStreak.toString(),
            save.starsCollected.toString(), if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
        )
    }

    // ===================================================================== Zeichnen

    fun draw(c: Canvas) {
        c.drawColor(Gfx.BG)
        if (!ready) return
        c.save()
        c.scale(scale, scale)

        if (screen != Screen.MAP) world.draw(c, save.skin, save.trail)

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
        }

        if (save.showFps) {
            if (fpsShown != fpsVal) { fpsVal = fpsShown; fpsStr = "$fpsShown FPS" }
            gfx.text(c, fpsStr, 30f, H - 36f, 30f, Color.WHITE, false, Paint.Align.LEFT, 120)
        }
        if (dialog != D_NONE) drawDialog(c)
        if (toastT > 0f) drawToast(c)
        c.restore()
    }

    private fun dim(c: Canvas, alpha: Int) {
        gfx.fill.color = Gfx.BG; gfx.fill.alpha = alpha
        c.drawRect(0f, 0f, W, H, gfx.fill)
    }

    private fun drawStarsCounter(c: Canvas, value: Int) {
        if (value != starsVal) { starsVal = value; starsStr = value.toString() }
        val tw = gfx.textWidth(starsStr, 46f)
        val right = W - 40f
        gfx.starShape(c, right - tw - 34f, 108f, 22f, 0f, Gfx.GOLD)
        gfx.text(c, starsStr, right, 124f, 46f, Color.WHITE, align = Paint.Align.RIGHT)
    }

    private fun drawBtn(c: Canvas, b: Btn) {
        if (!b.visible) return
        val p = b === pressed
        val ins = if (p) 6f else 0f
        val l = b.x + ins; val t = b.y + ins; val r = b.x + b.w - ins; val bt = b.y + b.h - ins
        val cy = b.y + b.h / 2f
        when (b.style) {
            S_PRIMARY, S_GREEN -> {
                val col = if (b.style == S_GREEN) Gfx.GREEN else Gfx.ACCENT
                gfx.roundRect(c, l, t, r, bt, b.h / 2f, col, if (p) 200 else 255)
                gfx.text(c, b.label, b.x + b.w / 2f, cy + b.h * 0.13f, b.h * 0.34f, Gfx.BG)
            }
            S_SECONDARY -> {
                gfx.roundRect(c, l, t, r, bt, b.h / 2f, Gfx.PANEL2, if (p) 255 else 235)
                gfx.strokeRoundRect(c, l, t, r, bt, b.h / 2f, Color.WHITE, 2f, 40)
                gfx.text(c, b.label, b.x + b.w / 2f, cy + b.h * 0.12f, b.h * 0.3f, Color.WHITE)
            }
            S_DANGER -> {
                gfx.roundRect(c, l, t, r, bt, b.h / 2f, Gfx.RED, if (p) 170 else 210)
                gfx.text(c, b.label, b.x + b.w / 2f, cy + b.h * 0.11f, b.h * 0.28f, Color.WHITE)
            }
            S_PAUSE -> {
                gfx.roundRect(c, l, t, r, bt, 30f, Gfx.PANEL, 200)
                val cx = b.x + b.w / 2f
                gfx.roundRect(c, cx - 22f, cy - 28f, cx - 8f, cy + 28f, 5f, Color.WHITE)
                gfx.roundRect(c, cx + 8f, cy - 28f, cx + 22f, cy + 28f, 5f, Color.WHITE)
            }
            S_BACK -> {
                gfx.roundRect(c, l, t, r, bt, 30f, Gfx.PANEL2, 230)
                val cx = b.x + b.w / 2f
                gfx.line.color = Color.WHITE; gfx.line.alpha = 255; gfx.line.strokeWidth = 9f
                c.drawLine(cx + 10f, cy - 26f, cx - 16f, cy, gfx.line)
                c.drawLine(cx - 16f, cy, cx + 10f, cy + 26f, gfx.line)
            }
            S_TAB -> {
                val sel = (b === bTab0 && shopTab == 0) || (b === bTab1 && shopTab == 1) || (b === bTab2 && shopTab == 2)
                gfx.roundRect(c, l, t, r, bt, b.h / 2f, if (sel) Gfx.ACCENT else Gfx.PANEL2, 255)
                gfx.text(c, b.label, b.x + b.w / 2f, cy + 13f, 36f, if (sel) Gfx.BG else Color.WHITE)
            }
        }
    }

    private fun header(c: Canvas, title: String) {
        dim(c, 235)
        drawBtn(c, bBack)
        gfx.text(c, title, W / 2f, 135f, 62f, Color.WHITE)
    }

    // ---------------------------------------------------------------- Menü

    private fun drawMenu(c: Canvas) {
        drawStarsCounter(c, save.stars)
        val ty = H * 0.17f
        gfx.text(c, "ORBIT", W / 2f, ty, 190f, Color.WHITE)
        gfx.text(c, "P L A N E T   J U M P", W / 2f, ty + 75f, 38f, Gfx.ACCENT)
        if (save.best > 0 || save.levelDone > 0) {
            gfx.text(c, "Level ${save.levelDone} / ${Levels.COUNT}   ·   Rekord ${save.best}", W / 2f, ty + 150f, 36f, Gfx.DIM, false)
        }
        for (b in menuBtns) drawBtn(c, b)
        gfx.text(c, "v$VERSION_NAME · Engelcode", W / 2f, H - 40f, 28f, Gfx.DIM, false, alpha = 160)
    }

    // ---------------------------------------------------------------- Spiel-HUD

    private fun drawHud(c: Canvas) {
        drawBtn(c, bPause)
        drawStarsCounter(c, save.stars + world.runStars - cStars)
        if (runLevel > 0) {
            val prog = min(world.progress, world.target)
            if (prog != progVal) { progVal = prog; progStr = "$prog / ${world.target}" }
            gfx.text(c, levelStr, W / 2f, 92f, 34f, Gfx.DIM)
            val bw = 420f; val bx = W / 2f - bw / 2f
            gfx.roundRect(c, bx, 115f, bx + bw, 133f, 9f, Gfx.PANEL2, 230)
            gfx.roundRect(c, bx, 115f, bx + bw * prog / max(1, world.target), 133f, 9f, Gfx.GREEN)
            gfx.text(c, progStr, W / 2f, 215f, 72f, Color.WHITE, alpha = 230)
        } else {
            if (world.score != scoreVal) { scoreVal = world.score; scoreStr = world.score.toString() }
            gfx.text(c, scoreStr, W / 2f, 200f, 120f, Color.WHITE, alpha = 230)
        }
        if (save.games == 0 && world.captures == 0 && world.orbiting && runLevel <= 1) {
            val a = (170 + 85 * sin(time * 3f)).toInt().coerceIn(0, 255)
            gfx.text(c, "Tippe, wenn die Punkte", W / 2f, H * 0.33f, 44f, Color.WHITE, false, alpha = a)
            gfx.text(c, "zum nächsten Planeten zeigen", W / 2f, H * 0.33f + 60f, 44f, Color.WHITE, false, alpha = a)
        }
    }

    private fun drawPause(c: Canvas) {
        dim(c, 200)
        gfx.text(c, "Pause", W / 2f, H * 0.32f, 110f, Color.WHITE)
        for (b in pauseBtns) drawBtn(c, b)
    }

    // ---------------------------------------------------------------- Game Over / Level geschafft

    private fun drawGameOver(c: Canvas) {
        val f = min(1f, screenTime / 0.35f)
        dim(c, (190 * f).toInt())
        val a = (255 * f).toInt()
        val y = H * 0.2f
        gfx.text(c, if (runLevel > 0) "Level $runLevel verloren" else "Verloren im All", W / 2f, y, 52f, Color.WHITE, false, alpha = a)
        if (runLevel > 0) {
            gfx.text(c, progStr, W / 2f, y + 200f, 150f, Color.WHITE, alpha = a)
            gfx.text(c, "Planeten geschafft", W / 2f, y + 270f, 40f, Gfx.DIM, false, alpha = a)
        } else {
            if (world.score != scoreVal) { scoreVal = world.score; scoreStr = world.score.toString() }
            gfx.text(c, scoreStr, W / 2f, y + 210f, 200f, Color.WHITE, alpha = a)
            if (newRecord) gfx.text(c, "Neuer Rekord!", W / 2f, y + 290f, 46f, Gfx.GOLD, alpha = a)
            else gfx.text(c, bestStr, W / 2f, y + 290f, 42f, Gfx.DIM, false, alpha = a)
        }
        // verdiente Sterne
        val tw = gfx.textWidth(earnedStr, 56f)
        gfx.starShape(c, W / 2f - tw / 2f - 30f, y + 368f, 24f, 0f, Gfx.GOLD, a)
        gfx.text(c, earnedStr, W / 2f + 15f, y + 388f, 56f, Gfx.GOLD, alpha = a)

        if (screenTime > 0.5f) {
            bRevive.visible = save.revives > 0
            for (b in overBtns) drawBtn(c, b)
        }
    }

    private fun drawLevelDone(c: Canvas) {
        val f = min(1f, screenTime / 0.35f)
        dim(c, (190 * f).toInt())
        val a = (255 * f).toInt()
        val y = H * 0.2f
        val body = Levels.goalBody(runLevel)
        if (body != 0) {
            gfx.drawBody(c, body, W / 2f, y + 60f, 110f, time, save.lowGfx)
            gfx.text(c, "Du hast den ${Levels.bodyNames[body]} erreicht!", W / 2f, y + 260f, 42f, Gfx.GREEN, alpha = a)
        } else {
            gfx.text(c, "GESCHAFFT!", W / 2f, y + 120f, 120f, Gfx.GREEN, alpha = a)
        }
        gfx.text(c, levelStr, W / 2f, y + 340f, 50f, Color.WHITE, alpha = a)
        val tw = gfx.textWidth(earnedStr, 60f)
        gfx.starShape(c, W / 2f - tw / 2f - 32f, y + 418f, 26f, time * 40f, Gfx.GOLD, a)
        gfx.text(c, earnedStr, W / 2f + 15f, y + 440f, 60f, Gfx.GOLD, alpha = a)
        if (!firstClear) gfx.text(c, "(Belohnung nur beim ersten Mal)", W / 2f, y + 500f, 32f, Gfx.DIM, false, alpha = a)
        if (runLevel >= Levels.COUNT) {
            gfx.text(c, "Weitere Level kommen bald!", W / 2f, H * 0.58f, 40f, Gfx.ACCENT, false, alpha = a)
        }
        if (screenTime > 0.5f) {
            bNext.visible = runLevel < Levels.COUNT
            for (b in doneBtns) drawBtn(c, b)
        }
    }

    // ---------------------------------------------------------------- Level-Karte

    private fun drawMap(c: Canvas) {
        // Sternenhimmel bleibt von der Welt (ohne Spieler) – einfacher Hintergrund
        c.drawColor(Gfx.BG)
        val cur = min(save.levelDone + 1, Levels.COUNT)

        // Verbindungslinien
        for (i in 1 until Levels.COUNT) {
            val y1 = mapScreenY(mapU(i)); val y2 = mapScreenY(mapU(i + 1))
            if (max(y1, y2) < 150f || min(y1, y2) > H + 80f) continue
            dottedLine(c, mapX(i), y1, mapX(i + 1), y2, if (i < cur) Gfx.ACCENT else Gfx.DIM, if (i < cur) 200 else 90)
        }
        // Himmelskörper
        for (s in 0..5) {
            val by = mapScreenY(bodyU(s))
            if (by < -300f || by > H + 300f) continue
            val body = s + 1
            val r = if (s == 0) 120f else 100f
            gfx.drawBody(c, body, W / 2f, by, r, time, save.lowGfx)
            val reached = s == 0 || save.levelDone >= s * 10
            gfx.text(c, Levels.bodyNames[body], W / 2f, by + r + 64f, 44f, if (reached) Color.WHITE else Gfx.DIM)
        }
        // Level-Punkte
        for (i in 1..Levels.COUNT) {
            val ny = mapScreenY(mapU(i))
            if (ny < 120f || ny > H + 80f) continue
            val nx = mapX(i)
            val big = i % 10 == 0
            val r = if (big) 60f else 50f
            when {
                i <= save.levelDone -> {
                    gfx.circle(c, nx, ny, r, Gfx.ACCENT)
                    gfx.text(c, levelNum[i], nx, ny + 15f, 42f, Gfx.BG)
                }
                i == cur -> {
                    gfx.ring(c, nx, ny, r + 14f + sin(time * 4f) * 6f, Gfx.GOLD, 5f, 180)
                    gfx.circle(c, nx, ny, r, Gfx.GOLD)
                    gfx.text(c, levelNum[i], nx, ny + 15f, 42f, Gfx.BG)
                }
                else -> {
                    gfx.circle(c, nx, ny, r, Gfx.PANEL2)
                    gfx.ring(c, nx, ny, r, Color.WHITE, 2f, 40)
                    gfx.text(c, levelNum[i], nx, ny + 15f, 40f, Gfx.DIM)
                }
            }
        }
        // Kopfzeile
        gfx.roundRect(c, -20f, -20f, W + 20f, 205f, 0f, Gfx.BG, 235)
        drawBtn(c, bBack)
        gfx.text(c, "REISE", W / 2f, 135f, 62f, Color.WHITE)
        drawStarsCounter(c, save.stars)
    }

    private fun dottedLine(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, col: Int, alpha: Int) {
        val n = 7
        for (k in 1 until n) {
            val f = k / n.toFloat()
            gfx.circle(c, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, 6f, col, alpha)
        }
    }

    // ---------------------------------------------------------------- Shop

    private fun drawShop(c: Canvas) {
        dim(c, 235)
        bBuyRevive.visible = shopTab == 2
        if (shopTab == 2) {
            drawExtras(c)
        } else {
            c.save()
            c.clipRect(0f, gridTop - 20f, W, H)
            val n = shopCount()
            for (i in 0 until n) {
                val col = i % 3; val row = i / 3
                val x = 40f + col * (cardW + 24f)
                val y = gridTop + row * (cardH + 24f) - shopScroll
                if (y > H || y + cardH < gridTop - 20f) continue
                drawCard(c, i, x, y)
            }
            c.restore()
        }
        gfx.roundRect(c, -20f, -20f, W + 20f, gridTop - 20f, 0f, Gfx.BG, 250)
        drawBtn(c, bBack)
        gfx.text(c, "SHOP", W / 2f, 135f, 62f, Color.WHITE)
        drawStarsCounter(c, save.stars)
        drawBtn(c, bTab0); drawBtn(c, bTab1); drawBtn(c, bTab2)
    }

    private fun drawCard(c: Canvas, i: Int, x: Float, y: Float) {
        val skinsTab = shopTab == 0
        val owned = if (skinsTab) save.ownsSkin(i) else save.ownsTrail(i)
        val selected = if (skinsTab) save.skin == i else save.trail == i
        gfx.roundRect(c, x, y, x + cardW, y + cardH, 34f, Gfx.PANEL)
        if (selected) gfx.strokeRoundRect(c, x, y, x + cardW, y + cardH, 34f, Gfx.GREEN, 5f)
        val cx = x + cardW / 2f
        val py = y + 125f
        if (skinsTab) {
            gfx.drawSkin(c, i, cx, py, 2.2f, time, save.lowGfx)
            gfx.text(c, Skins.skins[i].name, cx, y + 252f, 34f, Color.WHITE)
        } else {
            for (k in 0 until 24) {
                prevX[k] = cx - 105f + k * (210f / 23f)
                prevY[k] = py + sin(k * 0.35f + time * 4f) * 28f
            }
            gfx.drawTrail(c, i, prevX, prevY, 0, 24, 24, gfx.skinColor(save.skin, time), time)
            gfx.drawSkin(c, save.skin, prevX[23], prevY[23], 1f, time, true)
            gfx.text(c, Skins.trails[i].name, cx, y + 252f, 34f, Color.WHITE)
        }
        when {
            selected -> gfx.text(c, "AKTIV", cx, y + 310f, 32f, Gfx.GREEN)
            owned -> gfx.text(c, "Auswählen", cx, y + 310f, 32f, Gfx.DIM, false)
            else -> {
                val price = if (skinsTab) skinPrice[i] else trailPrice[i]
                val afford = save.stars >= (if (skinsTab) Skins.skins[i].price else Skins.trails[i].price)
                val tw = gfx.textWidth(price, 36f)
                gfx.starShape(c, cx - tw / 2f - 20f, y + 298f, 16f, 0f, Gfx.GOLD)
                gfx.text(c, price, cx + 12f, y + 311f, 36f, if (afford) Gfx.GOLD else Gfx.DIM)
            }
        }
    }

    private fun drawExtras(c: Canvas) {
        val x = 40f; val y = gridTop; val w = 920f; val h = 660f
        gfx.roundRect(c, x, y, x + w, y + h, 40f, Gfx.PANEL)
        val cx = W / 2f
        // Symbol: grünes Herz-Plus
        gfx.circle(c, cx, y + 140f, 80f, Gfx.GREEN, 60)
        gfx.circle(c, cx, y + 140f, 62f, Gfx.GREEN)
        gfx.roundRect(c, cx - 12f, y + 100f, cx + 12f, y + 180f, 6f, Color.WHITE)
        gfx.roundRect(c, cx - 40f, y + 128f, cx + 40f, y + 152f, 6f, Color.WHITE)
        gfx.text(c, "Wiederbelebung", cx, y + 290f, 52f, Color.WHITE)
        gfx.text(c, "Nach einem Absturz auf dem", cx, y + 355f, 36f, Gfx.DIM, false)
        gfx.text(c, "letzten Planeten weiterspielen.", cx, y + 400f, 36f, Gfx.DIM, false)
        // Anzeige x/3
        for (k in 0 until Skins.REVIVE_MAX) {
            val dx = cx + (k - 1) * 70f
            if (k < save.revives) gfx.circle(c, dx, y + 455f, 22f, Gfx.GREEN)
            else gfx.ring(c, dx, y + 455f, 22f, Gfx.DIM, 4f)
        }
        bBuyRevive.set(cx - 260f, y + 500f, 520f, 125f)
        bBuyRevive.label = if (save.revives >= Skins.REVIVE_MAX) "VOLL (3/3)" else "KAUFEN  ·  ${Skins.REVIVE_PRICE}"
        drawBtn(c, bBuyRevive)
        if (save.revives < Skins.REVIVE_MAX) {
            val tw = gfx.textWidth(bBuyRevive.label, bBuyRevive.h * 0.34f)
            gfx.starShape(c, cx + tw / 2f + 30f, bBuyRevive.y + bBuyRevive.h / 2f, 20f, 0f, Gfx.BG)
        }
    }

    // ---------------------------------------------------------------- Einstellungen

    private fun drawSettings(c: Canvas) {
        header(c, "EINSTELLUNGEN")
        val labels = SETTING_LABELS
        for (i in 0 until 5) {
            val b = bRow[i]
            gfx.roundRect(c, b.x, b.y, b.x + b.w, b.y + b.h, 34f, if (b === pressed) Gfx.PANEL2 else Gfx.PANEL)
            gfx.text(c, labels[i], b.x + 40f, b.y + b.h / 2f + 15f, 42f, Color.WHITE, false, Paint.Align.LEFT)
            if (i == 4) {
                val v = if (save.lowGfx) "Sparsam" else "Hoch"
                gfx.roundRect(c, b.x + b.w - 260f, b.y + 25f, b.x + b.w - 30f, b.y + b.h - 25f, 33f, Gfx.PANEL2)
                gfx.text(c, v, b.x + b.w - 145f, b.y + b.h / 2f + 13f, 36f, Gfx.ACCENT)
            } else {
                val on = when (i) { 0 -> save.sound; 1 -> save.music; 2 -> save.vibration; else -> save.showFps }
                toggle(c, b.x + b.w - 180f, b.y + b.h / 2f, on)
            }
        }
        drawBtn(c, bReset)
        gfx.text(c, "Grafik „Sparsam“ = noch flüssiger auf sehr alten Handys", W / 2f, bReset.y + bReset.h + 80f, 30f, Gfx.DIM, false)
        gfx.text(c, "Orbit v$VERSION_NAME · Engelcode", W / 2f, H - 40f, 28f, Gfx.DIM, false, alpha = 160)
    }

    private val SETTING_LABELS = arrayOf("Soundeffekte", "Musik", "Vibration", "FPS-Anzeige", "Grafik")

    private fun toggle(c: Canvas, x: Float, cy: Float, on: Boolean) {
        gfx.roundRect(c, x, cy - 36f, x + 140f, cy + 36f, 36f, if (on) Gfx.ACCENT else Gfx.PANEL2)
        gfx.circle(c, if (on) x + 104f else x + 36f, cy, 28f, Color.WHITE)
    }

    // ---------------------------------------------------------------- Statistik

    private fun drawStats(c: Canvas) {
        header(c, "STATISTIK")
        for (i in statLabels.indices) {
            val y = 260f + i * 125f
            gfx.roundRect(c, 40f, y, 960f, y + 105f, 30f, Gfx.PANEL)
            gfx.text(c, statLabels[i], 80f, y + 66f, 40f, Gfx.DIM, false, Paint.Align.LEFT)
            gfx.text(c, statValues[i], 920f, y + 68f, 46f, Color.WHITE, align = Paint.Align.RIGHT)
        }
    }

    // ---------------------------------------------------------------- Dialog & Toast

    private fun drawDialog(c: Canvas) {
        dim(c, 190)
        gfx.roundRect(c, 70f, dlgTop, 930f, dlgTop + dlgH, 44f, Gfx.PANEL)
        gfx.strokeRoundRect(c, 70f, dlgTop, 930f, dlgTop + dlgH, 44f, Gfx.ACCENT, 3f, 120)
        gfx.text(c, dlgTitle, W / 2f, dlgTop + 105f, 54f, Color.WHITE)
        val news = dialog == D_NEWS
        for (i in dlgLines.indices) {
            val ly = dlgTop + 180f + i * 62f
            if (news) gfx.text(c, dlgLines[i], 120f, ly, 36f, Color.WHITE, false, Paint.Align.LEFT)
            else gfx.text(c, dlgLines[i], W / 2f, ly, 44f, if (i == dlgLines.size - 1 && dialog == D_DAILY) Gfx.GOLD else Color.WHITE, dialog == D_DAILY)
        }
        drawBtn(c, bDlgA)
        if (dlgTwo) drawBtn(c, bDlgB)
    }

    private fun drawToast(c: Canvas) {
        val a = (min(1f, toastT / 0.4f) * 255f).toInt()
        val tw = gfx.textWidth(toast, 40f, false) + 80f
        val y = H * 0.86f
        gfx.roundRect(c, W / 2f - tw / 2f, y - 60f, W / 2f + tw / 2f, y + 30f, 45f, Gfx.PANEL2, a)
        gfx.text(c, toast, W / 2f, y, 40f, Color.WHITE, false, alpha = a)
    }
}
