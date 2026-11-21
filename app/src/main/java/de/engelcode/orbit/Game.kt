package de.engelcode.orbit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * ORBIT – Spiellogik und Zeichnen.
 *
 * Welt-Koordinaten: Bildschirmbreite = 1000 Einheiten, egal welches Handy.
 * Alles wird einmal vorbereitet (Glow-Bilder, Farben, Pfade) – im Spiel selbst
 * wird pro Bild nichts Neues erzeugt. Das hält alte Geräte bei 60 FPS.
 */
class Game(context: Context) {

    companion object {
        const val WORLD_W = 1000f
        const val FLY_SPEED = 950f        // Fluggeschwindigkeit
        const val ORBIT_SPEED = 520f      // Geschwindigkeit auf der Umlaufbahn
        const val GRAVITY = 2.6e7f        // Stärke der Anziehung beim Fliegen
        const val CAPTURE = 110f          // Fangzone um jeden Planeten
        const val ORBIT_GAP = 50f         // Abstand der Umlaufbahn zur Oberfläche
        const val MAX_PLANETS = 7
        const val MAX_STARS = 7
        const val MAX_PARTICLES = 120
        const val TRAIL = 24
        const val BG = 0xFF0B0E1A.toInt()

        val PALETTE = intArrayOf(
            0xFF2EC4B6.toInt(), // Türkis
            0xFFFF6B6B.toInt(), // Koralle
            0xFF9B5DE5.toInt(), // Violett
            0xFFF4A261.toInt(), // Bernstein
            0xFF4CC9F0.toInt(), // Himmelblau
            0xFFF15BB5.toInt()  // Pink
        )
        val STAR_COLOR = 0xFFFFD166.toInt()
        val PLAYER_COLOR = 0xFFE8FBFF.toInt()
    }

    private enum class State { MENU, PLAYING, DEAD }

    private class Planet {
        var x = 0f; var y = 0f; var r = 0f
        var baseX = 0f; var amp = 0f; var speed = 0f; var phase = 0f
        var colorIdx = 0
        var active = false
    }

    private class Star {
        var x = 0f; var y = 0f; var spin = 0f
        var active = false
    }

    private class Particle {
        var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
        var life = 0f; var maxLife = 1f; var size = 0f; var color = 0
    }

    // ---------- Speicher ----------
    private val prefs = context.getSharedPreferences("orbit", Context.MODE_PRIVATE)
    private var best = prefs.getInt("best", 0)
    private var totalStars = prefs.getInt("stars", 0)

    var onHaptic: ((Boolean) -> Unit)? = null

    // ---------- Bildschirm ----------
    private var scale = 1f
    private var worldH = 1800f
    private var ready = false

    // ---------- Zustand ----------
    private var state = State.MENU
    private var time = 0f
    private var stateTime = 0f
    @Volatile private var tapPending = false
    @Volatile private var paused = false
    private var newRecord = false

    private var camY = 0f
    private var shake = 0f

    private val planets = Array(MAX_PLANETS) { Planet() }
    private val stars = Array(MAX_STARS) { Star() }
    private val particles = Array(MAX_PARTICLES) { Particle() }
    private var genCount = 0
    private var lastGenX = 0f
    private var lastGenY = 0f

    // Spieler
    private var plX = 0f; private var plY = 0f
    private var vx = 0f; private var vy = 0f
    private var current: Planet? = null
    private var lastPlanet: Planet? = null
    private var angle = 0f
    private var orbitR = 0f
    private var orbitTarget = 0f
    private var dir = 1f
    private var flyTime = 0f
    private val trailX = FloatArray(TRAIL)
    private val trailY = FloatArray(TRAIL)
    private var trailHead = 0
    private var trailCount = 0

    private var score = 0
    private var runStars = 0

    // "PERFEKT!"-Einblendung
    private var popT = 0f
    private var popX = 0f
    private var popY = 0f

    // FPS-Anzeige (zum Testen)
    private var fpsAcc = 0f
    private var fpsFrames = 0
    private var fpsShown = 60

    // ---------- Grafik (einmal vorbereitet) ----------
    private val planetGlow = Array(PALETTE.size) { makeGlow(PALETTE[it], 0x80) }
    private val starGlow = makeGlow(STAR_COLOR, 0x70)
    private val playerGlow = makeGlow(0xFF7FE7FF.toInt(), 0xA0)

    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = PLAYER_COLOR
    }
    private val starfieldPaint = Array(3) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.5f + it * 1.5f
            alpha = 60 + it * 60
        }
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val overlayPaint = Paint().apply { color = Color.argb(170, 5, 7, 15) }
    private val rect = RectF()
    private val starPath = Path().apply {
        // 4-zackiger Stern, Radius 1 (wird beim Zeichnen skaliert)
        moveTo(0f, -1f)
        quadTo(0.18f, -0.18f, 1f, 0f)
        quadTo(0.18f, 0.18f, 0f, 1f)
        quadTo(-0.18f, 0.18f, -1f, 0f)
        quadTo(-0.18f, -0.18f, 0f, -1f)
        close()
    }

    private val layerPts = Array(3) { FloatArray(0) }
    private val layerDepth = floatArrayOf(0.04f, 0.12f, 0.25f)

    // Text-Cache, damit pro Bild keine neuen Strings entstehen
    private var scoreStr = "0"; private var scoreStrVal = -1
    private var starsStr = "0"; private var starsStrVal = -1
    private var fpsStr = "60 FPS"; private var fpsStrVal = -1
    private var bestStr = ""

    // =====================================================================
    //  Steuerung von außen
    // =====================================================================

    fun tap() { tapPending = true }

    fun onPause() {
        if (state == State.PLAYING) paused = true
    }

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        scale = w / WORLD_W
        worldH = h / scale
        buildStarfield()
        if (!ready) {
            resetWorld()
            ready = true
        }
    }

    // =====================================================================
    //  Welt aufbauen
    // =====================================================================

    private fun buildStarfield() {
        val rnd = Random(7)
        for (l in 0 until 3) {
            val n = 40 + l * 10
            val arr = FloatArray(n * 2)
            for (i in 0 until n) {
                arr[i * 2] = rnd.nextFloat() * WORLD_W
                arr[i * 2 + 1] = rnd.nextFloat() * worldH
            }
            layerPts[l] = arr
        }
    }

    private fun resetWorld() {
        for (p in planets) p.active = false
        for (s in stars) s.active = false
        for (p in particles) p.life = 0f
        camY = 0f
        score = 0
        runStars = 0
        genCount = 0
        newRecord = false
        shake = 0f
        popT = 0f
        bestStr = "Rekord: $best"

        val first = planets[0]
        setupPlanet(first, WORLD_W / 2f, worldH * 0.70f, 95f, 0f)
        lastGenX = first.x
        lastGenY = first.y
        genCount = 1
        fillPlanets()

        current = first
        lastPlanet = null
        angle = -PI.toFloat() / 2f
        orbitTarget = first.r + ORBIT_GAP
        orbitR = orbitTarget
        dir = 1f
        plX = first.x + cos(angle) * orbitR
        plY = first.y + sin(angle) * orbitR
        trailCount = 0
        camY = first.y - worldH * 0.70f
    }

    private fun setupPlanet(p: Planet, x: Float, y: Float, r: Float, amp: Float) {
        p.baseX = x; p.x = x; p.y = y; p.r = r
        p.amp = amp
        p.speed = 0.6f + Random.nextFloat() * 0.8f
        p.phase = Random.nextFloat() * 6.28f
        p.colorIdx = Random.nextInt(PALETTE.size)
        p.active = true
    }

    private fun fillPlanets() {
        var guard = 0
        while (guard++ < MAX_PLANETS && spawnNext()) { }
    }

    /** Setzt den nächsten Planeten oberhalb des letzten. */
    private fun spawnNext(): Boolean {
        var p: Planet? = null
        for (q in planets) if (!q.active) { p = q; break }
        if (p == null) return false

        val d = min(1f, genCount / 40f)              // Schwierigkeit 0..1
        val r = (80f - 30f * d) + Random.nextFloat() * (35f - 5f * d)
        val gap = 400f + 120f * d + Random.nextFloat() * 160f
        var amp = 0f
        if (genCount >= 8 && Random.nextFloat() < 0.25f + 0.45f * d) {
            amp = 60f + Random.nextFloat() * 120f * (0.5f + d)
        }
        val maxAmp = WORLD_W / 2f - r - 80f
        if (amp > maxAmp) amp = max(0f, maxAmp)
        val margin = r + 70f + amp
        val x = margin + Random.nextFloat() * (WORLD_W - 2f * margin)
        val y = lastGenY - gap

        setupPlanet(p, x, y, r, amp)

        // Stern zwischen den Planeten
        if (Random.nextFloat() < 0.65f) {
            for (s in stars) if (!s.active) {
                s.x = ((lastGenX + x) / 2f + (Random.nextFloat() - 0.5f) * 240f).coerceIn(60f, WORLD_W - 60f)
                s.y = (lastGenY + y) / 2f
                s.spin = Random.nextFloat() * 6f
                s.active = true
                break
            }
        }
        lastGenX = x
        lastGenY = y
        genCount++
        return true
    }

    // =====================================================================
    //  Update
    // =====================================================================

    fun update(dt: Float) {
        if (!ready) return

        // FPS messen
        fpsAcc += dt; fpsFrames++
        if (fpsAcc >= 0.5f) {
            fpsShown = (fpsFrames / fpsAcc + 0.5f).toInt()
            fpsAcc = 0f; fpsFrames = 0
        }

        val tap = tapPending
        tapPending = false

        if (paused) {
            if (tap) paused = false
            return
        }

        time += dt
        stateTime += dt

        when (state) {
            State.MENU -> if (tap) {
                state = State.PLAYING; stateTime = 0f
                launch()
            }
            State.PLAYING -> if (tap && current != null) launch()
            State.DEAD -> if (tap && stateTime > 0.6f) {
                resetWorld()
                state = State.PLAYING; stateTime = 0f
            }
        }

        // Planeten bewegen
        for (p in planets) if (p.active && p.amp > 0f) {
            p.x = p.baseX + sin(time * p.speed + p.phase) * p.amp
        }

        // Physik in kleinen Schritten (genaue Fangzone, auch bei 30 FPS)
        if (state != State.DEAD) {
            val steps = max(1, ceil(dt / (1f / 240f)).toInt())
            val h = dt / steps
            for (i in 0 until steps) {
                if (!stepPlayer(h)) break
            }
            // Spur
            trailX[trailHead] = plX; trailY[trailHead] = plY
            trailHead = (trailHead + 1) % TRAIL
            if (trailCount < TRAIL) trailCount++
        }

        // Sterne drehen + einsammeln
        for (s in stars) if (s.active) {
            s.spin += dt * 2f
            if (state != State.DEAD) {
                val dx = s.x - plX; val dy = s.y - plY
                if (dx * dx + dy * dy < 55f * 55f) {
                    s.active = false
                    runStars++
                    burst(s.x, s.y, STAR_COLOR, 12, 260f)
                    onHaptic?.invoke(false)
                }
            }
            if (s.y > camY + worldH + 200f) s.active = false
        }

        // Kamera
        val anchor = current ?: lastPlanet
        var target = camY
        if (anchor != null) target = anchor.y - worldH * 0.70f
        if (current == null && state == State.PLAYING) target = min(target, plY - worldH * 0.40f)
        camY += (target - camY) * min(1f, dt * 3.5f)

        // Alte Planeten unten recyceln
        for (p in planets) if (p.active && p !== current && p !== lastPlanet && p.y > camY + worldH + 300f) {
            p.active = false
        }
        fillPlanets()

        // Partikel
        for (p in particles) if (p.life > 0f) {
            p.life -= dt
            p.x += p.vx * dt; p.y += p.vy * dt
            p.vx *= 0.96f; p.vy *= 0.96f
        }

        if (shake > 0f) shake = max(0f, shake - dt * 2.5f)
        if (popT > 0f) { popT -= dt; popY -= dt * 60f }
    }

    /** Ein Physik-Schritt. Gibt false zurück, wenn der Spieler gestorben ist. */
    private fun stepPlayer(h: Float): Boolean {
        val c = current
        if (c != null) {
            orbitR += (orbitTarget - orbitR) * min(1f, h * 5f)
            angle += dir * ORBIT_SPEED / orbitR * h
            if (angle > 6.2832f) angle -= 6.2832f
            if (angle < -6.2832f) angle += 6.2832f
            plX = c.x + cos(angle) * orbitR
            plY = c.y + sin(angle) * orbitR
            return true
        }

        if (state != State.PLAYING) return true
        flyTime += h
        var ax = 0f
        var ay = 0f
        for (p in planets) {
            if (!p.active || p === lastPlanet) continue
            val dx = p.x - plX
            val dy = p.y - plY
            val d2 = dx * dx + dy * dy
            val d = sqrt(d2)
            if (d < p.r + CAPTURE) {
                capture(p, d)
                return true
            }
            if (d < 750f) {
                val k = p.r / 100f
                val a = GRAVITY * k * k / d2
                ax += dx / d * a
                ay += dy / d * a
            }
        }
        vx += ax * h
        vy += ay * h
        val sp = sqrt(vx * vx + vy * vy)
        if (sp > 0.001f) { vx = vx / sp * FLY_SPEED; vy = vy / sp * FLY_SPEED }
        plX += vx * h
        plY += vy * h

        if (plX < -60f || plX > WORLD_W + 60f ||
            plY > camY + worldH + 80f || plY < camY - worldH * 0.6f ||
            flyTime > 2.6f
        ) {
            die()
            return false
        }
        return true
    }

    private fun launch() {
        val c = current ?: return
        // Tangente der Kreisbahn
        vx = -sin(angle) * dir * FLY_SPEED
        vy = cos(angle) * dir * FLY_SPEED
        lastPlanet = c
        current = null
        flyTime = 0f
    }

    private fun capture(p: Planet, d: Float) {
        val rx = plX - p.x
        val ry = plY - p.y
        current = p
        angle = atan2(ry, rx)
        orbitR = d
        orbitTarget = p.r + ORBIT_GAP
        val cross = rx * vy - ry * vx
        dir = if (cross >= 0f) 1f else -1f

        val perfect = d < p.r + 72f
        score += if (perfect) 2 else 1
        if (perfect) { popT = 0.9f; popX = plX; popY = plY - 40f }
        burst(plX, plY, PALETTE[p.colorIdx], if (perfect) 22 else 12, if (perfect) 380f else 260f)
        onHaptic?.invoke(false)
    }

    private fun die() {
        state = State.DEAD
        stateTime = 0f
        shake = 1f
        burst(plX, plY, PLAYER_COLOR, 36, 520f)
        onHaptic?.invoke(true)
        if (score > best) {
            best = score
            newRecord = true
        }
        totalStars += runStars
        prefs.edit().putInt("best", best).putInt("stars", totalStars).apply()
        bestStr = "Rekord: $best"
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int, speed: Float) {
        var made = 0
        for (p in particles) {
            if (made >= n) break
            if (p.life > 0f) continue
            val a = Random.nextFloat() * 6.2832f
            val s = speed * (0.3f + Random.nextFloat() * 0.7f)
            p.x = x; p.y = y
            p.vx = cos(a) * s; p.vy = sin(a) * s
            p.maxLife = 0.4f + Random.nextFloat() * 0.5f
            p.life = p.maxLife
            p.size = 4f + Random.nextFloat() * 6f
            p.color = color
            made++
        }
    }

    // =====================================================================
    //  Zeichnen
    // =====================================================================

    fun draw(c: Canvas) {
        c.drawColor(BG)
        if (!ready) return

        c.save()
        c.scale(scale, scale)

        drawStarfield(c)

        var sx = 0f; var sy = 0f
        if (shake > 0f) {
            sx = (Random.nextFloat() - 0.5f) * 28f * shake
            sy = (Random.nextFloat() - 0.5f) * 28f * shake
        }
        c.save()
        c.translate(sx, -camY + sy)

        drawPlanets(c)
        drawStars(c)
        if (state != State.DEAD) {
            drawTrail(c)
            drawAim(c)
            drawPlayer(c)
        }
        drawParticles(c)
        drawPop(c)

        c.restore()
        drawHud(c)
        c.restore()
    }

    private fun drawStarfield(c: Canvas) {
        for (l in 0 until 3) {
            var off = (-camY * layerDepth[l]) % worldH
            if (off < 0f) off += worldH
            c.save()
            c.translate(0f, off)
            c.drawPoints(layerPts[l], starfieldPaint[l])
            c.translate(0f, -worldH)
            c.drawPoints(layerPts[l], starfieldPaint[l])
            c.restore()
        }
    }

    private fun drawPlanets(c: Canvas) {
        val top = camY - 300f
        val bottom = camY + worldH + 300f
        for (p in planets) {
            if (!p.active || p.y < top || p.y > bottom) continue
            val col = PALETTE[p.colorIdx]

            // Glow
            val g = p.r * 2.3f
            rect.set(p.x - g, p.y - g, p.x + g, p.y + g)
            c.drawBitmap(planetGlow[p.colorIdx], null, rect, bmpPaint)

            // Fangzone bzw. Umlaufbahn
            stroke.color = col
            if (p === current) {
                stroke.alpha = 70; stroke.strokeWidth = 3f
                c.drawCircle(p.x, p.y, orbitTarget, stroke)
            } else if (p !== lastPlanet) {
                stroke.alpha = 40; stroke.strokeWidth = 2f
                c.drawCircle(p.x, p.y, p.r + CAPTURE, stroke)
            }

            // Körper
            fill.color = col
            c.drawCircle(p.x, p.y, p.r, fill)
            // Schatten unten rechts
            fill.color = Color.BLACK; fill.alpha = 55
            c.drawCircle(p.x + p.r * 0.18f, p.y + p.r * 0.18f, p.r * 0.82f, fill)
            fill.color = col; fill.alpha = 255
            c.drawCircle(p.x - p.r * 0.06f, p.y - p.r * 0.06f, p.r * 0.80f, fill)
            // Glanzpunkt
            fill.color = Color.WHITE; fill.alpha = 70
            c.drawCircle(p.x - p.r * 0.35f, p.y - p.r * 0.35f, p.r * 0.22f, fill)
            fill.alpha = 255
        }
    }

    private fun drawStars(c: Canvas) {
        for (s in stars) {
            if (!s.active) continue
            if (s.y < camY - 100f || s.y > camY + worldH + 100f) continue
            rect.set(s.x - 60f, s.y - 60f, s.x + 60f, s.y + 60f)
            c.drawBitmap(starGlow, null, rect, bmpPaint)
            val pulse = 22f + sin(time * 5f + s.spin) * 3f
            c.save()
            c.translate(s.x, s.y)
            c.rotate(s.spin * 57.3f)
            c.scale(pulse, pulse)
            fill.color = STAR_COLOR
            c.drawPath(starPath, fill)
            c.restore()
        }
    }

    private fun drawTrail(c: Canvas) {
        if (trailCount < 2) return
        val start = (trailHead - trailCount + TRAIL) % TRAIL
        var px = trailX[start]
        var py = trailY[start]
        for (i in 1 until trailCount) {
            val idx = (start + i) % TRAIL
            val f = i.toFloat() / trailCount
            trailPaint.alpha = (f * 150f).toInt()
            trailPaint.strokeWidth = 3f + f * 13f
            c.drawLine(px, py, trailX[idx], trailY[idx], trailPaint)
            px = trailX[idx]; py = trailY[idx]
        }
    }

    private fun drawAim(c: Canvas) {
        if (current == null) return
        val tx = -sin(angle) * dir
        val ty = cos(angle) * dir
        fill.color = Color.WHITE
        for (i in 1..6) {
            fill.alpha = 150 - i * 22
            val d = 30f + i * 26f
            c.drawCircle(plX + tx * d, plY + ty * d, 5f - i * 0.4f, fill)
        }
        fill.alpha = 255
    }

    private fun drawPlayer(c: Canvas) {
        rect.set(plX - 55f, plY - 55f, plX + 55f, plY + 55f)
        c.drawBitmap(playerGlow, null, rect, bmpPaint)
        fill.color = PLAYER_COLOR
        c.drawCircle(plX, plY, 14f, fill)
    }

    private fun drawParticles(c: Canvas) {
        for (p in particles) {
            if (p.life <= 0f) continue
            val f = p.life / p.maxLife
            fill.color = p.color
            fill.alpha = (f * 255f).toInt()
            c.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * f), fill)
        }
        fill.alpha = 255
    }

    private fun drawPop(c: Canvas) {
        if (popT <= 0f) return
        textPaint.textSize = 54f
        textPaint.color = STAR_COLOR
        textPaint.alpha = (min(1f, popT / 0.4f) * 255f).toInt()
        c.drawText("PERFEKT!", popX, popY, textPaint)
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
    }

    private fun drawHud(c: Canvas) {
        val cx = WORLD_W / 2f

        // FPS (Testanzeige)
        if (fpsShown != fpsStrVal) { fpsStrVal = fpsShown; fpsStr = "$fpsShown FPS" }
        smallText.textAlign = Paint.Align.LEFT
        smallText.textSize = 30f
        smallText.alpha = 110
        c.drawText(fpsStr, 30f, 60f, smallText)
        smallText.textAlign = Paint.Align.CENTER
        smallText.alpha = 255

        // Sterne oben rechts
        val starsNow = totalStars + if (state == State.DEAD) 0 else runStars
        if (starsNow != starsStrVal) { starsStrVal = starsNow; starsStr = starsNow.toString() }
        c.save()
        c.translate(WORLD_W - 150f, 52f)
        c.scale(18f, 18f)
        fill.color = STAR_COLOR
        c.drawPath(starPath, fill)
        c.restore()
        smallText.textAlign = Paint.Align.LEFT
        smallText.textSize = 40f
        c.drawText(starsStr, WORLD_W - 120f, 66f, smallText)
        smallText.textAlign = Paint.Align.CENTER

        when (state) {
            State.MENU -> {
                textPaint.textSize = 170f
                textPaint.alpha = 255
                c.drawText("ORBIT", cx, worldH * 0.28f, textPaint)
                smallText.textSize = 46f
                smallText.alpha = (170 + 85 * sin(time * 3f)).toInt().coerceIn(0, 255)
                c.drawText("Tippe, um abzuspringen", cx, worldH * 0.28f + 100f, smallText)
                smallText.alpha = 180
                smallText.textSize = 40f
                c.drawText(bestStr, cx, worldH * 0.28f + 170f, smallText)
                smallText.alpha = 255
            }
            State.PLAYING -> {
                if (score != scoreStrVal) { scoreStrVal = score; scoreStr = score.toString() }
                textPaint.textSize = 120f
                textPaint.alpha = 230
                c.drawText(scoreStr, cx, 200f, textPaint)
                textPaint.alpha = 255
            }
            State.DEAD -> {
                val f = min(1f, stateTime / 0.35f)
                overlayPaint.alpha = (170 * f).toInt()
                c.drawRect(0f, 0f, WORLD_W, worldH, overlayPaint)
                if (score != scoreStrVal) { scoreStrVal = score; scoreStr = score.toString() }
                val y = worldH * 0.36f
                smallText.textSize = 50f
                smallText.alpha = (255 * f).toInt()
                c.drawText("Verloren im All", cx, y - 150f, smallText)
                textPaint.textSize = 200f
                textPaint.alpha = (255 * f).toInt()
                c.drawText(scoreStr, cx, y + 40f, textPaint)
                smallText.textSize = 44f
                if (newRecord) {
                    smallText.color = STAR_COLOR
                    c.drawText("Neuer Rekord!", cx, y + 130f, smallText)
                    smallText.color = Color.WHITE
                } else {
                    c.drawText(bestStr, cx, y + 130f, smallText)
                }
                if (stateTime > 0.6f) {
                    smallText.alpha = (170 + 85 * sin(time * 3f)).toInt().coerceIn(0, 255)
                    c.drawText("Tippen für Neustart", cx, y + 260f, smallText)
                }
                smallText.alpha = 255
                textPaint.alpha = 255
            }
        }

        if (paused) {
            c.drawRect(0f, 0f, WORLD_W, worldH, overlayPaint.apply { alpha = 170 })
            textPaint.textSize = 90f
            c.drawText("Pause", cx, worldH * 0.45f, textPaint)
            smallText.textSize = 44f
            c.drawText("Tippen zum Weiterspielen", cx, worldH * 0.45f + 90f, smallText)
        }
    }

    // =====================================================================
    //  Hilfen
    // =====================================================================

    private fun makeGlow(color: Int, alpha: Int): Bitmap {
        val size = 128
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val r = Color.red(color); val g = Color.green(color); val bl = Color.blue(color)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(
            size / 2f, size / 2f, size / 2f,
            intArrayOf(Color.argb(alpha, r, g, bl), Color.argb(alpha / 3, r, g, bl), Color.argb(0, r, g, bl)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        cv.drawCircle(size / 2f, size / 2f, size / 2f, p)
        return b
    }
}
